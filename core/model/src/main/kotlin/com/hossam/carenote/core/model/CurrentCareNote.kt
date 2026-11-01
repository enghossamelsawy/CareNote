package com.hossam.carenote.core.model

import java.time.Instant

/**
 * The latest version of a note, as the list shows it. [firstWrittenAt] is when the first version was
 * written, so an edited note keeps its place and time; [note]'s own createdAt is when it was edited.
 *
 * [earlierNotSynced] is true when the server refused one of the versions [note] replaced. That version
 * still counts as not synced, so the list says so instead of hiding it behind the edit.
 */
data class CurrentCareNote(val note: CareNote, val firstWrittenAt: Instant, val earlierNotSynced: Boolean = false) {
    val isEdited: Boolean
        get() = note.revisionOf != null
}
