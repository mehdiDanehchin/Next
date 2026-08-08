package com.example.next.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.next.models.Order

/**
 * Room entity for a placed order. Totals and address are snapshotted at
 * purchase time so an order stays immutable even if products change later.
 */
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "order_date")
    val orderDate: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "full_name")
    val fullName: String,
    @ColumnInfo(name = "phone")
    val phone: String,
    @ColumnInfo(name = "address")
    val address: String,
    @ColumnInfo(name = "city")
    val city: String,
    @ColumnInfo(name = "shipping_method")
    val shippingMethod: String,
    @ColumnInfo(name = "shipping_price")
    val shippingPrice: Double,
    @ColumnInfo(name = "subtotal")
    val subtotal: Double,
    @ColumnInfo(name = "total")
    val total: Double
)

fun OrderEntity.toModel(): Order = Order(
    id = id,
    orderDate = orderDate,
    status = status,
    fullName = fullName,
    phone = phone,
    address = address,
    city = city,
    shippingMethod = shippingMethod,
    shippingPrice = shippingPrice,
    subtotal = subtotal,
    total = total
)
