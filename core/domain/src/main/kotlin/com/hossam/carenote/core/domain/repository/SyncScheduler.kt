package com.hossam.carenote.core.domain.repository

/** Asks for a sync as soon as the device is online: queued notes are sent, then other phones' notes pulled. */
interface SyncScheduler {
    suspend fun requestSync()
}
