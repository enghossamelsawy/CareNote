package com.hossam.carenote.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Sends the outbox, then pulls other phones' notes, in the background. WorkManager runs it only with a
 * network, survives app restarts and reboots, and backs off between retries; the rules live in [NoteSyncer]
 * and [NotePuller].
 *
 * Sending comes first, so this phone's notes are on the server before it reads the feed. If sending can't
 * reach the server, pulling isn't tried either: it would fail the same way.
 */
@HiltWorker
internal class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val noteSyncer: NoteSyncer,
    private val notePuller: NotePuller,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val sent = noteSyncer.syncPending()
        val outcome = if (sent == SyncOutcome.Done) notePuller.pull() else sent
        return when (outcome) {
            SyncOutcome.Done -> Result.success()
            SyncOutcome.RetryLater -> Result.retry()
            // The next request starts a new sync; a failed one doesn't block it.
            SyncOutcome.Failed -> Result.failure()
        }
    }
}
