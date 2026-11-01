package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.vitals.VitalLevel
import com.hossam.carenote.core.domain.vitals.VitalsAssessment
import com.hossam.carenote.core.domain.vitals.VitalsAssessor
import com.hossam.carenote.core.model.Vitals
import org.junit.Assert.assertEquals
import org.junit.Test

class AssessVitalsUseCaseTest {
    private val assessVitals = AssessVitalsUseCase(VitalsAssessor())

    @Test
    fun `given a note's vitals, when assessing, then the out of range ones are marked`() {
        // Given
        val vitals = Vitals(systolic = 160, diastolic = 95, pulse = 72)

        // When
        val assessment = assessVitals(vitals)

        // Then
        assertEquals(VitalsAssessment(bloodPressure = VitalLevel.High, pulse = VitalLevel.Normal), assessment)
    }
}
