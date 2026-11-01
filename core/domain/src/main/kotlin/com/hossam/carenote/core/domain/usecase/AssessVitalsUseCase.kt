package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.vitals.VitalsAssessment
import com.hossam.carenote.core.domain.vitals.VitalsAssessor
import com.hossam.carenote.core.model.Vitals
import javax.inject.Inject

/** Which of a note's vitals are outside the usual range, for showing them as needing attention. */
class AssessVitalsUseCase @Inject constructor(private val assessor: VitalsAssessor) {
    operator fun invoke(vitals: Vitals): VitalsAssessment = assessor.assess(vitals)
}
