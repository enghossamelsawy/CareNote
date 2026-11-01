package com.hossam.carenote.feature.residents

import com.hossam.carenote.core.ui.mvi.UiEffect
import com.hossam.carenote.core.ui.mvi.UiIntent
import com.hossam.carenote.core.ui.mvi.UiState

data class ResidentsState(
    val isLoading: Boolean = true,
    val residents: List<ResidentItem> = emptyList(),
    /** Shown as a banner: recording still works, notes just wait to sync. */
    val isOffline: Boolean = false,
    /** A download from the server is running. */
    val isRefreshing: Boolean = false,
    /** Why the last download didn't work; shown only while there are no residents to show instead. */
    val refreshProblem: RefreshProblem? = null,
) : UiState

enum class RefreshProblem {
    /** Offline, or the server is down: trying again later can work. */
    Unavailable,

    /** The server answered with something unusable. */
    Failed,
}

/** One row of the residents list, already in the shape the screen shows. */
data class ResidentItem(val id: String, val name: String, val room: String, val pendingCount: Int)

sealed interface ResidentsIntent : UiIntent {
    data class ResidentClicked(val residentId: String) : ResidentsIntent

    /** Offered when the list is empty, e.g. the first download failed; ignored while a download runs. */
    data object RetryClicked : ResidentsIntent
}

sealed interface ResidentsEffect : UiEffect {
    data class OpenResident(val residentId: String) : ResidentsEffect
}
