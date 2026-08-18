package com.example.next.models

/**
 * The user profile document stored in Firestore at `users/{uid}`.
 *
 * Firestore field mapping (kept symmetric with [toMap] so data can be
 * round-tripped and unit-tested without a Firestore instance):
 *
 * ```
 * users/{uid}
 *   ├── uid          : String   (mirrors FirebaseUser.uid — the primary key)
 *   ├── displayName  : String?
 *   ├── email        : String?
 *   ├── photoUrl     : String?
 *   ├── createdAt    : Long     (epoch millis, set once on first login)
 *   ├── lastLoginAt  : Long     (epoch millis, refreshed on every login)
 *   └── preferences  : Map<String, String>  (reserved for future account sync;
 *                                            theme currently stays local)
 * ```
 *
 * @param createdAt Never overwritten by later logins — [toMap] excludes it
 * except on first creation via [newUserMap].
 */
data class UserProfile(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val createdAt: Long,
    val lastLoginAt: Long,
    val preferences: Map<String, String> = emptyMap()
) {

    /**
     * Fields that are safe to merge into an existing document on every login.
     * [createdAt] is intentionally excluded so it is preserved.
     */
    fun syncedFieldsMap(): Map<String, Any> = mapOf(
        "displayName" to (displayName ?: ""),
        "email" to (email ?: ""),
        "photoUrl" to (photoUrl ?: ""),
        "lastLoginAt" to lastLoginAt
    )

    /** Full document for first-time creation (includes [createdAt]). */
    fun newUserMap(): Map<String, Any> = syncedFieldsMap() + mapOf(
        "uid" to uid,
        "createdAt" to createdAt,
        "preferences" to preferences
    )

    companion object {
        private const val KEY_UID = "uid"
        private const val KEY_DISPLAY_NAME = "displayName"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHOTO_URL = "photoUrl"
        private const val KEY_CREATED_AT = "createdAt"
        private const val KEY_LAST_LOGIN_AT = "lastLoginAt"
        private const val KEY_PREFERENCES = "preferences"

        /** Builds a [UserProfile] from a Firestore document snapshot. */
        fun fromMap(uid: String, data: Map<String, Any>): UserProfile? {
            val createdAt = (data[KEY_CREATED_AT] as? Number)?.toLong() ?: return null
            val lastLoginAt = (data[KEY_LAST_LOGIN_AT] as? Number)?.toLong() ?: createdAt
            @Suppress("UNCHECKED_CAST")
            val preferences = (data[KEY_PREFERENCES] as? Map<String, Any>)?.mapValues { it.value.toString() } ?: emptyMap()
            return UserProfile(
                uid = uid,
                displayName = data[KEY_DISPLAY_NAME] as? String,
                email = data[KEY_EMAIL] as? String,
                photoUrl = data[KEY_PHOTO_URL] as? String,
                createdAt = createdAt,
                lastLoginAt = lastLoginAt,
                preferences = preferences
            )
        }
    }
}