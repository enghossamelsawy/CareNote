package com.hossam.carenote.core.data.sync

import com.hossam.carenote.core.common.coroutines.ApplicationScope
import com.hossam.carenote.core.domain.repository.CareNoteRepository
import com.hossam.carenote.core.domain.repository.NetworkMonitor
import com.hossam.carenote.core.domain.repository.StorageException
import com.hossam.carenote.core.domain.repository.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Called once at app start: requests a sync whenever the device comes online, including right away if it
 * already is. WorkManager would also run a waiting sync on reconnect; this covers notes from earlier sessions
 * and gets a FAILED-then-retried note out without waiting for the backoff.
 *
 * First it puts notes left "syncing" by a killed app back to waiting, so the list is right even offline.
 */
class SyncStartup @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val syncScheduler: SyncScheduler,
    private val careNoteRepository: CareNoteRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    fun start() {
        applicationScope.launch {
            try {
                careNoteRepository.resetInterruptedSyncs()
            } catch (error: StorageException) {
                // Only the shown status is affected; the sync worker resets these again before sending.
            }
            networkMonitor.isOnline
                .filter { online -> online }
                .collect { syncScheduler.requestSync() }
        }
    }
}
