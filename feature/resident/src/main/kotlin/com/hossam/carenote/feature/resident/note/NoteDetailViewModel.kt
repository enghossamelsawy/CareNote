package com.hossam.carenote.feature.resident.note

import androidx.lifecycle.viewModelScope
import com.hossam.carenote.core.domain.usecase.AssessVitalsUseCase
import com.hossam.carenote.core.domain.usecase.ObserveNoteHistoryUseCase
import com.hossam.carenote.core.domain.usecase.RetryCareNoteSyncUseCase
import com.hossam.carenote.core.ui.mvi.MviViewModel
import com.hossam.carenote.feature.resident.asItem
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * One note in full, with its earlier versions. Opened with any version's [noteId], it always shows
 * the latest one, so coming back from an edit shows the edit.
 */
@HiltViewModel(assistedFactory = NoteDetailViewModel.Factory::class)
class NoteDetailViewModel @AssistedInject constructor(
    @Assisted residentId: String,
    @Assisted noteId: UUID,
    observeNoteHistory: ObserveNoteHistoryUseCase,
    private val assessVitals: AssessVitalsUseCase,
    private val retryCareNoteSync: RetryCareNoteSyncUseCase,
) : MviViewModel<NoteDetailState, NoteDetailIntent, NoteDetailEffect>(NoteDetailState()) {
    init {
        observeNoteHistory(residentId, noteId)
            .onEach { history ->
                setState {
                    if (history == null) {
                        NoteDetailState(isLoading = false, notFound = true)
                    } else {
                        NoteDetailState(
                            isLoading = false,
                            note = history.current.asItem(history.firstWrittenAt, assessVitals(history.current.vitals)),
                            earlier = history.earlier.map { version ->
                                EarlierVersion(
                                    id = version.id,
                                    writtenAt = version.createdAt,
                                    transcript = version.transcript,
                                    status = version.syncStatus,
                                )
                            },
                        )
                    }
                }
            }.launchIn(viewModelScope)
    }

    override fun onIntent(intent: NoteDetailIntent) {
        val note = currentState.note
        when (intent) {
            NoteDetailIntent.BackClicked -> sendEffect(NoteDetailEffect.NavigateBack)
            NoteDetailIntent.EditClicked -> if (note != null) sendEffect(NoteDetailEffect.OpenEditor(note.id))
            is NoteDetailIntent.RetryClicked -> if (isShown(intent.noteId)) {
                viewModelScope.launch { retryCareNoteSync(intent.noteId) }
            }
        }
    }

    /** Only versions of the note on screen are retried, never an id from elsewhere. */
    private fun isShown(noteId: UUID): Boolean =
        currentState.note?.id == noteId || currentState.earlier.any { it.id == noteId }

    @AssistedFactory
    interface Factory {
        fun create(residentId: String, noteId: UUID): NoteDetailViewModel
    }
}
