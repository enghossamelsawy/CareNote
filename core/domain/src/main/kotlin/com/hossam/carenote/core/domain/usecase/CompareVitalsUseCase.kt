package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.vitals.VitalKind
import com.hossam.carenote.core.domain.vitals.VitalsComparer
import com.hossam.carenote.core.model.Vitals
import javax.inject.Inject

/** Which vitals an edit changed, from the version [before] it to the version [after] it. */
class CompareVitalsUseCase @Inject constructor(private val comparer: VitalsComparer) {
    operator fun invoke(before: Vitals, after: Vitals): Set<VitalKind> = comparer.changed(before, after)
}
