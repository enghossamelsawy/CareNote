package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.vitals.VitalsParser
import com.hossam.carenote.core.model.Vitals
import javax.inject.Inject

/** The vitals dictated in [transcript]. No range check here; [SaveCareNoteUseCase] validates when saving. */
class ParseVitalsUseCase @Inject constructor(private val parser: VitalsParser) {
    operator fun invoke(transcript: String): Vitals = parser.parse(transcript)
}
