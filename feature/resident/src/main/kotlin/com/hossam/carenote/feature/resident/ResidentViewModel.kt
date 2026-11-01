package com.hossam.carenote.feature.resident

import androidx.lifecycle.viewModelScope
import com.hossam.carenote.core.domain.usecase.ObserveCareNotesUseCase
import com.hossam.carenote.core.domain.usecase.ObserveResidentUseCase
import com.hossam.carenote.core.domain.usecase.RetryCareNoteSyncUseCase
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.ui.mvi.MviViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * [residentId] comes straight from the nav key through assisted injection, so no SavedStateHandle or string args.
 * The notes list updates by itself as the sync worker changes each note's status.
 */
@HiltViewModel(assistedFactory = ResidentViewModel.Factory::class)
class ResidentViewModel @AssistedInject constructor(
    @Assisted private val residentId: String,
    observeResident: ObserveResidentUseCase,
    observeCareNotes: ObserveCareNotesUseCase,
    private val retryCareNoteSync: RetryCareNoteSyncUseCase,
) : MviViewModel<ResidentState, ResidentIntent, ResidentEffect>(ResidentState()) {
    init {
        combine(observeResident(residentId), observeCareNotes(residentId)) { resident, notes ->
            if (resident == null) {
                ResidentState(isLoading = false, notFound = true)
            } else {
                ResidentState(
                    isLoading = false,
                    name = "${resident.firstName} ${resident.lastName}",
                    room = resident.room,
                    notes = notes.map { it.asItem() },
                )
            }
        }.onEach { state -> setState { state } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: ResidentIntent) {
        when (intent) {
            ResidentIntent.BackClicked -> sendEffect(ResidentEffect.NavigateBack)
            ResidentIntent.RecordClicked -> sendEffect(ResidentEffect.OpenRecord(residentId))
            is ResidentIntent.RetryClicked -> viewModelScope.launch { retryCareNoteSync(intent.noteId) }
        }
    }

    private fun CareNote.asItem() = NoteItem(
        id = id,
        createdAt = createdAt,
        transcript = transcript,
        vitals = vitals,
        status = syncStatus,
    )

    @AssistedFactory
    interface Factory {
        fun create(residentId: String): ResidentViewModel
    }
}
