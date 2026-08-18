package com.example.next.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The guest->account merge data policy (spec: cart sums with a 99 cap;
 * wishlist/orders never lose account data).
 */
class MergePolicyTest {

    @Test
    fun `cart quantities sum`() {
        assertEquals(7, MergePolicy.mergedCartQuantity(5, 2))
        assertEquals(1, MergePolicy.mergedCartQuantity(0, 1))
    }

    @Test
    fun `cart sum caps at 99`() {
        assertEquals(99, MergePolicy.mergedCartQuantity(98, 5))
        assertEquals(99, MergePolicy.mergedCartQuantity(0, 150))
        assertEquals(99, MergePolicy.mergedCartQuantity(99, 1))
    }

    @Test
    fun `reown decision drops colliding guest rows`() {
        assertTrue(MergePolicy.shouldReownGuestRow(accountAlreadyHasProduct = false))
        assertFalse(MergePolicy.shouldReownGuestRow(accountAlreadyHasProduct = true))
    }
}
