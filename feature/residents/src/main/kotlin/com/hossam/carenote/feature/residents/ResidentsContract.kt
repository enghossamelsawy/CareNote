package com.hossam.carenote.feature.residents

import com.hossam.carenote.core.ui.mvi.UiEffect
import com.hossam.carenote.core.ui.mvi.UiIntent
import com.hossam.carenote.core.ui.mvi.UiState

data class ResidentsState(val isLoading: Boolean = true, val residents: List<ResidentItem> = emptyList()) : UiState

/** One row of the residents list, already in the shape the screen shows. */
data class ResidentItem(val id: String, val name: String, val room: String, val pendingCount: Int)

sealed interface ResidentsIntent : UiIntent {
    data class ResidentClicked(val residentId: String) : ResidentsIntent
}

sealed interface ResidentsEffect : UiEffect {
    data class OpenResident(val residentId: String) : ResidentsEffect
}
