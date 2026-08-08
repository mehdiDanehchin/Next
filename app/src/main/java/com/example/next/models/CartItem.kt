package com.example.next.models

import java.util.Locale

data class CartItem(
    val id: Int = 0,
    val productId: Int = 0,
    val productName: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val quantity: Int = 1
) {
    val formattedPrice: String get() = "$${String.format(Locale.US, "%.2f", price)}"
    val formattedTotal: String get() = "$${String.format(Locale.US, "%.2f", price * quantity)}"
}
