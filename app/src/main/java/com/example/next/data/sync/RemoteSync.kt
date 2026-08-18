package com.example.next.data.sync

import com.example.next.database.entity.CartItemEntity
import com.example.next.database.entity.OrderEntity
import com.example.next.database.entity.OrderItemEntity
import com.example.next.database.entity.WishlistItemEntity
import com.example.next.models.ThemeMode

/**
 * Firestore write path for user-owned data.
 *
 * Repositories always commit to Room FIRST (instant UI), then call into this
 * interface. Implementations decide how the write reaches the cloud:
 * write-through while online, pending_ops outbox while offline. Guest owners
 * are a no-op — guests never create Firestore documents.
 */
interface RemoteSync {

    /** Wishlist upsert (set semantics, keyed by productId). */
    suspend fun pushWishlist(owner: String, item: WishlistItemEntity)

    suspend fun deleteWishlist(owner: String, productId: Int)

    /** Cart upsert (LWW, keyed by productId). */
    suspend fun pushCart(owner: String, item: CartItemEntity)

    suspend fun deleteCart(owner: String, productId: Int)

    /** Order snapshot + line items, keyed by the deterministic cloud order id. */
    suspend fun pushOrder(owner: String, order: OrderEntity, items: List<OrderItemEntity>)

    /** Order status transition (PENDING -> CANCELLED only; rules-enforced). */
    suspend fun pushOrderStatus(owner: String, order: OrderEntity, status: String)

    /** Settings upsert (LWW). */
    suspend fun pushSettings(owner: String, themeMode: ThemeMode)
}