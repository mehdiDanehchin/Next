package com.example.next.data.session

/**
 * Canonical owner identifiers for user-owned data.
 *
 * Every row of user-owned data in Room and every Firestore document belongs
 * to exactly one owner:
 *  - guest sessions: `guest:<uuid v4>` (device-local, never sent to Firestore)
 *  - authenticated sessions: `uid:<firebase uid>`
 *
 * The prefix makes the two namespaces collision-free: a Firebase uid can
 * never collide with a guest uuid (and vice versa), so `owner` works as a
 * single scoping column everywhere.
 */
object Owner {

    const val GUEST_PREFIX = "guest:"
    const val USER_PREFIX = "uid:"

    /** Owner string for a guest session (uuid v4 from DataStore). */
    fun guest(guestId: String): String = GUEST_PREFIX + guestId

    /** Owner string for an authenticated Firebase user. */
    fun user(uid: String): String = USER_PREFIX + uid

    /** True when [owner] belongs to an authenticated Firebase user. */
    fun isUser(owner: String): Boolean = owner.startsWith(USER_PREFIX)

    /** True when [owner] belongs to a device-local guest session. */
    fun isGuest(owner: String): Boolean = owner.startsWith(GUEST_PREFIX)

    /** The Firebase uid for a user owner, or null when [owner] is not a user owner. */
    fun uidOf(owner: String): String? =
        if (isUser(owner)) owner.removePrefix(USER_PREFIX) else null

    /** The guest uuid for a guest owner, or null when [owner] is not a guest owner. */
    fun guestIdOf(owner: String): String? =
        if (isGuest(owner)) owner.removePrefix(GUEST_PREFIX) else null
}
