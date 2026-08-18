package com.example.next.data.repository

import androidx.room.withTransaction
import com.example.next.data.session.MergeId
import com.example.next.data.session.Owner
import com.example.next.data.session.SessionManager
import com.example.next.data.session.ownerNow
import com.example.next.data.sync.RemoteSync
import com.example.next.database.AppDatabase
import com.example.next.database.dao.CartDao
import com.example.next.database.dao.OrderDao
import com.example.next.database.entity.OrderEntity
import com.example.next.database.entity.OrderItemEntity
import com.example.next.database.entity.toModel
import com.example.next.models.Order
import com.example.next.models.OrderItem
import com.example.next.models.OrderStatus
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Order store scoped to the ACTIVE OWNER (see WishlistRepository).
 *
 * [placeOrder] persists order + line items and clears the cart in one Room
 * transaction, then mirrors the order to Firestore keyed by a
 * CLIENT-GENERATED cloud id (never the device-local autoincrement Long):
 *  - guest orders: deterministic `g_<sha1(guestId:localId)>` — a merge or
 *    flush retry maps to the same document, so duplicates are impossible
 *  - signed-in orders: `u_<uuid>`
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OrderRepository(
    private val database: AppDatabase,
    private val orderDao: OrderDao,
    private val cartDao: CartDao,
    private val sessionManager: SessionManager,
    private val remoteSync: RemoteSync
) {

    private val ownerFlow = sessionManager.observeActiveOwner()

    fun observeOrders(): Flow<List<Order>> =
        ownerFlow.flatMapLatest { owner ->
            orderDao.observeOrders(owner).map { orders -> orders.map { it.toModel() } }
        }

    suspend fun placeOrder(order: Order, items: List<OrderItem>): Long {
        val owner = sessionManager.ownerNow()
        val orderId = database.withTransaction {
            val localId = orderDao.insertOrder(
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
                    total = order.total,
                    owner = owner
                )
            )
            val cloudId = if (Owner.isUser(owner)) {
                "u_${UUID.randomUUID()}"
            } else {
                MergeId.guestOrderCloudId(Owner.guestIdOf(owner)!!, localId)
            }
            orderDao.updateCloudId(localId, cloudId)
            orderDao.insertItems(
                items.map {
                    OrderItemEntity(
                        orderId = localId,
                        productId = it.productId,
                        productName = it.productName,
                        price = it.price,
                        imageUrl = it.imageUrl,
                        quantity = it.quantity
                    )
                }
            )
            cartDao.clear(owner)
            localId
        }
        val persisted = OrderEntity(
            id = orderId,
            orderDate = order.orderDate,
            status = order.status,
            fullName = order.fullName,
            phone = order.phone,
            address = order.address,
            city = order.city,
            shippingMethod = order.shippingMethod,
            shippingPrice = order.shippingPrice,
            subtotal = order.subtotal,
            total = order.total,
            owner = owner,
            cloudId = orderDao.getCloudId(owner, orderId).orEmpty()
        )
        remoteSync.pushOrder(
            owner,
            persisted,
            orderDao.getItemsForOrder(orderId)
        )
        return orderId
    }

    suspend fun getItemsForOrder(orderId: Long): List<OrderItem> =
        orderDao.getItemsForOrder(orderId).map { it.toModel() }

    suspend fun updateStatus(orderId: Long, status: OrderStatus) {
        val owner = sessionManager.ownerNow()
        val row = orderDao.getOrderById(owner, orderId) ?: return
        orderDao.updateStatus(owner, orderId, status.code)
        remoteSync.pushOrderStatus(owner, row.copy(status = status.code), status.code)
    }
}