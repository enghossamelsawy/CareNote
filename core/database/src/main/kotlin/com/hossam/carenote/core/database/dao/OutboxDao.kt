package com.hossam.carenote.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.hossam.carenote.core.database.model.OutboxEntity
import java.time.Instant

/** The sync queue, as the sync worker sees it. Entries are added together with their note, see [CareNoteDao]. */
@Dao
interface OutboxDao {
    @Query("SELECT * FROM outbox ORDER BY enqueuedAt LIMIT :limit")
    suspend fun oldest(limit: Int): List<OutboxEntity>

    @Query("UPDATE outbox SET attempts = attempts + 1, lastAttemptAt = :at, lastError = :error WHERE noteId = :noteId")
    suspend fun recordAttempt(noteId: String, at: Instant, error: String?)

    @Query("DELETE FROM outbox WHERE noteId = :noteId")
    suspend fun delete(noteId: String)
}
