package com.hossam.carenote.feature.resident

import app.cash.turbine.test
import com.hossam.carenote.core.domain.usecase.AssessVitalsUseCase
import com.hossam.carenote.core.domain.usecase.ObserveCareNotesUseCase
import com.hossam.carenote.core.domain.usecase.ObserveFreshnessUseCase
import com.hossam.carenote.core.domain.usecase.ObserveResidentUseCase
import com.hossam.carenote.core.domain.usecase.RetryCareNoteSyncUseCase
import com.hossam.carenote.core.domain.vitals.VitalLevel
import com.hossam.carenote.core.domain.vitals.VitalsAssessment
import com.hossam.carenote.core.domain.vitals.VitalsAssessor
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.Resident
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.testing.MainDispatcherRule
import com.hossam.carenote.core.testing.repository.FakeCareNoteRepository
import com.hossam.carenote.core.testing.repository.FakeCurrentTime
import com.hossam.carenote.core.testing.repository.FakeNoteFeedRepository
import com.hossam.carenote.core.testing.repository.FakeResidentRepository
import com.hossam.carenote.core.testing.repository.FakeSyncScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    private val noteFeedRepository = FakeNoteFeedRepository()

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
    fun `given a listed note, when it is clicked, then it opens in full`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))
        careNoteRepository.save(note(id = 1, createdAt = 10))
        val viewModel = createViewModel(anna.id)

        // When
        viewModel.onIntent(ResidentIntent.NoteClicked(UUID(0, 1)))

        // Then
        viewModel.effects.test {
            assertEquals(ResidentEffect.OpenNote(anna.id, UUID(0, 1)), awaitItem())
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
    fun `given an edited note, when the view model starts, then it shows once with both times`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))
        careNoteRepository.save(note(id = 1, createdAt = 10))
        careNoteRepository.save(note(id = 2, createdAt = 20).copy(transcript = "Pulse 76", revisionOf = UUID(0, 1)))

        // When
        val viewModel = createViewModel(anna.id)

        // Then
        val shown = viewModel.state.value.notes.single()
        assertEquals(UUID(0, 2), shown.id)
        assertEquals(Instant.ofEpochSecond(10), shown.createdAt)
        assertEquals(Instant.ofEpochSecond(20), shown.editedAt)
    }

    @Test
    fun `given a refused note that was edited, when the view model starts, then the edit says so`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))
        careNoteRepository.save(note(id = 1, createdAt = 10, status = SyncStatus.FAILED))
        careNoteRepository.save(note(id = 2, createdAt = 20).copy(revisionOf = UUID(0, 1)))

        // When
        val viewModel = createViewModel(anna.id)

        // Then
        assertTrue(viewModel.state.value.notes.single().earlierNotSynced)
    }

    @Test
    fun `given a note with high blood pressure, when the view model starts, then it is marked as high`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))
        val vitals = Vitals(systolic = 160, diastolic = 95, pulse = 72)
        careNoteRepository.save(note(id = 1, createdAt = 10, vitals = vitals))

        // When
        val viewModel = createViewModel(anna.id)

        // Then
        assertEquals(
            VitalsAssessment(bloodPressure = VitalLevel.High, pulse = VitalLevel.Normal),
            viewModel.state.value.notes.single().assessment,
        )
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

    @Test
    fun `given colleagues' notes haven't arrived, when a resident without notes is shown, then it says so`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))

        // When
        val viewModel = createViewModel(anna.id).also { show(it) }

        // Then
        assertTrue(viewModel.state.value.colleagueNotesNotArrived)
    }

    @Test
    fun `given a resident without notes, when colleagues' notes arrive, then it is a plain empty list`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))
        val viewModel = createViewModel(anna.id).also { show(it) }

        // When
        noteFeedRepository.caughtUpAt.value = Instant.EPOCH

        // Then
        assertEquals(emptyList<NoteItem>(), viewModel.state.value.notes)
        assertFalse(viewModel.state.value.colleagueNotesNotArrived)
    }

    /** Collects the state like the visible screen would; freshness only updates while it does. */
    private fun TestScope.show(viewModel: ResidentViewModel): Job =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }

    private fun createViewModel(residentId: String) = ResidentViewModel(
        residentId = residentId,
        observeResident = ObserveResidentUseCase(residentRepository),
        observeCareNotes = ObserveCareNotesUseCase(careNoteRepository),
        retryCareNoteSync = RetryCareNoteSyncUseCase(careNoteRepository, syncScheduler),
        assessVitals = AssessVitalsUseCase(VitalsAssessor()),
        observeFreshness = ObserveFreshnessUseCase(noteFeedRepository, FakeCurrentTime()),
    )

    private fun note(
        id: Long,
        createdAt: Long,
        status: SyncStatus = SyncStatus.PENDING,
        vitals: Vitals = Vitals(pulse = 72),
    ) = CareNote(
        id = UUID(0, id),
        residentId = anna.id,
        transcript = "Pulse 72",
        vitals = vitals,
        createdAt = Instant.ofEpochSecond(createdAt),
        syncStatus = status,
    )
}
