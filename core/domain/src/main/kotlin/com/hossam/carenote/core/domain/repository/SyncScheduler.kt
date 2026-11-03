package com.hossam.carenote.core.domain.repository

/** Asks for queued notes to be sent to the server as soon as the device is online. */
interface SyncScheduler {
    suspend fun requestSync()
}
