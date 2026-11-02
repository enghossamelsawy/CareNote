package com.hossam.carenote.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.hossam.carenote.core.database.model.CareNoteEntity
import com.hossam.carenote.core.database.model.OutboxEntity
import com.hossam.carenote.core.database.model.PendingCount
import com.hossam.carenote.core.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CareNoteDao {
    @Query("SELECT * FROM care_notes WHERE residentId = :residentId ORDER BY createdAt DESC")
    fun observeForResident(residentId: String): Flow<List<CareNoteEntity>>

    /** Every resident's notes in one query, newest first, so the residents list doesn't run one per resident. */
    @Query("SELECT * FROM care_notes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CareNoteEntity>>

    /** Notes not yet on the server, per resident. Residents without any are left out. */
    @Query(
        "SELECT residentId, COUNT(*) AS count FROM care_notes WHERE syncStatus != 'SYNCED' " +
            "GROUP BY residentId ORDER BY residentId",
    )
    fun observePendingCounts(): Flow<List<PendingCount>>

    /**
     * Notes are append-only, so saving an existing id throws instead of replacing it. The app saves through
     * [insertAndQueue], which also numbers the note; this stays for tests that only need rows.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(note: CareNoteEntity)

    @Query("SELECT COALESCE(MAX(saveOrder), 0) + 1 FROM care_notes")
    suspend fun nextSaveOrder(): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOutboxEntry(entry: OutboxEntity)

    /**
     * Saves the note and queues it for sync in one transaction, so a note can't exist without its queue entry.
     * The note gets the next [CareNoteEntity.saveOrder]; the transaction keeps two saves from getting the same one.
     */
    @Transaction
    suspend fun insertAndQueue(note: CareNoteEntity, entry: OutboxEntity) {
        insert(note.copy(saveOrder = nextSaveOrder()))
        insertOutboxEntry(entry)
    }

    @Query("SELECT * FROM care_notes WHERE id = :id")
    suspend fun getById(id: String): CareNoteEntity?

    /**
     * Notes left SYNCING by a sync that never finished, e.g. because the app was killed mid-send, go back to
     * waiting. Their outbox entries were never removed, so they are sent again.
     */
    @Query("UPDATE care_notes SET syncStatus = 'PENDING' WHERE syncStatus = 'SYNCING'")
    suspend fun resetSyncing()

    /** True when another note revises this one; edits never change a note, they add a new one. */
    @Query("SELECT EXISTS(SELECT 1 FROM care_notes WHERE revisionOf = :id)")
    suspend fun hasRevision(id: String): Boolean

    @Query("UPDATE care_notes SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("DELETE FROM outbox WHERE noteId = :noteId")
    suspend fun deleteOutboxEntry(noteId: String)

    /** The server has the note: mark it and take it off the queue, in one transaction. */
    @Transaction
    suspend fun markSynced(id: String) {
        updateSyncStatus(id, SyncStatus.SYNCED)
        deleteOutboxEntry(id)
    }

    /** The server refused the note: it stays visible as FAILED until someone retries it. */
    @Transaction
    suspend fun markFailed(id: String) {
        updateSyncStatus(id, SyncStatus.FAILED)
        deleteOutboxEntry(id)
    }

    /** Adds the entry, or replaces the note's existing one, e.g. to reset its attempts. */
    @Upsert
    suspend fun upsertOutboxEntry(entry: OutboxEntity)

    /** Sets a refused note back to waiting; returns how many rows changed, 0 if the note isn't FAILED. */
    @Query("UPDATE care_notes SET syncStatus = 'PENDING' WHERE id = :id AND syncStatus = 'FAILED'")
    suspend fun resetFailed(id: String): Int

    /**
     * Marks a refused note as waiting again and gives it a fresh queue entry, in one transaction. A note that
     * isn't FAILED, e.g. one already synced by the time Retry is tapped, is left as it is.
     *
     * @return true when the note was queued again.
     */
    @Transaction
    suspend fun requeue(entry: OutboxEntity): Boolean {
        if (resetFailed(entry.noteId) == 0) return false
        upsertOutboxEntry(entry)
        return true
    }
}
