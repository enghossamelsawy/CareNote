package com.hossam.carenote.feature.residents

import androidx.lifecycle.viewModelScope
import com.hossam.carenote.core.domain.repository.RefreshResult
import com.hossam.carenote.core.domain.usecase.ObserveNetworkStatusUseCase
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
    private val refreshResidents: RefreshResidentsUseCase,
    observeNetworkStatus: ObserveNetworkStatusUseCase,
) : MviViewModel<ResidentsState, ResidentsIntent, ResidentsEffect>(ResidentsState()) {
    init {
        observeResidentsWithPendingCount()
            .onEach { summaries -> setState { copy(isLoading = false, residents = summaries.map { it.asItem() }) } }
            .launchIn(viewModelScope)
        observeNetworkStatus()
            .onEach { online -> setState { copy(isOffline = !online) } }
            .launchIn(viewModelScope)
        // Offline-first: the stored list shows right away; fresh data, if the server answers, arrives through
        // the observed flow above. A failed refresh only shows while there is no stored list to show instead.
        refresh()
    }

    override fun onIntent(intent: ResidentsIntent) {
        when (intent) {
            is ResidentsIntent.ResidentClicked -> sendEffect(ResidentsEffect.OpenResident(intent.residentId))
            ResidentsIntent.RetryClicked -> refresh()
        }
    }

    /** One download at a time; repeated taps while it runs are ignored. */
    private fun refresh() {
        if (currentState.isRefreshing) return
        setState { copy(isRefreshing = true, refreshProblem = null) }
        viewModelScope.launch {
            val problem = when (refreshResidents()) {
                RefreshResult.Updated -> null
                RefreshResult.Unavailable -> RefreshProblem.Unavailable
                RefreshResult.Failed -> RefreshProblem.Failed
            }
            setState { copy(isRefreshing = false, refreshProblem = problem) }
        }
    }

    private fun ResidentSummary.asItem() = ResidentItem(
        id = resident.id,
        name = "${resident.firstName} ${resident.lastName}",
        room = resident.room,
        pendingCount = pendingCount,
    )
}
