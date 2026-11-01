package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.repository.CareNoteRepository
import com.hossam.carenote.core.domain.repository.SyncScheduler
import java.util.UUID
import javax.inject.Inject

/** Queues a note the server refused for another attempt, and asks for a sync right away. */
class RetryCareNoteSyncUseCase @Inject constructor(
    private val careNoteRepository: CareNoteRepository,
    private val syncScheduler: SyncScheduler,
) {
    suspend operator fun invoke(noteId: UUID) {
        careNoteRepository.retry(noteId)
        syncScheduler.requestSync()
    }
}
