package com.example.next.models

/**
 * Single source of truth for the authentication state of the app.
 *
 * Exposed by [com.example.next.data.repository.AuthRepository] and consumed by
 * ViewModels/UI. The UI must never manage authentication state with local
 * `remember` — every screen observes this state.
 */
sealed interface AuthState {
    /** Initial state — the app is still checking whether a user is signed in. */
    data object Loading : AuthState

    /** No signed-in user; the app is fully usable as a guest. */
    data object Guest : AuthState

    /** A real Firebase-authenticated user. */
    data class Authenticated(val user: UserAccount) : AuthState

    /** Authentication is unavailable (e.g. Firebase is not configured). */
    data object Error : AuthState
}

/**
 * The Firebase-authenticated account, independent of Firestore profile data.
 * `uid` is the primary identifier — never the email.
 */
data class UserAccount(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?
)