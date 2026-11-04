package com.hossam.carenote.core.domain.repository

/**
 * Asks for a sync as soon as the device is online: queued notes are sent, then other phones' notes pulled.
 *
 * [requestSync] never throws: a request that can't be scheduled is logged, and the next app start, network
 * change or save asks again. Callers can't do better than that, so none of them needs to handle an error.
 */
interface SyncScheduler {
    suspend fun requestSync()
}
