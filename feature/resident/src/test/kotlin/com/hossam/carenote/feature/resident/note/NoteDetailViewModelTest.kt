package com.hossam.carenote.feature.resident.note

import app.cash.turbine.test
import com.hossam.carenote.core.domain.usecase.AssessVitalsUseCase
import com.hossam.carenote.core.domain.usecase.ObserveNoteHistoryUseCase
import com.hossam.carenote.core.domain.usecase.RetryCareNoteSyncUseCase
import com.hossam.carenote.core.domain.vitals.VitalLevel
import com.hossam.carenote.core.domain.vitals.VitalsAssessor
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.testing.MainDispatcherRule
import com.hossam.carenote.core.testing.repository.FakeCareNoteRepository
import com.hossam.carenote.core.testing.repository.FakeSyncScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.util.UUID

class NoteDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val careNoteRepository = FakeCareNoteRepository()
    private val syncScheduler = FakeSyncScheduler()

    private val original = note(id = 1, writtenAt = 10, transcript = "Temperature is 85.")
    private val edit = note(id = 2, writtenAt = 20, transcript = "Temperature is 37.", revisionOf = original.id)

    @Test
    fun `given an edited note, when it opens, then the edit shows with the original as earlier`() = runTest {
        // Given
        careNoteRepository.save(original)
        careNoteRepository.save(edit)

        // When
        val viewModel = createViewModel(noteId = original.id)

        // Then
        val state = viewModel.state.value
        assertEquals(edit.id, state.note?.id)
        assertEquals("Temperature is 37.", state.note?.transcript)
        assertEquals(original.createdAt, state.note?.createdAt)
        assertEquals(edit.createdAt, state.note?.editedAt)
        assertEquals(VitalLevel.Normal, state.note?.assessment?.temperature)
        val earlier = EarlierVersion(
            id = original.id,
            writtenAt = original.createdAt,
            transcript = "Temperature is 85.",
            status = SyncStatus.PENDING,
        )
        assertEquals(listOf(earlier), state.earlier)
    }

    @Test
    fun `given an open note, when it is edited elsewhere, then the screen shows the edit`() = runTest {
        // Given
        careNoteRepository.save(original)
        val viewModel = createViewModel(noteId = original.id)

        // When
        careNoteRepository.save(edit)

        // Then
        assertEquals(edit.id, viewModel.state.value.note?.id)
    }

    @Test
    fun `given an open note, when edit is clicked, then the editor opens for its latest version`() = runTest {
        // Given
        careNoteRepository.save(original)
        careNoteRepository.save(edit)
        val viewModel = createViewModel(noteId = original.id)

        // When
        viewModel.onIntent(NoteDetailIntent.EditClicked)

        // Then
        viewModel.effects.test {
            assertEquals(NoteDetailEffect.OpenEditor(edit.id), awaitItem())
        }
    }

    @Test
    fun `given a note the server refused, when retry is clicked, then it is pending and a sync is requested`() =
        runTest {
            // Given
            careNoteRepository.save(original.copy(syncStatus = SyncStatus.FAILED))
            val viewModel = createViewModel(noteId = original.id)

            // When
            viewModel.onIntent(NoteDetailIntent.RetryClicked(original.id))

            // Then
            assertEquals(SyncStatus.PENDING, viewModel.state.value.note?.status)
            assertEquals(1, syncScheduler.requestCount)
        }

    @Test
    fun `given a refused earlier version, when retry is clicked on it, then it is pending and a sync is requested`() =
        runTest {
            // Given
            careNoteRepository.save(original.copy(syncStatus = SyncStatus.FAILED))
            careNoteRepository.save(edit)
            val viewModel = createViewModel(noteId = edit.id)

            // When
            viewModel.onIntent(NoteDetailIntent.RetryClicked(original.id))

            // Then
            assertEquals(SyncStatus.PENDING, viewModel.state.value.earlier.single().status)
            assertEquals(1, syncScheduler.requestCount)
        }

    @Test
    fun `given an open note, when retry is clicked for a note not on screen, then nothing is retried`() = runTest {
        // Given
        val other = note(id = 3, writtenAt = 30, transcript = "Pulse 72").copy(syncStatus = SyncStatus.FAILED)
        careNoteRepository.save(original)
        careNoteRepository.save(other)
        val viewModel = createViewModel(noteId = original.id)

        // When
        viewModel.onIntent(NoteDetailIntent.RetryClicked(other.id))

        // Then
        assertEquals(SyncStatus.FAILED, careNoteRepository.getNote(other.id)?.syncStatus)
        assertEquals(0, syncScheduler.requestCount)
        assertFalse(viewModel.state.value.notFound)
    }

    @Test
    fun `given an unknown note, when it opens, then not found is shown`() = runTest {
        // When
        val viewModel = createViewModel(noteId = UUID(0, 9))

        // Then
        assertTrue(viewModel.state.value.notFound)
    }

    @Test
    fun `given an open note, when back is clicked, then it navigates back`() = runTest {
        // Given
        careNoteRepository.save(original)
        val viewModel = createViewModel(noteId = original.id)

        // When
        viewModel.onIntent(NoteDetailIntent.BackClicked)

        // Then
        viewModel.effects.test {
            assertEquals(NoteDetailEffect.NavigateBack, awaitItem())
        }
    }

    private fun createViewModel(noteId: UUID) = NoteDetailViewModel(
        residentId = "res-1",
        noteId = noteId,
        observeNoteHistory = ObserveNoteHistoryUseCase(careNoteRepository),
        assessVitals = AssessVitalsUseCase(VitalsAssessor()),
        retryCareNoteSync = RetryCareNoteSyncUseCase(careNoteRepository, syncScheduler),
    )

    private fun note(id: Long, writtenAt: Long, transcript: String, revisionOf: UUID? = null) = CareNote(
        id = UUID(0, id),
        residentId = "res-1",
        transcript = transcript,
        vitals = Vitals(temperatureCelsius = 37.0),
        createdAt = Instant.ofEpochSecond(writtenAt),
        revisionOf = revisionOf,
    )
}
