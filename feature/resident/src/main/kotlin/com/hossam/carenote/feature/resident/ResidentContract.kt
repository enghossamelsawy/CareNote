package com.hossam.carenote.feature.resident

import com.hossam.carenote.core.domain.vitals.VitalsAssessment
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
    /** Picks the avatar's color, so it matches the one in the residents list. */
    val residentId: String = "",
    val name: String = "",
    val room: String = "",
    val notFound: Boolean = false,
    /** Newest first. */
    val notes: List<NoteItem> = emptyList(),
) : UiState

/**
 * The latest version of one note; the screen formats the times and vitals for the reader's locale.
 * [createdAt] is when it was first written, [editedAt] when it was last edited, or null if never.
 */
data class NoteItem(
    val id: UUID,
    val createdAt: Instant,
    val transcript: String,
    val vitals: Vitals,
    /** Which [vitals] are outside the usual range; decided by the domain, not the screen. */
    val assessment: VitalsAssessment,
    val status: SyncStatus,
    val editedAt: Instant? = null,
)

sealed interface ResidentIntent : UiIntent {
    data object BackClicked : ResidentIntent

    data object RecordClicked : ResidentIntent

    /** Only offered for notes the server refused. */
    data class RetryClicked(val noteId: UUID) : ResidentIntent

    data class EditClicked(val noteId: UUID) : ResidentIntent
}

sealed interface ResidentEffect : UiEffect {
    data object NavigateBack : ResidentEffect

    data class OpenRecord(val residentId: String) : ResidentEffect

    data class OpenEditor(val residentId: String, val noteId: UUID) : ResidentEffect
}
