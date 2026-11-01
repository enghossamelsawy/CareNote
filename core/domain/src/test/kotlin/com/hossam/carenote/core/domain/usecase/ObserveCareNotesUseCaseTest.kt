package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.CurrentCareNote
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.testing.repository.FakeCareNoteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ObserveCareNotesUseCaseTest {
    private val careNoteRepository = FakeCareNoteRepository()
    private val observeCareNotes = ObserveCareNotesUseCase(careNoteRepository)

    @Test
    fun `given notes without edits, when observing, then all show newest first`() = runTest {
        // Given
        val older = note(id = 1, writtenAt = 10)
        val newer = note(id = 2, writtenAt = 20)
        careNoteRepository.save(older)
        careNoteRepository.save(newer)

        // When
        val notes = observeCareNotes("res-1").first()

        // Then
        assertEquals(
            listOf(
                CurrentCareNote(note = newer, firstWrittenAt = newer.createdAt),
                CurrentCareNote(note = older, firstWrittenAt = older.createdAt),
            ),
            notes,
        )
    }

    @Test
    fun `given an edited note, when observing, then only the edit shows at the original's place`() = runTest {
        // Given
        val original = note(id = 1, writtenAt = 10)
        val later = note(id = 2, writtenAt = 20)
        val edit = note(id = 3, writtenAt = 30, revisionOf = original.id)
        careNoteRepository.save(original)
        careNoteRepository.save(later)
        careNoteRepository.save(edit)

        // When
        val notes = observeCareNotes("res-1").first()

        // Then
        assertEquals(
            listOf(
                CurrentCareNote(note = later, firstWrittenAt = later.createdAt),
                CurrentCareNote(note = edit, firstWrittenAt = original.createdAt),
            ),
            notes,
        )
    }

    @Test
    fun `given a note edited twice, when observing, then the latest edit shows with the first time`() = runTest {
        // Given
        val original = note(id = 1, writtenAt = 10)
        val firstEdit = note(id = 2, writtenAt = 20, revisionOf = original.id)
        val secondEdit = note(id = 3, writtenAt = 30, revisionOf = firstEdit.id)
        careNoteRepository.save(original)
        careNoteRepository.save(firstEdit)
        careNoteRepository.save(secondEdit)

        // When
        val notes = observeCareNotes("res-1").first()

        // Then
        assertEquals(listOf(CurrentCareNote(note = secondEdit, firstWrittenAt = original.createdAt)), notes)
    }

    @Test
    fun `given an edit whose original is missing, when observing, then the edit shows with its own time`() = runTest {
        // Given
        val edit = note(id = 2, writtenAt = 20, revisionOf = UUID(0, 1))
        careNoteRepository.save(edit)

        // When
        val notes = observeCareNotes("res-1").first()

        // Then
        assertEquals(listOf(CurrentCareNote(note = edit, firstWrittenAt = edit.createdAt)), notes)
    }

    private fun note(id: Long, writtenAt: Long, revisionOf: UUID? = null) = CareNote(
        id = UUID(0, id),
        residentId = "res-1",
        transcript = "Pulse 72",
        vitals = Vitals(pulse = 72),
        createdAt = Instant.ofEpochSecond(writtenAt),
        revisionOf = revisionOf,
    )
}
