package com.hossam.carenote.feature.record

import app.cash.turbine.test
import com.hossam.carenote.core.domain.speech.TranscriptionError
import com.hossam.carenote.core.domain.speech.TranscriptionEvent
import com.hossam.carenote.core.domain.usecase.ObserveResidentUseCase
import com.hossam.carenote.core.domain.usecase.TranscribeSpeechUseCase
import com.hossam.carenote.core.model.Resident
import com.hossam.carenote.core.testing.MainDispatcherRule
import com.hossam.carenote.core.testing.repository.FakeResidentRepository
import com.hossam.carenote.core.testing.speech.FakeSpeechTranscriber
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecordViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val residentRepository = FakeResidentRepository()
    private val speechTranscriber = FakeSpeechTranscriber()

    private val anna = Resident(id = "res-1", firstName = "Anna", lastName = "Becker", room = "101")

    @Test
    fun `given a resident, when the screen opens, then their name is shown`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))

        // When
        val viewModel = createViewModel()

        // Then
        assertEquals("Anna Becker", viewModel.state.value.residentName)
    }

    @Test
    fun `given the screen, when record is clicked, then the mic permission is requested`() = runTest {
        // Given
        val viewModel = createViewModel()

        // When
        viewModel.onIntent(RecordIntent.RecordClicked)

        // Then
        viewModel.effects.test {
            assertEquals(RecordEffect.RequestMicPermission, awaitItem())
        }
    }

    @Test
    fun `given record was clicked, when the permission is granted, then listening starts`() = runTest {
        // Given
        val viewModel = createViewModel()

        // When
        viewModel.onIntent(granted)

        // Then
        assertTrue(speechTranscriber.isListening)
        assertEquals(RecordPhase.Recording, viewModel.state.value.phase)
        viewModel.effects.test {
            assertEquals(RecordEffect.Haptic(HapticKind.Start), awaitItem())
        }
    }

    @Test
    fun `given recording, when words are heard, then the transcript is updated`() = runTest {
        // Given
        val viewModel = createViewModel()
        viewModel.onIntent(granted)

        // When
        speechTranscriber.emit(TranscriptionEvent.Partial("Pulse"))
        speechTranscriber.emit(TranscriptionEvent.Partial("Pulse 72"))

        // Then
        assertEquals("Pulse 72", viewModel.state.value.transcript)
    }

    @Test
    fun `given recording, when the engine finishes, then the note is done`() = runTest {
        // Given
        val viewModel = createViewModel()
        viewModel.onIntent(granted)

        // When
        speechTranscriber.emit(TranscriptionEvent.Final("Pulse 72"))

        // Then
        assertEquals(RecordPhase.Done, viewModel.state.value.phase)
        assertEquals("Pulse 72", viewModel.state.value.transcript)
    }

    @Test
    fun `given recording with words heard, when stop is clicked, then listening stops and the words stay`() = runTest {
        // Given
        val viewModel = createViewModel()
        viewModel.onIntent(granted)
        speechTranscriber.emit(TranscriptionEvent.Partial("Pulse 72"))

        // When
        viewModel.onIntent(RecordIntent.StopClicked)

        // Then
        assertFalse(speechTranscriber.isListening)
        assertEquals(RecordPhase.Done, viewModel.state.value.phase)
        assertEquals("Pulse 72", viewModel.state.value.transcript)
    }

    @Test
    fun `given recording with nothing heard, when stop is clicked, then no speech is shown`() = runTest {
        // Given
        val viewModel = createViewModel()
        viewModel.onIntent(granted)

        // When
        viewModel.onIntent(RecordIntent.StopClicked)

        // Then
        assertEquals(RecordPhase.Idle, viewModel.state.value.phase)
        assertEquals(RecordError.NoSpeech, viewModel.state.value.error)
    }

    @Test
    fun `given record was clicked, when the permission is refused but can be asked again, then why is shown`() =
        runTest {
            // Given
            val viewModel = createViewModel()

            // When
            viewModel.onIntent(RecordIntent.PermissionResult(granted = false, canAskAgain = true))

            // Then
            assertEquals(MicPermission.Denied, viewModel.state.value.permission)
            assertFalse(speechTranscriber.isListening)
        }

    @Test
    fun `given record was clicked, when the permission is refused for good, then settings are offered`() = runTest {
        // Given
        val viewModel = createViewModel()

        // When
        viewModel.onIntent(RecordIntent.PermissionResult(granted = false, canAskAgain = false))

        // Then
        assertEquals(MicPermission.PermanentlyDenied, viewModel.state.value.permission)
    }

    @Test
    fun `given recording, when the engine is unavailable, then the error is shown`() = runTest {
        // Given
        val viewModel = createViewModel()
        viewModel.onIntent(granted)

        // When
        speechTranscriber.emit(TranscriptionEvent.Error(TranscriptionError.Unavailable))

        // Then
        assertEquals(RecordPhase.Idle, viewModel.state.value.phase)
        assertEquals(RecordError.Unavailable, viewModel.state.value.error)
    }

    private val granted = RecordIntent.PermissionResult(granted = true, canAskAgain = true)

    private fun createViewModel() = RecordViewModel(
        residentId = anna.id,
        observeResident = ObserveResidentUseCase(residentRepository),
        transcribeSpeech = TranscribeSpeechUseCase(speechTranscriber),
    )
}
