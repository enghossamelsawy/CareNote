package com.hossam.carenote.feature.residents

import androidx.lifecycle.viewModelScope
import com.hossam.carenote.core.domain.usecase.ObserveResidentsWithPendingCountUseCase
import com.hossam.carenote.core.domain.usecase.RefreshResidentsUseCase
import com.hossam.carenote.core.model.ResidentSummary
import com.hossam.carenote.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResidentsViewModel @Inject constructor(
    observeResidentsWithPendingCount: ObserveResidentsWithPendingCountUseCase,
    refreshResidents: RefreshResidentsUseCase,
) : MviViewModel<ResidentsState, ResidentsIntent, ResidentsEffect>(ResidentsState()) {
    init {
        observeResidentsWithPendingCount()
            .onEach { summaries -> setState { copy(isLoading = false, residents = summaries.map { it.asItem() }) } }
            .launchIn(viewModelScope)
        // Offline-first: the stored list shows right away; fresh data, if the server answers, arrives through
        // the observed flow above. A failed refresh changes nothing on screen.
        viewModelScope.launch { refreshResidents() }
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
