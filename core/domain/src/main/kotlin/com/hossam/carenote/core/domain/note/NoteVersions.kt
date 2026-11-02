package com.hossam.carenote.core.domain.note

import com.hossam.carenote.core.model.CareNote
import java.time.Instant
import java.util.UUID

/**
 * How one resident's notes link up through edits: each edit is a new note whose revisionOf points at
 * the version it replaces. Missing links and loops end a chain instead of failing, since notes may
 * arrive from the server in any order.
 */
internal class NoteVersions(notes: List<CareNote>) {
    private val byId = notes.associateBy { it.id }
    private val replacedBy = notes.mapNotNull { note -> note.revisionOf?.let { it to note } }.toMap()

    /** The latest version of every note. */
    val current: List<CareNote> = notes.filter { it.id !in replacedBy }

    /** The latest version of the note [noteId] belongs to, from any of its versions; null if unknown. */
    fun latest(noteId: UUID): CareNote? {
        var note = byId[noteId] ?: return null
        val seen = mutableSetOf(note.id)
        while (true) {
            val next = replacedBy[note.id] ?: return note
            if (!seen.add(next.id)) return note
            note = next
        }
    }

    /** When the first version of [note] was written; an edit corrects that note, so it keeps this time. */
    fun firstWrittenAt(note: CareNote): Instant = (earlier(note).lastOrNull() ?: note).createdAt

    /** The versions [note] replaced, newest first. */
    fun earlier(note: CareNote): List<CareNote> = buildList {
        val seen = mutableSetOf(note.id)
        var previous = note.revisionOf?.let(byId::get)
        while (previous != null && seen.add(previous.id)) {
            add(previous)
            previous = previous.revisionOf?.let(byId::get)
        }
    }
}
