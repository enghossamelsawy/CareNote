package com.hossam.carenote.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A note that still has to reach the server. The row is the sync queue's bookkeeping;
 * what the UI shows is [CareNoteEntity.syncStatus], written in the same transaction.
 */
@Entity(
    tableName = "outbox",
    foreignKeys = [
        ForeignKey(
            entity = CareNoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class OutboxEntity(
    @PrimaryKey val noteId: String,
    val enqueuedAt: Instant,
    val attempts: Int = 0,
    val lastAttemptAt: Instant? = null,
    val lastError: String? = null,
)
