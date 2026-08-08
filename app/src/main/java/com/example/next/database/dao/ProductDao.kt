package com.example.next.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.next.database.entity.ProductEntity

@Dao
interface ProductDao {

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): ProductEntity?

    @Query("SELECT * FROM products WHERE category = :category ORDER BY name ASC")
    suspend fun getProductsByCategory(category: String): List<ProductEntity>

    @Query("SELECT * FROM products WHERE is_featured = 1")
    suspend fun getFeaturedProducts(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE is_popular = 1")
    suspend fun getPopularProducts(): List<ProductEntity>

    @Query(
        "SELECT * FROM products WHERE name LIKE '%' || :query || '%' " +
            "OR description LIKE '%' || :query || '%' " +
            "OR category LIKE '%' || :query || '%' " +
            "OR specifications LIKE '%' || :query || '%' ORDER BY name ASC"
    )
    suspend fun searchProducts(query: String): List<ProductEntity>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int
}
