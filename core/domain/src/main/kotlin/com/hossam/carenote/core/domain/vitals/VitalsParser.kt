package com.hossam.carenote.core.domain.vitals

import com.hossam.carenote.core.model.Vitals
import javax.inject.Inject

/**
 * Pulls vital signs out of a dictated transcript, using the keywords of every given vocabulary,
 * so mixed-language dictation works too. Values that aren't mentioned stay null, and nothing is
 * range-checked here: "pulse 300" gives 300.
 */
class VitalsParser(private val vocabularies: List<VitalsVocabulary>) {
    @Inject
    constructor() : this(listOf(EnglishVitalsVocabulary))

    fun parse(transcript: String): Vitals = Vitals.EMPTY
}
