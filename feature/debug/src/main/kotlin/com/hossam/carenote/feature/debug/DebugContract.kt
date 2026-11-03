package com.hossam.carenote.feature.debug

import com.hossam.carenote.core.ui.mvi.UiEffect
import com.hossam.carenote.core.ui.mvi.UiIntent
import com.hossam.carenote.core.ui.mvi.UiState

data class DebugState(
    val serverOffline: Boolean = false,
    val failureRate: FailureRate = FailureRate.None,
    val latency: Latency = Latency.Normal,
    /** Null until the first recording. */
    val speechEngine: String? = null,
    val receivedNoteCount: Int = 0,
    /** Newest first, at most [DebugViewModel.RECENT_NOTES]. Only ids and times: never a note's content. */
    val recentNotes: List<ReceivedNote> = emptyList(),
) : UiState

/** Share of requests the mock server answers with 503 Service Unavailable. */
enum class FailureRate(val share: Double) {
    None(0.0),
    Quarter(0.25),
    Half(0.5),
    All(1.0),
}

enum class Latency(val millis: LongRange) {
    Fast(0L..200L),
    Normal(300L..1200L),
    Slow(2_000L..5_000L),
}

data class ReceivedNote(val residentId: String, val createdAt: String)

sealed interface DebugIntent : UiIntent {
    data class ServerOfflineChanged(val offline: Boolean) : DebugIntent

    data class FailureRateSelected(val rate: FailureRate) : DebugIntent

    data class LatencySelected(val latency: Latency) : DebugIntent

    /** Puts a colleague's note on the server and syncs, to show other phones' notes arriving. */
    data object NoteFromAnotherPhoneClicked : DebugIntent

    data object BackClicked : DebugIntent
}

sealed interface DebugEffect : UiEffect {
    data object NavigateBack : DebugEffect
}
