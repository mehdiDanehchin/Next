package com.example.next.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.next.models.CartItem

/**
 * Room entity mirroring the legacy `cart` table schema (kept 1:1 so the
 * SQLite -> Room migration can preserve existing data).
 */
@Entity(tableName = "cart")
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name = "product_id")
    val productId: Int,
    @ColumnInfo(name = "product_name")
    val productName: String,
    @ColumnInfo(name = "price")
    val price: Double = 0.0,
    @ColumnInfo(name = "image_url")
    val imageUrl: String = "",
    @ColumnInfo(name = "quantity")
    val quantity: Int = 1
)

fun CartItemEntity.toModel(): CartItem = CartItem(
    id = id,
    productId = productId,
    productName = productName,
    price = price,
    imageUrl = imageUrl,
    quantity = quantity
)
