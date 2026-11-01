package com.hossam.carenote.core.domain.vitals

import com.hossam.carenote.core.model.Vitals
import org.junit.Assert.assertEquals
import org.junit.Test

class VitalsComparerTest {
    private val comparer = VitalsComparer()

    @Test
    fun `given the same vitals, when comparing, then nothing changed`() {
        // Given
        val vitals = Vitals(systolic = 120, diastolic = 80, pulse = 72, temperatureCelsius = 37.0)

        // When
        val changed = comparer.changed(before = vitals, after = vitals.copy())

        // Then
        assertEquals(emptySet<VitalKind>(), changed)
    }

    @Test
    fun `given a corrected pulse, when comparing, then only the pulse changed`() {
        // When
        val changed = comparer.changed(
            before = Vitals(pulse = 72, temperatureCelsius = 37.0),
            after = Vitals(pulse = 76, temperatureCelsius = 37.0),
        )

        // Then
        assertEquals(setOf(VitalKind.Pulse), changed)
    }

    @Test
    fun `given one blood pressure number corrected, when comparing, then blood pressure changed`() {
        // When
        val changed = comparer.changed(
            before = Vitals(systolic = 120, diastolic = 80),
            after = Vitals(systolic = 120, diastolic = 85),
        )

        // Then
        assertEquals(setOf(VitalKind.BloodPressure), changed)
    }

    @Test
    fun `given a value added in the edit, when comparing, then it changed`() {
        // When
        val changed = comparer.changed(before = Vitals(pulse = 72), after = Vitals(pulse = 72, oxygenSaturation = 96))

        // Then
        assertEquals(setOf(VitalKind.OxygenSaturation), changed)
    }

    @Test
    fun `given a value taken out in the edit, when comparing, then it changed`() {
        // When
        val changed = comparer.changed(before = Vitals(temperatureCelsius = 38.5), after = Vitals())

        // Then
        assertEquals(setOf(VitalKind.Temperature), changed)
    }
}
