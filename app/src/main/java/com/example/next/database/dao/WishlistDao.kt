package com.example.next.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.next.database.entity.WishlistItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Wishlist DAO — every query is scoped by [owner] (`guest:<uuid>` /
 * `uid:<uid>`). No owner-less query exists: rows of one user can never be
 * observed by another owner, not even by accident.
 */
@Dao
interface WishlistDao {

    @Query("SELECT * FROM wishlist WHERE owner = :owner ORDER BY id DESC")
    fun observeWishlistItems(owner: String): Flow<List<WishlistItemEntity>>

    @Query("SELECT COUNT(*) FROM wishlist WHERE owner = :owner")
    fun observeWishlistCount(owner: String): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist WHERE owner = :owner AND product_id = :productId)")
    fun observeIsInWishlist(owner: String, productId: Int): Flow<Boolean>

    @Query("SELECT * FROM wishlist WHERE owner = :owner ORDER BY id ASC")
    suspend fun getWishlistItems(owner: String): List<WishlistItemEntity>

    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insert(item: WishlistItemEntity): Long

    /** Upsert path for remote->local sync; updates every row of that product. */
    @Query(
        "UPDATE wishlist SET product_name = :productName, price = :price, image_url = :imageUrl " +
            "WHERE owner = :owner AND product_id = :productId"
    )
    suspend fun updateByProductId(
        owner: String,
        productId: Int,
        productName: String,
        price: Double,
        imageUrl: String
    ): Int

    @Query("DELETE FROM wishlist WHERE owner = :owner AND product_id = :productId")
    suspend fun deleteByProductId(owner: String, productId: Int): Int

    /** Hard purge of one owner's rows (account-switch no-leak guarantee). */
    @Query("DELETE FROM wishlist WHERE owner = :owner")
    suspend fun deleteAllForOwner(owner: String): Int

    /** Re-owns ONE row after a successful guest->account merge. */
    @Query("UPDATE wishlist SET owner = :newOwner WHERE id = :id")
    suspend fun reownById(id: Int, newOwner: String): Int
}