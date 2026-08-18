package com.example.next.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.next.data.repository.AuthRepository
import com.example.next.models.AuthState
import java.util.UUID
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// File-level delegate: a DataStore instance must be a singleton per file,
// otherwise multiple instances crash the app (the same rule as next_settings).
private val Context.sessionDataStore by preferencesDataStore(name = "next_session")

/**
 * Single source of truth for "who owns the current user data".
 *
 * The active owner is derived reactively from the Firebase auth state:
 *  - authenticated: `uid:<firebase uid>`
 *  - guest (and auth-unavailable): `guest:<uuid v4>`
 *
 * The guest uuid is generated once per install, persisted in DataStore,
 * survives switching between guest and account, and dies with a data wipe.
 * Guest ids are LOCAL ONLY — they are never sent to Firestore.
 *
 * Repositories observe [observeActiveOwner] and re-scope every query with
 * flatMapLatest, so the UI can never show the previous owner's data after an
 * account switch.
 */
class SessionManager(
    context: Context,
    private val authRepository: AuthRepository
) {

    private val dataStore = context.applicationContext.sessionDataStore

    private val guestIdKey = stringPreferencesKey("guest_id")

    /** Which uid this install's guest data was merged into (absent = not merged yet). */
    private val guestMergedIntoKey = stringPreferencesKey("guest_merged_into")

    @Volatile
    private var cachedGuestId: String? = null

    private val guestIdMutex = Mutex()

    /**
     * The device-local guest uuid. Generated and persisted on first call,
     * cached afterwards so [observeActiveOwner] can map synchronously.
     */
    suspend fun guestId(): String {
        cachedGuestId?.let { return it }
        return guestIdMutex.withLock {
            cachedGuestId?.let { return it }
            val existing = dataStore.data.first()[guestIdKey]
            val id = existing ?: UUID.randomUUID().toString()
            if (existing == null) {
                dataStore.edit { prefs -> prefs[guestIdKey] = id }
            }
            cachedGuestId = id
            id
        }
    }

    /**
     * Emits the active owner on every auth-state change (guest -> user,
     * user -> guest, user A -> user B). Also emits on collection so the UI
     * never waits for the first auth event.
     */
    fun observeActiveOwner(): Flow<String> = callbackFlow {
        val job = launch {
            val gid = guestId()
            authRepository.observeAuthState().collect { state ->
                trySend(
                    when (state) {
                        is AuthState.Authenticated -> Owner.user(state.user.uid)
                        else -> Owner.guest(gid)
                    }
                )
            }
        }
        awaitClose { job.cancel() }
    }

    /** True once this install's guest data has been merged into an account. */
    suspend fun isGuestMerged(): Boolean =
        dataStore.data.first().contains(guestMergedIntoKey)

    /** Marks the guest->account merge as done for this install (idempotent). */
    suspend fun markGuestMerged(uid: String) {
        dataStore.edit { prefs -> prefs[guestMergedIntoKey] = uid }
    }
}

/** Current owner for one-shot mutations (waits for the very first emission). */
suspend fun SessionManager.ownerNow(): String = observeActiveOwner().first()
