package com.example.next.data.session

import com.example.next.database.AppDatabase

/**
 * Backfills ownership on databases migrated from v4 (or older).
 *
 * The v4->v5 Migration cannot compute owners itself — the guest id is
 * generated at runtime — so pre-v5 rows keep the `owner = ''` placeholder
 * until this runs at startup. Rows created before v5 belong to "this
 * install's guest", so they are re-owned to the current guest id and get a
 * deterministic cloud id (they will be merged into an account on first login).
 *
 * Idempotent: only touches rows that still carry the placeholder.
 */
class OwnershipBackfill(
    private val database: AppDatabase,
    private val sessionManager: SessionManager
) {

    suspend fun run() {
        val guestOwner = Owner.guest(sessionManager.guestId())
        val db = database.openHelper.writableDatabase

        // Cloud ids first: deterministic per (guestId, local id) so a later
        // merge/upload can never duplicate an order document.
        val legacyOrderIds = db.query(
            "SELECT `id` FROM `orders` WHERE `cloud_id` = ''"
        ).use { cursor ->
            buildList<Long> {
                while (cursor.moveToNext()) add(cursor.getLong(0))
            }
        }
        legacyOrderIds.forEach { localId ->
            val cloudId = MergeId.guestOrderCloudId(guestIdOf(guestOwner), localId)
            db.execSQL(
                "UPDATE `orders` SET `cloud_id` = ? WHERE `id` = ?",
                arrayOf<Any>(cloudId, localId)
            )
        }

        db.execSQL("UPDATE `wishlist` SET `owner` = ? WHERE `owner` = ''", arrayOf(guestOwner))
        db.execSQL("UPDATE `cart` SET `owner` = ? WHERE `owner` = ''", arrayOf(guestOwner))
        db.execSQL("UPDATE `orders` SET `owner` = ? WHERE `owner` = ''", arrayOf(guestOwner))
    }

    private fun guestIdOf(guestOwner: String): String = Owner.guestIdOf(guestOwner)!!
}