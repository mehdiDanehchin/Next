package com.example.next.viewmodel

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.next.R
import com.example.next.data.repository.FirebaseNotConfiguredException
import com.example.next.data.repository.InvalidGoogleCredentialException
import com.example.next.data.repository.NoGoogleCredentialException
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import java.net.UnknownHostException

/**
 * Categorizes auth-related exceptions into user-facing error kinds.
 *
 * Pure function (no Android dependencies) so it is unit-testable on the JVM.
 * The UI string for each kind is resolved in [toStringRes] — messages stay in
 * localization resources and are never technical exception names.
 */
enum class AuthErrorKind {
    /** User intentionally dismissed the account picker — no error shown. */
    CANCELLED,

    NO_GOOGLE_ACCOUNT,
    NETWORK,
    INVALID_CREDENTIAL,
    AUTH_FAILED,
    NOT_CONFIGURED,
    PROFILE_SYNC,
    UNKNOWN
}

/**
 * Maps exceptions thrown by the auth/Firestore layer to [AuthErrorKind].
 */
object AuthErrorMapper {

    fun kindOf(throwable: Throwable): AuthErrorKind = kindOfClass(throwable.javaClass)

    /**
     * Class-based variant of [kindOf] — identical dispatch rules, but takes
     * the exception type instead of an instance. This makes the mapping
     * testable on the JVM: Firebase exception classes (FirebaseNetworkException,
     * FirebaseAuthException, FirebaseFirestoreException, ...) cannot be
     * *constructed* in plain JVM unit tests because their constructors and
     * static initializers call Android framework code (TextUtils, SparseArray)
     * that the unit-test android.jar stubs do not implement. A class literal
     * only *loads* the class — it never runs initializers — so the branches
     * are fully covered without Robolectric.
     */
    internal fun kindOfClass(clazz: Class<out Throwable>): AuthErrorKind = when {
        GetCredentialCancellationException::class.java.isAssignableFrom(clazz) -> AuthErrorKind.CANCELLED
        NoCredentialException::class.java.isAssignableFrom(clazz) ||
            NoGoogleCredentialException::class.java.isAssignableFrom(clazz) -> AuthErrorKind.NO_GOOGLE_ACCOUNT
        FirebaseNetworkException::class.java.isAssignableFrom(clazz) ||
            UnknownHostException::class.java.isAssignableFrom(clazz) ||
            IOException::class.java.isAssignableFrom(clazz) -> AuthErrorKind.NETWORK
        // Order matters: the FirebaseAuthInvalid* classes extend FirebaseAuthException,
        // so INVALID_CREDENTIAL must be tested before AUTH_FAILED.
        GoogleIdTokenParsingException::class.java.isAssignableFrom(clazz) ||
            InvalidGoogleCredentialException::class.java.isAssignableFrom(clazz) ||
            FirebaseAuthInvalidCredentialsException::class.java.isAssignableFrom(clazz) ||
            FirebaseAuthInvalidUserException::class.java.isAssignableFrom(clazz) -> AuthErrorKind.INVALID_CREDENTIAL
        FirebaseNotConfiguredException::class.java.isAssignableFrom(clazz) -> AuthErrorKind.NOT_CONFIGURED
        FirebaseFirestoreException::class.java.isAssignableFrom(clazz) -> AuthErrorKind.PROFILE_SYNC
        FirebaseAuthException::class.java.isAssignableFrom(clazz) -> AuthErrorKind.AUTH_FAILED
        else -> AuthErrorKind.UNKNOWN
    }

    /** Resource id of the user-facing message; null means "show nothing". */
    fun toStringRes(kind: AuthErrorKind): Int? = when (kind) {
        AuthErrorKind.CANCELLED -> null
        AuthErrorKind.NO_GOOGLE_ACCOUNT -> R.string.error_signin_no_account
        AuthErrorKind.NETWORK -> R.string.error_signin_network
        AuthErrorKind.INVALID_CREDENTIAL -> R.string.error_signin_failed
        AuthErrorKind.AUTH_FAILED -> R.string.error_signin_failed
        AuthErrorKind.NOT_CONFIGURED -> R.string.error_signin_unavailable
        AuthErrorKind.PROFILE_SYNC -> R.string.error_profile_sync
        AuthErrorKind.UNKNOWN -> R.string.error_signin_failed
    }
}