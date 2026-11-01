package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.repository.CareNoteRepository
import com.hossam.carenote.core.domain.repository.SyncScheduler
import java.util.UUID
import javax.inject.Inject

/**
 * Queues a note the server refused for another attempt, and asks for a sync right away. Only refused notes
 * are retried: a double tap, or a note that synced meanwhile, changes nothing and starts no sync.
 */
class RetryCareNoteSyncUseCase @Inject constructor(
    private val careNoteRepository: CareNoteRepository,
    private val syncScheduler: SyncScheduler,
) {
    /** @return true when the note was queued again. */
    suspend operator fun invoke(noteId: UUID): Boolean {
        val queued = careNoteRepository.retry(noteId)
        if (queued) syncScheduler.requestSync()
        return queued
    }
}
