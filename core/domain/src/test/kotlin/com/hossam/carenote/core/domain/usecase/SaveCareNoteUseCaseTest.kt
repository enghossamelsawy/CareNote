package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.vitals.VitalsProblem
import com.hossam.carenote.core.domain.vitals.VitalsValidator
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.testing.repository.FakeCareNoteRepository
import com.hossam.carenote.core.testing.repository.FakeSyncScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class SaveCareNoteUseCaseTest {
    private val now = Instant.parse("2026-11-01T08:30:00Z")
    private val careNoteRepository = FakeCareNoteRepository()
    private val syncScheduler = FakeSyncScheduler()
    private val saveCareNote = SaveCareNoteUseCase(
        careNoteRepository = careNoteRepository,
        vitalsValidator = VitalsValidator(),
        clock = Clock.fixed(now, ZoneOffset.UTC),
        syncScheduler = syncScheduler,
    )

    @Test
    fun `given a valid note, when saving, then a sync is requested`() = runTest {
        // When
        saveCareNote(residentId = "res-1", transcript = "Pulse 72", vitals = Vitals(pulse = 72))

        // Then
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `given a valid note, when saving, then it is stored as pending at the current time`() = runTest {
        // Given
        val vitals = Vitals(pulse = 72)

        // When
        val result = saveCareNote(residentId = "res-1", transcript = "Pulse 72", vitals = vitals)

        // Then
        val saved = careNoteRepository.savedNotes.single()
        assertEquals(SaveResult.Saved(saved.id), result)
        assertEquals(
            CareNote(id = saved.id, residentId = "res-1", transcript = "Pulse 72", vitals = vitals, createdAt = now),
            saved,
        )
        assertEquals(SyncStatus.PENDING, saved.syncStatus)
    }

    @Test
    fun `given implausible vitals, when saving, then nothing is stored and the problems are returned`() = runTest {
        // When
        val result = saveCareNote(residentId = "res-1", transcript = "Pulse 720", vitals = Vitals(pulse = 720))

        // Then
        assertEquals(SaveResult.Invalid(setOf(VitalsProblem.PulseOutOfRange)), result)
        assertTrue(careNoteRepository.savedNotes.isEmpty())
        assertEquals(0, syncScheduler.requestCount)
    }

    @Test
    fun `given no words and no vitals, when saving, then nothing is stored`() = runTest {
        // When
        val result = saveCareNote(residentId = "res-1", transcript = "   ", vitals = Vitals.EMPTY)

        // Then
        assertEquals(SaveResult.Empty, result)
        assertTrue(careNoteRepository.savedNotes.isEmpty())
    }

    @Test
    fun `given a transcript with spaces around it, when saving, then it is stored trimmed`() = runTest {
        // When
        saveCareNote(residentId = "res-1", transcript = "  Slept well \n", vitals = Vitals.EMPTY)

        // Then
        assertEquals("Slept well", careNoteRepository.savedNotes.single().transcript)
    }

    @Test
    fun `given the phone's storage fails, when saving, then it fails without storing or syncing`() = runTest {
        // Given
        careNoteRepository.failNextSave = true

        // When
        val result = saveCareNote(residentId = "res-1", transcript = "Pulse 72", vitals = Vitals(pulse = 72))

        // Then
        assertEquals(SaveResult.Failed, result)
        assertTrue(careNoteRepository.savedNotes.isEmpty())
        assertEquals(0, syncScheduler.requestCount)
    }

    @Test
    fun `given the current version of a note, when saving an edit, then a new note points at it`() = runTest {
        // Given
        careNoteRepository.save(original)

        // When
        val result = saveCareNote(
            residentId = "res-1",
            transcript = "Pulse 76",
            vitals = Vitals(pulse = 76),
            revisionOf = original.id,
        )

        // Then
        val edit = careNoteRepository.savedNotes.last()
        assertEquals(SaveResult.Saved(edit.id), result)
        assertEquals(
            CareNote(
                id = edit.id,
                residentId = "res-1",
                transcript = "Pulse 76",
                vitals = Vitals(pulse = 76),
                createdAt = now,
                revisionOf = original.id,
            ),
            edit,
        )
        assertEquals(original, careNoteRepository.savedNotes.first())
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `given an unknown note, when saving an edit of it, then nothing is stored`() = runTest {
        // When
        val result = saveCareNote(
            residentId = "res-1",
            transcript = "Pulse 76",
            vitals = Vitals(pulse = 76),
            revisionOf = original.id,
        )

        // Then
        assertEquals(SaveResult.CannotRevise, result)
        assertTrue(careNoteRepository.savedNotes.isEmpty())
        assertEquals(0, syncScheduler.requestCount)
    }

    @Test
    fun `given a note of another resident, when saving an edit of it, then nothing is stored`() = runTest {
        // Given
        careNoteRepository.save(original)

        // When
        val result = saveCareNote(
            residentId = "res-2",
            transcript = "Pulse 76",
            vitals = Vitals(pulse = 76),
            revisionOf = original.id,
        )

        // Then
        assertEquals(SaveResult.CannotRevise, result)
        assertEquals(listOf(original), careNoteRepository.savedNotes)
    }

    @Test
    fun `given a note that was already edited, when editing it again, then nothing is stored`() = runTest {
        // Given
        careNoteRepository.save(original)
        val firstEdit = original.copy(id = UUID(0, 2), transcript = "Pulse 76", revisionOf = original.id)
        careNoteRepository.save(firstEdit)

        // When
        val result = saveCareNote(
            residentId = "res-1",
            transcript = "Pulse 80",
            vitals = Vitals(pulse = 80),
            revisionOf = original.id,
        )

        // Then
        assertEquals(SaveResult.CannotRevise, result)
        assertEquals(listOf(original, firstEdit), careNoteRepository.savedNotes)
    }

    @Test
    fun `given two edits of one note saved at once, when the second is saved, then it is refused`() = runTest {
        // Given
        careNoteRepository.save(original)
        val firstEdit = original.copy(id = UUID(0, 2), transcript = "Pulse 76", revisionOf = original.id)
        careNoteRepository.save(firstEdit)
        // The second save checked for an edit just before the first one was stored.
        careNoteRepository.missNextRevision = true

        // When
        val result = saveCareNote(
            residentId = "res-1",
            transcript = "Pulse 80",
            vitals = Vitals(pulse = 80),
            revisionOf = original.id,
        )

        // Then
        assertEquals(SaveResult.CannotRevise, result)
        assertEquals(listOf(original, firstEdit), careNoteRepository.savedNotes)
        assertEquals(0, syncScheduler.requestCount)
    }

    private val original: CareNote
        get() = CareNote(
            id = UUID(0, 1),
            residentId = "res-1",
            transcript = "Pulse 72",
            vitals = Vitals(pulse = 72),
            createdAt = Instant.parse("2026-11-01T08:00:00Z"),
        )
}
