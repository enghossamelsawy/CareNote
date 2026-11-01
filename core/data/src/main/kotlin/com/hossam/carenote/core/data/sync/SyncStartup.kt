package com.hossam.carenote.core.data.sync

import com.hossam.carenote.core.common.coroutines.ApplicationScope
import com.hossam.carenote.core.domain.repository.NetworkMonitor
import com.hossam.carenote.core.domain.repository.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Called once at app start: requests a sync whenever the device comes online, including right away if it
 * already is. WorkManager would also run a waiting sync on reconnect; this covers notes from earlier sessions
 * and gets a FAILED-then-retried note out without waiting for the backoff.
 */
class SyncStartup @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val syncScheduler: SyncScheduler,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    fun start() {
        applicationScope.launch {
            networkMonitor.isOnline
                .filter { online -> online }
                .collect { syncScheduler.requestSync() }
        }
    }
}
