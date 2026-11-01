package com.hossam.carenote.core.domain.vitals

import com.hossam.carenote.core.model.Vitals
import javax.inject.Inject

/** Why a set of vitals can't be saved. */
enum class VitalsProblem {
    /** Only one of the two blood pressure values is given. */
    BloodPressureIncomplete,
    SystolicOutOfRange,
    DiastolicOutOfRange,
    DiastolicNotBelowSystolic,
    PulseOutOfRange,
    TemperatureOutOfRange,
    OxygenSaturationOutOfRange,
}

/**
 * Checks vitals against plausible adult ranges. A value outside them is almost always a misheard
 * word ("pulse 720"), and in a care record a wrong number is worse than a missing one.
 * Missing values are always fine: staff often dictate only some vitals.
 */
class VitalsValidator @Inject constructor() {
    fun validate(vitals: Vitals): Set<VitalsProblem> = buildSet {
        val systolic = vitals.systolic
        val diastolic = vitals.diastolic
        if ((systolic == null) != (diastolic == null)) add(VitalsProblem.BloodPressureIncomplete)
        if (systolic != null && systolic !in SYSTOLIC) add(VitalsProblem.SystolicOutOfRange)
        if (diastolic != null && diastolic !in DIASTOLIC) add(VitalsProblem.DiastolicOutOfRange)
        if (systolic != null && diastolic != null && diastolic >= systolic) {
            add(VitalsProblem.DiastolicNotBelowSystolic)
        }
        vitals.pulse?.let { if (it !in PULSE) add(VitalsProblem.PulseOutOfRange) }
        vitals.temperatureCelsius?.let { if (it !in TEMPERATURE_CELSIUS) add(VitalsProblem.TemperatureOutOfRange) }
        vitals.oxygenSaturation?.let { if (it !in OXYGEN_SATURATION) add(VitalsProblem.OxygenSaturationOutOfRange) }
    }

    companion object {
        val SYSTOLIC = 50..260
        val DIASTOLIC = 30..160
        val PULSE = 20..250
        val TEMPERATURE_CELSIUS = 30.0..43.0
        val OXYGEN_SATURATION = 50..100
    }
}
