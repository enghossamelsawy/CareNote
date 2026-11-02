package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.note.NoteVersions
import com.hossam.carenote.core.domain.repository.CareNoteRepository
import com.hossam.carenote.core.domain.repository.CurrentTime
import com.hossam.carenote.core.model.CareNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * The residents with no note for [OVERDUE_AFTER] or longer, by resident id, with when their last note was
 * written. Emits again when a note is saved and as time passes, but only when the result changes.
 *
 * Any note counts, with vitals or without: "ate well" still means someone checked. An edit keeps the time
 * of the note it corrects, so editing an old note doesn't reset the clock. Residents with no notes at all
 * are left out, since a new resident isn't overdue.
 */
class ObserveOverdueResidentsUseCase @Inject constructor(
    private val careNoteRepository: CareNoteRepository,
    private val currentTime: CurrentTime,
) {
    operator fun invoke(): Flow<Map<String, Instant>> =
        combine(careNoteRepository.observeAllNotes(), currentTime.now) { notes, now ->
            val byResident = notes.groupBy { it.residentId }
            val lastNotes = byResident.mapNotNull { (residentId, residentNotes) ->
                lastNoteAt(residentNotes)?.let { residentId to it }
            }
            lastNotes.filter { (_, lastNoteAt) -> Duration.between(lastNoteAt, now) >= OVERDUE_AFTER }.toMap()
        }.distinctUntilChanged()

    /** [notes] are one resident's; null only if their edits loop, so no version is current. */
    private fun lastNoteAt(notes: List<CareNote>): Instant? {
        val versions = NoteVersions(notes)
        return versions.current.maxOfOrNull { versions.firstWrittenAt(it) }
    }

    companion object {
        /** A demo baseline; a real care home would set it per resident, such as every 2 hours at night. */
        val OVERDUE_AFTER: Duration = Duration.ofHours(8)
    }
}
