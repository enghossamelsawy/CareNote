package com.hossam.carenote.core.domain.vitals

import com.hossam.carenote.core.domain.vitals.VitalLevel.High
import com.hossam.carenote.core.domain.vitals.VitalLevel.Low
import com.hossam.carenote.core.domain.vitals.VitalLevel.Normal
import com.hossam.carenote.core.model.Vitals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class VitalsAssessorTest(private val vitals: Vitals, private val expected: VitalsAssessment) {
    private val assessor = VitalsAssessor()

    @Test
    fun `given vitals, when assessing, then each one gets its level`() {
        // Given
        val input = vitals

        // When
        val assessment = assessor.assess(input)

        // Then
        assertEquals(expected, assessment)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "given {0}, when assessing, then {1}")
        fun cases(): List<Array<Any>> = listOf(
            // Nothing dictated, nothing to assess
            arrayOf(Vitals.EMPTY, VitalsAssessment()),
            // Blood pressure, judged as one reading
            arrayOf(Vitals(systolic = 120, diastolic = 80), VitalsAssessment(bloodPressure = Normal)),
            arrayOf(Vitals(systolic = 90, diastolic = 60), VitalsAssessment(bloodPressure = Normal)),
            arrayOf(Vitals(systolic = 139, diastolic = 89), VitalsAssessment(bloodPressure = Normal)),
            arrayOf(Vitals(systolic = 140, diastolic = 80), VitalsAssessment(bloodPressure = High)),
            arrayOf(Vitals(systolic = 120, diastolic = 90), VitalsAssessment(bloodPressure = High)),
            arrayOf(Vitals(systolic = 89, diastolic = 70), VitalsAssessment(bloodPressure = Low)),
            arrayOf(Vitals(systolic = 120, diastolic = 59), VitalsAssessment(bloodPressure = Low)),
            arrayOf(Vitals(systolic = 150, diastolic = 55), VitalsAssessment(bloodPressure = High)),
            arrayOf(Vitals(systolic = 160), VitalsAssessment(bloodPressure = High)),
            // Pulse
            arrayOf(Vitals(pulse = 59), VitalsAssessment(pulse = Low)),
            arrayOf(Vitals(pulse = 60), VitalsAssessment(pulse = Normal)),
            arrayOf(Vitals(pulse = 100), VitalsAssessment(pulse = Normal)),
            arrayOf(Vitals(pulse = 101), VitalsAssessment(pulse = High)),
            // Temperature
            arrayOf(Vitals(temperatureCelsius = 36.0), VitalsAssessment(temperature = Low)),
            arrayOf(Vitals(temperatureCelsius = 36.1), VitalsAssessment(temperature = Normal)),
            arrayOf(Vitals(temperatureCelsius = 37.9), VitalsAssessment(temperature = Normal)),
            arrayOf(Vitals(temperatureCelsius = 38.0), VitalsAssessment(temperature = High)),
            // Oxygen saturation is never too high
            arrayOf(Vitals(oxygenSaturation = 94), VitalsAssessment(oxygenSaturation = Low)),
            arrayOf(Vitals(oxygenSaturation = 95), VitalsAssessment(oxygenSaturation = Normal)),
            arrayOf(Vitals(oxygenSaturation = 100), VitalsAssessment(oxygenSaturation = Normal)),
            // All at once
            arrayOf(
                Vitals(systolic = 160, diastolic = 95, pulse = 72, temperatureCelsius = 38.4, oxygenSaturation = 97),
                VitalsAssessment(bloodPressure = High, pulse = Normal, temperature = High, oxygenSaturation = Normal),
            ),
        )
    }
}
