package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.repository.ResidentRepository
import com.hossam.carenote.core.model.Resident
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** One resident, kept up to date. Emits null when no resident has this id. */
class ObserveResidentUseCase @Inject constructor(private val residentRepository: ResidentRepository) {
    operator fun invoke(residentId: String): Flow<Resident?> = residentRepository.observeResident(residentId)
}
