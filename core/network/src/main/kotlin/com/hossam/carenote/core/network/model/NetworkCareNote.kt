package com.hossam.carenote.core.network.model

import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

/**
 * A note as the server receives and returns it. Ids and times travel as text (UUID and ISO-8601),
 * so the JSON is the same whatever language the server is written in.
 */
@Serializable
data class NetworkCareNote(
    val id: String,
    val residentId: String,
    val transcript: String,
    val vitals: NetworkVitals,
    val createdAt: String,
    val revisionOf: String? = null,
)

/** Missing values are left out of the JSON instead of being sent as null. */
@Serializable
data class NetworkVitals(
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val pulse: Int? = null,
    val temperatureCelsius: Double? = null,
    val oxygenSaturation: Int? = null,
)

internal fun CareNote.asNetwork() = NetworkCareNote(
    id = id.toString(),
    residentId = residentId,
    transcript = transcript,
    vitals = vitals.asNetwork(),
    createdAt = createdAt.toString(),
    revisionOf = revisionOf?.toString(),
)

internal fun Vitals.asNetwork() = NetworkVitals(
    systolic = systolic,
    diastolic = diastolic,
    pulse = pulse,
    temperatureCelsius = temperatureCelsius,
    oxygenSaturation = oxygenSaturation,
)

/**
 * A note from the server is on the server, so it is [SyncStatus.SYNCED]. Throws IllegalArgumentException or
 * DateTimeParseException if its id or time can't be read.
 */
internal fun NetworkCareNote.asExternalModel() = CareNote(
    id = UUID.fromString(id),
    residentId = residentId,
    transcript = transcript,
    vitals = vitals.asExternalModel(),
    createdAt = Instant.parse(createdAt),
    revisionOf = revisionOf?.let(UUID::fromString),
    syncStatus = SyncStatus.SYNCED,
)

internal fun NetworkVitals.asExternalModel() = Vitals(
    systolic = systolic,
    diastolic = diastolic,
    pulse = pulse,
    temperatureCelsius = temperatureCelsius,
    oxygenSaturation = oxygenSaturation,
)
