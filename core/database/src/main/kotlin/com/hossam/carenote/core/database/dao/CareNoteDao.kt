package com.hossam.carenote.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.hossam.carenote.core.database.model.CareNoteEntity
import com.hossam.carenote.core.database.model.OutboxEntity
import com.hossam.carenote.core.database.model.PendingCount
import com.hossam.carenote.core.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CareNoteDao {
    @Query("SELECT * FROM care_notes WHERE residentId = :residentId ORDER BY createdAt DESC")
    fun observeForResident(residentId: String): Flow<List<CareNoteEntity>>

    /** Notes not yet on the server, per resident. Residents without any are left out. */
    @Query(
        "SELECT residentId, COUNT(*) AS count FROM care_notes WHERE syncStatus != 'SYNCED' " +
            "GROUP BY residentId ORDER BY residentId",
    )
    fun observePendingCounts(): Flow<List<PendingCount>>

    /** Notes are append-only, so saving an existing id throws instead of replacing it. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(note: CareNoteEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOutboxEntry(entry: OutboxEntity)

    /** Saves the note and queues it for sync in one transaction, so a note can't exist without its queue entry. */
    @Transaction
    suspend fun insertAndQueue(note: CareNoteEntity, entry: OutboxEntity) {
        insert(note)
        insertOutboxEntry(entry)
    }

    @Query("UPDATE care_notes SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)
}
