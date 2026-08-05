package com.example.next.models

data class Product(
    val id: Int = 0,
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val specifications: String = "",
    val isFeatured: Boolean = false,
    val isPopular: Boolean = false,
    val rating: Float = 0f
) {
    val formattedPrice: String get() = "$${String.format("%.2f", price)}"
}
