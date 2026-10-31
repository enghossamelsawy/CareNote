package com.hossam.carenote.core.domain.vitals

import com.hossam.carenote.core.model.Vitals
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Ignore("VitalsParser not implemented yet")
@RunWith(Parameterized::class)
class EnglishVitalsParserTest(
    private val transcript: String,
    private val expected: Vitals,
) {
    private val parser = VitalsParser(listOf(EnglishVitalsVocabulary))

    @Test
    fun `given an English transcript, when parsing, then the dictated vitals are returned`() {
        // Given
        val input = transcript

        // When
        val vitals = parser.parse(input)

        // Then
        assertEquals(expected, vitals)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "given \"{0}\", when parsing, then {1}")
        fun cases(): List<Array<Any>> = listOf(
            // Blood pressure
            arrayOf("blood pressure 130 over 85", Vitals(systolic = 130, diastolic = 85)),
            arrayOf("BP 130/85", Vitals(systolic = 130, diastolic = 85)),
            arrayOf("bp 130 / 85", Vitals(systolic = 130, diastolic = 85)),
            arrayOf("blood pressure: 120 over 80", Vitals(systolic = 120, diastolic = 80)),
            // Pulse
            arrayOf("pulse 72", Vitals(pulse = 72)),
            arrayOf("heart rate 72", Vitals(pulse = 72)),
            arrayOf("pulse: 64 bpm", Vitals(pulse = 64)),
            // Temperature
            arrayOf("temperature 37.2", Vitals(temperatureCelsius = 37.2)),
            arrayOf("temp 37.2 degrees", Vitals(temperatureCelsius = 37.2)),
            arrayOf("temperature 38", Vitals(temperatureCelsius = 38.0)),
            arrayOf("temperature 37,2", Vitals(temperatureCelsius = 37.2)),
            // Oxygen saturation
            arrayOf("oxygen saturation 97 percent", Vitals(oxygenSaturation = 97)),
            arrayOf("SpO2 97", Vitals(oxygenSaturation = 97)),
            arrayOf("sats 97%", Vitals(oxygenSaturation = 97)),
            arrayOf("SPO2 95 %", Vitals(oxygenSaturation = 95)),
            // Several values in one note
            arrayOf(
                "BP 120/80, pulse 64, temp 36.8, sats 98%",
                Vitals(systolic = 120, diastolic = 80, pulse = 64, temperatureCelsius = 36.8, oxygenSaturation = 98),
            ),
            arrayOf(
                "Slept well. Blood pressure 140 over 90 and pulse 88.",
                Vitals(systolic = 140, diastolic = 90, pulse = 88),
            ),
            // Case doesn't matter
            arrayOf("PULSE 72", Vitals(pulse = 72)),
            arrayOf("Blood Pressure 130 OVER 85", Vitals(systolic = 130, diastolic = 85)),
            // Only what was actually dictated
            arrayOf("pulse 72, blood pressure not measured", Vitals(pulse = 72)),
            arrayOf("Resident slept well", Vitals.EMPTY),
            arrayOf("room 12", Vitals.EMPTY),
            arrayOf("", Vitals.EMPTY),
            // Keywords match whole words only
            arrayOf("temporary 37", Vitals.EMPTY),
            arrayOf("repulse 72", Vitals.EMPTY),
            // No range check in the parser; validation happens when saving
            arrayOf("pulse 300", Vitals(pulse = 300)),
        )
    }
}
