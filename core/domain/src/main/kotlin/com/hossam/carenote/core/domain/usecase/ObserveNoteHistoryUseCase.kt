package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.note.NoteVersions
import com.hossam.carenote.core.domain.repository.CareNoteRepository
import com.hossam.carenote.core.model.NoteHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

/**
 * One note with all its versions, for reading it in full. [noteId] can be any of its versions: the
 * history always follows to the latest one, so a screen showing it moves on by itself after an edit.
 * Emits null when the note doesn't exist for this resident.
 */
class ObserveNoteHistoryUseCase @Inject constructor(private val careNoteRepository: CareNoteRepository) {
    operator fun invoke(residentId: String, noteId: UUID): Flow<NoteHistory?> =
        careNoteRepository.observeNotes(residentId).map { notes ->
            val versions = NoteVersions(notes)
            versions.latest(noteId)?.let { current ->
                NoteHistory(current = current, earlier = versions.earlier(current))
            }
        }
}
