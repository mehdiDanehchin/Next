package com.example.next.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.next.models.WishlistItem

/**
 * Room entity mirroring the legacy `wishlist` table schema (kept 1:1 so the
 * SQLite -> Room migration can preserve existing data).
 */
@Entity(tableName = "wishlist")
data class WishlistItemEntity(
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
    // Owner-scoping (v5), see CartItemEntity.owner.
    @ColumnInfo(name = "owner", defaultValue = "''")
    val owner: String = ""
)

fun WishlistItemEntity.toModel(): WishlistItem = WishlistItem(
    id = id,
    productId = productId,
    productName = productName,
    price = price,
    imageUrl = imageUrl
)
