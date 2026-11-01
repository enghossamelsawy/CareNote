package com.hossam.carenote.core.domain.vitals

import com.hossam.carenote.core.domain.vitals.VitalsProblem.BloodPressureIncomplete
import com.hossam.carenote.core.domain.vitals.VitalsProblem.DiastolicNotBelowSystolic
import com.hossam.carenote.core.domain.vitals.VitalsProblem.DiastolicOutOfRange
import com.hossam.carenote.core.domain.vitals.VitalsProblem.OxygenSaturationOutOfRange
import com.hossam.carenote.core.domain.vitals.VitalsProblem.PulseOutOfRange
import com.hossam.carenote.core.domain.vitals.VitalsProblem.SystolicOutOfRange
import com.hossam.carenote.core.domain.vitals.VitalsProblem.TemperatureOutOfRange
import com.hossam.carenote.core.model.Vitals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class VitalsValidatorTest(private val vitals: Vitals, private val expected: Set<VitalsProblem>) {
    private val validator = VitalsValidator()

    @Test
    fun `given vitals, when validating, then only real problems are reported`() {
        // Given
        val input = vitals

        // When
        val problems = validator.validate(input)

        // Then
        assertEquals(expected, problems)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "given {0}, when validating, then {1}")
        fun cases(): List<Array<Any>> = listOf(
            // Nothing or something normal is fine
            arrayOf(Vitals.EMPTY, emptySet<VitalsProblem>()),
            arrayOf(
                Vitals(systolic = 120, diastolic = 80, pulse = 72, temperatureCelsius = 36.8, oxygenSaturation = 97),
                emptySet<VitalsProblem>(),
            ),
            // Range boundaries are allowed
            arrayOf(Vitals(systolic = 50, diastolic = 30), emptySet<VitalsProblem>()),
            arrayOf(Vitals(systolic = 260, diastolic = 160), emptySet<VitalsProblem>()),
            arrayOf(Vitals(pulse = 20), emptySet<VitalsProblem>()),
            arrayOf(Vitals(pulse = 250), emptySet<VitalsProblem>()),
            arrayOf(Vitals(temperatureCelsius = 30.0), emptySet<VitalsProblem>()),
            arrayOf(Vitals(temperatureCelsius = 43.0), emptySet<VitalsProblem>()),
            arrayOf(Vitals(oxygenSaturation = 50), emptySet<VitalsProblem>()),
            arrayOf(Vitals(oxygenSaturation = 100), emptySet<VitalsProblem>()),
            // Just outside the ranges
            arrayOf(Vitals(systolic = 261, diastolic = 80), setOf(SystolicOutOfRange)),
            arrayOf(Vitals(systolic = 120, diastolic = 29), setOf(DiastolicOutOfRange)),
            arrayOf(Vitals(pulse = 19), setOf(PulseOutOfRange)),
            arrayOf(Vitals(pulse = 720), setOf(PulseOutOfRange)),
            arrayOf(Vitals(temperatureCelsius = 43.1), setOf(TemperatureOutOfRange)),
            arrayOf(Vitals(oxygenSaturation = 101), setOf(OxygenSaturationOutOfRange)),
            // Blood pressure needs both values, the lower one below the upper one
            arrayOf(Vitals(systolic = 120), setOf(BloodPressureIncomplete)),
            arrayOf(Vitals(diastolic = 80), setOf(BloodPressureIncomplete)),
            arrayOf(Vitals(systolic = 80, diastolic = 120), setOf(DiastolicNotBelowSystolic)),
            arrayOf(Vitals(systolic = 90, diastolic = 90), setOf(DiastolicNotBelowSystolic)),
            // Every problem is reported, not just the first
            arrayOf(Vitals(pulse = 0, oxygenSaturation = 0), setOf(PulseOutOfRange, OxygenSaturationOutOfRange)),
        )
    }
}
