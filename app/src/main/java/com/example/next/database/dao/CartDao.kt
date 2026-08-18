package com.example.next.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.next.database.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Cart DAO — every query is scoped by [owner] (`guest:<uuid>` / `uid:<uid>`).
 * No owner-less query exists, so an account switch can never leak another
 * owner's cart into the UI.
 */
@Dao
interface CartDao {

    @Query("SELECT * FROM cart WHERE owner = :owner ORDER BY id DESC")
    fun observeCartItems(owner: String): Flow<List<CartItemEntity>>

    @Query("SELECT COALESCE(SUM(price * quantity), 0) FROM cart WHERE owner = :owner")
    fun observeCartTotal(owner: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM cart WHERE owner = :owner")
    fun observeCartCount(owner: String): Flow<Int>

    @Query("SELECT * FROM cart WHERE owner = :owner AND product_id = :productId")
    suspend fun getCartItemByProductId(owner: String, productId: Int): CartItemEntity?

    @Query("SELECT * FROM cart WHERE owner = :owner AND id = :cartId")
    suspend fun getCartItemById(owner: String, cartId: Int): CartItemEntity?

    @Query("SELECT * FROM cart WHERE owner = :owner ORDER BY id ASC")
    suspend fun getCartItems(owner: String): List<CartItemEntity>

    @Insert
    suspend fun insert(item: CartItemEntity): Long

    @Query("UPDATE cart SET quantity = :quantity WHERE owner = :owner AND id = :cartId")
    suspend fun updateQuantity(owner: String, cartId: Int, quantity: Int): Int

    /** Sync/merge path: set the quantity of a whole (owner, product) row. */
    @Query("UPDATE cart SET quantity = :quantity WHERE owner = :owner AND product_id = :productId")
    suspend fun updateQuantityByProductId(owner: String, productId: Int, quantity: Int): Int

    /** Remote->local sync upsert: quantity + snapshot fields for one product. */
    @Query(
        "UPDATE cart SET product_name = :productName, price = :price, image_url = :imageUrl, " +
            "quantity = :quantity WHERE owner = :owner AND product_id = :productId"
    )
    suspend fun updateByProductId(
        owner: String,
        productId: Int,
        productName: String,
        price: Double,
        imageUrl: String,
        quantity: Int
    ): Int

    @Query("DELETE FROM cart WHERE owner = :owner AND id = :cartId")
    suspend fun deleteById(owner: String, cartId: Int): Int

    @Query("DELETE FROM cart WHERE owner = :owner AND product_id = :productId")
    suspend fun deleteByProductId(owner: String, productId: Int): Int

    /** Clears the cart of one owner after checkout. */
    @Query("DELETE FROM cart WHERE owner = :owner")
    suspend fun clear(owner: String): Int

    /** Hard purge of one owner's rows (account-switch no-leak guarantee). */
    @Query("DELETE FROM cart WHERE owner = :owner")
    suspend fun deleteAllForOwner(owner: String): Int

    /** Re-owns ONE row after a successful guest->account merge. */
    @Query("UPDATE cart SET owner = :newOwner WHERE id = :id")
    suspend fun reownById(id: Int, newOwner: String): Int
}