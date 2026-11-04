package com.hossam.carenote.core.domain.vitals

import com.hossam.carenote.core.model.Vitals
import org.junit.Assert.assertEquals
import org.junit.Test

/** Language-independent behavior, checked with made-up vocabularies. */
class VitalsParserTest {
    @Test
    fun `given two vocabularies, when parsing, then keywords of both are recognized`() {
        // Given
        val parser = VitalsParser(
            listOf(
                FakeVocabulary(pulseKeywords = listOf("beat")),
                FakeVocabulary(temperatureKeywords = listOf("heat")),
            ),
        )

        // When
        val vitals = parser.parse("beat 70, heat 36.5")

        // Then
        assertEquals(Vitals(pulse = 70, temperatureCelsius = 36.5), vitals)
    }

    @Test
    fun `given a value mentioned twice, when parsing, then the first one wins`() {
        // Given
        val parser = VitalsParser(listOf(FakeVocabulary(pulseKeywords = listOf("beat"))))

        // When
        val vitals = parser.parse("beat 70, later beat 90")

        // Then
        assertEquals(Vitals(pulse = 70), vitals)
    }

    @Test
    fun `given a keyword inside another word, when parsing, then it is ignored`() {
        // Given
        val parser = VitalsParser(listOf(FakeVocabulary(pulseKeywords = listOf("beat"))))

        // When
        val vitals = parser.parse("upbeat 70, beaten 80")

        // Then
        assertEquals(Vitals.EMPTY, vitals)
    }

    @Test
    fun `given a keyword with regex characters, when parsing, then it matches literally`() {
        // Given
        val parser = VitalsParser(
            listOf(
                FakeVocabulary(
                    bloodPressureKeywords = listOf("b.p."),
                    bloodPressureSeparators = listOf("/"),
                ),
            ),
        )

        // When
        val literal = parser.parse("b.p. 120/80")
        val lookalike = parser.parse("bxpx 120/80")

        // Then
        assertEquals(Vitals(systolic = 120, diastolic = 80), literal)
        assertEquals(Vitals.EMPTY, lookalike)
    }

    @Test
    fun `given a vocabulary without keywords, when parsing, then nothing is recognized`() {
        // Given
        val parser = VitalsParser(listOf(FakeVocabulary()))

        // When
        val vitals = parser.parse("pulse 72, temperature 37.2")

        // Then
        assertEquals(Vitals.EMPTY, vitals)
    }

    @Test
    fun `given pauses transcribed as punctuation, when parsing, then the values are still found`() {
        // Given
        val parser = VitalsParser(listOf(EnglishVitalsVocabulary))

        // When
        val vitals = parser.parse("Blood pressure is 120. Over 80. Pulse. 72. Temperature, is 36.8.")

        // Then
        assertEquals(Vitals(systolic = 120, diastolic = 80, pulse = 72, temperatureCelsius = 36.8), vitals)
    }

    @Test
    fun `given a garbled dictation, when parsing, then only the clear value is found`() {
        // Given
        val parser = VitalsParser(listOf(EnglishVitalsVocabulary))

        // When
        val vitals = parser.parse(
            "Temperature.  Is 37.  Uh, blood pressure is.  Do any over?  Blood pressure is to 12.  120.  Over.",
        )

        // Then
        assertEquals(Vitals(temperatureCelsius = 37.0), vitals)
    }

    @Test
    fun `given a keyword followed by another vital, when parsing, then its value stays with the other vital`() {
        // Given
        val parser = VitalsParser(listOf(EnglishVitalsVocabulary))

        // When
        val vitals = parser.parse("Pulse. Temperature 37")

        // Then
        assertEquals(Vitals(temperatureCelsius = 37.0), vitals)
    }

    private data class FakeVocabulary(
        override val bloodPressureKeywords: List<String> = emptyList(),
        override val bloodPressureSeparators: List<String> = emptyList(),
        override val pulseKeywords: List<String> = emptyList(),
        override val temperatureKeywords: List<String> = emptyList(),
        override val oxygenSaturationKeywords: List<String> = emptyList(),
    ) : VitalsVocabulary
}
