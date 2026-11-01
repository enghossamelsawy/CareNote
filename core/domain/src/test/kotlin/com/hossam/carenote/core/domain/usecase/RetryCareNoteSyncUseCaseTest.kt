package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.testing.repository.FakeCareNoteRepository
import com.hossam.carenote.core.testing.repository.FakeSyncScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class RetryCareNoteSyncUseCaseTest {
    private val careNoteRepository = FakeCareNoteRepository()
    private val syncScheduler = FakeSyncScheduler()
    private val retryCareNoteSync = RetryCareNoteSyncUseCase(careNoteRepository, syncScheduler)

    @Test
    fun `given a failed note, when retrying, then it is pending again and a sync is requested`() = runTest {
        // Given
        careNoteRepository.save(note(SyncStatus.FAILED))

        // When
        val queued = retryCareNoteSync(noteId)

        // Then
        assertTrue(queued)
        assertEquals(SyncStatus.PENDING, careNoteRepository.savedNotes.single().syncStatus)
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `given a note that synced meanwhile, when retrying, then it stays synced and no sync is requested`() = runTest {
        // Given
        careNoteRepository.save(note(SyncStatus.SYNCED))

        // When
        val queued = retryCareNoteSync(noteId)

        // Then
        assertFalse(queued)
        assertEquals(SyncStatus.SYNCED, careNoteRepository.savedNotes.single().syncStatus)
        assertEquals(0, syncScheduler.requestCount)
    }

    @Test
    fun `given a failed note, when retry is tapped twice, then only one sync is requested`() = runTest {
        // Given
        careNoteRepository.save(note(SyncStatus.FAILED))

        // When
        retryCareNoteSync(noteId)
        retryCareNoteSync(noteId)

        // Then
        assertEquals(1, syncScheduler.requestCount)
    }

    private val noteId = UUID(0, 1)

    private fun note(status: SyncStatus) = CareNote(
        id = noteId,
        residentId = "res-1",
        transcript = "Pulse 72",
        vitals = Vitals(pulse = 72),
        createdAt = Instant.EPOCH,
        syncStatus = status,
    )
}
