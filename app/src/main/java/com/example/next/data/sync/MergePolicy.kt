package com.example.next.data.sync

/**
 * Pure merge semantics for the guest -> account merge (unit-testable without
 * Firestore). These are the spec's data policies:
 *  - cart: quantity Guest + quantity Account, capped at 99
 *  - wishlist/orders: union/append — the account's data is never deleted
 */
object MergePolicy {

    const val MAX_CART_QUANTITY = 99

    /** account + guest quantities, clamped to the rule-compliant 1..99 band. */
    fun mergedCartQuantity(accountQuantity: Int, guestQuantity: Int): Int =
        (accountQuantity + guestQuantity).coerceIn(1, MAX_CART_QUANTITY)

    /**
     * Whether a guest row should be re-owned into the account namespace.
     * When the account already has the same product locally, the guest row is
     * dropped instead (union semantics; cloud converges via the listener).
     */
    fun shouldReownGuestRow(accountAlreadyHasProduct: Boolean): Boolean =
        !accountAlreadyHasProduct
}