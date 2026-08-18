package com.example.next.data.sync

import com.example.next.data.session.Owner
import com.example.next.data.session.SessionManager
import com.example.next.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Watches the active owner and enforces the account-switching rules:
 *
 *  1. user -> guest   (logout): the previous user's Room rows are HARD-PURGED
 *     (their source of truth is Firestore) — the guest session starts empty.
 *  2. user A -> user B (switch): A's local cache is purged the same way — the
 *     UI can never show A's data under B, even through a buggy future query.
 *  3. guest -> user   (login): nothing is purged here — the guest rows are
 *     merged into the account by the merge hook (see onGuestToUser) and that
 *     hook owns the data.
 *
 * Purge ordering: the flush hook best-effort uploads the previous owner's
 * pending outbox first; the outbox is only deleted when the flush succeeded
 * (otherwise the self-contained ops survive until that uid signs in again).
 */
class AccountSwitchHandler(
    private val sessionManager: SessionManager,
    private val database: AppDatabase,
    private val scope: CoroutineScope,
    /** Best-effort flush of the previous owner's outbox; true when clean. */
    private val flushHook: suspend (String) -> Boolean = { true },
    /** Runs once per guest->user transition (Phase 5: idempotent guest merge). */
    private val onGuestToUser: suspend (uid: String) -> Unit = {}
) {

    @Volatile
    private var lastOwner: String? = null

    fun start() {
        scope.launch {
            sessionManager.observeActiveOwner().collect { owner ->
                val previous = lastOwner
                lastOwner = owner
                if (previous == null || previous == owner) return@collect

                when {
                    Owner.isGuest(previous) && Owner.isUser(owner) -> {
                        runCatching { onGuestToUser(Owner.uidOf(owner)!!) }
                    }
                    Owner.isUser(previous) -> {
                        handleUserLeft(previous)
                    }
                    else -> Unit // guest -> guest: nothing to do
                }
            }
        }
    }

    private suspend fun handleUserLeft(owner: String) {
        withContext(Dispatchers.IO) {
            val flushed = flushHook(owner)
            database.wishlistDao().deleteAllForOwner(owner)
            database.cartDao().deleteAllForOwner(owner)
            database.orderDao().deleteAllForOwner(owner)
            if (flushed) {
                database.pendingOpsDao().deleteForOwner(owner)
            }
        }
    }
}