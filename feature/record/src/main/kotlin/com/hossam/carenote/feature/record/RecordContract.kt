package com.hossam.carenote.feature.record

import com.hossam.carenote.core.ui.mvi.UiEffect
import com.hossam.carenote.core.ui.mvi.UiIntent
import com.hossam.carenote.core.ui.mvi.UiState

data class RecordState(
    val residentName: String = "",
    val phase: RecordPhase = RecordPhase.Idle,
    /** The latest words heard; kept when recording stops. */
    val transcript: String = "",
    val error: RecordError? = null,
    val permission: MicPermission = MicPermission.Unknown,
) : UiState

enum class RecordPhase { Idle, Recording, Done }

enum class MicPermission {
    /** Not asked yet in this screen. */
    Unknown,
    Granted,

    /** Refused, but the system lets us ask again: show why we need it. */
    Denied,

    /** Refused for good: only the app settings can change it. */
    PermanentlyDenied,
}

enum class RecordError { NoSpeech, Busy, Unavailable, Unknown }

sealed interface RecordIntent : UiIntent {
    data object RecordClicked : RecordIntent

    data object StopClicked : RecordIntent

    /** [canAskAgain] is Android's "should show rationale" after a refusal. */
    data class PermissionResult(val granted: Boolean, val canAskAgain: Boolean) : RecordIntent

    data object OpenSettingsClicked : RecordIntent

    data object BackClicked : RecordIntent
}

sealed interface RecordEffect : UiEffect {
    /** The route checks the permission and asks only if needed, then answers with [RecordIntent.PermissionResult]. */
    data object RequestMicPermission : RecordEffect

    data object OpenAppSettings : RecordEffect

    data class Haptic(val kind: HapticKind) : RecordEffect

    data object NavigateBack : RecordEffect
}

enum class HapticKind { Start, Stop }
