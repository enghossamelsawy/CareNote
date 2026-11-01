package com.hossam.carenote.core.domain.vitals

import com.hossam.carenote.core.model.Vitals
import javax.inject.Inject

/**
 * Pulls vital signs out of a dictated transcript, using the keywords of every given vocabulary,
 * so mixed-language dictation works too. Values that aren't mentioned stay null, and nothing is
 * range-checked here: "pulse 300" gives 300.
 *
 * Each value must follow its keyword, with at most a colon or a couple of filler words in between
 * ("pulse 72", "pulse: 72", "pulse is 72"); the parser never guesses which number belongs to which
 * vital. If a value is mentioned twice, the first one wins.
 */
class VitalsParser(vocabularies: List<VitalsVocabulary>) {
    @Inject
    constructor() : this(listOf(EnglishVitalsVocabulary))

    private val fillers = vocabularies.flatMap { it.fillerWords }

    // Built once per parser; null when no vocabulary has words for that vital.
    private val bloodPressure: Regex? = vocabularies.flatMap { it.bloodPressureSeparators }
        .takeIf { it.isNotEmpty() }
        ?.let { separators ->
            pattern(
                keywords = vocabularies.flatMap { it.bloodPressureKeywords },
                fillers = fillers,
                value = """($INTEGER)\s*(?:${alternatives(separators)})\s*($INTEGER)""",
            )
        }
    private val pulse = pattern(vocabularies.flatMap { it.pulseKeywords }, fillers, "($INTEGER)")
    private val temperature = pattern(vocabularies.flatMap { it.temperatureKeywords }, fillers, "($DECIMAL)")
    private val oxygenSaturation =
        pattern(vocabularies.flatMap { it.oxygenSaturationKeywords }, fillers, "($INTEGER)")

    fun parse(transcript: String): Vitals {
        val bloodPressureValues = bloodPressure?.find(transcript)?.groupValues
        return Vitals(
            systolic = bloodPressureValues?.get(1)?.toInt(),
            diastolic = bloodPressureValues?.get(2)?.toInt(),
            pulse = pulse.firstGroup(transcript)?.toInt(),
            temperatureCelsius = temperature.firstGroup(transcript)?.replace(',', '.')?.toDouble(),
            oxygenSaturation = oxygenSaturation.firstGroup(transcript)?.toInt(),
        )
    }

    private fun Regex?.firstGroup(transcript: String): String? = this?.find(transcript)?.groupValues?.get(1)

    private companion object {
        /** Up to three digits, not followed by another digit. */
        const val INTEGER = """\d{1,3}(?!\d)"""

        /** "37", "37.2" or "37,2". */
        const val DECIMAL = """\d{1,2}(?:[.,]\d{1,2})?(?!\d)"""

        /** At most this many filler words between a keyword and its value: "temperature was about 36.8". */
        const val MAX_FILLERS = 2

        /**
         * Keyword as a whole word (no letter right before or after it), an optional colon, up to
         * [MAX_FILLERS] filler words, then the value. Returns null when there are no keywords.
         */
        fun pattern(keywords: List<String>, fillers: List<String>, value: String): Regex? {
            if (keywords.isEmpty()) return null
            val skippedFillers = if (fillers.isEmpty()) {
                ""
            } else {
                """(?:(?:${alternatives(fillers)})(?!\p{L})\s*){0,$MAX_FILLERS}"""
            }
            return Regex(
                """(?<!\p{L})(?:${alternatives(keywords)})(?!\p{L})\s*:?\s*$skippedFillers$value""",
                RegexOption.IGNORE_CASE,
            )
        }

        /** Longest first, so "temperature" is tried before "temp"; escaped, so "b.p." is literal. */
        fun alternatives(words: List<String>): String = words
            .distinct()
            .sortedByDescending { it.length }
            .joinToString("|") { Regex.escape(it) }
    }
}
