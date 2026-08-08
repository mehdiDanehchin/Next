package com.example.next.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.next.database.entity.WishlistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {

    @Query("SELECT * FROM wishlist ORDER BY id DESC")
    fun observeWishlistItems(): Flow<List<WishlistItemEntity>>

    @Query("SELECT COUNT(*) FROM wishlist")
    fun observeWishlistCount(): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist WHERE product_id = :productId)")
    fun observeIsInWishlist(productId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: WishlistItemEntity): Long

    @Query("DELETE FROM wishlist WHERE product_id = :productId")
    suspend fun deleteByProductId(productId: Int)
}
