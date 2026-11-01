package com.hossam.carenote.feature.residents

import app.cash.turbine.test
import com.hossam.carenote.core.domain.repository.RefreshResult
import com.hossam.carenote.core.domain.usecase.ObserveResidentsWithPendingCountUseCase
import com.hossam.carenote.core.domain.usecase.RefreshResidentsUseCase
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.Resident
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.testing.MainDispatcherRule
import com.hossam.carenote.core.testing.repository.FakeCareNoteRepository
import com.hossam.carenote.core.testing.repository.FakeResidentRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ResidentsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val residentRepository = FakeResidentRepository()
    private val careNoteRepository = FakeCareNoteRepository()

    private val anna = Resident(id = "res-1", firstName = "Anna", lastName = "Becker", room = "101")
    private val max = Resident(id = "res-2", firstName = "Max", lastName = "Weber", room = "102")

    @Test
    fun `given residents, when the view model starts, then they are shown as items`() = runTest {
        // Given
        residentRepository.setResidents(listOf(max, anna))
        careNoteRepository.save(note(id = 1, residentId = anna.id))

        // When
        val viewModel = createViewModel()

        // Then
        assertEquals(
            ResidentsState(
                isLoading = false,
                residents = listOf(
                    ResidentItem(id = "res-1", name = "Anna Becker", room = "101", pendingCount = 1),
                    ResidentItem(id = "res-2", name = "Max Weber", room = "102", pendingCount = 0),
                ),
            ),
            viewModel.state.value,
        )
    }

    @Test
    fun `given a shown resident, when their note syncs, then the pending count is updated`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))
        careNoteRepository.save(note(id = 1, residentId = anna.id))
        val viewModel = createViewModel()

        // When
        careNoteRepository.setSyncStatus(UUID(0, 1), SyncStatus.SYNCED)

        // Then
        assertEquals(0, viewModel.state.value.residents.single().pendingCount)
    }

    @Test
    fun `given the list, when a resident is clicked, then the resident is opened`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))
        val viewModel = createViewModel()

        // When
        viewModel.onIntent(ResidentsIntent.ResidentClicked(anna.id))

        // Then
        viewModel.effects.test {
            assertEquals(ResidentsEffect.OpenResident(anna.id), awaitItem())
        }
    }

    @Test
    fun `given the list, when the screen opens, then residents are refreshed from the server`() = runTest {
        // When
        createViewModel()

        // Then
        assertEquals(1, residentRepository.refreshCount)
    }

    @Test
    fun `given the server is unreachable, when the screen opens, then the stored residents still show`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))
        residentRepository.refreshResult = RefreshResult.Unavailable

        // When
        val viewModel = createViewModel()

        // Then
        assertEquals(listOf("res-1"), viewModel.state.value.residents.map { it.id })
    }

    private fun createViewModel() = ResidentsViewModel(
        observeResidentsWithPendingCount = ObserveResidentsWithPendingCountUseCase(
            residentRepository = residentRepository,
            careNoteRepository = careNoteRepository,
        ),
        refreshResidents = RefreshResidentsUseCase(residentRepository),
    )

    private fun note(id: Int, residentId: String) = CareNote(
        id = UUID(0, id.toLong()),
        residentId = residentId,
        transcript = "Pulse 72",
        vitals = Vitals(pulse = 72),
        createdAt = Instant.EPOCH,
    )
}
