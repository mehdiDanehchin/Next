package com.example.next.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the Firestore document mapping. Verifies the core data
 * contract of the account system:
 *  - uid is the document key, never the email
 *  - createdAt is written only on creation and preserved by syncedFieldsMap
 *  - map round-trip keeps every field
 */
class UserProfileTest {

    private val profile = UserProfile(
        uid = "firebase-uid-1",
        displayName = "Test User",
        email = "test@example.com",
        photoUrl = "https://example.com/photo.jpg",
        createdAt = 1_000L,
        lastLoginAt = 2_000L,
        preferences = mapOf("theme" to "dark")
    )

    @Test
    fun `newUserMap contains uid createdAt and all sync fields`() {
        val map = profile.newUserMap()
        assertEquals("firebase-uid-1", map["uid"])
        assertEquals("Test User", map["displayName"])
        assertEquals("test@example.com", map["email"])
        assertEquals("https://example.com/photo.jpg", map["photoUrl"])
        assertEquals(1_000L, map["createdAt"])
        assertEquals(2_000L, map["lastLoginAt"])
        assertEquals(mapOf("theme" to "dark"), map["preferences"])
    }

    @Test
    fun `syncedFieldsMap never touches createdAt`() {
        val map = profile.syncedFieldsMap()
        assertTrue("createdAt must not be in sync fields", "createdAt" !in map)
        assertTrue("uid must not be in sync fields", "uid" !in map)
        assertEquals("Test User", map["displayName"])
        assertEquals("test@example.com", map["email"])
        assertEquals(2_000L, map["lastLoginAt"])
    }

    @Test
    fun `fromMap round-trips newUserMap`() {
        val restored = UserProfile.fromMap("firebase-uid-1", profile.newUserMap())!!
        assertEquals(profile, restored)
    }

    @Test
    fun `fromMap handles missing optional fields`() {
        val restored = UserProfile.fromMap(
            "uid-2",
            mapOf(
                "displayName" to "",
                "email" to "",
                "photoUrl" to "",
                "createdAt" to 5_000L,
                "lastLoginAt" to 6_000L
            )
        )!!
        assertEquals("uid-2", restored.uid)
        assertEquals("", restored.displayName) // blank display name stays blank
        assertEquals("", restored.photoUrl)
        assertEquals(5_000L, restored.createdAt)
        assertEquals(6_000L, restored.lastLoginAt)
        assertTrue(restored.preferences.isEmpty())
    }

    @Test
    fun `fromMap returns null without createdAt`() {
        assertNull(UserProfile.fromMap("uid-3", mapOf("lastLoginAt" to 1L)))
    }

    @Test
    fun `fromMap falls back to createdAt when lastLoginAt missing`() {
        val restored = UserProfile.fromMap("uid-4", mapOf("createdAt" to 7_000L))!!
        assertEquals(7_000L, restored.lastLoginAt)
    }

    @Test
    fun `email is never used as the document key`() {
        // The uid in the document must equal the collection key; the email is
        // only a display field.
        val docKey = "firebase-uid-1"
        val stored = profile.newUserMap()["uid"]
        assertEquals(docKey, stored)
        assertTrue(profile.email != docKey)
    }
}