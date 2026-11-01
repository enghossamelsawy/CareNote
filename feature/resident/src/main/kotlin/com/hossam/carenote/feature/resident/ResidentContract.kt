package com.hossam.carenote.feature.resident

import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.ui.mvi.UiEffect
import com.hossam.carenote.core.ui.mvi.UiIntent
import com.hossam.carenote.core.ui.mvi.UiState
import java.time.Instant
import java.util.UUID

/** [notFound] is true when the resident no longer exists, e.g. after a server-side removal. */
data class ResidentState(
    val isLoading: Boolean = true,
    val name: String = "",
    val room: String = "",
    val notFound: Boolean = false,
    /** Newest first. */
    val notes: List<NoteItem> = emptyList(),
) : UiState

/** One note in the list; the screen formats the time and vitals for the reader's locale. */
data class NoteItem(
    val id: UUID,
    val createdAt: Instant,
    val transcript: String,
    val vitals: Vitals,
    val status: SyncStatus,
)

sealed interface ResidentIntent : UiIntent {
    data object BackClicked : ResidentIntent

    data object RecordClicked : ResidentIntent

    /** Only offered for notes the server refused. */
    data class RetryClicked(val noteId: UUID) : ResidentIntent
}

sealed interface ResidentEffect : UiEffect {
    data object NavigateBack : ResidentEffect

    data class OpenRecord(val residentId: String) : ResidentEffect
}
