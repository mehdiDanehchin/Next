package com.example.next.data.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerTest {

    private val guestId = "550e8400-e29b-41d4-a716-446655440000"
    private val uid = "abc123xyz"

    @Test
    fun `guest owner carries prefix`() {
        assertEquals("guest:$guestId", Owner.guest(guestId))
    }

    @Test
    fun `user owner carries prefix`() {
        assertEquals("uid:$uid", Owner.user(uid))
    }

    @Test
    fun `guest and user ids never collide`() {
        // A uuid-shaped string as a Firebase uid must still be distinguishable.
        assertFalse(Owner.isUser(Owner.guest(uid)))
        assertTrue(Owner.isGuest(Owner.guest(uid)))
        assertTrue(Owner.isUser(Owner.user(guestId)))
        assertFalse(Owner.isGuest(Owner.user(guestId)))
    }

    @Test
    fun `uidOf extracts uid only for user owners`() {
        assertEquals(uid, Owner.uidOf("uid:$uid"))
        assertNull(Owner.uidOf("guest:$uid"))
        assertNull(Owner.uidOf(""))
    }

    @Test
    fun `guestIdOf extracts uuid only for guest owners`() {
        assertEquals(guestId, Owner.guestIdOf("guest:$guestId"))
        assertNull(Owner.guestIdOf("uid:$guestId"))
    }

    @Test
    fun `empty owner is neither`() {
        assertFalse(Owner.isUser(""))
        assertFalse(Owner.isGuest(""))
    }
}
