package com.hossam.carenote.feature.resident

import androidx.lifecycle.viewModelScope
import com.hossam.carenote.core.domain.usecase.ObserveResidentUseCase
import com.hossam.carenote.core.ui.mvi.MviViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** [residentId] comes straight from the nav key through assisted injection, so no SavedStateHandle or string args. */
@HiltViewModel(assistedFactory = ResidentViewModel.Factory::class)
class ResidentViewModel @AssistedInject constructor(
    @Assisted private val residentId: String,
    observeResident: ObserveResidentUseCase,
) : MviViewModel<ResidentState, ResidentIntent, ResidentEffect>(ResidentState()) {
    init {
        observeResident(residentId)
            .onEach { resident ->
                setState {
                    if (resident == null) {
                        ResidentState(isLoading = false, notFound = true)
                    } else {
                        ResidentState(
                            isLoading = false,
                            name = "${resident.firstName} ${resident.lastName}",
                            room = resident.room,
                        )
                    }
                }
            }.launchIn(viewModelScope)
    }

    override fun onIntent(intent: ResidentIntent) {
        when (intent) {
            ResidentIntent.BackClicked -> sendEffect(ResidentEffect.NavigateBack)
            ResidentIntent.RecordClicked -> sendEffect(ResidentEffect.OpenRecord(residentId))
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(residentId: String): ResidentViewModel
    }
}
