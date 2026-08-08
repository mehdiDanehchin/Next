package com.example.next.models

import java.util.Locale

data class WishlistItem(
    val id: Int = 0,
    val productId: Int = 0,
    val productName: String = "",
    val price: Double = 0.0,
    val imageUrl: String = ""
) {
    val formattedPrice: String get() = "$${String.format(Locale.US, "%.2f", price)}"
}
