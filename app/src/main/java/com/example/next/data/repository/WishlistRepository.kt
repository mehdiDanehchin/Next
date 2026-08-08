package com.example.next.data.repository

import com.example.next.database.dao.WishlistDao
import com.example.next.database.entity.WishlistItemEntity
import com.example.next.database.entity.toModel
import com.example.next.models.Product
import com.example.next.models.WishlistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Reactive wishlist backed by Room flows. Unlike the legacy implementation there
 * is no manual "version bump" to trigger favorite re-checks on product cards;
 * [isInWishlist] is itself a Flow that emits whenever the row changes.
 */
class WishlistRepository(private val wishlistDao: WishlistDao) {

    fun observeWishlistItems(): Flow<List<WishlistItem>> =
        wishlistDao.observeWishlistItems().map { items -> items.map { it.toModel() } }

    fun observeWishlistCount(): Flow<Int> = wishlistDao.observeWishlistCount()

    fun isInWishlist(productId: Int): Flow<Boolean> =
        wishlistDao.observeIsInWishlist(productId)

    suspend fun isInWishlistNow(productId: Int): Boolean =
        wishlistDao.observeIsInWishlist(productId).first()

    suspend fun addToWishlist(product: Product) {
        wishlistDao.insert(
            WishlistItemEntity(
                productId = product.id,
                productName = product.name,
                price = product.price,
                imageUrl = product.imageUrl
            )
        )
    }

    suspend fun removeFromWishlist(productId: Int) =
        wishlistDao.deleteByProductId(productId)
}