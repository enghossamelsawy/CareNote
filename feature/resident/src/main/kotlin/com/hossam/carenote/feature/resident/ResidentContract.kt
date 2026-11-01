package com.hossam.carenote.feature.resident

import com.hossam.carenote.core.ui.mvi.UiEffect
import com.hossam.carenote.core.ui.mvi.UiIntent
import com.hossam.carenote.core.ui.mvi.UiState

/** [notFound] is true when the resident no longer exists, e.g. after a server-side removal. */
data class ResidentState(
    val isLoading: Boolean = true,
    val name: String = "",
    val room: String = "",
    val notFound: Boolean = false,
) : UiState

sealed interface ResidentIntent : UiIntent {
    data object BackClicked : ResidentIntent
}

sealed interface ResidentEffect : UiEffect {
    data object NavigateBack : ResidentEffect
}
