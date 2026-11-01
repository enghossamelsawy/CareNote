package com.hossam.carenote.feature.resident

import app.cash.turbine.test
import com.hossam.carenote.core.domain.usecase.ObserveCareNotesUseCase
import com.hossam.carenote.core.domain.usecase.ObserveResidentUseCase
import com.hossam.carenote.core.domain.usecase.RetryCareNoteSyncUseCase
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.Resident
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.testing.MainDispatcherRule
import com.hossam.carenote.core.testing.repository.FakeCareNoteRepository
import com.hossam.carenote.core.testing.repository.FakeResidentRepository
import com.hossam.carenote.core.testing.repository.FakeSyncScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ResidentViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val residentRepository = FakeResidentRepository()
    private val careNoteRepository = FakeCareNoteRepository()
    private val syncScheduler = FakeSyncScheduler()

    private val anna = Resident(id = "res-1", firstName = "Anna", lastName = "Becker", room = "101")

    @Test
    fun `given a known resident, when the view model starts, then their name and room are shown`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))

        // When
        val viewModel = createViewModel(anna.id)

        // Then
        assertEquals(
            ResidentState(isLoading = false, residentId = "res-1", name = "Anna Becker", room = "101"),
            viewModel.state.value,
        )
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

    @Test
    fun `given the detail screen, when record is clicked, then recording opens for that resident`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))
        val viewModel = createViewModel(anna.id)

        // When
        viewModel.onIntent(ResidentIntent.RecordClicked)

        // Then
        viewModel.effects.test {
            assertEquals(ResidentEffect.OpenRecord(anna.id), awaitItem())
        }
    }

    @Test
    fun `given notes for the resident, when the view model starts, then they are listed newest first`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))
        careNoteRepository.save(note(id = 1, createdAt = 10))
        careNoteRepository.save(note(id = 2, createdAt = 20))

        // When
        val viewModel = createViewModel(anna.id)

        // Then
        assertEquals(listOf(UUID(0, 2), UUID(0, 1)), viewModel.state.value.notes.map { it.id })
    }

    @Test
    fun `given a listed note, when its sync status changes, then the list shows the new status`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))
        careNoteRepository.save(note(id = 1, createdAt = 10))
        val viewModel = createViewModel(anna.id)

        // When
        careNoteRepository.setSyncStatus(UUID(0, 1), SyncStatus.SYNCED)

        // Then
        assertEquals(SyncStatus.SYNCED, viewModel.state.value.notes.single().status)
    }

    @Test
    fun `given a note the server refused, when retry is clicked, then it is pending and a sync is requested`() =
        runTest {
            // Given
            residentRepository.setResidents(listOf(anna))
            careNoteRepository.save(note(id = 1, createdAt = 10, status = SyncStatus.FAILED))
            val viewModel = createViewModel(anna.id)

            // When
            viewModel.onIntent(ResidentIntent.RetryClicked(UUID(0, 1)))

            // Then
            assertEquals(SyncStatus.PENDING, viewModel.state.value.notes.single().status)
            assertEquals(1, syncScheduler.requestCount)
        }

    private fun createViewModel(residentId: String) = ResidentViewModel(
        residentId = residentId,
        observeResident = ObserveResidentUseCase(residentRepository),
        observeCareNotes = ObserveCareNotesUseCase(careNoteRepository),
        retryCareNoteSync = RetryCareNoteSyncUseCase(careNoteRepository, syncScheduler),
    )

    private fun note(id: Long, createdAt: Long, status: SyncStatus = SyncStatus.PENDING) = CareNote(
        id = UUID(0, id),
        residentId = anna.id,
        transcript = "Pulse 72",
        vitals = Vitals(pulse = 72),
        createdAt = Instant.ofEpochSecond(createdAt),
        syncStatus = status,
    )
}
