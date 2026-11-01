package com.hossam.carenote.feature.resident.note

import com.hossam.carenote.core.ui.mvi.UiEffect
import com.hossam.carenote.core.ui.mvi.UiIntent
import com.hossam.carenote.core.ui.mvi.UiState
import com.hossam.carenote.feature.resident.NoteItem
import java.time.Instant
import java.util.UUID

/** One note in full: its latest version in [note], and the versions it replaced in [earlier], newest first. */
data class NoteDetailState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val note: NoteItem? = null,
    val earlier: List<EarlierVersion> = emptyList(),
) : UiState

/** A replaced version, kept for the record: when it was written and what it said. */
data class EarlierVersion(val id: UUID, val writtenAt: Instant, val transcript: String)

sealed interface NoteDetailIntent : UiIntent {
    data object BackClicked : NoteDetailIntent

    data object EditClicked : NoteDetailIntent

    /** Only offered when the server refused the latest version. */
    data object RetryClicked : NoteDetailIntent
}

sealed interface NoteDetailEffect : UiEffect {
    data object NavigateBack : NoteDetailEffect

    /** Always the latest version, whichever version the screen was opened with. */
    data class OpenEditor(val noteId: UUID) : NoteDetailEffect
}
