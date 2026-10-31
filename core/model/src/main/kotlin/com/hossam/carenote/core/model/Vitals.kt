package com.hossam.carenote.core.model

/**
 * Vital signs taken from a dictated note. A value is null when it wasn't mentioned:
 * staff often dictate only some of them, and nothing is ever guessed.
 */
data class Vitals(
    /** Blood pressure, upper value in mmHg. */
    val systolic: Int? = null,
    /** Blood pressure, lower value in mmHg. */
    val diastolic: Int? = null,
    /** Heart rate in beats per minute. */
    val pulse: Int? = null,
    val temperatureCelsius: Double? = null,
    /** SpO2 in percent. */
    val oxygenSaturation: Int? = null,
) {
    val isEmpty: Boolean
        get() = systolic == null &&
            diastolic == null &&
            pulse == null &&
            temperatureCelsius == null &&
            oxygenSaturation == null

    companion object {
        val EMPTY = Vitals()
    }
}
