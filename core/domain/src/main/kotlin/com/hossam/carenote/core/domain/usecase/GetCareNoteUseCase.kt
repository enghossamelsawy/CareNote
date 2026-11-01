package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.repository.CareNoteRepository
import com.hossam.carenote.core.model.CareNote
import java.util.UUID
import javax.inject.Inject

/** One note, e.g. to fill the record screen when editing it; null if it doesn't exist. */
class GetCareNoteUseCase @Inject constructor(private val careNoteRepository: CareNoteRepository) {
    suspend operator fun invoke(noteId: UUID): CareNote? = careNoteRepository.getNote(noteId)
}
