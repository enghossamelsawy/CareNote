package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.repository.CareNoteRepository
import com.hossam.carenote.core.domain.repository.ResidentRepository
import com.hossam.carenote.core.model.ResidentSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Every resident with how many of their notes are not on the server yet, in the repository's order.
 * Emits again whenever a resident or a note's sync status changes.
 */
class ObserveResidentsWithPendingCountUseCase @Inject constructor(
    private val residentRepository: ResidentRepository,
    private val careNoteRepository: CareNoteRepository,
) {
    operator fun invoke(): Flow<List<ResidentSummary>> = combine(
        residentRepository.observeResidents(),
        careNoteRepository.observePendingCounts(),
    ) { residents, pendingCounts ->
        residents.map { resident -> ResidentSummary(resident, pendingCount = pendingCounts[resident.id] ?: 0) }
    }
}
