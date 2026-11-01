package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.testing.repository.FakeCareNoteRepository
import com.hossam.carenote.core.testing.repository.FakeSyncScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
        val noteId = UUID(0, 1)
        careNoteRepository.save(
            CareNote(
                id = noteId,
                residentId = "res-1",
                transcript = "Pulse 72",
                vitals = Vitals(pulse = 72),
                createdAt = Instant.EPOCH,
                syncStatus = SyncStatus.FAILED,
            ),
        )

        // When
        retryCareNoteSync(noteId)

        // Then
        assertEquals(SyncStatus.PENDING, careNoteRepository.savedNotes.single().syncStatus)
        assertEquals(1, syncScheduler.requestCount)
    }
}
