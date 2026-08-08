package com.example.next.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.next.database.entity.OrderEntity
import com.example.next.database.entity.OrderItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Insert
    suspend fun insertOrder(order: OrderEntity): Long

    @Insert
    suspend fun insertItems(items: List<OrderItemEntity>)

    @Query("SELECT * FROM orders ORDER BY id DESC")
    fun observeOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM order_items WHERE order_id = :orderId ORDER BY id ASC")
    suspend fun getItemsForOrder(orderId: Long): List<OrderItemEntity>

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    suspend fun updateStatus(orderId: Long, status: String)
}
