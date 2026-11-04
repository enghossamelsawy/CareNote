package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.repository.CareNoteRepository
import com.hossam.carenote.core.domain.repository.StorageException
import com.hossam.carenote.core.model.CareNote
import java.util.UUID
import javax.inject.Inject

sealed interface GetNoteResult {
    data class Found(val note: CareNote) : GetNoteResult

    data object NotFound : GetNoteResult

    /** The phone's storage couldn't be read; trying again may work. */
    data object Failed : GetNoteResult
}

/** One note, e.g. to fill the record screen when editing it. */
class GetCareNoteUseCase @Inject constructor(private val careNoteRepository: CareNoteRepository) {
    suspend operator fun invoke(noteId: UUID): GetNoteResult = try {
        careNoteRepository.getNote(noteId)?.let { GetNoteResult.Found(it) } ?: GetNoteResult.NotFound
    } catch (error: StorageException) {
        GetNoteResult.Failed
    }
}
