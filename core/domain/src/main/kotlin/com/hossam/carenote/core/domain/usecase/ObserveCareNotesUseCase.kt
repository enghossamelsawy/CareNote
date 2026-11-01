package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.repository.CareNoteRepository
import com.hossam.carenote.core.model.CareNote
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** One resident's notes, newest first; emits again whenever one is saved or its sync status changes. */
class ObserveCareNotesUseCase @Inject constructor(private val careNoteRepository: CareNoteRepository) {
    operator fun invoke(residentId: String): Flow<List<CareNote>> = careNoteRepository.observeNotes(residentId)
}
