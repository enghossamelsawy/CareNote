package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.vitals.VitalsProblem
import com.hossam.carenote.core.domain.vitals.VitalsValidator
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.testing.repository.FakeCareNoteRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SaveCareNoteUseCaseTest {
    private val now = Instant.parse("2026-11-01T08:30:00Z")
    private val careNoteRepository = FakeCareNoteRepository()
    private val saveCareNote = SaveCareNoteUseCase(
        careNoteRepository = careNoteRepository,
        vitalsValidator = VitalsValidator(),
        clock = Clock.fixed(now, ZoneOffset.UTC),
    )

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
}
