package com.hossam.carenote.core.data.mapper

import com.hossam.carenote.core.database.model.CareNoteEntity
import com.hossam.carenote.core.database.model.PendingCount
import com.hossam.carenote.core.database.model.VitalsEntity
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.Vitals
import java.util.UUID

internal fun CareNoteEntity.asExternalModel() = CareNote(
    id = UUID.fromString(id),
    residentId = residentId,
    transcript = transcript,
    vitals = vitals.asExternalModel(),
    createdAt = createdAt,
    revisionOf = revisionOf?.let(UUID::fromString),
    syncStatus = syncStatus,
)

internal fun CareNote.asEntity() = CareNoteEntity(
    id = id.toString(),
    residentId = residentId,
    transcript = transcript,
    vitals = vitals.asEntity(),
    createdAt = createdAt,
    revisionOf = revisionOf?.toString(),
    syncStatus = syncStatus,
)

internal fun VitalsEntity.asExternalModel() = Vitals(
    systolic = systolic,
    diastolic = diastolic,
    pulse = pulse,
    temperatureCelsius = temperatureCelsius,
    oxygenSaturation = oxygenSaturation,
)

internal fun Vitals.asEntity() = VitalsEntity(
    systolic = systolic,
    diastolic = diastolic,
    pulse = pulse,
    temperatureCelsius = temperatureCelsius,
    oxygenSaturation = oxygenSaturation,
)

/** Resident id to number of notes not yet on the server; residents without any are absent. */
internal fun List<PendingCount>.asPendingCountMap(): Map<String, Int> = associate { it.residentId to it.count }
