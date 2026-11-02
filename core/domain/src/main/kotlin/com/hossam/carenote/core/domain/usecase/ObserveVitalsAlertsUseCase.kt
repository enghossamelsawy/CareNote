package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.note.NoteVersions
import com.hossam.carenote.core.domain.repository.CareNoteRepository
import com.hossam.carenote.core.domain.vitals.VitalsAlert
import com.hossam.carenote.core.domain.vitals.VitalsAssessor
import com.hossam.carenote.core.model.CareNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * The residents whose latest vitals need attention, by resident id; residents whose latest vitals are in
 * range, or who have none, are left out. Emits again whenever a note is saved.
 *
 * "Latest" is the newest current note that has vitals, by when it was first written: a note without vitals,
 * such as "ate well", says nothing about a fever, and an edit corrects the reading it replaced. So a newer
 * reading in range clears the alert by itself, with nothing to dismiss.
 */
class ObserveVitalsAlertsUseCase @Inject constructor(
    private val careNoteRepository: CareNoteRepository,
    private val assessor: VitalsAssessor,
) {
    operator fun invoke(): Flow<Map<String, VitalsAlert>> = careNoteRepository.observeAllNotes().map { notes ->
        val byResident = notes.groupBy { it.residentId }
        byResident.mapNotNull { (residentId, residentNotes) -> alert(residentNotes)?.let { residentId to it } }.toMap()
    }

    /** [notes] are one resident's. */
    private fun alert(notes: List<CareNote>): VitalsAlert? {
        val versions = NoteVersions(notes)
        val latest = versions.current.filterNot { it.vitals.isEmpty }.maxByOrNull { versions.firstWrittenAt(it) }
            ?: return null
        val assessment = assessor.assess(latest.vitals)
        return if (assessment.needsAttention) VitalsAlert(assessment, versions.firstWrittenAt(latest)) else null
    }
}
