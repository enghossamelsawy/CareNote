package com.hossam.carenote.core.domain.vitals

/**
 * The words one language uses for vital signs. [VitalsParser] builds its patterns from these,
 * so supporting a new language means adding a vocabulary, not changing the parser.
 *
 * Every pattern is "keyword, optional colon, number". Keywords match whole words, ignoring case.
 */
interface VitalsVocabulary {
    val bloodPressureKeywords: List<String>

    /** What is said between the upper and lower value, e.g. "/" or "over". */
    val bloodPressureSeparators: List<String>
    val pulseKeywords: List<String>
    val temperatureKeywords: List<String>
    val oxygenSaturationKeywords: List<String>
}

object EnglishVitalsVocabulary : VitalsVocabulary {
    override val bloodPressureKeywords = listOf("blood pressure", "bp")
    override val bloodPressureSeparators = listOf("/", "over")
    override val pulseKeywords = listOf("pulse", "heart rate")
    override val temperatureKeywords = listOf("temperature", "temp")
    override val oxygenSaturationKeywords = listOf("oxygen saturation", "spo2", "sats")
}
