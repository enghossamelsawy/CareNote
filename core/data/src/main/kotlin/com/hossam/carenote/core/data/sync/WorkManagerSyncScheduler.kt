package com.hossam.carenote.core.data.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.hossam.carenote.core.domain.repository.SyncScheduler
import dagger.Lazy
import java.time.Duration
import javax.inject.Inject

/**
 * One sync at a time, under a unique name. A request while one is running is appended after it,
 * so a note saved during a sync is never left waiting for the next trigger.
 *
 * [workManager] is lazy: creating WorkManager reads the app's configuration, which needs Hilt's worker
 * factory, and that isn't injected yet while the Application itself is being injected.
 */
internal class WorkManagerSyncScheduler @Inject constructor(private val workManager: Lazy<WorkManager>) :
    SyncScheduler {
    override fun requestSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, INITIAL_BACKOFF)
            .build()
        workManager.get().enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private companion object {
        const val WORK_NAME = "note-sync"
        val INITIAL_BACKOFF: Duration = Duration.ofSeconds(30)
    }
}
