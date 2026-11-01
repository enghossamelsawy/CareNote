package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.model.Resident
import com.hossam.carenote.core.testing.repository.FakeResidentRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObserveResidentUseCaseTest {
    private val residentRepository = FakeResidentRepository()
    private val observeResident = ObserveResidentUseCase(residentRepository)

    private val anna = Resident(id = "res-1", firstName = "Anna", lastName = "Becker", room = "101")
    private val max = Resident(id = "res-2", firstName = "Max", lastName = "Weber", room = "102")

    @Test
    fun `given a known resident id, when observing, then that resident is emitted`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna, max))

        // When
        val resident = observeResident(max.id).first()

        // Then
        assertEquals(max, resident)
    }

    @Test
    fun `given an unknown resident id, when observing, then null is emitted`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))

        // When
        val resident = observeResident("res-unknown").first()

        // Then
        assertNull(resident)
    }
}
