package com.hossam.carenote.core.domain.vitals

import java.time.Instant

/** A resident's latest vitals are outside the usual range: which ones, and when they were taken. */
data class VitalsAlert(val assessment: VitalsAssessment, val takenAt: Instant)
