package com.example.next.data.repository

import com.example.next.data.session.SessionManager
import com.example.next.data.session.ownerNow
import com.example.next.data.sync.RemoteSync
import com.example.next.database.dao.WishlistDao
import com.example.next.database.entity.WishlistItemEntity
import com.example.next.database.entity.toModel
import com.example.next.models.Product
import com.example.next.models.WishlistItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Reactive wishlist backed by Room flows, scoped to the ACTIVE OWNER.
 *
 * Every observation re-scopes through [SessionManager.observeActiveOwner]
 * with flatMapLatest, so when the owner changes (guest <-> account, or
 * account A -> account B) the emitted data switches immediately and the
 * previous owner's rows can never surface in the UI. Mutations read the
 * current owner at call time and push to Firestore after the Room commit.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WishlistRepository(
    private val wishlistDao: WishlistDao,
    private val sessionManager: SessionManager,
    private val remoteSync: RemoteSync
) {

    private val ownerFlow = sessionManager.observeActiveOwner()

    fun observeWishlistItems(): Flow<List<WishlistItem>> =
        ownerFlow.flatMapLatest { owner ->
            wishlistDao.observeWishlistItems(owner).map { items -> items.map { it.toModel() } }
        }

    fun observeWishlistCount(): Flow<Int> =
        ownerFlow.flatMapLatest { owner -> wishlistDao.observeWishlistCount(owner) }

    fun isInWishlist(productId: Int): Flow<Boolean> =
        ownerFlow.flatMapLatest { owner -> wishlistDao.observeIsInWishlist(owner, productId) }

    suspend fun isInWishlistNow(productId: Int): Boolean =
        wishlistDao.observeIsInWishlist(sessionManager.ownerNow(), productId).first()

    suspend fun addToWishlist(product: Product) {
        val owner = sessionManager.ownerNow()
        val entity = WishlistItemEntity(
            productId = product.id,
            productName = product.name,
            price = product.price,
            imageUrl = product.imageUrl,
            owner = owner
        )
        wishlistDao.insert(entity)
        remoteSync.pushWishlist(owner, entity)
    }

    suspend fun removeFromWishlist(productId: Int) {
        val owner = sessionManager.ownerNow()
        wishlistDao.deleteByProductId(owner, productId)
        remoteSync.deleteWishlist(owner, productId)
    }
}