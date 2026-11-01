package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.repository.CareNoteRepository
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.CurrentCareNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

/**
 * One resident's notes as they read now: only the latest version of each, newest first by when it
 * was first written. Emits again whenever a note is saved or its sync status changes.
 *
 * Replaced versions are only hidden; they stay stored and still sync. If a version the server
 * refused gets edited, it is hidden together with its retry button: the edit is what counts now.
 */
class ObserveCareNotesUseCase @Inject constructor(private val careNoteRepository: CareNoteRepository) {
    operator fun invoke(residentId: String): Flow<List<CurrentCareNote>> =
        careNoteRepository.observeNotes(residentId).map { notes -> currentVersions(notes) }

    private fun currentVersions(notes: List<CareNote>): List<CurrentCareNote> {
        val byId = notes.associateBy { it.id }
        val replaced = notes.mapNotNullTo(mutableSetOf()) { it.revisionOf }
        return notes
            .filter { it.id !in replaced }
            .map { note -> CurrentCareNote(note = note, firstWrittenAt = firstVersion(note, byId).createdAt) }
            .sortedByDescending { it.firstWrittenAt }
    }

    /** Follows the edits back to the first version; stops at a missing link instead of failing. */
    private fun firstVersion(note: CareNote, byId: Map<UUID, CareNote>): CareNote {
        var current = note
        val seen = mutableSetOf(current.id)
        while (true) {
            val previous = current.revisionOf?.let(byId::get) ?: return current
            if (!seen.add(previous.id)) return current
            current = previous
        }
    }
}
