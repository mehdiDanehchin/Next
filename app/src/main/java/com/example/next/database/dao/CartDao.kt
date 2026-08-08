package com.example.next.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.next.database.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {

    @Query("SELECT * FROM cart ORDER BY id DESC")
    fun observeCartItems(): Flow<List<CartItemEntity>>

    @Query("SELECT COALESCE(SUM(price * quantity), 0) FROM cart")
    fun observeCartTotal(): Flow<Double>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM cart")
    fun observeCartCount(): Flow<Int>

    @Query("SELECT * FROM cart WHERE product_id = :productId")
    suspend fun getCartItemByProductId(productId: Int): CartItemEntity?

    @Insert
    suspend fun insert(item: CartItemEntity): Long

    @Query("UPDATE cart SET quantity = :quantity WHERE id = :cartId")
    suspend fun updateQuantity(cartId: Int, quantity: Int)

    @Query("DELETE FROM cart WHERE id = :cartId")
    suspend fun deleteById(cartId: Int)

    @Query("DELETE FROM cart")
    suspend fun clear()
}
