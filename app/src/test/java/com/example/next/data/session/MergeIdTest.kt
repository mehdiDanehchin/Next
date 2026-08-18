package com.example.next.data.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MergeIdTest {

    @Test
    fun `guest order id is deterministic`() {
        val a = MergeId.guestOrderCloudId("guest-uuid-1", 42L)
        val b = MergeId.guestOrderCloudId("guest-uuid-1", 42L)
        assertEquals(a, b)
        // Retrying a merge/upload produces the same document id -> no duplicates.
    }

    @Test
    fun `guest order id depends on both guest id and local id`() {
        assertNotEquals(
            MergeId.guestOrderCloudId("guest-uuid-1", 42L),
            MergeId.guestOrderCloudId("guest-uuid-2", 42L)
        )
        assertNotEquals(
            MergeId.guestOrderCloudId("guest-uuid-1", 42L),
            MergeId.guestOrderCloudId("guest-uuid-1", 43L)
        )
    }

    @Test
    fun `guest order id has g prefix and fixed length`() {
        val id = MergeId.guestOrderCloudId("guest-uuid-1", 7L)
        assertTrue(id.startsWith("g_"))
        // "g_" + 20 hex chars = 22, short enough to be a doc id, long enough to be unique.
        assertEquals(22, id.length)
    }

    @Test
    fun `sha1Hex is stable and lowercase hex`() {
        assertEquals(MergeId.sha1Hex("abc"), MergeId.sha1Hex("abc"))
        assertEquals(40, MergeId.sha1Hex("abc").length)
        assertTrue(MergeId.sha1Hex("abc").matches(Regex("[0-9a-f]{40}")))
    }
}
