package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.repository.RefreshResult
import com.hossam.carenote.core.testing.repository.FakeResidentRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshResidentsUseCaseTest {
    private val residentRepository = FakeResidentRepository()
    private val refreshResidents = RefreshResidentsUseCase(residentRepository)

    @Test
    fun `given the server is unreachable, when refreshing, then that is reported`() = runTest {
        // Given
        residentRepository.refreshResult = RefreshResult.Unavailable

        // When
        val result = refreshResidents()

        // Then
        assertEquals(RefreshResult.Unavailable, result)
        assertEquals(1, residentRepository.refreshCount)
    }
}
