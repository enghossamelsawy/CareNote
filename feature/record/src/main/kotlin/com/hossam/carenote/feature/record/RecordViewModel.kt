package com.hossam.carenote.feature.record

import androidx.lifecycle.viewModelScope
import com.hossam.carenote.core.domain.speech.TranscriptionError
import com.hossam.carenote.core.domain.speech.TranscriptionEvent
import com.hossam.carenote.core.domain.usecase.ObserveResidentUseCase
import com.hossam.carenote.core.domain.usecase.TranscribeSpeechUseCase
import com.hossam.carenote.core.ui.mvi.MviViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Decides everything about recording; the route only runs the Android parts (permission dialog, settings, haptics).
 * Recording lives in [viewModelScope], so leaving the screen always releases the microphone.
 */
@HiltViewModel(assistedFactory = RecordViewModel.Factory::class)
class RecordViewModel @AssistedInject constructor(
    @Assisted residentId: String,
    observeResident: ObserveResidentUseCase,
    private val transcribeSpeech: TranscribeSpeechUseCase,
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
            RecordIntent.RecordClicked -> if (currentState.phase != RecordPhase.Recording) {
                sendEffect(RecordEffect.RequestMicPermission)
            }

            RecordIntent.StopClicked -> stopRecording()
            is RecordIntent.PermissionResult -> onPermissionResult(intent)
            RecordIntent.OpenSettingsClicked -> sendEffect(RecordEffect.OpenAppSettings)
            RecordIntent.BackClicked -> sendEffect(RecordEffect.NavigateBack)
        }
    }

    private fun onPermissionResult(result: RecordIntent.PermissionResult) {
        when {
            result.granted -> startRecording()
            result.canAskAgain -> setState { copy(permission = MicPermission.Denied) }
            else -> setState { copy(permission = MicPermission.PermanentlyDenied) }
        }
    }

    private fun startRecording() {
        setState {
            copy(phase = RecordPhase.Recording, transcript = "", error = null, permission = MicPermission.Granted)
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
                copy(phase = RecordPhase.Done)
            }
        }
        sendEffect(RecordEffect.Haptic(HapticKind.Stop))
    }

    private fun onTranscriptionEvent(event: TranscriptionEvent) {
        when (event) {
            is TranscriptionEvent.Partial -> setState { copy(transcript = event.text) }

            is TranscriptionEvent.Final -> {
                setState { copy(phase = RecordPhase.Done, transcript = event.text) }
                sendEffect(RecordEffect.Haptic(HapticKind.Stop))
            }

            is TranscriptionEvent.Error -> {
                setState { failedWith(event.reason) }
                sendEffect(RecordEffect.Haptic(HapticKind.Stop))
            }
        }
    }

    private fun RecordState.failedWith(reason: TranscriptionError): RecordState {
        val phase = if (transcript.isBlank()) RecordPhase.Idle else RecordPhase.Done
        return when (reason) {
            // Revoked while the screen was open, e.g. from the quick settings tile.
            TranscriptionError.NoPermission -> copy(phase = phase, permission = MicPermission.Denied)
            TranscriptionError.NoSpeech -> copy(phase = phase, error = RecordError.NoSpeech)
            TranscriptionError.Busy -> copy(phase = phase, error = RecordError.Busy)
            TranscriptionError.Unavailable -> copy(phase = phase, error = RecordError.Unavailable)
            TranscriptionError.Unknown -> copy(phase = phase, error = RecordError.Unknown)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(residentId: String): RecordViewModel
    }
}
