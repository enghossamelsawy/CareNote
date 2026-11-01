package com.hossam.carenote.feature.record

import androidx.lifecycle.viewModelScope
import com.hossam.carenote.core.domain.speech.TranscriptionError
import com.hossam.carenote.core.domain.speech.TranscriptionEvent
import com.hossam.carenote.core.domain.usecase.ObserveResidentUseCase
import com.hossam.carenote.core.domain.usecase.ParseVitalsUseCase
import com.hossam.carenote.core.domain.usecase.SaveCareNoteUseCase
import com.hossam.carenote.core.domain.usecase.SaveResult
import com.hossam.carenote.core.domain.usecase.TranscribeSpeechUseCase
import com.hossam.carenote.core.ui.mvi.MviViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Decides everything about recording and reviewing a note; the route only runs the Android parts
 * (permission dialog, settings, haptics). Recording lives in [viewModelScope], so leaving the screen
 * always releases the microphone.
 */
@HiltViewModel(assistedFactory = RecordViewModel.Factory::class)
class RecordViewModel @AssistedInject constructor(
    @Assisted private val residentId: String,
    observeResident: ObserveResidentUseCase,
    private val transcribeSpeech: TranscribeSpeechUseCase,
    private val parseVitals: ParseVitalsUseCase,
    private val saveCareNote: SaveCareNoteUseCase,
) : MviViewModel<RecordState, RecordIntent, RecordEffect>(RecordState()) {
    private var recording: Job? = null

    init {
        observeResident(residentId)
            .onEach { resident ->
                setState { copy(residentName = resident?.let { "${it.firstName} ${it.lastName}" }.orEmpty()) }
            }.launchIn(viewModelScope)
    }

    override fun onIntent(intent: RecordIntent) {
        when (intent) {
            RecordIntent.RecordClicked -> if (currentState.phase != RecordPhase.Recording && !currentState.isSaving) {
                sendEffect(RecordEffect.RequestMicPermission)
            }

            RecordIntent.StopClicked -> stopRecording()
            is RecordIntent.PermissionResult -> onPermissionResult(intent)
            RecordIntent.OpenSettingsClicked -> sendEffect(RecordEffect.OpenAppSettings)
            RecordIntent.BackClicked -> sendEffect(RecordEffect.NavigateBack)
            is RecordIntent.TranscriptChanged -> if (currentState.phase == RecordPhase.Done) {
                setState { copy(transcript = intent.text, error = null) }
            }

            is RecordIntent.VitalChanged -> if (currentState.phase == RecordPhase.Done) changeVital(intent)
            RecordIntent.SaveClicked -> save()
        }
    }

    private fun onPermissionResult(result: RecordIntent.PermissionResult) {
        when {
            result.granted -> startRecording()
            result.canAskAgain -> setState { copy(permission = MicPermission.Denied) }
            else -> setState { copy(permission = MicPermission.PermanentlyDenied) }
        }
    }

    /** Starts from scratch: a new recording replaces the transcript and the vitals. */
    private fun startRecording() {
        setState {
            copy(
                phase = RecordPhase.Recording,
                transcript = "",
                error = null,
                permission = MicPermission.Granted,
                vitals = VitalsForm(),
            )
        }
        sendEffect(RecordEffect.Haptic(HapticKind.Start))
        recording = transcribeSpeech()
            .onEach(::onTranscriptionEvent)
            .launchIn(viewModelScope)
    }

    /** Cancelling the collection stops the engine and frees the microphone; the words heard so far stay. */
    private fun stopRecording() {
        if (currentState.phase != RecordPhase.Recording) return
        recording?.cancel()
        setState {
            if (transcript.isBlank()) {
                copy(phase = RecordPhase.Idle, error = RecordError.NoSpeech)
            } else {
                review(transcript)
            }
        }
        sendEffect(RecordEffect.Haptic(HapticKind.Stop))
    }

    private fun onTranscriptionEvent(event: TranscriptionEvent) {
        when (event) {
            is TranscriptionEvent.Partial -> setState { copy(transcript = event.text) }

            is TranscriptionEvent.Final -> {
                setState { review(event.text) }
                sendEffect(RecordEffect.Haptic(HapticKind.Stop))
            }

            is TranscriptionEvent.Error -> {
                setState { failedWith(event.reason) }
                sendEffect(RecordEffect.Haptic(HapticKind.Stop))
            }
        }
    }

    /** Parses once, when recording ends; later transcript edits never overwrite vitals corrected by hand. */
    private fun RecordState.review(text: String): RecordState =
        copy(phase = RecordPhase.Done, transcript = text, vitals = parseVitals(text).asForm())

    private fun RecordState.failedWith(reason: TranscriptionError): RecordState {
        val stopped = if (transcript.isBlank()) copy(phase = RecordPhase.Idle) else review(transcript)
        return when (reason) {
            // Revoked while the screen was open, e.g. from the quick settings tile.
            TranscriptionError.NoPermission -> stopped.copy(permission = MicPermission.Denied)
            TranscriptionError.NoSpeech -> stopped.copy(error = RecordError.NoSpeech)
            TranscriptionError.Busy -> stopped.copy(error = RecordError.Busy)
            TranscriptionError.Unavailable -> stopped.copy(error = RecordError.Unavailable)
            TranscriptionError.Unknown -> stopped.copy(error = RecordError.Unknown)
        }
    }

    /** Changing a value clears its error; a blood pressure error belongs to both values, so both clear. */
    private fun changeVital(change: RecordIntent.VitalChanged) {
        val bloodPressure = setOf(VitalField.Systolic, VitalField.Diastolic)
        val cleared = if (change.field in bloodPressure) bloodPressure else setOf(change.field)
        setState {
            copy(
                vitals = vitals.copy(
                    values = vitals.values + (change.field to change.text),
                    errors = vitals.errors - cleared,
                ),
            )
        }
    }

    private fun save() {
        val state = currentState
        if (state.phase != RecordPhase.Done || state.isSaving) return

        val unreadable = state.vitals.unreadableFields()
        if (unreadable.isNotEmpty()) {
            setState { copy(vitals = vitals.copy(errors = unreadable.associateWith { FieldError.NotANumber })) }
            return
        }

        setState { copy(isSaving = true, error = null) }
        viewModelScope.launch {
            when (val result = saveCareNote(residentId, state.transcript, state.vitals.toVitals())) {
                // Stays "saving" until the screen closes, so a second tap can't save the note twice.
                is SaveResult.Saved -> sendEffect(RecordEffect.NoteSaved)

                is SaveResult.Invalid -> setState {
                    copy(isSaving = false, vitals = vitals.copy(errors = vitals.errorsFor(result.problems)))
                }

                SaveResult.Empty -> setState { copy(isSaving = false, error = RecordError.EmptyNote) }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(residentId: String): RecordViewModel
    }
}
