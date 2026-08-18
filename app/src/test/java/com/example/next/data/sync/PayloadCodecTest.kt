package com.example.next.data.sync

import com.example.next.database.entity.CartItemEntity
import com.example.next.database.entity.OrderEntity
import com.example.next.database.entity.OrderItemEntity
import com.example.next.database.entity.WishlistItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the pending_ops payload codec. The payload is the ONLY thing
 * an outbox flush has after the originating Room rows are gone, so round-trip
 * fidelity is a correctness requirement, not a nicety.
 */
class PayloadCodecTest {

    private val wishlistItem = WishlistItemEntity(
        id = 1, productId = 7, productName = "Galaxy Phone",
        price = 999.5, imageUrl = "https://x/img.png", owner = "uid:u1"
    )

    private val cartItem = CartItemEntity(
        id = 2, productId = 8, productName = "Pixel Tablet",
        price = 449.0, imageUrl = "https://x/t.png", quantity = 3, owner = "uid:u1"
    )

    private val order = OrderEntity(
        id = 10, orderDate = "2026-08-18 12:00", status = "PENDING",
        fullName = "Ali", phone = "0912", address = "Tehran", city = "Tehran",
        shippingMethod = "STANDARD", shippingPrice = 5.99, subtotal = 100.0,
        total = 105.99, owner = "uid:u1", cloudId = "g_abc"
    )

    private val orderItem = OrderItemEntity(
        id = 1, orderId = 10, productId = 7, productName = "Galaxy Phone",
        price = 100.0, imageUrl = "https://x/img.png", quantity = 1
    )

    @Test
    fun `wishlist payload round-trips every field`() {
        val json = PayloadCodec.wishlistSet(wishlistItem)
        assertEquals(7, json.getInt("productId"))
        assertEquals("Galaxy Phone", json.getString("productName"))
        assertEquals(999.5, json.getDouble("price"), 0.001)
        assertEquals("https://x/img.png", json.getString("imageUrl"))
    }

    @Test
    fun `cart payload round-trips quantity as int`() {
        val json = PayloadCodec.cartSet(cartItem)
        assertEquals(8, json.getInt("productId"))
        assertEquals(3, json.getInt("quantity"))
        assertEquals(449.0, json.getDouble("price"), 0.001)
    }

    @Test
    fun `order payload carries order and items`() {
        val json = PayloadCodec.orderSet(order, listOf(orderItem))
        val orderJson = json.getJSONObject("order")
        // cloudId is the document id, not a payload field.
        assertFalse(orderJson.has("cloudId"))
        assertEquals("PENDING", orderJson.getString("status"))
        assertEquals("Ali", orderJson.getString("fullName"))
        assertEquals(105.99, orderJson.getDouble("total"), 0.001)
        val items = json.getJSONArray("items")
        assertEquals(1, items.length())
        val item = items.getJSONObject(0)
        assertEquals(7, item.getInt("productId"))
        assertEquals(1, item.getInt("quantity"))
    }

    @Test
    fun `setMap decodes to Firestore-compatible types`() {
        val map = PayloadCodec.setMap(PayloadCodec.cartSet(cartItem).toString())
        // Integral numbers decode as Long (Firestore int64 — rules `is int`).
        assertEquals(8L, map["productId"])
        assertEquals(3L, map["quantity"])
        // Fractional prices keep their type.
        val fractional = PayloadCodec.setMap(PayloadCodec.cartSet(cartItem.copy(price = 449.5)).toString())
        assertTrue(fractional["price"] is Double)
        assertEquals("Pixel Tablet", map["productName"])
    }

    @Test
    fun `status payload and delete payload`() {
        assertEquals("CANCELLED", PayloadCodec.statusValue(PayloadCodec.statusSet("CANCELLED").toString()))
        assertTrue(PayloadCodec.delete().toString() == "{}")
    }

    @Test
    fun `order decode reads nested fields`() {
        val payload = PayloadCodec.orderSet(order, listOf(orderItem)).toString()
        val orderJson = PayloadCodec.orderJson(payload)
        assertEquals("Tehran", orderJson.getString("city"))
        assertEquals(5.99, orderJson.getDouble("shippingPrice"), 0.001)
        val items = PayloadCodec.itemsArray(payload)
        assertEquals(1, items.length())
        assertEquals("Galaxy Phone", items.getJSONObject(0).getString("productName"))
    }
}