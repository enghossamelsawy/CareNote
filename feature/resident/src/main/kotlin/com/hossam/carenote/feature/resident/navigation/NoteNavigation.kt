package com.hossam.carenote.feature.resident.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hossam.carenote.feature.resident.note.NoteDetailRoute
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * One note in full. [noteId] is the version it was opened with, as a UUID string so the back stack
 * survives process death; the screen always shows the latest version.
 */
@Serializable
data class NoteKey(val residentId: String, val noteId: String) : NavKey

/**
 * Adds the note screen to the app's nav graph; where back and editing lead is decided by the caller.
 * [onEditNote] gets this screen's own key, so the caller can open the editor on top of it.
 */
fun EntryProviderScope<NavKey>.noteEntry(onBack: () -> Unit, onEditNote: (from: NoteKey, noteId: UUID) -> Unit) {
    entry<NoteKey> { key ->
        NoteDetailRoute(
            residentId = key.residentId,
            noteId = UUID.fromString(key.noteId),
            onBack = onBack,
            onEdit = { noteId -> onEditNote(key, noteId) },
        )
    }
}
