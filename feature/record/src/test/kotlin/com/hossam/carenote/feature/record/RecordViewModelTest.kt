package com.hossam.carenote.feature.record

import app.cash.turbine.test
import com.hossam.carenote.core.domain.speech.TranscriptionError
import com.hossam.carenote.core.domain.speech.TranscriptionEvent
import com.hossam.carenote.core.domain.usecase.GetCareNoteUseCase
import com.hossam.carenote.core.domain.usecase.ObserveResidentUseCase
import com.hossam.carenote.core.domain.usecase.ParseVitalsUseCase
import com.hossam.carenote.core.domain.usecase.SaveCareNoteUseCase
import com.hossam.carenote.core.domain.usecase.TranscribeSpeechUseCase
import com.hossam.carenote.core.domain.vitals.VitalsParser
import com.hossam.carenote.core.domain.vitals.VitalsValidator
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.Resident
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.testing.MainDispatcherRule
import com.hossam.carenote.core.testing.repository.FakeCareNoteRepository
import com.hossam.carenote.core.testing.repository.FakeResidentRepository
import com.hossam.carenote.core.testing.repository.FakeSyncScheduler
import com.hossam.carenote.core.testing.speech.FakeSpeechTranscriber
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class RecordViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val residentRepository = FakeResidentRepository()
    private val speechTranscriber = FakeSpeechTranscriber()
    private val careNoteRepository = FakeCareNoteRepository()

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

    @Test
    fun `given recording, when it finishes, then the dictated vitals fill the form`() = runTest {
        // Given
        val viewModel = createViewModel()
        viewModel.onIntent(granted)

        // When
        speechTranscriber.emit(TranscriptionEvent.Final("Blood pressure 120 over 80, pulse 72, temperature 36.8"))

        // Then
        val vitals = viewModel.state.value.vitals
        assertEquals(
            listOf("120", "80", "72", "36.8", ""),
            VitalField.entries.map { vitals[it] },
        )
    }

    @Test
    fun `given a note in review, when a vital is edited, then the form shows the new value`() = runTest {
        // Given
        val viewModel = reviewing("Pulse 72")

        // When
        viewModel.onIntent(RecordIntent.VitalChanged(VitalField.Pulse, "76"))

        // Then
        assertEquals("76", viewModel.state.value.vitals[VitalField.Pulse])
    }

    @Test
    fun `given a corrected vital, when the transcript is edited, then the correction stays`() = runTest {
        // Given
        val viewModel = reviewing("Pulse 72")
        viewModel.onIntent(RecordIntent.VitalChanged(VitalField.Pulse, "76"))

        // When
        viewModel.onIntent(RecordIntent.TranscriptChanged("Pulse 72, slept well"))

        // Then
        assertEquals("Pulse 72, slept well", viewModel.state.value.transcript)
        assertEquals("76", viewModel.state.value.vitals[VitalField.Pulse])
    }

    @Test
    fun `given a vital that is not a number, when saving, then it is flagged and nothing is saved`() = runTest {
        // Given
        val viewModel = reviewing("Pulse 72")
        viewModel.onIntent(RecordIntent.VitalChanged(VitalField.Pulse, "7x"))

        // When
        viewModel.onIntent(RecordIntent.SaveClicked)

        // Then
        assertEquals(mapOf(VitalField.Pulse to FieldError.NotANumber), viewModel.state.value.vitals.errors)
        assertTrue(careNoteRepository.savedNotes.isEmpty())
    }

    @Test
    fun `given an implausible pulse, when saving, then it is flagged and nothing is saved`() = runTest {
        // Given
        val viewModel = reviewing("Pulse 720")

        // When
        viewModel.onIntent(RecordIntent.SaveClicked)

        // Then
        assertEquals(mapOf(VitalField.Pulse to FieldError.OutOfRange), viewModel.state.value.vitals.errors)
        assertFalse(viewModel.state.value.isSaving)
        assertTrue(careNoteRepository.savedNotes.isEmpty())
    }

    @Test
    fun `given a flagged vital, when it is changed, then its error clears`() = runTest {
        // Given
        val viewModel = reviewing("Pulse 720")
        viewModel.onIntent(RecordIntent.SaveClicked)

        // When
        viewModel.onIntent(RecordIntent.VitalChanged(VitalField.Pulse, "72"))

        // Then
        assertTrue(viewModel.state.value.vitals.errors.isEmpty())
    }

    @Test
    fun `given a valid note in review, when saving, then it is stored for this resident and the screen closes`() =
        runTest {
            // Given
            val viewModel = reviewing("Pulse 72, temperature 36,8")

            // When
            viewModel.onIntent(RecordIntent.SaveClicked)

            // Then
            val saved = careNoteRepository.savedNotes.single()
            assertEquals(anna.id, saved.residentId)
            assertEquals(Vitals(pulse = 72, temperatureCelsius = 36.8), saved.vitals)
            viewModel.effects.test {
                // The start and stop haptics from recording come first.
                assertEquals(RecordEffect.NoteSaved, expectMostRecentItem())
            }
        }

    @Test
    fun `given a note in review, when recording again, then the old words and vitals are replaced`() = runTest {
        // Given
        val viewModel = reviewing("Pulse 72")

        // When
        viewModel.onIntent(granted)
        speechTranscriber.emit(TranscriptionEvent.Final("Temperature 37.2"))

        // Then
        assertEquals("Temperature 37.2", viewModel.state.value.transcript)
        assertEquals("", viewModel.state.value.vitals[VitalField.Pulse])
        assertEquals("37.2", viewModel.state.value.vitals[VitalField.Temperature])
    }

    @Test
    fun `given three recordings added one by one, when they finish, then they make one note with every vital`() =
        runTest {
            // Given
            val viewModel = reviewing("Temperature 36.8")

            // When
            recordMore(viewModel, "blood pressure 120 over 80")
            recordMore(viewModel, "pulse 72")

            // Then
            val state = viewModel.state.value
            assertEquals("Temperature 36.8 blood pressure 120 over 80 pulse 72", state.transcript)
            assertEquals(
                listOf("120", "80", "72", "36.8", ""),
                VitalField.entries.map { state.vitals[it] },
            )
        }

    @Test
    fun `given a note in review, when recording more, then the earlier words show while speaking`() = runTest {
        // Given
        val viewModel = reviewing("Temperature 36.8")
        viewModel.onIntent(RecordIntent.RecordMoreClicked)
        viewModel.onIntent(granted)

        // When
        speechTranscriber.emit(TranscriptionEvent.Partial("pulse"))

        // Then
        assertEquals("Temperature 36.8 pulse", viewModel.state.value.transcript)
    }

    @Test
    fun `given a temperature, when a later recording says another, then the later one wins`() = runTest {
        // Given
        val viewModel = reviewing("Temperature 36.8")

        // When
        recordMore(viewModel, "temperature 37.4")

        // Then
        assertEquals("37.4", viewModel.state.value.vitals[VitalField.Temperature])
    }

    @Test
    fun `given a corrected pulse, when recording more without a pulse, then the correction stays`() = runTest {
        // Given
        val viewModel = reviewing("Pulse 72")
        viewModel.onIntent(RecordIntent.VitalChanged(VitalField.Pulse, "76"))

        // When
        recordMore(viewModel, "temperature 36.8")

        // Then
        assertEquals("76", viewModel.state.value.vitals[VitalField.Pulse])
        assertEquals("36.8", viewModel.state.value.vitals[VitalField.Temperature])
    }

    @Test
    fun `given a note in review, when recording more and nothing is heard, then the note stays as it was`() = runTest {
        // Given
        val viewModel = reviewing("Pulse 72")
        viewModel.onIntent(RecordIntent.RecordMoreClicked)
        viewModel.onIntent(granted)

        // When
        viewModel.onIntent(RecordIntent.StopClicked)

        // Then
        val state = viewModel.state.value
        assertEquals(RecordPhase.Done, state.phase)
        assertEquals("Pulse 72", state.transcript)
        assertEquals("72", state.vitals[VitalField.Pulse])
        assertEquals(RecordError.NoSpeech, state.error)
    }

    @Test
    fun `given a note in review, when starting over, then the old words and vitals are gone`() = runTest {
        // Given
        val viewModel = reviewing("Pulse 72")

        // When
        viewModel.onIntent(RecordIntent.RecordClicked)
        viewModel.onIntent(granted)
        speechTranscriber.emit(TranscriptionEvent.Final("Temperature 37.2"))

        // Then
        assertEquals("Temperature 37.2", viewModel.state.value.transcript)
        assertEquals("", viewModel.state.value.vitals[VitalField.Pulse])
    }

    @Test
    fun `given nothing dictated yet, when back is clicked, then the screen closes without asking`() = runTest {
        // Given
        val viewModel = createViewModel()

        // When
        viewModel.onIntent(RecordIntent.BackClicked)

        // Then
        assertFalse(viewModel.state.value.showDiscardDialog)
        viewModel.effects.test {
            assertEquals(RecordEffect.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `given an unsaved note, when back is clicked, then it asks before discarding`() = runTest {
        // Given
        val viewModel = reviewing("Pulse 72")

        // When
        viewModel.onIntent(RecordIntent.BackClicked)

        // Then
        assertTrue(viewModel.state.value.showDiscardDialog)
    }

    @Test
    fun `given the discard question, when discarding is confirmed, then the screen closes`() = runTest {
        // Given
        val viewModel = reviewing("Pulse 72")
        viewModel.onIntent(RecordIntent.BackClicked)

        // When
        viewModel.onIntent(RecordIntent.DiscardConfirmed)

        // Then
        assertFalse(viewModel.state.value.showDiscardDialog)
        viewModel.effects.test {
            assertEquals(RecordEffect.NavigateBack, expectMostRecentItem())
        }
    }

    @Test
    fun `given the discard question, when the user keeps editing, then the note stays`() = runTest {
        // Given
        val viewModel = reviewing("Pulse 72")
        viewModel.onIntent(RecordIntent.BackClicked)

        // When
        viewModel.onIntent(RecordIntent.DiscardDismissed)

        // Then
        assertFalse(viewModel.state.value.showDiscardDialog)
        assertEquals("Pulse 72", viewModel.state.value.transcript)
    }

    private val granted = RecordIntent.PermissionResult(granted = true, canAskAgain = true)

    /** "Record more", the permission granted, then the engine hears [words]. */
    private suspend fun recordMore(viewModel: RecordViewModel, words: String) {
        viewModel.onIntent(RecordIntent.RecordMoreClicked)
        viewModel.onIntent(granted)
        speechTranscriber.emit(TranscriptionEvent.Final(words))
    }

    /** A view model whose recording finished with [transcript], ready for review. */
    private suspend fun reviewing(transcript: String): RecordViewModel {
        val viewModel = createViewModel()
        viewModel.onIntent(granted)
        speechTranscriber.emit(TranscriptionEvent.Final(transcript))
        return viewModel
    }

    @Test
    fun `given a note in review, when record more and start over are tapped quickly, then only one recording runs`() =
        runTest {
            // Given
            val viewModel = reviewing("Pulse 72")

            // When
            viewModel.onIntent(RecordIntent.RecordMoreClicked)
            viewModel.onIntent(RecordIntent.RecordClicked)
            viewModel.onIntent(granted)
            viewModel.onIntent(granted)

            // Then
            assertEquals(1, speechTranscriber.listenerCount)
            assertEquals("Pulse 72", viewModel.state.value.transcript)
        }

    @Test
    fun `given a doubled start, when recording is stopped, then the microphone is free`() = runTest {
        // Given
        val viewModel = reviewing("Pulse 72")
        viewModel.onIntent(RecordIntent.RecordMoreClicked)
        viewModel.onIntent(RecordIntent.RecordClicked)
        viewModel.onIntent(granted)
        viewModel.onIntent(granted)

        // When
        viewModel.onIntent(RecordIntent.StopClicked)

        // Then
        assertEquals(0, speechTranscriber.listenerCount)
    }

    @Test
    fun `given a note being saved, when the words or a vital change, then the note stays as saved`() = runTest {
        // Given
        val viewModel = reviewing("Pulse 72")
        viewModel.onIntent(RecordIntent.SaveClicked)

        // When
        viewModel.onIntent(RecordIntent.TranscriptChanged("Pulse 72, slept well"))
        viewModel.onIntent(RecordIntent.VitalChanged(VitalField.Pulse, "80"))

        // Then
        assertEquals("Pulse 72", viewModel.state.value.transcript)
        assertEquals("72", viewModel.state.value.vitals[VitalField.Pulse])
    }

    @Test
    fun `given a note being saved, when back is clicked, then the screen waits for the save`() = runTest {
        // Given
        val viewModel = reviewing("Pulse 72")
        viewModel.onIntent(RecordIntent.SaveClicked)

        // When
        viewModel.onIntent(RecordIntent.BackClicked)

        // Then
        assertFalse(viewModel.state.value.showDiscardDialog)
        viewModel.effects.test {
            assertEquals(RecordEffect.NoteSaved, expectMostRecentItem())
        }
    }

    @Test
    fun `given a saved note, when editing it, then it opens in review with the note filled in`() = runTest {
        // Given
        careNoteRepository.save(savedNote)

        // When
        val viewModel = createViewModel(noteId = savedNote.id)

        // Then
        val state = viewModel.state.value
        assertEquals(RecordPhase.Done, state.phase)
        assertEquals("Pulse 72", state.transcript)
        assertEquals("72", state.vitals[VitalField.Pulse])
        assertTrue(state.isEditing)
    }

    @Test
    fun `given an opened edit, when it is saved, then a new version of the note is stored`() = runTest {
        // Given
        careNoteRepository.save(savedNote)
        val viewModel = createViewModel(noteId = savedNote.id)
        viewModel.onIntent(RecordIntent.VitalChanged(VitalField.Pulse, "76"))

        // When
        viewModel.onIntent(RecordIntent.SaveClicked)

        // Then
        val edit = careNoteRepository.savedNotes.last()
        assertEquals(savedNote.id, edit.revisionOf)
        assertEquals(Vitals(pulse = 76), edit.vitals)
        viewModel.effects.test {
            assertEquals(RecordEffect.NoteSaved, awaitItem())
        }
    }

    @Test
    fun `given an edit with no changes, when back is clicked, then the screen closes without asking`() = runTest {
        // Given
        careNoteRepository.save(savedNote)
        val viewModel = createViewModel(noteId = savedNote.id)

        // When
        viewModel.onIntent(RecordIntent.BackClicked)

        // Then
        assertFalse(viewModel.state.value.showDiscardDialog)
        viewModel.effects.test {
            assertEquals(RecordEffect.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `given a changed edit, when back is clicked, then it asks before discarding`() = runTest {
        // Given
        careNoteRepository.save(savedNote)
        val viewModel = createViewModel(noteId = savedNote.id)
        viewModel.onIntent(RecordIntent.TranscriptChanged("Pulse 72, slept well"))

        // When
        viewModel.onIntent(RecordIntent.BackClicked)

        // Then
        assertTrue(viewModel.state.value.showDiscardDialog)
    }

    @Test
    fun `given another resident's note, when editing it, then it doesn't open and says so`() = runTest {
        // Given
        careNoteRepository.save(savedNote.copy(residentId = "res-2"))

        // When
        val viewModel = createViewModel(noteId = savedNote.id)

        // Then
        assertEquals(RecordPhase.Idle, viewModel.state.value.phase)
        assertEquals(RecordError.CannotEdit, viewModel.state.value.error)
    }

    private val savedNote = CareNote(
        id = UUID(0, 1),
        residentId = "res-1",
        transcript = "Pulse 72",
        vitals = Vitals(pulse = 72),
        createdAt = Instant.EPOCH,
    )

    private fun createViewModel(noteId: UUID? = null) = RecordViewModel(
        residentId = anna.id,
        noteId = noteId,
        observeResident = ObserveResidentUseCase(residentRepository),
        getCareNote = GetCareNoteUseCase(careNoteRepository),
        transcribeSpeech = TranscribeSpeechUseCase(speechTranscriber),
        parseVitals = ParseVitalsUseCase(VitalsParser()),
        saveCareNote = SaveCareNoteUseCase(
            careNoteRepository = careNoteRepository,
            vitalsValidator = VitalsValidator(),
            clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
            syncScheduler = FakeSyncScheduler(),
        ),
    )
}
