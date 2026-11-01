package com.hossam.carenote.core.model

import java.time.Instant

/**
 * The latest version of a note, as the list shows it. [firstWrittenAt] is when the first version was
 * written, so an edited note keeps its place and time; [note]'s own createdAt is when it was edited.
 */
data class CurrentCareNote(val note: CareNote, val firstWrittenAt: Instant) {
    val isEdited: Boolean
        get() = note.revisionOf != null
}
