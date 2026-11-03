package com.hossam.carenote.core.data.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import com.hossam.carenote.core.domain.repository.SyncScheduler
import dagger.Lazy
import kotlinx.coroutines.flow.first
import java.time.Duration
import javax.inject.Inject

/**
 * One sync at a time, under a unique name, and at most one more waiting behind it.
 *
 * A request while a sync runs is appended after it, so a note saved during a sync is never left waiting for
 * the next trigger. A request while a sync is still waiting to start, such as offline or in backoff, is
 * skipped: that sync reads the outbox when it starts, so it already covers the new note. Without that, every
 * note saved offline would add one more sync to the chain.
 *
 * [workManager] is lazy: creating WorkManager reads the app's configuration, which needs Hilt's worker
 * factory, and that isn't injected yet while the Application itself is being injected.
 */
internal class WorkManagerSyncScheduler @Inject constructor(private val workManager: Lazy<WorkManager>) :
    SyncScheduler {
    override suspend fun requestSync() {
        val syncs = workManager.get().getWorkInfosForUniqueWorkFlow(WORK_NAME).first()
        if (syncs.any { it.state in WAITING }) return
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, INITIAL_BACKOFF)
            .build()
        workManager.get().enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request).await()
    }

    companion object {
        const val WORK_NAME = "note-sync"
        private val INITIAL_BACKOFF: Duration = Duration.ofSeconds(30)

        /** Not started yet: waiting for a network or a backoff (enqueued), or for the sync before it (blocked). */
        private val WAITING = setOf(WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED)
    }
}
