package com.hossam.carenote.core.database.dao

import com.hossam.carenote.core.database.CareNoteDatabase
import com.hossam.carenote.core.database.careNote
import com.hossam.carenote.core.database.inMemoryDatabase
import com.hossam.carenote.core.database.outboxEntry
import com.hossam.carenote.core.database.resident
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class OutboxDaoTest {
    private lateinit var database: CareNoteDatabase
    private lateinit var dao: OutboxDao

    @Before
    fun setUp() = runTest {
        database = inMemoryDatabase()
        dao = database.outboxDao()
        database.residentDao().upsertAll(listOf(resident("r1")))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `given notes saved one after another, when reading the oldest, then they come in save order`() = runTest {
        // Given
        // Queue times that disagree with the save order, as after the phone's clock was moved back.
        queue("first", enqueuedAt = Instant.ofEpochSecond(3))
        queue("second", enqueuedAt = Instant.ofEpochSecond(1))
        queue("third", enqueuedAt = Instant.ofEpochSecond(2))

        // When
        val entries = dao.oldest(limit = 2)

        // Then
        assertEquals(listOf("first", "second"), entries.map { it.noteId })
    }

    @Test
    fun `given notes saved in the same instant, when reading the oldest, then they come in save order`() = runTest {
        // Given
        listOf("a", "b", "c").forEach { queue(it, enqueuedAt = Instant.EPOCH) }

        // When
        val entries = dao.oldest(limit = 10)

        // Then
        assertEquals(listOf("a", "b", "c"), entries.map { it.noteId })
    }

    @Test
    fun `given a refused original that was edited, when it is retried, then it is sent before its edit`() = runTest {
        // Given
        queue("original", enqueuedAt = Instant.ofEpochSecond(1))
        database.careNoteDao().markFailed("original")
        database.careNoteDao().insertAndQueue(
            careNote("edit", "r1", revisionOf = "original"),
            outboxEntry("edit", enqueuedAt = Instant.ofEpochSecond(2)),
        )

        // When
        database.careNoteDao().requeue(outboxEntry("original", enqueuedAt = Instant.ofEpochSecond(3)))

        // Then
        assertEquals(listOf("original", "edit"), dao.oldest(limit = 10).map { it.noteId })
    }

    @Test
    fun `given an entry, when recording two attempts, then attempts is 2 and the last error is kept`() = runTest {
        // Given
        queue("1")

        // When
        dao.recordAttempt("1", at = Instant.ofEpochSecond(10), error = "timeout")
        dao.recordAttempt("1", at = Instant.ofEpochSecond(20), error = "HTTP 503")

        // Then
        val entry = dao.oldest(limit = 1).single()
        assertEquals(2, entry.attempts)
        assertEquals(Instant.ofEpochSecond(20), entry.lastAttemptAt)
        assertEquals("HTTP 503", entry.lastError)
    }

    @Test
    fun `given an entry, when deleting it, then it is gone`() = runTest {
        // Given
        queue("1")

        // When
        dao.delete("1")

        // Then
        assertTrue(dao.oldest(limit = 10).isEmpty())
    }

    @Test
    fun `given a queued note, when the note is deleted, then its outbox entry is deleted too`() = runTest {
        // Given
        queue("1")

        // When
        database.openHelper.writableDatabase.execSQL("DELETE FROM care_notes WHERE id = '1'")

        // Then
        assertTrue(dao.oldest(limit = 10).isEmpty())
    }

    private suspend fun queue(noteId: String, enqueuedAt: Instant = Instant.EPOCH) {
        database.careNoteDao().insertAndQueue(careNote(noteId, "r1"), outboxEntry(noteId, enqueuedAt))
    }
}
