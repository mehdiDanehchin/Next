package com.example.next.models

import java.util.Locale

/**
 * Order lifecycle. Stored in the DB as the enum [code]; UI maps it to a
 * localized label and color. A new order starts at [PENDING]; the user can
 * cancel it while it is still pending.
 */
enum class OrderStatus(val code: String) {
    PENDING("PENDING"),
    PROCESSING("PROCESSING"),
    SHIPPED("SHIPPED"),
    DELIVERED("DELIVERED"),
    CANCELLED("CANCELLED");

    companion object {
        fun fromCode(code: String): OrderStatus =
            entries.firstOrNull { it.code == code } ?: PENDING
    }
}

/** Delivery options offered at checkout, each with a fixed price. */
enum class ShippingMethod(val code: String, val price: Double) {
    STANDARD("STANDARD", 5.99),
    EXPRESS("EXPRESS", 12.99),
    PICKUP("PICKUP", 0.0);

    companion object {
        fun fromCode(code: String): ShippingMethod =
            entries.firstOrNull { it.code == code } ?: STANDARD
    }
}

/** A placed order: shipping address + totals snapshot (data survives product changes). */
data class Order(
    val id: Long = 0,
    val orderDate: String = "",
    val status: String = OrderStatus.PENDING.code,
    val fullName: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val shippingMethod: String = ShippingMethod.STANDARD.code,
    val shippingPrice: Double = 0.0,
    val subtotal: Double = 0.0,
    val total: Double = 0.0
) {
    val statusEnum: OrderStatus get() = OrderStatus.fromCode(status)
    val shippingMethodEnum: ShippingMethod get() = ShippingMethod.fromCode(shippingMethod)
}

/** Line item snapshot of a product at purchase time. */
data class OrderItem(
    val id: Long = 0,
    val orderId: Long = 0,
    val productId: Int = 0,
    val productName: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val quantity: Int = 1
) {
    val formattedTotal: String get() = "$${String.format(Locale.US, "%.2f", price * quantity)}"
}
