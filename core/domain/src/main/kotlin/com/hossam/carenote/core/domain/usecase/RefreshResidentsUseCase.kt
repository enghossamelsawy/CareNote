package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.repository.RefreshResult
import com.hossam.carenote.core.domain.repository.ResidentRepository
import javax.inject.Inject

/** Gets the latest residents from the server. Never blocks the UI: screens keep showing what's stored. */
class RefreshResidentsUseCase @Inject constructor(private val residentRepository: ResidentRepository) {
    suspend operator fun invoke(): RefreshResult = residentRepository.refresh()
}
