package com.example.next.data.repository

import androidx.room.withTransaction
import com.example.next.database.AppDatabase
import com.example.next.database.dao.CartDao
import com.example.next.database.dao.OrderDao
import com.example.next.database.entity.OrderEntity
import com.example.next.database.entity.OrderItemEntity
import com.example.next.database.entity.toModel
import com.example.next.models.Order
import com.example.next.models.OrderItem
import com.example.next.models.OrderStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Order store. [placeOrder] persists the order + its line items and clears
 * the cart inside a single database transaction, so a crash mid-checkout
 * cannot leave a half-placed order or a stale cart behind.
 */
class OrderRepository(
    private val database: AppDatabase,
    private val orderDao: OrderDao,
    private val cartDao: CartDao
) {

    fun observeOrders(): Flow<List<Order>> =
        orderDao.observeOrders().map { orders -> orders.map { it.toModel() } }

    suspend fun placeOrder(order: Order, items: List<OrderItem>): Long = database.withTransaction {
        val orderId = orderDao.insertOrder(
            OrderEntity(
                orderDate = order.orderDate,
                status = order.status,
                fullName = order.fullName,
                phone = order.phone,
                address = order.address,
                city = order.city,
                shippingMethod = order.shippingMethod,
                shippingPrice = order.shippingPrice,
                subtotal = order.subtotal,
                total = order.total
            )
        )
        orderDao.insertItems(
            items.map {
                OrderItemEntity(
                    orderId = orderId,
                    productId = it.productId,
                    productName = it.productName,
                    price = it.price,
                    imageUrl = it.imageUrl,
                    quantity = it.quantity
                )
            }
        )
        cartDao.clear()
        orderId
    }

    suspend fun getItemsForOrder(orderId: Long): List<OrderItem> =
        orderDao.getItemsForOrder(orderId).map { it.toModel() }

    suspend fun updateStatus(orderId: Long, status: OrderStatus) =
        orderDao.updateStatus(orderId, status.code)
}
