package com.hossam.carenote.core.database.model

/** Embedded in [CareNoteEntity]: one nullable column per vital sign. */
data class VitalsEntity(
    val systolic: Int?,
    val diastolic: Int?,
    val pulse: Int?,
    val temperatureCelsius: Double?,
    val oxygenSaturation: Int?,
)
