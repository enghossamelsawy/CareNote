package com.hossam.carenote.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Sends the outbox in the background. WorkManager runs it only with a network, survives app restarts and
 * reboots, and backs off between retries; all the sync rules live in [NoteSyncer].
 */
@HiltWorker
internal class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val noteSyncer: NoteSyncer,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (noteSyncer.syncPending()) {
        SyncOutcome.Done -> Result.success()
        SyncOutcome.RetryLater -> Result.retry()
    }
}
