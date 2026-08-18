package com.example.next.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.next.R
import com.example.next.data.repository.AuthRepository
import com.example.next.data.repository.SignInResult
import com.example.next.data.repository.UserRepository
import com.example.next.models.AuthState
import com.example.next.models.UserAccount
import com.example.next.models.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state of the Profile screen.
 *
 * [message] carries a one-shot user-facing error as a string resource id
 * (null = nothing to show). Cancelled sign-ins never produce a message.
 */
data class ProfileUiState(
    val authState: AuthState = AuthState.Loading,
    val profile: UserProfile? = null,
    val isSigningIn: Boolean = false,
    val message: Int? = null
)

/**
 * Profile screen state holder.
 *
 * Authentication state is a single source of truth derived from
 * [AuthRepository.observeAuthState] and shared with the whole app; the
 * Firestore profile follows the authenticated uid automatically.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val isSigningIn = MutableStateFlow(false)
    private val message = MutableStateFlow<Int?>(null)

    private val authState: StateFlow<AuthState> = authRepository.observeAuthState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AuthState.Loading
        )

    // Profile of the CURRENTLY authenticated uid only. On sign-out (or when a
    // different account signs in) the previous user's data is dropped: the
    // flatMapLatest key is the uid itself, so user A's document can never
    // appear under user B.
    private val profile: StateFlow<UserProfile?> = authState
        .mapNotNull { (it as? AuthState.Authenticated)?.user?.uid }
        .distinctUntilChanged()
        .flatMapLatest { uid -> userRepository.observeUserProfile(uid) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val uiState: StateFlow<ProfileUiState> = combine(authState, profile, isSigningIn, message) {
            auth, profile, signingIn, msg ->
        ProfileUiState(
            authState = auth,
            profile = profile,
            isSigningIn = signingIn,
            message = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState()
    )

    /**
     * Starts the Google sign-in flow. Safe to call repeatedly: while a
     * sign-in is in flight the button is disabled and further calls are
     * ignored. Errors are mapped to localized messages; user cancellation is
     * silent.
     */
    fun signInWithGoogle(activity: Activity) {
        if (isSigningIn.value) return
        viewModelScope.launch {
            isSigningIn.value = true
            message.value = null
            when (val result = authRepository.signInWithGoogle(activity)) {
                is SignInResult.Success -> ensureProfileSynced(result.user)
                SignInResult.Cancelled -> Unit // intentional — no error shown
                is SignInResult.Failure ->
                    message.value = AuthErrorMapper.toStringRes(AuthErrorMapper.kindOf(result.cause))
            }
            isSigningIn.value = false
        }
    }

    /** Signs out Firebase + clears the Credential Manager state. */
    fun signOut() {
        if (isSigningIn.value) return
        viewModelScope.launch { authRepository.signOut() }
    }

    /** Clears the one-shot error message (e.g. after it was displayed). */
    fun onMessageShown() {
        message.value = null
    }

    private suspend fun ensureProfileSynced(user: UserAccount) {
        try {
            userRepository.ensureUserProfile(user)
        } catch (e: Exception) {
            // Stay signed in; the profile card still shows the Google account
            // data. Only inform the user that the Firestore sync failed.
            message.value = AuthErrorMapper.toStringRes(AuthErrorMapper.kindOf(e))
                ?: R.string.error_profile_sync
        }
    }
}