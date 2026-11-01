package com.hossam.carenote.feature.record

import com.hossam.carenote.core.ui.mvi.UiEffect
import com.hossam.carenote.core.ui.mvi.UiIntent
import com.hossam.carenote.core.ui.mvi.UiState

data class RecordState(
    val residentName: String = "",
    val phase: RecordPhase = RecordPhase.Idle,
    /** The latest words heard; kept when recording stops, and editable once [RecordPhase.Done]. */
    val transcript: String = "",
    val error: RecordError? = null,
    val permission: MicPermission = MicPermission.Unknown,
    val vitals: VitalsForm = VitalsForm(),
    val isSaving: Boolean = false,
    /** Leaving would lose a recording or an unsaved note, so the screen asks first. */
    val showDiscardDialog: Boolean = false,
    /** The note as it was when editing started; null when writing a new note. */
    val editedFrom: EditedNote? = null,
    /** The note to edit is gone or not this resident's: nothing can be recorded or saved, only gone back from. */
    val cannotEdit: Boolean = false,
) : UiState {
    val isEditing: Boolean
        get() = editedFrom != null

    /** Whether going back now would throw away something the user dictated or typed. */
    val hasUnsavedNote: Boolean
        get() = phase == RecordPhase.Recording || (phase == RecordPhase.Done && hasChanges)

    /** An edit counts once it differs from the note it started from; a new note once it has anything in it. */
    private val hasChanges: Boolean
        get() = if (editedFrom != null) {
            transcript != editedFrom.transcript || vitals.values != editedFrom.vitals.values
        } else {
            transcript.isNotBlank() || vitals.values.values.any { it.isNotBlank() }
        }
}

/** What an edited note held when it was opened. */
data class EditedNote(val transcript: String, val vitals: VitalsForm)

enum class RecordPhase {
    Idle,
    Recording,

    /** Recording finished: the note can be reviewed, corrected and saved. */
    Done,
}

enum class MicPermission {
    /** Not asked yet in this screen. */
    Unknown,
    Granted,

    /** Refused, but the system lets us ask again: show why we need it. */
    Denied,

    /** Refused for good: only the app settings can change it. */
    PermanentlyDenied,
}

enum class RecordError {
    NoSpeech,
    Busy,
    Unavailable,
    Unknown,
    EmptyNote,

    /** The note being edited was already edited, or no longer exists. */
    CannotEdit,

    /** The phone couldn't store the note; it's still on screen and can be saved again. */
    SaveFailed,
}

enum class VitalField { Systolic, Diastolic, Pulse, Temperature, OxygenSaturation }

sealed interface FieldError {
    data object NotANumber : FieldError

    /** Outside what a person can have; [min] and [max] are the accepted range, ready to show. */
    data class OutOfRange(val min: String, val max: String) : FieldError

    /** The other blood pressure value is given, this one is missing. */
    data object Missing : FieldError

    data object NotBelowSystolic : FieldError
}

/** The vitals as typed: plain text, so half-typed or wrong input can be shown and corrected. */
data class VitalsForm(
    val values: Map<VitalField, String> = emptyMap(),
    val errors: Map<VitalField, FieldError> = emptyMap(),
) {
    operator fun get(field: VitalField): String = values[field].orEmpty()
}

sealed interface RecordIntent : UiIntent {
    /** Starts a recording; from [RecordPhase.Done] it starts the note over. */
    data object RecordClicked : RecordIntent

    /** From [RecordPhase.Done]: dictates more and adds it to the note, e.g. one vital at a time. */
    data object RecordMoreClicked : RecordIntent

    /**
     * From [RecordPhase.Idle]: opens the review form empty, to write the note by hand. For a phone without
     * speech recognition, a refused microphone, or a room where dictating isn't possible.
     */
    data object TypeInsteadClicked : RecordIntent

    /** Asks for the microphone again after a refusal, for whichever recording was asked for. */
    data object AllowMicrophoneClicked : RecordIntent

    data object StopClicked : RecordIntent

    /**
     * The answer to a microphone request. [rationaleBefore] and [rationaleAfter] are Android's "should show
     * rationale" right before and right after asking; together they tell a refusal from a dismissed dialog.
     */
    data class PermissionResult(
        val granted: Boolean,
        val rationaleBefore: Boolean = false,
        val rationaleAfter: Boolean = false,
    ) : RecordIntent

    /** The screen is in the foreground again, e.g. back from the app settings; [micGranted] is the permission now. */
    data class ScreenResumed(val micGranted: Boolean) : RecordIntent

    data object OpenSettingsClicked : RecordIntent

    /** The top bar arrow or the system back gesture. */
    data object BackClicked : RecordIntent

    data object DiscardConfirmed : RecordIntent

    data object DiscardDismissed : RecordIntent

    data class TranscriptChanged(val text: String) : RecordIntent

    data class VitalChanged(val field: VitalField, val text: String) : RecordIntent

    data object SaveClicked : RecordIntent
}

sealed interface RecordEffect : UiEffect {
    /** The route checks the permission and asks only if needed, then answers with [RecordIntent.PermissionResult]. */
    data object RequestMicPermission : RecordEffect

    data object OpenAppSettings : RecordEffect

    data class Haptic(val kind: HapticKind) : RecordEffect

    data object NavigateBack : RecordEffect

    /** Stored locally and queued for sync; the screen's job is done. */
    data object NoteSaved : RecordEffect
}

enum class HapticKind { Start, Stop }
