package com.hossam.carenote.core.data.sync

import com.hossam.carenote.core.data.mapper.asExternalModel
import com.hossam.carenote.core.database.dao.CareNoteDao
import com.hossam.carenote.core.database.dao.OutboxDao
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.network.RemoteDataSource
import com.hossam.carenote.core.network.SendResult
import java.time.Clock
import javax.inject.Inject

internal enum class SyncOutcome {
    /** The queue is empty. */
    Done,

    /** The server can't be reached right now; whatever runs this should try again later. */
    RetryLater,
}

/**
 * Sends queued notes to the server, oldest first, until the queue is empty or the server can't be reached.
 * Every send can be repeated safely: the note id is the idempotency key, so a note that reached the server
 * before a crash or timeout is answered with "already stored" and marked synced.
 */
internal class NoteSyncer @Inject constructor(
    private val careNoteDao: CareNoteDao,
    private val outboxDao: OutboxDao,
    private val remoteDataSource: RemoteDataSource,
    private val clock: Clock,
) {
    suspend fun syncPending(): SyncOutcome {
        while (true) {
            val batch = outboxDao.oldest(limit = BATCH_SIZE)
            if (batch.isEmpty()) return SyncOutcome.Done

            for (entry in batch) {
                val note = careNoteDao.getById(entry.noteId)
                if (note == null) {
                    // Can't happen with the foreign key, but a stuck entry must never block the queue.
                    outboxDao.delete(entry.noteId)
                    continue
                }

                careNoteDao.updateSyncStatus(note.id, SyncStatus.SYNCING)
                when (val result = remoteDataSource.sendNote(note.asExternalModel())) {
                    SendResult.Accepted -> careNoteDao.markSynced(note.id)

                    is SendResult.Rejected -> careNoteDao.markFailed(note.id)

                    // Later notes would fail the same way, so stop here instead of trying each one.
                    SendResult.RetryLater -> {
                        careNoteDao.updateSyncStatus(note.id, SyncStatus.PENDING)
                        outboxDao.recordAttempt(note.id, at = clock.instant(), error = "Server unreachable")
                        return SyncOutcome.RetryLater
                    }
                }
            }
        }
    }

    private companion object {
        const val BATCH_SIZE = 20
    }
}
