package com.hossam.carenote.core.data.sync

import com.hossam.carenote.core.data.mapper.asExternalModel
import com.hossam.carenote.core.database.dao.CareNoteDao
import com.hossam.carenote.core.database.dao.OutboxDao
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.network.RemoteDataSource
import com.hossam.carenote.core.network.SendResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
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
 *
 * Queue policy, and what it costs:
 * - "Oldest" is the order notes were saved in, so an original always reaches the server before its edit.
 * - A refused note (4xx) is set aside as FAILED and the queue moves on: retrying won't change the answer.
 * - "Try later" (offline, 408, 429, 5xx) stops the whole run instead of skipping to the next note. An outage
 *   usually hits every note, so trying each one would only multiply failed requests, and skipping would send
 *   an edit before its original.
 * - The cost is head-of-line blocking: if the server kept answering 5xx for one note only, every note behind
 *   it would wait, with WorkManager retrying with backoff. Each outbox entry records its attempts and last
 *   error, so a cap that sets such a note aside could be added; it isn't, because a server failing one note
 *   but not others is a server bug to fix there, not something to hide on the phone.
 */
internal class NoteSyncer @Inject constructor(
    private val careNoteDao: CareNoteDao,
    private val outboxDao: OutboxDao,
    private val remoteDataSource: RemoteDataSource,
    private val clock: Clock,
) {
    suspend fun syncPending(): SyncOutcome {
        // Only one sync runs at a time, so a note still marked as syncing is left over from one that never finished.
        careNoteDao.resetSyncing()
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
                val result = try {
                    remoteDataSource.sendNote(note.asExternalModel())
                } catch (cancellation: CancellationException) {
                    // Stopped mid-send, e.g. the network dropped: back to waiting; it's still queued.
                    withContext(NonCancellable) { careNoteDao.updateSyncStatus(note.id, SyncStatus.PENDING) }
                    throw cancellation
                }
                when (result) {
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
