package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.repository.SyncScheduler
import javax.inject.Inject

/** Asks for a sync as soon as the phone is online: this phone's queued notes go out, other phones' come in. */
class RequestSyncUseCase @Inject constructor(private val syncScheduler: SyncScheduler) {
    suspend operator fun invoke() = syncScheduler.requestSync()
}
