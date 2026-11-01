package com.hossam.carenote.core.model

import java.time.Instant

/**
 * One note with every version it went through: [current] is the latest, [earlier] the ones it
 * replaced, newest first.
 */
data class NoteHistory(val current: CareNote, val earlier: List<CareNote>) {
    /** When the first version was written; the note keeps this time however often it's edited. */
    val firstWrittenAt: Instant
        get() = (earlier.lastOrNull() ?: current).createdAt

    val isEdited: Boolean
        get() = current.revisionOf != null
}
