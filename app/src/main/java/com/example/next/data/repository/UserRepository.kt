package com.example.next.data.repository

import com.example.next.models.UserAccount
import com.example.next.models.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Firestore-backed user profile repository.
 *
 * Documents live at `users/{uid}` — the Firebase [UserAccount.uid] is the
 * primary key (never the email). Reads and writes are scoped to the current
 * uid, and the server security rules (see firebase/firestore.rules) enforce
 * that each user can only access their own document.
 */
class UserRepository(private val firestore: FirebaseFirestore?) {

    private fun usersCollection() = firestore?.collection("users")

    /**
     * Live profile for [uid]. Emits null when the document does not exist yet
     * and when Firestore is unavailable (offline / not configured) — the UI
     * then falls back to the Google account data from the auth state.
     */
    fun observeUserProfile(uid: String): Flow<UserProfile?> {
        val collection = usersCollection() ?: return flowOf(null)
        return callbackFlow {
            val registration = collection.document(uid).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val profile = snapshot
                    ?.takeIf { it.exists() }
                    ?.let { UserProfile.fromMap(uid, it.data.orEmpty()) }
                trySend(profile)
            }
            awaitClose { registration.remove() }
        }
    }

    /**
     * Creates `users/{uid}` on first login, or updates the syncable fields
     * (displayName, email, photoUrl, lastLoginAt) on later logins. [UserProfile.createdAt]
     * is never overwritten — it is only written by [UserProfile.newUserMap] on creation.
     */
    suspend fun ensureUserProfile(user: UserAccount) {
        val fs = firestore ?: throw FirebaseNotConfiguredException()
        val docRef = fs.collection("users").document(user.uid)
        val now = System.currentTimeMillis()

        fs.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            if (snapshot.exists()) {
                val profile = UserProfile(
                    uid = user.uid,
                    displayName = user.displayName,
                    email = user.email,
                    photoUrl = user.photoUrl,
                    createdAt = (snapshot.get("createdAt") as? Number)?.toLong() ?: now,
                    lastLoginAt = now
                )
                transaction.update(docRef, profile.syncedFieldsMap())
            } else {
                val profile = UserProfile(
                    uid = user.uid,
                    displayName = user.displayName,
                    email = user.email,
                    photoUrl = user.photoUrl,
                    createdAt = now,
                    lastLoginAt = now
                )
                transaction.set(docRef, profile.newUserMap())
            }
        }
    }
}