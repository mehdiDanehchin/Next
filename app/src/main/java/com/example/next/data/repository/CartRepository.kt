package com.example.next.data.repository

import com.example.next.data.session.SessionManager
import com.example.next.data.session.ownerNow
import com.example.next.data.sync.RemoteSync
import com.example.next.database.dao.CartDao
import com.example.next.database.entity.CartItemEntity
import com.example.next.database.entity.toModel
import com.example.next.models.CartItem
import com.example.next.models.Product
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Cart store scoped to the ACTIVE OWNER (see WishlistRepository for the
 * owner-switching contract). Mutations commit to Room first, then mirror to
 * Firestore via [RemoteSync] (no-op for guest owners).
 *
 * Quantities are capped at 99 locally to match the Firestore security rules
 * (quantity 1..99) — the cloud must never reject a write the UI allowed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CartRepository(
    private val cartDao: CartDao,
    private val sessionManager: SessionManager,
    private val remoteSync: RemoteSync
) {

    private val ownerFlow = sessionManager.observeActiveOwner()

    fun observeCartItems(): Flow<List<CartItem>> =
        ownerFlow.flatMapLatest { owner ->
            cartDao.observeCartItems(owner).map { items -> items.map { it.toModel() } }
        }

    fun observeCartTotal(): Flow<Double> =
        ownerFlow.flatMapLatest { owner -> cartDao.observeCartTotal(owner) }

    fun observeCartCount(): Flow<Int> =
        ownerFlow.flatMapLatest { owner -> cartDao.observeCartCount(owner) }

    suspend fun addToCart(product: Product) {
        val owner = sessionManager.ownerNow()
        val existing = cartDao.getCartItemByProductId(owner, product.id)
        if (existing != null) {
            val newQuantity = (existing.quantity + 1).coerceAtMost(MAX_QUANTITY)
            cartDao.updateQuantity(owner, existing.id, newQuantity)
            remoteSync.pushCart(owner, existing.copy(quantity = newQuantity))
        } else {
            cartDao.insert(
                CartItemEntity(
                    productId = product.id,
                    productName = product.name,
                    price = product.price,
                    imageUrl = product.imageUrl,
                    quantity = 1,
                    owner = owner
                )
            ).also { insertedId ->
                remoteSync.pushCart(
                    owner,
                    CartItemEntity(
                        id = insertedId.toInt(),
                        productId = product.id,
                        productName = product.name,
                        price = product.price,
                        imageUrl = product.imageUrl,
                        quantity = 1,
                        owner = owner
                    )
                )
            }
        }
    }

    suspend fun updateQuantity(cartId: Int, quantity: Int) {
        val owner = sessionManager.ownerNow()
        val current = cartDao.getCartItemById(owner, cartId) ?: return
        if (quantity <= 0) {
            cartDao.deleteById(owner, cartId)
            remoteSync.deleteCart(owner, current.productId)
        } else {
            val clamped = quantity.coerceAtMost(MAX_QUANTITY)
            cartDao.updateQuantity(owner, cartId, clamped)
            remoteSync.pushCart(owner, current.copy(quantity = clamped))
        }
    }

    suspend fun removeItem(cartId: Int) {
        val owner = sessionManager.ownerNow()
        val current = cartDao.getCartItemById(owner, cartId) ?: return
        cartDao.deleteById(owner, cartId)
        remoteSync.deleteCart(owner, current.productId)
    }

    suspend fun clearCart() {
        val owner = sessionManager.ownerNow()
        val items = cartDao.getCartItems(owner)
        cartDao.clear(owner)
        items.forEach { remoteSync.deleteCart(owner, it.productId) }
    }

    companion object {
        /** Mirrors the security-rule limit (quantity 1..99). */
        const val MAX_QUANTITY = 99
    }
}