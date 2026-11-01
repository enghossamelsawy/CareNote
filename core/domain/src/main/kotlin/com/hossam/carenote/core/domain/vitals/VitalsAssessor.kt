package com.hossam.carenote.core.domain.vitals

import com.hossam.carenote.core.model.Vitals
import javax.inject.Inject

/** Where one vital sits against the usual adult range. */
enum class VitalLevel { Low, Normal, High }

/** One level per vital; null for a vital that wasn't dictated, so there is nothing to show for it. */
data class VitalsAssessment(
    val bloodPressure: VitalLevel? = null,
    val pulse: VitalLevel? = null,
    val temperature: VitalLevel? = null,
    val oxygenSaturation: VitalLevel? = null,
) {
    /** True when any dictated vital is outside its usual range. */
    val needsAttention: Boolean
        get() = listOfNotNull(bloodPressure, pulse, temperature, oxygenSaturation).any { it != VitalLevel.Normal }
}

/**
 * Says whether each vital is within the usual adult range. Different from [VitalsValidator]: that one
 * rejects implausible values, which are usually misheard words; this one flags real values that a
 * nurse should look at, such as 160/95.
 *
 * The ranges are a simple demo baseline, not clinical advice: a real care home would set them per
 * resident or follow a scoring system such as NEWS2.
 */
class VitalsAssessor @Inject constructor() {
    fun assess(vitals: Vitals): VitalsAssessment = VitalsAssessment(
        bloodPressure = bloodPressure(vitals.systolic, vitals.diastolic),
        pulse = vitals.pulse?.let { level(it, low = PULSE_MIN, high = PULSE_MAX) },
        temperature = vitals.temperatureCelsius?.let { temperature(it) },
        oxygenSaturation = vitals.oxygenSaturation?.let {
            if (it < OXYGEN_SATURATION_MIN) VitalLevel.Low else VitalLevel.Normal
        },
    )

    /** One reading: high if either value is high, otherwise low if either is low. */
    private fun bloodPressure(systolic: Int?, diastolic: Int?): VitalLevel? {
        if (systolic == null && diastolic == null) return null
        val levels = listOfNotNull(
            systolic?.let { level(it, low = SYSTOLIC_MIN, high = SYSTOLIC_MAX) },
            diastolic?.let { level(it, low = DIASTOLIC_MIN, high = DIASTOLIC_MAX) },
        )
        return when {
            VitalLevel.High in levels -> VitalLevel.High
            VitalLevel.Low in levels -> VitalLevel.Low
            else -> VitalLevel.Normal
        }
    }

    private fun temperature(celsius: Double): VitalLevel = when {
        celsius < TEMPERATURE_MIN_CELSIUS -> VitalLevel.Low
        celsius >= TEMPERATURE_FEVER_CELSIUS -> VitalLevel.High
        else -> VitalLevel.Normal
    }

    /** Normal from [low] to [high], both included. */
    private fun level(value: Int, low: Int, high: Int): VitalLevel = when {
        value < low -> VitalLevel.Low
        value > high -> VitalLevel.High
        else -> VitalLevel.Normal
    }

    companion object {
        const val SYSTOLIC_MIN = 90
        const val SYSTOLIC_MAX = 139
        const val DIASTOLIC_MIN = 60
        const val DIASTOLIC_MAX = 89
        const val PULSE_MIN = 60
        const val PULSE_MAX = 100
        const val TEMPERATURE_MIN_CELSIUS = 36.1
        const val TEMPERATURE_FEVER_CELSIUS = 38.0
        const val OXYGEN_SATURATION_MIN = 95
    }
}
