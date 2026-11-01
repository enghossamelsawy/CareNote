package com.hossam.carenote.feature.resident

import com.hossam.carenote.core.domain.vitals.VitalsAssessment
import com.hossam.carenote.core.model.CareNote
import java.time.Instant

/** The latest version of a note as the screens show it; [firstWrittenAt] is when its first version was written. */
internal fun CareNote.asItem(firstWrittenAt: Instant, assessment: VitalsAssessment) = NoteItem(
    id = id,
    createdAt = firstWrittenAt,
    transcript = transcript,
    vitals = vitals,
    assessment = assessment,
    status = syncStatus,
    editedAt = if (revisionOf != null) createdAt else null,
)
