package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.note.NoteVersions
import com.hossam.carenote.core.domain.repository.CareNoteRepository
import com.hossam.carenote.core.model.CurrentCareNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        careNoteRepository.observeNotes(residentId).map { notes ->
            val versions = NoteVersions(notes)
            versions.current
                .map { note ->
                    val first = versions.earlier(note).lastOrNull() ?: note
                    CurrentCareNote(note = note, firstWrittenAt = first.createdAt)
                }
                .sortedByDescending { it.firstWrittenAt }
        }
}
