package com.example.next.data.session

import java.security.MessageDigest

/**
 * Deterministic cloud identifiers for orders.
 *
 * Firestore order documents are keyed by a CLIENT-GENERATED id — never the
 * Room autoincrement Long, which is device-local and would collide across
 * devices. Deterministic ids make every guest->account merge and every
 * offline flush retry idempotent: retrying produces the same id, so the
 * second attempt can never create a duplicate document.
 */
object MergeId {

    private const val GUEST_ID_PREFIX = "g_"

    /**
     * Cloud id for a guest-local order: `g_` + sha1(guestId:localId) prefix.
     * Stable for the lifetime of the local row, so a re-upload after a
     * mid-merge failure writes to the SAME document instead of duplicating.
     */
    fun guestOrderCloudId(guestId: String, localOrderId: Long): String =
        GUEST_ID_PREFIX + sha1Hex("$guestId:$localOrderId").take(20)

    /** sha1 of [input] as a lowercase hex string. */
    fun sha1Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
            .digest(input.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) {
            sb.append(HEX[(b.toInt() ushr 4) and 0x0F])
            sb.append(HEX[b.toInt() and 0x0F])
        }
        return sb.toString()
    }

    private const val HEX = "0123456789abcdef"
}
