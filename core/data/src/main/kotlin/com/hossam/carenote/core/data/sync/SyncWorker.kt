package com.hossam.carenote.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hossam.carenote.core.common.logging.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Sends the outbox, then pulls other phones' notes, in the background. WorkManager runs it only with a
 * network, survives app restarts and reboots, and backs off between retries; the rules live in [NoteSyncer]
 * and [NotePuller].
 *
 * Sending comes first, so this phone's notes are on the server before it reads the feed. If sending can't
 * reach the server, pulling isn't tried either: it would fail the same way.
 *
 * Once sending worked, a pull the server answers unusably is logged and the run still succeeds. A failed run
 * would also fail the sync queued behind it, and a note saved meanwhile would wait for the next trigger.
 */
@HiltWorker
internal class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val noteSyncer: NoteSyncer,
    private val notePuller: NotePuller,
    private val logger: Logger,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val sent = noteSyncer.syncPending()
        if (sent != SyncOutcome.Done) return sent.asResult()
        val pulled = notePuller.pull()
        if (pulled == SyncOutcome.Failed) {
            logger.e(TAG, "Pulling notes failed; they are pulled again on the next sync")
            return Result.success()
        }
        return pulled.asResult()
    }

    private fun SyncOutcome.asResult(): Result = when (this) {
        SyncOutcome.Done -> Result.success()
        SyncOutcome.RetryLater -> Result.retry()
        // The next request starts a new sync; a failed one doesn't block it.
        SyncOutcome.Failed -> Result.failure()
    }

    private companion object {
        const val TAG = "SyncWorker"
    }
}
