package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.testing.repository.FakeCareNoteRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.UUID

class GetCareNoteUseCaseTest {
    private val careNoteRepository = FakeCareNoteRepository()
    private val getCareNote = GetCareNoteUseCase(careNoteRepository)

    @Test
    fun `given a saved note, when getting it by id, then it is returned`() = runTest {
        // Given
        val note = CareNote(
            id = UUID(0, 1),
            residentId = "res-1",
            transcript = "Pulse 72",
            vitals = Vitals(pulse = 72),
            createdAt = Instant.EPOCH,
        )
        careNoteRepository.save(note)

        // When
        val result = getCareNote(note.id)

        // Then
        assertEquals(GetNoteResult.Found(note), result)
    }

    @Test
    fun `given no such note, when getting it by id, then there is none`() = runTest {
        // When
        val result = getCareNote(UUID(0, 1))

        // Then
        assertEquals(GetNoteResult.NotFound, result)
    }

    @Test
    fun `given storage can't be read, when getting a note, then it says so instead of throwing`() = runTest {
        // Given
        careNoteRepository.failGetNote = true

        // When
        val result = getCareNote(UUID(0, 1))

        // Then
        assertEquals(GetNoteResult.Failed, result)
    }
}
