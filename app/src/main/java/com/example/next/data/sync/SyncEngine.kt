package com.example.next.data.sync

import com.example.next.data.session.Owner
import com.example.next.database.dao.PendingOpsDao
import com.example.next.database.entity.CartItemEntity
import com.example.next.database.entity.OrderEntity
import com.example.next.database.entity.OrderItemEntity
import com.example.next.database.entity.PendingOpEntity
import com.example.next.database.entity.WishlistItemEntity
import com.example.next.models.ThemeMode
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await

/**
 * Firestore write path for authenticated owners, with an offline outbox.
 *
 * Strategy (spec): local mutation commits to Room first (instant UI), then:
 *  - online:  write-through to Firestore
 *  - offline: record the mutation in `pending_ops` and let [flushPendingOps]
 *    replay it (in creation order) when connectivity returns
 *
 * Guests are always no-ops (they never create Firestore documents), and all
 * writes are scoped to `users/{uid}` of the OWNER — see firebase/firestore.rules.
 *
 * Idempotency: wishlist/cart upserts are keyed by productId (set semantics),
 * orders by the deterministic client-side cloud id, so retrying an op can
 * never duplicate a document.
 */
class SyncEngine(
    private val firestore: FirebaseFirestore?,
    private val pendingOpsDao: PendingOpsDao?
) : RemoteSync {

    // ---------- Wishlist ----------

    override suspend fun pushWishlist(owner: String, item: WishlistItemEntity) {
        val doc = userDoc(owner, COLLECTION_WISHLIST, item.productId.toString()) ?: return
        try {
            doc.set(
                mapOf(
                    FIELD_PRODUCT_ID to item.productId,
                    FIELD_PRODUCT_NAME to item.productName,
                    FIELD_PRICE to item.price,
                    FIELD_IMAGE_URL to item.imageUrl,
                    FIELD_ADDED_AT to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            enqueue(owner, TABLE_WISHLIST, item.productId.toString(), OP_SET, PayloadCodec.wishlistSet(item).toString())
        }
    }

    override suspend fun deleteWishlist(owner: String, productId: Int) {
        val doc = userDoc(owner, COLLECTION_WISHLIST, productId.toString()) ?: return
        try {
            doc.delete().await()
        } catch (e: Exception) {
            enqueue(owner, TABLE_WISHLIST, productId.toString(), OP_DELETE, PayloadCodec.delete().toString())
        }
    }

    // ---------- Cart ----------

    override suspend fun pushCart(owner: String, item: CartItemEntity) {
        val doc = userDoc(owner, COLLECTION_CART, item.productId.toString()) ?: return
        try {
            doc.set(
                cartMap(item),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            enqueue(owner, TABLE_CART, item.productId.toString(), OP_SET, PayloadCodec.cartSet(item).toString())
        }
    }

    override suspend fun deleteCart(owner: String, productId: Int) {
        val doc = userDoc(owner, COLLECTION_CART, productId.toString()) ?: return
        try {
            doc.delete().await()
        } catch (e: Exception) {
            enqueue(owner, TABLE_CART, productId.toString(), OP_DELETE, PayloadCodec.delete().toString())
        }
    }

    // ---------- Orders ----------

    override suspend fun pushOrder(owner: String, order: OrderEntity, items: List<OrderItemEntity>) {
        val orderDoc = userDoc(owner, COLLECTION_ORDERS, order.cloudId) ?: return
        if (order.cloudId.isBlank()) return // defensive: never write without a cloud id
        try {
            val snapshot = orderDoc.get().await()
            // Order documents are atomic (order + items in ONE batch) and the
            // rules allow creation only with status PENDING — a cancelled-before-
            // sync order has nothing to upload.
            if (snapshot.exists() || order.status != ORDER_STATUS_PENDING) return
            val batch = firestore?.batch() ?: return
            batch.set(orderDoc, orderMap(order))
            val itemsCollection = orderDoc.collection(COLLECTION_ORDER_ITEMS)
            items.forEach { item ->
                batch.set(itemsCollection.document(item.productId.toString()), orderItemMap(item))
            }
            batch.commit().await()
        } catch (e: Exception) {
            enqueue(
                owner, TABLE_ORDERS, order.cloudId, OP_SET,
                PayloadCodec.orderSet(order, items).toString()
            )
        }
    }

    override suspend fun pushOrderStatus(owner: String, order: OrderEntity, status: String) {
        val orderDoc = userDoc(owner, COLLECTION_ORDERS, order.cloudId) ?: return
        if (order.cloudId.isBlank()) return
        try {
            val snapshot = orderDoc.get().await()
            if (snapshot.exists()) {
                val current = snapshot.getString(FIELD_STATUS) ?: return
                if (current == status) return
                // Rules only allow PENDING -> CANCELLED; PROCESSING/SHIPPED/
                // DELIVERED means the cancellation window has passed.
                if (current != ORDER_STATUS_PENDING || status != ORDER_STATUS_CANCELLED) return
                orderDoc.update(
                    mapOf(FIELD_STATUS to status, FIELD_UPDATED_AT to FieldValue.serverTimestamp())
                ).await()
            } else {
                // The order never reached the cloud (e.g. its create op is
                // still queued offline). Suppress the creation: a cancelled
                // order must not materialize later as PENDING.
                pendingOpsDao?.deleteByOwnerTableRow(owner, TABLE_ORDERS, order.cloudId)
            }
        } catch (e: Exception) {
            enqueue(owner, TABLE_ORDERS, order.cloudId, OP_SET_STATUS, PayloadCodec.statusSet(status).toString())
        }
    }

    // ---------- Settings ----------

    override suspend fun pushSettings(owner: String, themeMode: ThemeMode) {
        val doc = firestore?.collection("users")?.document(Owner.uidOf(owner) ?: return)
            ?.collection(COLLECTION_SETTINGS)?.document(DOC_SETTINGS) ?: return
        try {
            doc.set(
                mapOf(FIELD_THEME_MODE to themeMode.name, FIELD_UPDATED_AT to FieldValue.serverTimestamp()),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            // Settings fall back to local-only until the next successful push;
            // an outbox entry is overkill for a single LWW key.
            Unit
        }
    }

    // ---------- Outbox ----------

    /** Replays every pending op of [owner] in creation order. True when all succeeded. */
    suspend fun flushPendingOps(owner: String): Boolean {
        val dao = pendingOpsDao ?: return true
        if (!Owner.isUser(owner)) return true
        var allOk = true
        val ops = dao.getPendingOps(owner).filter { it.attempt <= MAX_ATTEMPTS }
        for (op in ops) {
            val applied = try {
                applyOp(owner, op)
            } catch (e: Exception) {
                dao.bumpAttempt(op.id)
                allOk = false
                false
            }
            if (applied) {
                dao.deleteById(op.id)
            } else {
                allOk = false
            }
        }
        return allOk
    }

    private suspend fun applyOp(owner: String, op: PendingOpEntity): Boolean = when (op.tableName) {
        TABLE_WISHLIST -> applyWishlistOp(owner, op)
        TABLE_CART -> applyCartOp(owner, op)
        TABLE_ORDERS -> applyOrderOp(owner, op)
        else -> true
    }

    private suspend fun applyWishlistOp(owner: String, op: PendingOpEntity): Boolean {
        val doc = userDoc(owner, COLLECTION_WISHLIST, op.rowId) ?: return true
        return when (op.op) {
            OP_DELETE -> {
                doc.delete().await()
                true
            }
            else -> {
                val map = PayloadCodec.setMap(op.payload)
                doc.set(map + (FIELD_ADDED_AT to FieldValue.serverTimestamp()), SetOptions.merge()).await()
                true
            }
        }
    }

    private suspend fun applyCartOp(owner: String, op: PendingOpEntity): Boolean {
        val doc = userDoc(owner, COLLECTION_CART, op.rowId) ?: return true
        return when (op.op) {
            OP_DELETE -> {
                doc.delete().await()
                true
            }
            else -> {
                val map = PayloadCodec.setMap(op.payload)
                doc.set(map + (FIELD_UPDATED_AT to FieldValue.serverTimestamp()), SetOptions.merge()).await()
                true
            }
        }
    }

    /**
     * Order replay. `set`: create order + items atomically when the document
     * does not exist yet (never duplicate). `setStatus`: apply the
     * PENDING -> CANCELLED transition when the document exists; a missing
     * document means the creation was suppressed — nothing left to do.
     */
    private suspend fun applyOrderOp(owner: String, op: PendingOpEntity): Boolean {
        val orderDoc = userDoc(owner, COLLECTION_ORDERS, op.rowId) ?: return true
        return when (op.op) {
            OP_SET_STATUS -> {
                val snapshot = orderDoc.get().await()
                if (!snapshot.exists()) return true // order never reached the cloud
                val current = snapshot.getString(FIELD_STATUS)
                if (current == ORDER_STATUS_PENDING) {
                    val status = PayloadCodec.statusValue(op.payload)
                    if (status == ORDER_STATUS_CANCELLED) {
                        orderDoc.update(
                            mapOf(FIELD_STATUS to status, FIELD_UPDATED_AT to FieldValue.serverTimestamp())
                        ).await()
                    }
                }
                true // already CANCELLED / window closed / applied: op is done
            }
            else -> {
                val snapshot = orderDoc.get().await()
                if (snapshot.exists()) return true // already created
                val orderJson = PayloadCodec.orderJson(op.payload)
                val status = orderJson.getString(FIELD_STATUS)
                if (status != ORDER_STATUS_PENDING) return true // cancelled before upload
                val batch = firestore?.batch() ?: return true
                val orderMap = mapOf(
                    FIELD_ORDER_ID to op.rowId,
                    FIELD_ORDER_DATE to orderJson.getString(FIELD_ORDER_DATE),
                    FIELD_STATUS to status,
                    FIELD_FULL_NAME to orderJson.getString(FIELD_FULL_NAME),
                    FIELD_PHONE to orderJson.getString(FIELD_PHONE),
                    FIELD_ADDRESS to orderJson.getString(FIELD_ADDRESS),
                    FIELD_CITY to orderJson.getString(FIELD_CITY),
                    FIELD_SHIPPING_METHOD to orderJson.getString(FIELD_SHIPPING_METHOD),
                    FIELD_SHIPPING_PRICE to orderJson.getDouble(FIELD_SHIPPING_PRICE),
                    FIELD_SUBTOTAL to orderJson.getDouble(FIELD_SUBTOTAL),
                    FIELD_TOTAL to orderJson.getDouble(FIELD_TOTAL),
                    FIELD_CREATED_AT to op.createdAt,
                    FIELD_UPDATED_AT to FieldValue.serverTimestamp()
                )
                batch.set(orderDoc, orderMap)
                val itemsCollection = orderDoc.collection(COLLECTION_ORDER_ITEMS)
                val items = PayloadCodec.itemsArray(op.payload)
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    batch.set(
                        itemsCollection.document(item.getInt(FIELD_PRODUCT_ID).toString()),
                        mapOf(
                            FIELD_PRODUCT_ID to item.getInt(FIELD_PRODUCT_ID),
                            FIELD_PRODUCT_NAME to item.getString(FIELD_PRODUCT_NAME),
                            FIELD_PRICE to item.getDouble(FIELD_PRICE),
                            FIELD_IMAGE_URL to item.getString(FIELD_IMAGE_URL),
                            FIELD_QUANTITY to item.getInt(FIELD_QUANTITY)
                        )
                    )
                }
                batch.commit().await()
                true
            }
        }
    }

    /** Folds a newer mutation into the outbox (last write wins per row). */
    private suspend fun enqueue(owner: String, tableName: String, rowId: String, op: String, payload: String) {
        val dao = pendingOpsDao ?: return
        if (!Owner.isUser(owner)) return
        val now = System.currentTimeMillis()
        val updated = dao.updateOp(owner, tableName, rowId, op, payload, now)
        if (updated == 0) {
            dao.insert(
                PendingOpEntity(
                    owner = owner,
                    tableName = tableName,
                    rowId = rowId,
                    op = op,
                    payload = payload,
                    createdAt = now
                )
            )
        }
    }

    // ---------- helpers ----------

    private fun userDoc(owner: String, collection: String, docId: String): DocumentReference? {
        val uid = Owner.uidOf(owner) ?: return null // guests never touch Firestore
        return firestore?.collection("users")?.document(uid)?.collection(collection)?.document(docId)
    }

    /** Document map for an order (shared with the guest merger). */
    internal fun orderMap(order: OrderEntity): Map<String, Any> = mapOf(
        FIELD_ORDER_ID to order.cloudId,
        FIELD_ORDER_DATE to order.orderDate,
        FIELD_STATUS to order.status,
        FIELD_FULL_NAME to order.fullName,
        FIELD_PHONE to order.phone,
        FIELD_ADDRESS to order.address,
        FIELD_CITY to order.city,
        FIELD_SHIPPING_METHOD to order.shippingMethod,
        FIELD_SHIPPING_PRICE to order.shippingPrice,
        FIELD_SUBTOTAL to order.subtotal,
        FIELD_TOTAL to order.total,
        FIELD_CREATED_AT to System.currentTimeMillis(),
        FIELD_UPDATED_AT to FieldValue.serverTimestamp()
    )

    /** Document map for one order line item (shared with the guest merger). */
    internal fun orderItemMap(item: OrderItemEntity): Map<String, Any> = mapOf(
        FIELD_PRODUCT_ID to item.productId,
        FIELD_PRODUCT_NAME to item.productName,
        FIELD_PRICE to item.price,
        FIELD_IMAGE_URL to item.imageUrl,
        FIELD_QUANTITY to item.quantity
    )

    private fun cartMap(item: CartItemEntity): Map<String, Any> = mapOf(
        FIELD_PRODUCT_ID to item.productId,
        FIELD_PRODUCT_NAME to item.productName,
        FIELD_PRICE to item.price,
        FIELD_IMAGE_URL to item.imageUrl,
        FIELD_QUANTITY to item.quantity,
        FIELD_UPDATED_AT to FieldValue.serverTimestamp()
    )

    companion object {
        const val COLLECTION_WISHLIST = "wishlist"
        const val COLLECTION_CART = "cart"
        const val COLLECTION_ORDERS = "orders"
        const val COLLECTION_ORDER_ITEMS = "items"
        const val COLLECTION_SETTINGS = "settings"
        const val DOC_SETTINGS = "settings"

        const val ORDER_STATUS_PENDING = "PENDING"
        const val ORDER_STATUS_CANCELLED = "CANCELLED"

        const val FIELD_PRODUCT_ID = "productId"
        const val FIELD_PRODUCT_NAME = "productName"
        const val FIELD_PRICE = "price"
        const val FIELD_IMAGE_URL = "imageUrl"
        const val FIELD_QUANTITY = "quantity"
        const val FIELD_ADDED_AT = "addedAt"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_STATUS = "status"
        const val FIELD_ORDER_ID = "orderId"
        const val FIELD_ORDER_DATE = "orderDate"
        const val FIELD_FULL_NAME = "fullName"
        const val FIELD_PHONE = "phone"
        const val FIELD_ADDRESS = "address"
        const val FIELD_CITY = "city"
        const val FIELD_SHIPPING_METHOD = "shippingMethod"
        const val FIELD_SHIPPING_PRICE = "shippingPrice"
        const val FIELD_SUBTOTAL = "subtotal"
        const val FIELD_TOTAL = "total"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_THEME_MODE = "themeMode"

        const val TABLE_WISHLIST = PendingOpEntity.TABLE_WISHLIST
        const val TABLE_CART = PendingOpEntity.TABLE_CART
        const val TABLE_ORDERS = PendingOpEntity.TABLE_ORDERS
        const val OP_SET = PendingOpEntity.OP_SET
        const val OP_DELETE = PendingOpEntity.OP_DELETE
        const val OP_SET_STATUS = PendingOpEntity.OP_SET_STATUS

        /** Ops beyond this many failed attempts are left alone (stuck ops). */
        const val MAX_ATTEMPTS = 15
    }
}