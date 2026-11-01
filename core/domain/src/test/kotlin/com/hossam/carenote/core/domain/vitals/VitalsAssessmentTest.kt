package com.hossam.carenote.core.domain.vitals

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VitalsAssessmentTest {
    @Test
    fun `given only normal vitals, when checking for attention, then none is needed`() {
        // Given
        val assessment = VitalsAssessment(bloodPressure = VitalLevel.Normal, pulse = VitalLevel.Normal)

        // When
        val needsAttention = assessment.needsAttention

        // Then
        assertFalse(needsAttention)
    }

    @Test
    fun `given one vital out of range, when checking for attention, then it is needed`() {
        // Given
        val assessment = VitalsAssessment(pulse = VitalLevel.Normal, oxygenSaturation = VitalLevel.Low)

        // When
        val needsAttention = assessment.needsAttention

        // Then
        assertTrue(needsAttention)
    }

    @Test
    fun `given no vitals, when checking for attention, then none is needed`() {
        // Given
        val assessment = VitalsAssessment()

        // When
        val needsAttention = assessment.needsAttention

        // Then
        assertFalse(needsAttention)
    }
}
