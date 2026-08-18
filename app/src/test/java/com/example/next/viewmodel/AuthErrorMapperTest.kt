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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

/**
 * JVM tests for the exception -> user-facing message mapping.
 * No Android framework code involved — pure logic.
 *
 * Firebase exception types are asserted via [AuthErrorMapper.kindOfClass]
 * (class literals), because their instances cannot be constructed on the
 * JVM: their constructors / static initializers call Android framework
 * code (TextUtils, SparseArray) that the unit-test android.jar stubs do
 * not implement. A class literal only loads the class — it never runs
 * initializers — so every dispatch branch is covered without Robolectric.
 */
class AuthErrorMapperTest {

    @Test
    fun `user cancellation maps to CANCELLED and no message`() {
        val kind = AuthErrorMapper.kindOf(GetCredentialCancellationException())
        assertEquals(AuthErrorKind.CANCELLED, kind)
        assertNull(AuthErrorMapper.toStringRes(kind))
    }

    @Test
    fun `no credential maps to NO_GOOGLE_ACCOUNT`() {
        assertEquals(
            AuthErrorKind.NO_GOOGLE_ACCOUNT,
            AuthErrorMapper.kindOf(NoCredentialException())
        )
        assertEquals(
            AuthErrorKind.NO_GOOGLE_ACCOUNT,
            AuthErrorMapper.kindOf(NoGoogleCredentialException())
        )
    }

    @Test
    fun `network errors map to NETWORK`() {
        assertEquals(AuthErrorKind.NETWORK, AuthErrorMapper.kindOfClass(FirebaseNetworkException::class.java))
        assertEquals(AuthErrorKind.NETWORK, AuthErrorMapper.kindOf(UnknownHostException()))
        assertEquals(AuthErrorKind.NETWORK, AuthErrorMapper.kindOf(IOException("boom")))
    }

    @Test
    fun `invalid credentials map to INVALID_CREDENTIAL`() {
        assertEquals(
            AuthErrorKind.INVALID_CREDENTIAL,
            AuthErrorMapper.kindOf(GoogleIdTokenParsingException(RuntimeException("bad token")))
        )
        assertEquals(
            AuthErrorKind.INVALID_CREDENTIAL,
            AuthErrorMapper.kindOf(InvalidGoogleCredentialException())
        )
        assertEquals(
            AuthErrorKind.INVALID_CREDENTIAL,
            AuthErrorMapper.kindOfClass(FirebaseAuthInvalidCredentialsException::class.java)
        )
        assertEquals(
            AuthErrorKind.INVALID_CREDENTIAL,
            AuthErrorMapper.kindOfClass(FirebaseAuthInvalidUserException::class.java)
        )
    }

    @Test
    fun `firebase auth failure maps to AUTH_FAILED`() {
        assertEquals(
            AuthErrorKind.AUTH_FAILED,
            AuthErrorMapper.kindOfClass(FirebaseAuthException::class.java)
        )
    }

    @Test
    fun `missing firebase config maps to NOT_CONFIGURED`() {
        assertEquals(
            AuthErrorKind.NOT_CONFIGURED,
            AuthErrorMapper.kindOf(FirebaseNotConfiguredException())
        )
    }

    @Test
    fun `firestore failure maps to PROFILE_SYNC`() {
        assertEquals(
            AuthErrorKind.PROFILE_SYNC,
            AuthErrorMapper.kindOfClass(FirebaseFirestoreException::class.java)
        )
    }

    @Test
    fun `unknown errors fall back to UNKNOWN`() {
        assertEquals(AuthErrorKind.UNKNOWN, AuthErrorMapper.kindOf(IllegalStateException("?")))
    }

    @Test
    fun `every non-cancelled kind maps to a known localized message resource`() {
        val knownResources = setOf(
            R.string.error_signin_failed,
            R.string.error_signin_network,
            R.string.error_signin_no_account,
            R.string.error_signin_unavailable,
            R.string.error_profile_sync
        )
        for (kind in AuthErrorKind.entries) {
            if (kind == AuthErrorKind.CANCELLED) continue
            val res = AuthErrorMapper.toStringRes(kind)
            assertNotNull("kind $kind must map to a message", res)
            assertTrue("kind $kind must map to a known resource", res in knownResources)
        }
    }
}