package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.NoteHistory
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.testing.repository.FakeCareNoteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ObserveNoteHistoryUseCaseTest {
    private val careNoteRepository = FakeCareNoteRepository()
    private val observeNoteHistory = ObserveNoteHistoryUseCase(careNoteRepository)

    private val original = note(id = 1, writtenAt = 10)
    private val firstEdit = note(id = 2, writtenAt = 20, revisionOf = original.id)
    private val secondEdit = note(id = 3, writtenAt = 30, revisionOf = firstEdit.id)

    @Test
    fun `given a note never edited, when observing its history, then it has no earlier versions`() = runTest {
        // Given
        careNoteRepository.save(original)

        // When
        val history = observeNoteHistory("res-1", original.id).first()

        // Then
        assertEquals(NoteHistory(current = original, earlier = emptyList()), history)
    }

    @Test
    fun `given a note edited twice, when observing its history, then the earlier versions come newest first`() =
        runTest {
            // Given
            saveAll(original, firstEdit, secondEdit)

            // When
            val history = observeNoteHistory("res-1", secondEdit.id).first()

            // Then
            assertEquals(NoteHistory(current = secondEdit, earlier = listOf(firstEdit, original)), history)
            assertEquals(original.createdAt, history?.firstWrittenAt)
        }

    @Test
    fun `given an old version's id, when observing its history, then it follows to the latest version`() = runTest {
        // Given
        saveAll(original, firstEdit, secondEdit)

        // When
        val history = observeNoteHistory("res-1", original.id).first()

        // Then
        assertEquals(secondEdit, history?.current)
    }

    @Test
    fun `given an open history, when the note is edited, then the new version is emitted`() = runTest {
        // Given
        careNoteRepository.save(original)
        val histories = observeNoteHistory("res-1", original.id)

        // When
        careNoteRepository.save(firstEdit)

        // Then
        assertEquals(NoteHistory(current = firstEdit, earlier = listOf(original)), histories.first())
    }

    @Test
    fun `given another resident's note, when observing its history, then there is none`() = runTest {
        // Given
        careNoteRepository.save(original)

        // When
        val history = observeNoteHistory("res-2", original.id).first()

        // Then
        assertNull(history)
    }

    private suspend fun saveAll(vararg notes: CareNote) {
        notes.forEach { careNoteRepository.save(it) }
    }

    private fun note(id: Long, writtenAt: Long, revisionOf: UUID? = null) = CareNote(
        id = UUID(0, id),
        residentId = "res-1",
        transcript = "Version $id",
        vitals = Vitals(pulse = 72),
        createdAt = Instant.ofEpochSecond(writtenAt),
        revisionOf = revisionOf,
    )
}
