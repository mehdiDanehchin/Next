package com.example.next.data.repository

import com.example.next.database.dao.CartDao
import com.example.next.database.entity.CartItemEntity
import com.example.next.database.entity.toModel
import com.example.next.models.CartItem
import com.example.next.models.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Cart store. Exposes reactive [Flow]s backed by Room so every screen
 * (and the bottom-navigation badge) automatically stays in sync after
 * any insert/update/delete — no manual refresh() calls needed.
 */
class CartRepository(private val cartDao: CartDao) {

    fun observeCartItems(): Flow<List<CartItem>> =
        cartDao.observeCartItems().map { items -> items.map { it.toModel() } }

    fun observeCartTotal(): Flow<Double> = cartDao.observeCartTotal()

    fun observeCartCount(): Flow<Int> = cartDao.observeCartCount()

    suspend fun addToCart(product: Product) {
        val existing = cartDao.getCartItemByProductId(product.id)
        if (existing != null) {
            cartDao.updateQuantity(existing.id, existing.quantity + 1)
        } else {
            cartDao.insert(
                CartItemEntity(
                    productId = product.id,
                    productName = product.name,
                    price = product.price,
                    imageUrl = product.imageUrl,
                    quantity = 1
                )
            )
        }
    }

    suspend fun updateQuantity(cartId: Int, quantity: Int) {
        if (quantity <= 0) {
            cartDao.deleteById(cartId)
        } else {
            cartDao.updateQuantity(cartId, quantity)
        }
    }

    suspend fun removeItem(cartId: Int) = cartDao.deleteById(cartId)

    suspend fun clearCart() = cartDao.clear()
}