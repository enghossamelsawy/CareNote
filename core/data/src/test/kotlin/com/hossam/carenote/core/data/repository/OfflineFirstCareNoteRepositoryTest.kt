package com.hossam.carenote.core.data.repository

import com.hossam.carenote.core.data.inMemoryDatabase
import com.hossam.carenote.core.database.CareNoteDatabase
import com.hossam.carenote.core.database.model.OutboxEntity
import com.hossam.carenote.core.database.model.ResidentEntity
import com.hossam.carenote.core.domain.repository.NoteAlreadyRevisedException
import com.hossam.carenote.core.domain.repository.StorageException
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class OfflineFirstCareNoteRepositoryTest {
    private val now = Instant.ofEpochSecond(1_700_000_000)
    private lateinit var database: CareNoteDatabase
    private lateinit var repository: OfflineFirstCareNoteRepository

    @Before
    fun setUp() = runTest {
        database = inMemoryDatabase()
        repository = OfflineFirstCareNoteRepository(database.careNoteDao(), Clock.fixed(now, ZoneOffset.UTC))
        database.residentDao().upsertAll(
            listOf(
                ResidentEntity(id = "res-1", firstName = "Anna", lastName = "Becker", room = "101"),
                ResidentEntity(id = "res-2", firstName = "Max", lastName = "Weber", room = "102"),
            ),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `given a new note, when saving it, then it is stored and queued at the current time`() = runTest {
        // Given
        val note = careNote(id = 1)

        // When
        repository.save(note)

        // Then
        assertEquals(listOf(note), repository.observeNotes("res-1").first())
        assertEquals(
            listOf(OutboxEntity(noteId = note.id.toString(), enqueuedAt = now)),
            database.outboxDao().oldest(limit = 10),
        )
    }

    @Test
    fun `given notes for a resident, when observing them, then they come newest first`() = runTest {
        // Given
        repository.save(careNote(id = 1, createdAt = Instant.ofEpochSecond(1)))
        repository.save(careNote(id = 2, createdAt = Instant.ofEpochSecond(2)))

        // When
        val notes = repository.observeNotes("res-1").first()

        // Then
        assertEquals(listOf(uuid(2), uuid(1)), notes.map { it.id })
    }

    @Test
    fun `given notes for two residents, when observing all notes, then both come newest first`() = runTest {
        // Given
        repository.save(careNote(id = 1, residentId = "res-1", createdAt = Instant.ofEpochSecond(1)))
        repository.save(careNote(id = 2, residentId = "res-2", createdAt = Instant.ofEpochSecond(2)))

        // When
        val notes = repository.observeAllNotes().first()

        // Then
        assertEquals(listOf(uuid(2), uuid(1)), notes.map { it.id })
    }

    @Test
    fun `given unsynced notes, when observing pending counts, then each resident maps to their count`() = runTest {
        // Given
        repository.save(careNote(id = 1, residentId = "res-1"))
        repository.save(careNote(id = 2, residentId = "res-1"))
        repository.save(careNote(id = 3, residentId = "res-2"))

        // When
        val counts = repository.observePendingCounts().first()

        // Then
        assertEquals(mapOf("res-1" to 2, "res-2" to 1), counts)
    }

    @Test
    fun `given a failed note, when retrying it, then it is pending and queued again`() = runTest {
        // Given
        val note = careNote(id = 1)
        repository.save(note)
        database.careNoteDao().updateSyncStatus(note.id.toString(), SyncStatus.FAILED)
        database.outboxDao().delete(note.id.toString())

        // When
        repository.retry(note.id)

        // Then
        assertEquals(SyncStatus.PENDING, repository.observeNotes("res-1").first().single().syncStatus)
        assertEquals(
            listOf(OutboxEntity(noteId = note.id.toString(), enqueuedAt = now)),
            database.outboxDao().oldest(limit = 10),
        )
    }

    @Test
    fun `given a note and an edit of it, when reading them back, then the edit points at the original`() = runTest {
        // Given
        val original = careNote(id = 1)
        val edit = careNote(id = 2).copy(transcript = "Pulse 76", revisionOf = original.id)
        repository.save(original)
        repository.save(edit)

        // When
        val readBack = repository.getNote(edit.id)

        // Then
        assertEquals(edit, readBack)
        assertTrue(repository.hasRevision(original.id))
        assertFalse(repository.hasRevision(edit.id))
    }

    @Test
    fun `given a database error, when saving, then it comes back as a storage error`() = runTest {
        // Given: notes are append-only, so the database refuses the same id twice
        val note = careNote(id = 1)
        repository.save(note)

        // When
        val error = runCatching { repository.save(note) }.exceptionOrNull()

        // Then
        assertTrue(error is StorageException)
    }

    @Test
    fun `given an edited note, when saving a second edit of it, then it is refused as already edited`() = runTest {
        // Given
        val original = careNote(id = 1)
        repository.save(original)
        repository.save(careNote(id = 2).copy(revisionOf = original.id))

        // When
        val error = runCatching { repository.save(careNote(id = 3).copy(revisionOf = original.id)) }.exceptionOrNull()

        // Then
        assertEquals(original.id, (error as NoteAlreadyRevisedException).noteId)
        assertFalse(repository.observeNotes("res-1").first().any { it.id == uuid(3) })
    }

    private fun uuid(id: Int): UUID = UUID(0, id.toLong())

    private fun careNote(id: Int, residentId: String = "res-1", createdAt: Instant = Instant.EPOCH) = CareNote(
        id = uuid(id),
        residentId = residentId,
        transcript = "Pulse 72",
        vitals = Vitals(pulse = 72),
        createdAt = createdAt,
    )
}
