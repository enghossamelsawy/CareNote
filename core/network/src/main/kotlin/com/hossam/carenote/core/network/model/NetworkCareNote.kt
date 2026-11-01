package com.hossam.carenote.core.network.model

import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.Vitals
import kotlinx.serialization.Serializable

/**
 * A note as the server receives it. Ids and times travel as text (UUID and ISO-8601),
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
