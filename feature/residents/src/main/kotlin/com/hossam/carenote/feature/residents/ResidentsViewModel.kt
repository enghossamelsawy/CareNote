package com.hossam.carenote.feature.residents

import androidx.lifecycle.viewModelScope
import com.hossam.carenote.core.domain.usecase.ObserveResidentsWithPendingCountUseCase
import com.hossam.carenote.core.model.ResidentSummary
import com.hossam.carenote.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class ResidentsViewModel @Inject constructor(
    observeResidentsWithPendingCount: ObserveResidentsWithPendingCountUseCase,
) : MviViewModel<ResidentsState, ResidentsIntent, ResidentsEffect>(ResidentsState()) {
    init {
        observeResidentsWithPendingCount()
            .onEach { summaries -> setState { copy(isLoading = false, residents = summaries.map { it.asItem() }) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: ResidentsIntent) {
        when (intent) {
            is ResidentsIntent.ResidentClicked -> sendEffect(ResidentsEffect.OpenResident(intent.residentId))
        }
    }

    private fun ResidentSummary.asItem() = ResidentItem(
        id = resident.id,
        name = "${resident.firstName} ${resident.lastName}",
        room = resident.room,
        pendingCount = pendingCount,
    )
}
