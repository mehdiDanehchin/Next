package com.example.next.data.repository

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.next.R
import com.example.next.models.AuthState
import com.example.next.models.UserAccount
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.IOException

/** Thrown when Firebase is not configured (e.g. google-services.json missing). */
class FirebaseNotConfiguredException : Exception("Firebase is not configured")

/** Thrown when the device has no Google account that can produce a credential. */
class NoGoogleCredentialException : Exception("No Google credential available")

/** Thrown when the returned credential is not a valid Google ID token. */
class InvalidGoogleCredentialException : Exception("Invalid Google credential")

/** Result of a Credential Manager based Google sign-in attempt. */
sealed interface SignInResult {
    data class Success(val user: UserAccount) : SignInResult
    data object Cancelled : SignInResult
    data class Failure(val cause: Throwable) : SignInResult
}

/**
 * Authentication repository — the single entry point for the account system.
 *
 * Flow: Google Account -> Credential Manager -> Google ID Token ->
 * Firebase Credential -> Firebase Authentication -> [UserAccount].
 *
 * Uses the modern Credential Manager API (androidx.credentials +
 * credentials-play-services-auth + googleid). The deprecated
 * GoogleSignInClient APIs are NOT used.
 */
class AuthRepository(context: Context) {

    private val appContext = context.applicationContext

    // Firebase is optional at runtime: when google-services.json is missing or
    // invalid, the whole auth feature degrades to [AuthState.Error] instead of
    // crashing the app at startup.
    private val firebaseAuth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()

    private val credentialManager: CredentialManager = CredentialManager.create(appContext)

    /**
     * Observes the Firebase auth state. Emits instantly on collection with the
     * current state (authenticated / guest) so the UI never shows a wrong
     * Guest frame while the check runs. Also re-emits on every sign-in/sign-out.
     */
    fun observeAuthState(): Flow<AuthState> = callbackFlow {
        val auth = firebaseAuth
        if (auth == null) {
            trySend(AuthState.Error)
            close()
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener {
            val user = it.currentUser
            trySend(user?.toAccount()?.let(AuthState::Authenticated) ?: AuthState.Guest)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /** The currently signed-in user, or null when signed out / not configured. */
    fun getCurrentUser(): UserAccount? = firebaseAuth?.currentUser?.toAccount()

    /**
     * Runs the full Google sign-in flow. Suspends until the user picks an
     * account (or cancels) and Firebase Authentication completes.
     *
     * @param activity host activity for the Credential Manager system UI
     */
    suspend fun signInWithGoogle(activity: Activity): SignInResult {
        val auth = firebaseAuth
        if (auth == null) {
            return SignInResult.Failure(FirebaseNotConfiguredException())
        }
        val idToken = obtainGoogleIdToken(activity)
            ?: return SignInResult.Cancelled // user dismissed the account picker
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val firebaseUser = auth.signInWithCredential(credential).await().user
            if (firebaseUser == null) {
                SignInResult.Failure(FirebaseAuthException("com.example.next", "Sign-in returned no user"))
            } else {
                SignInResult.Success(firebaseUser.toAccount())
            }
        } catch (e: FirebaseAuthException) {
            SignInResult.Failure(e)
        } catch (e: Exception) {
            SignInResult.Failure(e)
        }
    }

    /**
     * Signs out of Firebase and clears the Credential Manager state so the
     * next sign-in shows the full Google account picker again (no silent
     * re-sign-in of the previous account).
     */
    suspend fun signOut() {
        firebaseAuth?.signOut()
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }

    /**
     * Fetches a Google ID token via Credential Manager.
     *
     * @return the ID token, or `null` when the user cancelled the flow.
     * @throws NoGoogleCredentialException, InvalidGoogleCredentialException,
     *         IOException, GetCredentialCancellationException (on cancellation
     *         we return null instead) or FirebaseNotConfiguredException.
     */
    private suspend fun obtainGoogleIdToken(activity: Activity): String? {
        val serverClientId = runCatching {
            appContext.getString(R.string.default_web_client_id)
        }.getOrNull()
        if (serverClientId.isNullOrBlank()) {
            throw FirebaseNotConfiguredException()
        }

        val request = GetCredentialRequest.Builder()
            .setCredentialOptions(
                listOf(
                    GetGoogleIdOption.Builder()
                        .setServerClientId(serverClientId)
                        // Allow choosing any Google account, not only the ones
                        // that already signed in (supports account switching).
                        .setFilterByAuthorizedAccounts(false)
                        .build()
                )
            )
            .build()

        val response: GetCredentialResponse = try {
            credentialManager.getCredential(activity, request)
        } catch (e: GetCredentialCancellationException) {
            return null // intentional user cancellation — not an error
        } catch (e: NoCredentialException) {
            throw NoGoogleCredentialException().initCause(e)
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw e
        }

        val credential = response.credential
        if (credential !is androidx.credentials.CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw NoGoogleCredentialException()
        }
        return try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (e: GoogleIdTokenParsingException) {
            throw InvalidGoogleCredentialException().initCause(e)
        }
    }

    private fun FirebaseUser.toAccount() = UserAccount(
        uid = uid,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl?.toString()
    )
}