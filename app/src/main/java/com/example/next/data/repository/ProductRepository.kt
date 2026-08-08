package com.example.next.data.repository

import com.example.next.database.dao.ProductDao
import com.example.next.database.entity.toModel
import com.example.next.models.Product

/**
 * Single source of truth for the product catalog.
 */
class ProductRepository(private val productDao: ProductDao) {

    suspend fun getProductById(id: Int): Product? =
        productDao.getProductById(id)?.toModel()

    suspend fun getFeaturedProducts(): List<Product> =
        productDao.getFeaturedProducts().map { it.toModel() }

    suspend fun getPopularProducts(): List<Product> =
        productDao.getPopularProducts().map { it.toModel() }

    suspend fun getProductsByCategory(category: String): List<Product> =
        productDao.getProductsByCategory(category).map { it.toModel() }

    suspend fun searchProducts(query: String): List<Product> =
        productDao.searchProducts(query).map { it.toModel() }
}