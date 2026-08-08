package com.example.next.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.next.models.Product

/**
 * Room entity mirroring the legacy `products` table schema (kept 1:1 so the
 * SQLite -> Room migration can preserve existing data).
 */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "description")
    val description: String = "",
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "price")
    val price: Double = 0.0,
    @ColumnInfo(name = "image_url")
    val imageUrl: String = "",
    @ColumnInfo(name = "specifications")
    val specifications: String = "",
    @ColumnInfo(name = "is_featured")
    val isFeatured: Boolean = false,
    @ColumnInfo(name = "is_popular")
    val isPopular: Boolean = false,
    @ColumnInfo(name = "rating")
    val rating: Float = 0f
)

fun ProductEntity.toModel(): Product = Product(
    id = id,
    name = name,
    description = description,
    category = category,
    price = price,
    imageUrl = imageUrl,
    specifications = specifications,
    isFeatured = isFeatured,
    isPopular = isPopular,
    rating = rating
)
