package com.example.next.data.sync

import com.example.next.data.session.Owner
import com.example.next.data.session.SessionManager
import com.example.next.database.dao.CartDao
import com.example.next.database.dao.OrderDao
import com.example.next.database.dao.WishlistDao
import com.example.next.database.entity.CartItemEntity
import com.example.next.database.entity.OrderEntity
import com.example.next.database.entity.WishlistItemEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * Idempotent guest -> account merge, run once per install on the first
 * successful Google sign-in (marker in DataStore).
 *
 *  wishlist: set-upsert per productId (union — the account's data is never
 *            deleted, retries rewrite the same documents)
 *  cart:     quantity = min(99, accountQuantity + guestQuantity)
 *  orders:   append-only, keyed by the DETERMINISTIC guest cloud id — a retry
 *            sees the document already exists and skips, so duplicates are
 *            impossible
 *  settings: NOT merged — cloud wins (pull-on-resume applies the account's)
 *
 * Failure semantics: local guest rows are touched ONLY after every upload
 * succeeded (re-own + marker are the last steps), so a failed merge loses
 * nothing and is retried at the next login.
 *
 * Known residual window (honest limitation): a process crash between the
 * final batch commit and the re-own step would re-sum guest cart quantities
 * on retry — wishlist/orders stay duplicate-free (idempotent keys), and the
 * marker closes the window as soon as the re-own lands.
 */
class GuestMerger(
    private val firestore: FirebaseFirestore?,
    private val sessionManager: SessionManager,
    private val wishlistDao: WishlistDao,
    private val cartDao: CartDao,
    private val orderDao: OrderDao,
    private val syncEngine: SyncEngine
) {

    suspend fun mergeIfNeeded(uid: String) {
        val fs = firestore ?: return
        if (sessionManager.isGuestMerged()) return
        val guestOwner = Owner.guest(sessionManager.guestId())

        val guestWishlist = wishlistDao.getWishlistItems(guestOwner)
        val guestCart = cartDao.getCartItems(guestOwner)
        val guestOrders = orderDao.getOrdersForOwner(guestOwner)

        if (guestWishlist.isEmpty() && guestCart.isEmpty() && guestOrders.isEmpty()) {
            sessionManager.markGuestMerged(uid) // nothing to merge
            return
        }

        uploadWishlist(fs, uid, guestWishlist)
        uploadCart(fs, uid, guestCart)
        uploadOrders(fs, uid, guestOrders)

        // ---- everything uploaded: only now touch local data ----
        reownLocal(guestOwner, Owner.user(uid), guestWishlist, guestCart)
        sessionManager.markGuestMerged(uid)
        runCatching {
            fs.collection("users").document(uid)
                .update("guestMergedAt", System.currentTimeMillis())
        }
    }

    private suspend fun uploadWishlist(fs: FirebaseFirestore, uid: String, items: List<WishlistItemEntity>) {
        val collection = fs.collection("users").document(uid).collection("wishlist")
        items.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = fs.batch()
            chunk.forEach { item ->
                batch.set(
                    collection.document(item.productId.toString()),
                    mapOf(
                        "productId" to item.productId,
                        "productName" to item.productName,
                        "price" to item.price,
                        "imageUrl" to item.imageUrl,
                        "addedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
        }
    }

    private suspend fun uploadCart(fs: FirebaseFirestore, uid: String, items: List<CartItemEntity>) {
        val collection = fs.collection("users").document(uid).collection("cart")
        items.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = fs.batch()
            chunk.forEach { item ->
                val doc = collection.document(item.productId.toString())
                val account = doc.get().await()
                val accountQuantity = (account.get("quantity") as? Number)?.toInt() ?: 0
                val mergedQuantity = MergePolicy.mergedCartQuantity(accountQuantity, item.quantity)
                if (mergedQuantity == accountQuantity) return@forEach // already at the cap
                batch.set(
                    doc,
                    mapOf(
                        "productId" to item.productId,
                        "productName" to item.productName,
                        "price" to item.price,
                        "imageUrl" to item.imageUrl,
                        "quantity" to mergedQuantity,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
        }
    }

    private suspend fun uploadOrders(fs: FirebaseFirestore, uid: String, orders: List<OrderEntity>) {
        val collection = fs.collection("users").document(uid).collection("orders")
        orders.forEach { order ->
            if (order.cloudId.isBlank()) return@forEach
            // Rules allow creation only with status PENDING — a cancelled
            // guest order is never created on the cloud (local record only).
            if (order.status != "PENDING") return@forEach
            val doc = collection.document(order.cloudId)
            if (doc.get().await().exists()) return@forEach // retry-safe: already merged
            val batch = fs.batch()
            batch.set(doc, syncEngine.orderMap(order))
            orderDao.getItemsForOrder(order.id).forEach { item ->
                batch.set(
                    doc.collection("items").document(item.productId.toString()),
                    syncEngine.orderItemMap(item)
                )
            }
            batch.commit().await()
        }
    }

    /** Folds guest rows into the account owner's namespace, duplicate-free. */
    private suspend fun reownLocal(
        guestOwner: String,
        accountOwner: String,
        guestWishlist: List<WishlistItemEntity>,
        guestCart: List<CartItemEntity>
    ) {
        // Wishlist: union — if the account already has the product locally,
        // the guest row is dropped instead of duplicated.
        guestWishlist.forEach { item ->
            if (MergePolicy.shouldReownGuestRow(wishlistDao.observeIsInWishlist(accountOwner, item.productId).first())) {
                wishlistDao.reownById(item.id, accountOwner)
            } else {
                wishlistDao.deleteByProductId(guestOwner, item.productId)
            }
        }
        // Cart: same rule — the account's local row wins; the cloud quantity
        // (sum) converges via the realtime listener.
        guestCart.forEach { item ->
            if (MergePolicy.shouldReownGuestRow(cartDao.getCartItemByProductId(accountOwner, item.productId) != null)) {
                cartDao.reownById(item.id, accountOwner)
            } else {
                cartDao.deleteByProductId(guestOwner, item.productId)
            }
        }
        // Orders: cloud ids are namespace-disjoint (g_ vs u_), no collisions.
        orderDao.reown(guestOwner, accountOwner)
    }

    companion object {
        private const val BATCH_LIMIT = 400
    }
}