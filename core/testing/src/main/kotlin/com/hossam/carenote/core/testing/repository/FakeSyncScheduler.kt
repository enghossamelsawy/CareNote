package com.hossam.carenote.core.testing.repository

import com.hossam.carenote.core.domain.repository.SyncScheduler

/** Counts sync requests instead of scheduling work. */
class FakeSyncScheduler : SyncScheduler {
    var requestCount = 0
        private set

    /** When true, every request throws, like WorkManager failing to enqueue. */
    var failRequests = false

    override fun requestSync() {
        check(!failRequests) { "Couldn't schedule the sync" }
        requestCount++
    }
}
