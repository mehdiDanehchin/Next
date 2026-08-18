package com.example.next.data.sync

import com.example.next.database.entity.CartItemEntity
import com.example.next.database.entity.OrderEntity
import com.example.next.database.entity.OrderItemEntity
import com.example.next.database.entity.WishlistItemEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * JSON encoding of pending_ops payloads (org.json — Android built-in).
 *
 * A payload is a COMPLETE, self-contained description of one Firestore
 * mutation, so a flush can replay it even after the originating Room rows are
 * gone (account-switch purge). Fields mirror the Firestore document schema in
 * firebase/firestore.rules.
 */
object PayloadCodec {

    const val KEY_OP_ORDER = "order"
    const val KEY_OP_ITEMS = "items"
    const val KEY_STATUS = "status"
    const val KEY_PRODUCT_ID = "productId"
    const val KEY_PRODUCT_NAME = "productName"
    const val KEY_PRICE = "price"
    const val KEY_IMAGE_URL = "imageUrl"
    const val KEY_QUANTITY = "quantity"
    const val KEY_ORDER_DATE = "orderDate"
    const val KEY_FULL_NAME = "fullName"
    const val KEY_PHONE = "phone"
    const val KEY_ADDRESS = "address"
    const val KEY_CITY = "city"
    const val KEY_SHIPPING_METHOD = "shippingMethod"
    const val KEY_SHIPPING_PRICE = "shippingPrice"
    const val KEY_SUBTOTAL = "subtotal"
    const val KEY_TOTAL = "total"

    // ---------- encode (Room entity -> payload) ----------

    fun wishlistSet(item: WishlistItemEntity): JSONObject = JSONObject()
        .put(KEY_PRODUCT_ID, item.productId)
        .put(KEY_PRODUCT_NAME, item.productName)
        .put(KEY_PRICE, item.price)
        .put(KEY_IMAGE_URL, item.imageUrl)

    fun cartSet(item: CartItemEntity): JSONObject = JSONObject()
        .put(KEY_PRODUCT_ID, item.productId)
        .put(KEY_PRODUCT_NAME, item.productName)
        .put(KEY_PRICE, item.price)
        .put(KEY_IMAGE_URL, item.imageUrl)
        .put(KEY_QUANTITY, item.quantity)

    fun orderSet(order: OrderEntity, items: List<OrderItemEntity>): JSONObject {
        val orderJson = JSONObject()
            .put(KEY_ORDER_DATE, order.orderDate)
            .put(KEY_STATUS, order.status)
            .put(KEY_FULL_NAME, order.fullName)
            .put(KEY_PHONE, order.phone)
            .put(KEY_ADDRESS, order.address)
            .put(KEY_CITY, order.city)
            .put(KEY_SHIPPING_METHOD, order.shippingMethod)
            .put(KEY_SHIPPING_PRICE, order.shippingPrice)
            .put(KEY_SUBTOTAL, order.subtotal)
            .put(KEY_TOTAL, order.total)
        val itemsJson = JSONArray()
        items.forEach { item ->
            itemsJson.put(
                JSONObject()
                    .put(KEY_PRODUCT_ID, item.productId)
                    .put(KEY_PRODUCT_NAME, item.productName)
                    .put(KEY_PRICE, item.price)
                    .put(KEY_IMAGE_URL, item.imageUrl)
                    .put(KEY_QUANTITY, item.quantity)
            )
        }
        return JSONObject().put(KEY_OP_ORDER, orderJson).put(KEY_OP_ITEMS, itemsJson)
    }

    fun statusSet(status: String): JSONObject = JSONObject().put(KEY_STATUS, status)

    /** Empty object marks a delete op. */
    fun delete(): JSONObject = JSONObject()

    // ---------- decode (payload -> Firestore maps) ----------

    fun setMap(payload: String): Map<String, Any> = jsonToMap(JSONObject(payload))

    fun orderJson(payload: String): JSONObject = JSONObject(payload).getJSONObject(KEY_OP_ORDER)

    fun itemsArray(payload: String): JSONArray = JSONObject(payload).getJSONArray(KEY_OP_ITEMS)

    fun statusValue(payload: String): String = JSONObject(payload).getString(KEY_STATUS)

    private fun jsonToMap(json: JSONObject): Map<String, Any> = buildMap {
        json.keys().forEach { key ->
            val value = json.get(key)
            if (value is Number) {
                // org.json 20240303 parses fractional numbers as BigDecimal and
                // integral ones as Integer/Long; it also serializes integral
                // doubles ("449.0") as "449", losing the fractional type on
                // round-trip. Firestore accepts both int64 and double, and the
                // rules check `quantity is int` (int64), so: fractional -> Double,
                // integral -> Long.
                put(
                    key,
                    when (value) {
                        is Double, is Float -> value.toDouble()
                        is java.math.BigDecimal -> value.toDouble()
                        else -> (value as Number).toLong()
                    }
                )
            } else if (value is String) {
                put(key, value)
            } else if (value == JSONObject.NULL) {
                put(key, "")
            }
        }
    }
}