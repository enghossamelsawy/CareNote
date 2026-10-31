package com.hossam.carenote.core.domain.usecase

import app.cash.turbine.test
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.Resident
import com.hossam.carenote.core.model.ResidentSummary
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.testing.repository.FakeCareNoteRepository
import com.hossam.carenote.core.testing.repository.FakeResidentRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ObserveResidentsWithPendingCountUseCaseTest {
    private val residentRepository = FakeResidentRepository()
    private val careNoteRepository = FakeCareNoteRepository()
    private val observeResidentsWithPendingCount =
        ObserveResidentsWithPendingCountUseCase(residentRepository, careNoteRepository)

    private val anna = Resident(id = "res-1", firstName = "Anna", lastName = "Becker", room = "101")
    private val max = Resident(id = "res-2", firstName = "Max", lastName = "Weber", room = "102")

    @Test
    fun `given residents with unsynced notes, when observing, then each gets their pending count`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna, max))
        careNoteRepository.save(note(id = 1, residentId = anna.id))
        careNoteRepository.save(note(id = 2, residentId = anna.id))
        careNoteRepository.save(note(id = 3, residentId = max.id))

        // When
        val summaries = observeResidentsWithPendingCount().first()

        // Then
        assertEquals(listOf(ResidentSummary(anna, pendingCount = 2), ResidentSummary(max, pendingCount = 1)), summaries)
    }

    @Test
    fun `given a resident without unsynced notes, when observing, then their pending count is 0`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))
        careNoteRepository.save(note(id = 1, residentId = anna.id, syncStatus = SyncStatus.SYNCED))

        // When
        val summaries = observeResidentsWithPendingCount().first()

        // Then
        assertEquals(listOf(ResidentSummary(anna, pendingCount = 0)), summaries)
    }

    @Test
    fun `given residents being observed, when a note syncs, then the pending count goes down`() = runTest {
        // Given
        residentRepository.setResidents(listOf(anna))
        careNoteRepository.save(note(id = 1, residentId = anna.id))

        observeResidentsWithPendingCount().test {
            assertEquals(listOf(ResidentSummary(anna, pendingCount = 1)), awaitItem())

            // When
            careNoteRepository.setSyncStatus(uuid(1), SyncStatus.SYNCED)

            // Then
            assertEquals(listOf(ResidentSummary(anna, pendingCount = 0)), awaitItem())
        }
    }

    private fun uuid(id: Int) = UUID(0, id.toLong())

    private fun note(id: Int, residentId: String, syncStatus: SyncStatus = SyncStatus.PENDING) = CareNote(
        id = uuid(id),
        residentId = residentId,
        transcript = "Pulse 72",
        vitals = Vitals(pulse = 72),
        createdAt = Instant.EPOCH,
        syncStatus = syncStatus,
    )
}
