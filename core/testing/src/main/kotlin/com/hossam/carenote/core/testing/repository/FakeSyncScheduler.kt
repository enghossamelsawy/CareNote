package com.hossam.carenote.core.testing.repository

import com.hossam.carenote.core.domain.repository.SyncScheduler

/** Counts sync requests instead of scheduling work. */
class FakeSyncScheduler : SyncScheduler {
    var requestCount = 0
        private set

    override suspend fun requestSync() {
        requestCount++
    }
}
