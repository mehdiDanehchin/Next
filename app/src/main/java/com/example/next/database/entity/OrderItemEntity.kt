package com.example.next.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.next.models.OrderItem

/** Room entity for one line of an order (product snapshot at purchase time). */
@Entity(tableName = "order_items")
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "order_id")
    val orderId: Long,
    @ColumnInfo(name = "product_id")
    val productId: Int,
    @ColumnInfo(name = "product_name")
    val productName: String,
    @ColumnInfo(name = "price")
    val price: Double,
    @ColumnInfo(name = "image_url")
    val imageUrl: String,
    @ColumnInfo(name = "quantity")
    val quantity: Int
)

fun OrderItemEntity.toModel(): OrderItem = OrderItem(
    id = id,
    orderId = orderId,
    productId = productId,
    productName = productName,
    price = price,
    imageUrl = imageUrl,
    quantity = quantity
)
