package com.hossam.carenote.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.hossam.carenote.core.database.model.OutboxEntity
import java.time.Instant

/** The sync queue, as the sync worker sees it. Entries are added together with their note, see [CareNoteDao]. */
@Dao
interface OutboxDao {
    /**
     * The next entries to send, in the order their notes were saved. A retried note keeps its place, so an
     * original always goes before its edit, whenever it was queued again.
     */
    @Query(
        "SELECT outbox.* FROM outbox JOIN care_notes ON care_notes.id = outbox.noteId " +
            "ORDER BY care_notes.saveOrder LIMIT :limit",
    )
    suspend fun oldest(limit: Int): List<OutboxEntity>

    @Query("UPDATE outbox SET attempts = attempts + 1, lastAttemptAt = :at, lastError = :error WHERE noteId = :noteId")
    suspend fun recordAttempt(noteId: String, at: Instant, error: String?)

    @Query("DELETE FROM outbox WHERE noteId = :noteId")
    suspend fun delete(noteId: String)
}
