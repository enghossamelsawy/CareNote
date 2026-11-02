package com.hossam.carenote.core.domain.usecase

import app.cash.turbine.test
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.testing.repository.FakeCareNoteRepository
import com.hossam.carenote.core.testing.repository.FakeCurrentTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ObserveOverdueResidentsUseCaseTest {
    private val careNoteRepository = FakeCareNoteRepository()
    private val currentTime = FakeCurrentTime()
    private val observeOverdueResidents = ObserveOverdueResidentsUseCase(careNoteRepository, currentTime)

    private val written = Instant.ofEpochSecond(1_000)
    private val overdueAt = written.plus(ObserveOverdueResidentsUseCase.OVERDUE_AFTER)

    @Test
    fun `given a note 8 hours ago, when observing, then the resident is overdue since that note`() = runTest {
        // Given
        careNoteRepository.save(note(id = 1, writtenAt = written))
        currentTime.set(overdueAt)

        // When
        val overdue = observeOverdueResidents().first()

        // Then
        assertEquals(mapOf(ANNA to written), overdue)
    }

    @Test
    fun `given a recent note, when time passes 8 hours, then the resident becomes overdue`() = runTest {
        // Given
        careNoteRepository.save(note(id = 1, writtenAt = written))
        currentTime.set(overdueAt.minusSeconds(60))

        observeOverdueResidents().test {
            assertEquals(emptyMap<String, Instant>(), awaitItem())

            // When
            currentTime.set(overdueAt)

            // Then
            assertEquals(mapOf(ANNA to written), awaitItem())
        }
    }

    @Test
    fun `given an overdue resident, when a new note without vitals is saved, then they are no longer overdue`() =
        runTest {
            // Given
            careNoteRepository.save(note(id = 1, writtenAt = written))
            currentTime.set(overdueAt)

            observeOverdueResidents().test {
                assertEquals(setOf(ANNA), awaitItem().keys)

                // When
                careNoteRepository.save(note(id = 2, writtenAt = overdueAt))

                // Then
                assertEquals(emptyMap<String, Instant>(), awaitItem())
            }
        }

    @Test
    fun `given an overdue resident, when an old note is edited now, then they stay overdue`() = runTest {
        // Given
        careNoteRepository.save(note(id = 1, writtenAt = written))
        currentTime.set(overdueAt)

        // When
        careNoteRepository.save(note(id = 2, writtenAt = overdueAt, revisionOf = UUID(0, 1)))

        // Then
        assertEquals(mapOf(ANNA to written), observeOverdueResidents().first())
    }

    @Test
    fun `given an overdue resident, when time passes without a change, then nothing new is emitted`() = runTest {
        // Given
        careNoteRepository.save(note(id = 1, writtenAt = written))
        currentTime.set(overdueAt)

        observeOverdueResidents().test {
            assertEquals(setOf(ANNA), awaitItem().keys)

            // When
            currentTime.set(overdueAt.plusSeconds(60))

            // Then
            expectNoEvents()
        }
    }

    @Test
    fun `given two residents, when only one has an old note, then only that one is overdue`() = runTest {
        // Given
        careNoteRepository.save(note(id = 1, writtenAt = written, residentId = ANNA))
        careNoteRepository.save(note(id = 2, writtenAt = overdueAt, residentId = MAX))
        currentTime.set(overdueAt)

        // When
        val overdue = observeOverdueResidents().first()

        // Then
        assertEquals(setOf(ANNA), overdue.keys)
    }

    @Test
    fun `given no notes, when a long time passes, then no resident is overdue`() = runTest {
        // Given
        currentTime.set(overdueAt)

        // When
        val overdue = observeOverdueResidents().first()

        // Then
        assertEquals(emptyMap<String, Instant>(), overdue)
    }

    private fun note(id: Long, writtenAt: Instant, residentId: String = ANNA, revisionOf: UUID? = null) = CareNote(
        id = UUID(0, id),
        residentId = residentId,
        transcript = "Ate well",
        vitals = Vitals.EMPTY,
        createdAt = writtenAt,
        revisionOf = revisionOf,
    )

    private companion object {
        const val ANNA = "res-1"
        const val MAX = "res-2"
    }
}
