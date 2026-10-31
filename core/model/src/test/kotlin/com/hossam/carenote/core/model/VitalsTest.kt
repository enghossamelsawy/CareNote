package com.hossam.carenote.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VitalsTest {
    @Test
    fun `given no values, when checking isEmpty, then it is true`() {
        // Given
        val vitals = Vitals.EMPTY

        // When
        val empty = vitals.isEmpty

        // Then
        assertTrue(empty)
    }

    @Test
    fun `given only a pulse, when checking isEmpty, then it is false`() {
        // Given
        val vitals = Vitals(pulse = 72)

        // When
        val empty = vitals.isEmpty

        // Then
        assertFalse(empty)
    }

    @Test
    fun `given only a temperature, when checking isEmpty, then it is false`() {
        // Given
        val vitals = Vitals(temperatureCelsius = 37.2)

        // When
        val empty = vitals.isEmpty

        // Then
        assertFalse(empty)
    }

    @Test
    fun `given only blood pressure, when checking isEmpty, then it is false`() {
        // Given
        val vitals = Vitals(systolic = 130, diastolic = 85)

        // When
        val empty = vitals.isEmpty

        // Then
        assertFalse(empty)
    }
}
