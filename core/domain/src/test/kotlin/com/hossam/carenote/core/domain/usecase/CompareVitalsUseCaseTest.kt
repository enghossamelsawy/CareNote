package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.vitals.VitalKind
import com.hossam.carenote.core.domain.vitals.VitalsComparer
import com.hossam.carenote.core.model.Vitals
import org.junit.Assert.assertEquals
import org.junit.Test

class CompareVitalsUseCaseTest {
    private val compareVitals = CompareVitalsUseCase(VitalsComparer())

    @Test
    fun `given an edit that corrected the temperature, when comparing, then the temperature changed`() {
        // When
        val changed = compareVitals(
            before = Vitals(temperatureCelsius = 38.5),
            after = Vitals(temperatureCelsius = 37.0),
        )

        // Then
        assertEquals(setOf(VitalKind.Temperature), changed)
    }
}
