package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.vitals.VitalsParser
import com.hossam.carenote.core.model.Vitals
import org.junit.Assert.assertEquals
import org.junit.Test

class ParseVitalsUseCaseTest {
    private val parseVitals = ParseVitalsUseCase(VitalsParser())

    @Test
    fun `given a dictated note, when parsing, then its vitals are returned`() {
        // Given
        val transcript = "Blood pressure 120 over 80, pulse 72, temperature 36.8, SpO2 97"

        // When
        val vitals = parseVitals(transcript)

        // Then
        assertEquals(
            Vitals(systolic = 120, diastolic = 80, pulse = 72, temperatureCelsius = 36.8, oxygenSaturation = 97),
            vitals,
        )
    }
}
