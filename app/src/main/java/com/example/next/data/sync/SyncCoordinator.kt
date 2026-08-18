package com.example.next.data.sync

import com.example.next.data.repository.SettingsRepository
import com.example.next.data.session.Owner
import com.example.next.data.session.SessionManager
import com.example.next.database.dao.CartDao
import com.example.next.database.dao.OrderDao
import com.example.next.database.dao.PendingOpsDao
import com.example.next.database.dao.WishlistDao
import com.example.next.database.entity.CartItemEntity
import com.example.next.database.entity.OrderEntity
import com.example.next.database.entity.OrderItemEntity
import com.example.next.database.entity.WishlistItemEntity
import com.example.next.models.ThemeMode
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Cloud -> local sync for authenticated owners (guests never start it).
 *
 *  - wishlist + cart: REALTIME snapshot listeners (small, interactive) —
 *    remote upserts/deletes are mirrored into Room; rows with a pending local
 *    outbox entry are skipped so an offline mutation is never clobbered by a
 *    stale snapshot before it flushes.
 *  - orders + settings: pull-on-resume (one get() per owner activation),
 *    keeping read cost low for append-only data.
 *  - a periodic flush loop replays the outbox while the owner is active.
 *
 * All of this is cancelled automatically when the owner changes (flatMapLatest),
 * so device A's listeners can never deliver into user B's Room cache.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncCoordinator(
    private val firestore: FirebaseFirestore?,
    private val sessionManager: SessionManager,
    private val wishlistDao: WishlistDao,
    private val cartDao: CartDao,
    private val orderDao: OrderDao,
    private val pendingOpsDao: PendingOpsDao,
    private val settingsRepository: SettingsRepository,
    private val syncEngine: SyncEngine,
    private val scope: CoroutineScope
) {

    fun start() {
        scope.launch {
            sessionManager.observeActiveOwner()
                .flatMapLatest { owner ->
                    val uid = Owner.uidOf(owner)
                    if (uid == null) flowOf(Unit) // guests: no cloud work at all
                    else cloudSyncFor(uid)
                }
                .collect {}
        }
    }

    private fun cloudSyncFor(uid: String): Flow<Unit> = callbackFlow {
        val job = launch {
            pullOrdersAndSettings(uid)
            syncEngine.flushPendingOps(Owner.user(uid))
            startWishlistListener(uid, this@callbackFlow)
            startCartListener(uid, this@callbackFlow)
            flushLoop(uid)
        }
        awaitClose { job.cancel() }
    }

    // ---------- realtime listeners ----------

    private fun startWishlistListener(uid: String, scope: CoroutineScope) {
        val fs = firestore ?: return
        fs.collection("users").document(uid).collection(COLLECTION_WISHLIST)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch { applyWishlistSnapshot(uid, snapshot) }
            }
    }

    private fun startCartListener(uid: String, scope: CoroutineScope) {
        val fs = firestore ?: return
        fs.collection("users").document(uid).collection(COLLECTION_CART)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch { applyCartSnapshot(uid, snapshot) }
            }
    }

    private suspend fun applyWishlistSnapshot(uid: String, snapshot: QuerySnapshot) {
        val owner = Owner.user(uid)
        snapshot.documentChanges.forEach { change ->
            val productId = change.document.id.toIntOrNull() ?: return@forEach
            // Skip rows with an unresolved local mutation (outbox wins until flushed).
            if (pendingOpsDao.findByOwnerTableRow(owner, TABLE_WISHLIST, change.document.id) != null) return@forEach
            val data = change.document.data
            when (change.type) {
                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                    wishlistDao.deleteByProductId(owner, productId)
                }
                else -> {
                    val name = data[FIELD_PRODUCT_NAME]?.toString() ?: return@forEach
                    val price = (data[FIELD_PRICE] as? Number)?.toDouble() ?: return@forEach
                    val imageUrl = data[FIELD_IMAGE_URL]?.toString().orEmpty()
                    val updated = wishlistDao.updateByProductId(owner, productId, name, price, imageUrl)
                    if (updated == 0) {
                        wishlistDao.insert(
                            WishlistItemEntity(
                                productId = productId,
                                productName = name,
                                price = price,
                                imageUrl = imageUrl,
                                owner = owner
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun applyCartSnapshot(uid: String, snapshot: QuerySnapshot) {
        val owner = Owner.user(uid)
        snapshot.documentChanges.forEach { change ->
            val productId = change.document.id.toIntOrNull() ?: return@forEach
            if (pendingOpsDao.findByOwnerTableRow(owner, TABLE_CART, change.document.id) != null) return@forEach
            val data = change.document.data
            when (change.type) {
                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                    cartDao.deleteByProductId(owner, productId)
                }
                else -> {
                    val name = data[FIELD_PRODUCT_NAME]?.toString() ?: return@forEach
                    val price = (data[FIELD_PRICE] as? Number)?.toDouble() ?: return@forEach
                    val imageUrl = data[FIELD_IMAGE_URL]?.toString().orEmpty()
                    val quantity = (data[FIELD_QUANTITY] as? Number)?.toInt() ?: return@forEach
                    val updated = cartDao.updateByProductId(owner, productId, name, price, imageUrl, quantity)
                    if (updated == 0) {
                        cartDao.insert(
                            CartItemEntity(
                                productId = productId,
                                productName = name,
                                price = price,
                                imageUrl = imageUrl,
                                quantity = quantity,
                                owner = owner
                            )
                        )
                    }
                }
            }
        }
    }

    // ---------- pull-on-resume ----------

    /** One-shot fetch of orders (+ items) and settings when an owner activates. */
    private suspend fun pullOrdersAndSettings(uid: String) {
        val fs = firestore ?: return
        val owner = Owner.user(uid)
        val userRef = fs.collection("users").document(uid)
        try {
            val ordersSnapshot = userRef.collection(COLLECTION_ORDERS).get().await()
            ordersSnapshot.documents.forEach { orderDoc ->
                val cloudId = orderDoc.id
                val local = orderDao.getOrderByCloudId(owner, cloudId)
                if (local != null) {
                    // Append-only: only the status may differ remotely.
                    val remoteStatus = orderDoc.getString(FIELD_STATUS) ?: return@forEach
                    if (remoteStatus != local.status) {
                        orderDao.updateStatusByCloudId(owner, cloudId, remoteStatus)
                    }
                } else {
                    val items = runCatching {
                        orderDoc.reference.collection(COLLECTION_ORDER_ITEMS).get().await()
                    }.getOrDefault(null)
                    val order = OrderEntity(
                        orderDate = orderDoc.getString(FIELD_ORDER_DATE).orEmpty(),
                        status = orderDoc.getString(FIELD_STATUS).orEmpty(),
                        fullName = orderDoc.getString(FIELD_FULL_NAME).orEmpty(),
                        phone = orderDoc.getString(FIELD_PHONE).orEmpty(),
                        address = orderDoc.getString(FIELD_ADDRESS).orEmpty(),
                        city = orderDoc.getString(FIELD_CITY).orEmpty(),
                        shippingMethod = orderDoc.getString(FIELD_SHIPPING_METHOD).orEmpty(),
                        shippingPrice = (orderDoc.get(FIELD_SHIPPING_PRICE) as? Number)?.toDouble() ?: 0.0,
                        subtotal = (orderDoc.get(FIELD_SUBTOTAL) as? Number)?.toDouble() ?: 0.0,
                        total = (orderDoc.get(FIELD_TOTAL) as? Number)?.toDouble() ?: 0.0,
                        owner = owner,
                        cloudId = cloudId
                    )
                    val localId = orderDao.insertOrder(order)
                    items?.forEach { itemDoc ->
                        orderDao.insertItems(
                            listOf(
                                OrderItemEntity(
                                    orderId = localId,
                                    productId = itemDoc.id.toIntOrNull() ?: 0,
                                    productName = itemDoc.getString(FIELD_PRODUCT_NAME).orEmpty(),
                                    price = (itemDoc.get(FIELD_PRICE) as? Number)?.toDouble() ?: 0.0,
                                    imageUrl = itemDoc.getString(FIELD_IMAGE_URL).orEmpty(),
                                    quantity = (itemDoc.get(FIELD_QUANTITY) as? Number)?.toInt() ?: 1
                                )
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Offline or rules-denied: local cache stays authoritative.
        }

        try {
            val settingsDoc = userRef.collection(COLLECTION_SETTINGS).document(DOC_SETTINGS).get().await()
            if (settingsDoc.exists()) {
                val modeName = settingsDoc.getString(FIELD_THEME_MODE)
                val mode = modeName?.let { name ->
                    runCatching { ThemeMode.valueOf(name) }.getOrNull()
                }
                if (mode != null) {
                    // Cloud wins at resume; written directly so it does not echo
                    // back to Firestore through the push path.
                    settingsRepository.applyRemoteThemeMode(mode)
                }
            }
        } catch (_: Exception) {
            // Offline: keep the local theme.
        }
    }

    // ---------- outbox flush loop ----------

    private suspend fun flushLoop(uid: String) {
        while (true) {
            delay(FLUSH_INTERVAL_MS)
            runCatching { syncEngine.flushPendingOps(Owner.user(uid)) }
        }
    }

    companion object {
        const val COLLECTION_WISHLIST = SyncEngine.COLLECTION_WISHLIST
        const val COLLECTION_CART = SyncEngine.COLLECTION_CART
        const val COLLECTION_ORDERS = SyncEngine.COLLECTION_ORDERS
        const val COLLECTION_ORDER_ITEMS = SyncEngine.COLLECTION_ORDER_ITEMS
        const val COLLECTION_SETTINGS = SyncEngine.COLLECTION_SETTINGS
        const val DOC_SETTINGS = SyncEngine.DOC_SETTINGS
        const val TABLE_WISHLIST = SyncEngine.TABLE_WISHLIST
        const val TABLE_CART = SyncEngine.TABLE_CART
        const val FIELD_PRODUCT_NAME = SyncEngine.FIELD_PRODUCT_NAME
        const val FIELD_PRICE = SyncEngine.FIELD_PRICE
        const val FIELD_IMAGE_URL = SyncEngine.FIELD_IMAGE_URL
        const val FIELD_QUANTITY = SyncEngine.FIELD_QUANTITY
        const val FIELD_STATUS = SyncEngine.FIELD_STATUS
        const val FIELD_ORDER_DATE = SyncEngine.FIELD_ORDER_DATE
        const val FIELD_FULL_NAME = SyncEngine.FIELD_FULL_NAME
        const val FIELD_PHONE = SyncEngine.FIELD_PHONE
        const val FIELD_ADDRESS = SyncEngine.FIELD_ADDRESS
        const val FIELD_CITY = SyncEngine.FIELD_CITY
        const val FIELD_SHIPPING_METHOD = SyncEngine.FIELD_SHIPPING_METHOD
        const val FIELD_SHIPPING_PRICE = SyncEngine.FIELD_SHIPPING_PRICE
        const val FIELD_SUBTOTAL = SyncEngine.FIELD_SUBTOTAL
        const val FIELD_TOTAL = SyncEngine.FIELD_TOTAL
        const val FIELD_THEME_MODE = SyncEngine.FIELD_THEME_MODE

        private const val FLUSH_INTERVAL_MS = 30_000L
    }
}