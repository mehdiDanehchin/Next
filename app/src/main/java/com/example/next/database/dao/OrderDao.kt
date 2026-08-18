package com.example.next.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.next.database.entity.OrderEntity
import com.example.next.database.entity.OrderItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Order DAO. Orders are owner-scoped like every other user-owned table;
 * order_items have NO owner column — their ownership is inherited from the
 * parent order (items are only reachable through an owner-scoped order id).
 */
@Dao
interface OrderDao {

    @Insert
    suspend fun insertOrder(order: OrderEntity): Long

    @Insert
    suspend fun insertItems(items: List<OrderItemEntity>)

    @Query("SELECT * FROM orders WHERE owner = :owner ORDER BY id DESC")
    fun observeOrders(owner: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE owner = :owner ORDER BY id ASC")
    suspend fun getOrdersForOwner(owner: String): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE owner = :owner AND id = :orderId")
    suspend fun getOrderById(owner: String, orderId: Long): OrderEntity?

    @Query("SELECT * FROM order_items WHERE order_id = :orderId ORDER BY id ASC")
    suspend fun getItemsForOrder(orderId: Long): List<OrderItemEntity>

    @Query("SELECT cloud_id FROM orders WHERE owner = :owner AND id = :orderId")
    suspend fun getCloudId(owner: String, orderId: Long): String?

    /** Pull-on-resume: find the local row that mirrors a cloud order. */
    @Query("SELECT * FROM orders WHERE owner = :owner AND cloud_id = :cloudId")
    suspend fun getOrderByCloudId(owner: String, cloudId: String): OrderEntity?

    /** Pull-on-resume: apply a remote status (append-only orders). */
    @Query("UPDATE orders SET status = :status WHERE owner = :owner AND cloud_id = :cloudId")
    suspend fun updateStatusByCloudId(owner: String, cloudId: String, status: String): Int

    @Query("UPDATE orders SET status = :status WHERE owner = :owner AND id = :orderId")
    suspend fun updateStatus(owner: String, orderId: Long, status: String): Int

    @Query("UPDATE orders SET cloud_id = :cloudId WHERE id = :orderId")
    suspend fun updateCloudId(orderId: Long, cloudId: String): Int

    /** Hard purge of one owner's rows (account-switch no-leak guarantee). */
    @Query("DELETE FROM orders WHERE owner = :owner")
    suspend fun deleteAllForOwner(owner: String): Int

    /** Re-owns every order of one owner after a successful guest->account merge. */
    @Query("UPDATE orders SET owner = :newOwner WHERE owner = :oldOwner")
    suspend fun reown(oldOwner: String, newOwner: String): Int
}