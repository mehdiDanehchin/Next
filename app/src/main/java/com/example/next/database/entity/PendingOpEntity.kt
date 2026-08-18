package com.example.next.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Outbox record for an offline mutation (v5).
 *
 * When an authenticated user mutates data while offline (or the Firestore
 * write fails), the mutation is recorded here and flushed when connectivity
 * returns. Guests NEVER write pending ops — guest data is Room-only by design.
 *
 * [payload] is a JSON map of the document data to write (or "{}" for a
 * delete); [rowId] is the Firestore document id. [attempt] counts flush
 * retries so the flush loop can back off.
 */
@Entity(tableName = "pending_ops")
data class PendingOpEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    /** Owner that performed the mutation (`uid:<uid>` — guests never enqueue). */
    @ColumnInfo(name = "owner")
    val owner: String,
    /** Target table: wishlist | cart | orders. */
    @ColumnInfo(name = "table_name")
    val tableName: String,
    /** Firestore document id (productId / cloud order id). */
    @ColumnInfo(name = "row_id")
    val rowId: String,
    /** Mutation kind: set | delete | setStatus. */
    @ColumnInfo(name = "op")
    val op: String,
    /** JSON document data for `set` ops ("" for delete/setStatus). */
    @ColumnInfo(name = "payload")
    val payload: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "attempt")
    val attempt: Int = 0
) {
    companion object {
        const val OP_SET = "set"
        const val OP_DELETE = "delete"
        const val OP_SET_STATUS = "setStatus"

        const val TABLE_WISHLIST = "wishlist"
        const val TABLE_CART = "cart"
        const val TABLE_ORDERS = "orders"
    }
}