package com.hossam.carenote.feature.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hossam.carenote.core.domain.speech.TranscriptionError
import com.hossam.carenote.core.domain.speech.TranscriptionEvent
import com.hossam.carenote.core.domain.usecase.GetCareNoteUseCase
import com.hossam.carenote.core.domain.usecase.ObserveResidentUseCase
import com.hossam.carenote.core.domain.usecase.ParseVitalsUseCase
import com.hossam.carenote.core.domain.usecase.SaveCareNoteUseCase
import com.hossam.carenote.core.domain.usecase.SaveResult
import com.hossam.carenote.core.domain.usecase.TranscribeSpeechUseCase
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.ui.mvi.MviViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Decides everything about recording and reviewing a note; the route only runs the Android parts
 * (permission dialog, settings, haptics). Recording lives in [viewModelScope], so leaving the screen
 * always releases the microphone.
 *
 * With [noteId] set it edits that note: the screen opens in review with the note filled in, "Record
 * more" adds to it, and saving stores the result as a new version of it.
 *
 * The unfinished note is kept in [RecordDraft], so it's still there if Android stops the app in the background.
 */
@HiltViewModel(assistedFactory = RecordViewModel.Factory::class)
class RecordViewModel @AssistedInject constructor(
    @Assisted private val residentId: String,
    @Assisted private val noteId: UUID?,
    savedStateHandle: SavedStateHandle,
    observeResident: ObserveResidentUseCase,
    getCareNote: GetCareNoteUseCase,
    private val transcribeSpeech: TranscribeSpeechUseCase,
    private val parseVitals: ParseVitalsUseCase,
    private val saveCareNote: SaveCareNoteUseCase,
) : MviViewModel<RecordState, RecordIntent, RecordEffect>(RecordState()) {
    private val draft = RecordDraft(savedStateHandle)
    private var recording: Job? = null

    // A permission request is on its way; further taps wait for it, so the first tap decides.
    private var awaitingPermission = false

    // Refusals in a row that Android gave no hint about: a dismissed dialog, or one it didn't show at all.
    private var silentRefusals = 0

    // One recording's bookkeeping: what the note held before it started, and the words heard since.
    private var appendNext = false
    private var keptTranscript = ""
    private var keptVitals = VitalsForm()
    private var heard = ""

    init {
        draft.restore()?.let { restored -> setState { restored } }
        state.onEach(draft::keep).launchIn(viewModelScope)
        observeResident(residentId)
            .onEach { resident ->
                setState { copy(residentName = resident?.let { "${it.firstName} ${it.lastName}" }.orEmpty()) }
            }.launchIn(viewModelScope)
        if (noteId != null) {
            setState { copy(isLoadingNote = true) }
            viewModelScope.launch { startEditing(getCareNote(noteId)) }
        }
    }

    /** Opens the note in review, unless it's already open (restored or being recorded) or isn't this resident's. */
    private fun startEditing(note: CareNote?) {
        if (note == null || note.residentId != residentId) {
            // A dictation here could never be saved as an edit, so recording isn't offered at all.
            setState { copy(isLoadingNote = false, cannotEdit = true) }
            return
        }
        if (currentState.phase != RecordPhase.Idle) {
            setState { copy(isLoadingNote = false) }
            return
        }
        val vitals = note.vitals.asForm()
        setState {
            copy(
                isLoadingNote = false,
                phase = RecordPhase.Done,
                transcript = note.transcript,
                vitals = vitals,
                editedFrom = EditedNote(transcript = note.transcript, vitals = vitals),
            )
        }
    }

    override fun onIntent(intent: RecordIntent) {
        when (intent) {
            RecordIntent.RecordClicked -> requestRecording(append = false)
            RecordIntent.RecordMoreClicked -> requestRecording(append = true)
            RecordIntent.TypeInsteadClicked -> typeInstead()
            RecordIntent.AllowMicrophoneClicked -> sendEffect(RecordEffect.RequestMicPermission)
            RecordIntent.StopClicked -> stopRecording()
            is RecordIntent.PermissionResult -> onPermissionResult(intent)
            is RecordIntent.ScreenResumed -> onScreenResumed(intent.micGranted)
            RecordIntent.OpenSettingsClicked -> sendEffect(RecordEffect.OpenAppSettings)
            RecordIntent.BackClicked -> when {
                // The local save takes a moment and then closes the screen itself; leaving now could cut it short.
                currentState.isSaving -> Unit
                currentState.hasUnsavedNote -> setState { copy(showDiscardDialog = true) }
                else -> sendEffect(RecordEffect.NavigateBack)
            }

            RecordIntent.DiscardConfirmed -> {
                setState { copy(showDiscardDialog = false) }
                // Closing the screen clears this ViewModel, which also stops a running recording.
                sendEffect(RecordEffect.NavigateBack)
            }

            RecordIntent.DiscardDismissed -> setState { copy(showDiscardDialog = false) }
            // While saving, the note is locked: it's saved exactly as it was when Save was tapped.
            is RecordIntent.TranscriptChanged -> if (currentState.phase == RecordPhase.Done && !currentState.isSaving) {
                setState { copy(transcript = intent.text, error = null) }
            }

            is RecordIntent.VitalChanged -> if (currentState.phase == RecordPhase.Done && !currentState.isSaving) {
                changeVital(intent)
            }
            RecordIntent.SaveClicked -> save()
        }
    }

    /** [append] keeps the note in review and adds the new words to it; otherwise the next recording starts over. */
    private fun requestRecording(append: Boolean) {
        val state = currentState
        if (awaitingPermission || state.phase == RecordPhase.Recording || state.isSaving || !state.canRecord) return
        awaitingPermission = true
        appendNext = append
        sendEffect(RecordEffect.RequestMicPermission)
    }

    /** The same review form as after dictating, empty; saving, checks and "Record more" all work as usual. */
    private fun typeInstead() {
        if (currentState.phase != RecordPhase.Idle || !currentState.canRecord) return
        setState { copy(phase = RecordPhase.Done, transcript = "", vitals = VitalsForm(), error = null) }
    }

    /**
     * Android's "should show rationale" alone can't tell a dismissed dialog from "Don't ask again": both answer
     * false. Comparing it before and after the request can:
     * - true after: refused once, Android will ask again.
     * - true before, false after: the carer chose not to be asked again; only the settings can help.
     * - false both times: the dialog was dismissed, or Android refused without showing it. Asking once more is
     *   worth it, but a second time in a row means asking doesn't work, so the settings are offered instead of
     *   a button that would loop.
     */
    private fun onPermissionResult(result: RecordIntent.PermissionResult) {
        awaitingPermission = false
        when {
            result.granted -> {
                silentRefusals = 0
                startRecording()
            }

            result.rationaleAfter -> {
                silentRefusals = 0
                setState { copy(permission = MicPermission.Denied) }
            }

            result.rationaleBefore -> setState { copy(permission = MicPermission.PermanentlyDenied) }

            else -> {
                silentRefusals++
                val permission = if (silentRefusals >= SILENT_REFUSALS_BEFORE_SETTINGS) {
                    MicPermission.PermanentlyDenied
                } else {
                    MicPermission.Denied
                }
                setState { copy(permission = permission) }
            }
        }
    }

    /**
     * Android answers a permission request before the screen resumes, so a request still open here was lost;
     * it must not keep blocking Record. Access granted in the settings hides the card, without recording.
     */
    private fun onScreenResumed(micGranted: Boolean) {
        awaitingPermission = false
        val refused = currentState.permission == MicPermission.Denied ||
            currentState.permission == MicPermission.PermanentlyDenied
        if (micGranted && refused) {
            silentRefusals = 0
            setState { copy(permission = MicPermission.Granted) }
        }
    }

    /** Only ever one recording: a second start is ignored, and no earlier listener is left holding the microphone. */
    private fun startRecording() {
        if (currentState.phase == RecordPhase.Recording || !currentState.canRecord) return
        recording?.cancel()
        val append = appendNext && currentState.phase == RecordPhase.Done
        keptTranscript = if (append) currentState.transcript else ""
        keptVitals = if (append) currentState.vitals else VitalsForm()
        heard = ""
        setState {
            copy(
                phase = RecordPhase.Recording,
                transcript = keptTranscript,
                error = null,
                permission = MicPermission.Granted,
                vitals = keptVitals,
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
            when {
                heard.isNotBlank() -> review()
                hasKeptNote -> review().copy(error = RecordError.NoSpeech)
                else -> copy(phase = RecordPhase.Idle, error = RecordError.NoSpeech)
            }
        }
        sendEffect(RecordEffect.Haptic(HapticKind.Stop))
    }

    private fun onTranscriptionEvent(event: TranscriptionEvent) {
        when (event) {
            is TranscriptionEvent.Partial -> {
                heard = event.text
                setState { copy(transcript = joined(keptTranscript, heard)) }
            }

            is TranscriptionEvent.Final -> {
                heard = event.text
                setState { review() }
                sendEffect(RecordEffect.Haptic(HapticKind.Stop))
            }

            is TranscriptionEvent.Error -> {
                setState { failedWith(event.reason) }
                sendEffect(RecordEffect.Haptic(HapticKind.Stop))
            }
        }
    }

    /**
     * The note after a recording: the kept words plus the new ones. Only the new words are parsed, and they
     * replace just the vitals they mention; every other field, hand corrections included, stays as it was.
     * Later transcript edits never change the vitals.
     */
    private fun RecordState.review(): RecordState = copy(
        phase = RecordPhase.Done,
        transcript = joined(keptTranscript, heard),
        vitals = keptVitals.withDictated(parseVitals(heard)),
    )

    private val hasKeptNote: Boolean
        get() = keptTranscript.isNotBlank() || keptVitals.values.values.any { it.isNotBlank() }

    private fun joined(kept: String, new: String) = listOf(kept, new).filter { it.isNotBlank() }.joinToString(" ")

    private fun RecordState.failedWith(reason: TranscriptionError): RecordState {
        val stopped = if (heard.isBlank() && !hasKeptNote) copy(phase = RecordPhase.Idle) else review()
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
        if (state.phase != RecordPhase.Done || state.isSaving || state.cannotEdit) return

        val unreadable = state.vitals.unreadableFields()
        if (unreadable.isNotEmpty()) {
            setState { copy(vitals = vitals.copy(errors = unreadable.associateWith { FieldError.NotANumber })) }
            return
        }

        setState { copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val result = saveCareNote(
                residentId = residentId,
                transcript = state.transcript,
                vitals = state.vitals.toVitals(),
                revisionOf = noteId,
            )
            when (result) {
                // Stays "saving" until the screen closes, so a second tap can't save the note twice.
                is SaveResult.Saved -> {
                    draft.clear()
                    sendEffect(RecordEffect.NoteSaved)
                }

                is SaveResult.Invalid -> setState {
                    copy(isSaving = false, vitals = vitals.copy(errors = vitals.errorsFor(result.problems)))
                }

                SaveResult.Empty -> setState { copy(isSaving = false, error = RecordError.EmptyNote) }

                SaveResult.CannotRevise -> setState { copy(isSaving = false, error = RecordError.CannotEdit) }

                // The note stays on screen and unlocked, so Save can simply be tapped again.
                SaveResult.Failed -> setState { copy(isSaving = false, error = RecordError.SaveFailed) }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(residentId: String, noteId: UUID?): RecordViewModel
    }

    private companion object {
        const val SILENT_REFUSALS_BEFORE_SETTINGS = 2
    }
}
