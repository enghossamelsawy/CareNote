package com.hossam.carenote.core.domain.vitals

import com.hossam.carenote.core.model.Vitals
import javax.inject.Inject

/** The vitals a note can hold, as a reader sees them: blood pressure is one reading of two numbers. */
enum class VitalKind { BloodPressure, Pulse, Temperature, OxygenSaturation }

/**
 * Which vitals differ between two versions of a note, so the history can show what an edit changed:
 * a corrected value, one added later, or one taken out.
 */
class VitalsComparer @Inject constructor() {
    fun changed(before: Vitals, after: Vitals): Set<VitalKind> = buildSet {
        if (before.systolic != after.systolic || before.diastolic != after.diastolic) add(VitalKind.BloodPressure)
        if (before.pulse != after.pulse) add(VitalKind.Pulse)
        if (before.temperatureCelsius != after.temperatureCelsius) add(VitalKind.Temperature)
        if (before.oxygenSaturation != after.oxygenSaturation) add(VitalKind.OxygenSaturation)
    }
}
