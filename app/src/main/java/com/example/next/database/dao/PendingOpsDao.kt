package com.example.next.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.next.database.entity.PendingOpEntity

/**
 * Outbox of offline mutations (only ever written for `uid:` owners — guest
 * data stays Room-only). Flushed in creation order when connectivity returns.
 */
@Dao
interface PendingOpsDao {

    @Insert
    suspend fun insert(op: PendingOpEntity): Long

    @Query("SELECT * FROM pending_ops WHERE owner = :owner ORDER BY created_at ASC, id ASC")
    suspend fun getPendingOps(owner: String): List<PendingOpEntity>

    @Query("SELECT * FROM pending_ops WHERE owner = :owner AND table_name = :tableName AND row_id = :rowId")
    suspend fun findByOwnerTableRow(
        owner: String,
        tableName: String,
        rowId: String
    ): PendingOpEntity?

    /** Folds a newer mutation into an older queued one (last write wins). */
    @Query(
        "UPDATE pending_ops SET op = :op, payload = :payload, created_at = :createdAt " +
            "WHERE owner = :owner AND table_name = :tableName AND row_id = :rowId"
    )
    suspend fun updateOp(
        owner: String,
        tableName: String,
        rowId: String,
        op: String,
        payload: String,
        createdAt: Long
    ): Int

    @Query("DELETE FROM pending_ops WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Drops any queued op for one (owner, table, row) — e.g. cancelled-before-upload orders. */
    @Query("DELETE FROM pending_ops WHERE owner = :owner AND table_name = :tableName AND row_id = :rowId")
    suspend fun deleteByOwnerTableRow(owner: String, tableName: String, rowId: String): Int

    @Query("UPDATE pending_ops SET attempt = attempt + 1 WHERE id = :id")
    suspend fun bumpAttempt(id: Long)

    /** Deletes the outbox of one owner (e.g. after a successful flush). */
    @Query("DELETE FROM pending_ops WHERE owner = :owner")
    suspend fun deleteForOwner(owner: String): Int
}