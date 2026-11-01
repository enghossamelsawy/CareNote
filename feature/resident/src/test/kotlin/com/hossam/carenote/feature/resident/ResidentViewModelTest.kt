package com.hossam.carenote.feature.resident

import app.cash.turbine.test
import com.hossam.carenote.core.domain.usecase.ObserveResidentUseCase
import com.hossam.carenote.core.model.Resident
import com.hossam.carenote.core.testing.MainDispatcherRule
import com.hossam.carenote.core.testing.repository.FakeResidentRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ResidentViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val residentRepository = FakeResidentRepository()

    private val anna = Resident(id = "res-1", firstName = "Anna", lastName = "Becker", room = "101")

    @Test
    fun `given a known resident, when the view model starts, then their name and room are shown`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))

        // When
        val viewModel = createViewModel(anna.id)

        // Then
        assertEquals(ResidentState(isLoading = false, name = "Anna Becker", room = "101"), viewModel.state.value)
    }

    @Test
    fun `given an unknown resident, when the view model starts, then not found is shown`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))

        // When
        val viewModel = createViewModel("res-unknown")

        // Then
        assertEquals(ResidentState(isLoading = false, notFound = true), viewModel.state.value)
    }

    @Test
    fun `given the detail screen, when back is clicked, then it navigates back`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))
        val viewModel = createViewModel(anna.id)

        // When
        viewModel.onIntent(ResidentIntent.BackClicked)

        // Then
        viewModel.effects.test {
            assertEquals(ResidentEffect.NavigateBack, awaitItem())
        }
    }

    private fun createViewModel(residentId: String) =
        ResidentViewModel(residentId, ObserveResidentUseCase(residentRepository))
}
