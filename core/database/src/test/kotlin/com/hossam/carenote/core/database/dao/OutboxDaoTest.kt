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
    fun `given several entries, when reading the oldest, then they come in enqueue order up to the limit`() = runTest {
        // Given
        queue("late", enqueuedAt = Instant.ofEpochSecond(3))
        queue("early", enqueuedAt = Instant.ofEpochSecond(1))
        queue("middle", enqueuedAt = Instant.ofEpochSecond(2))

        // When
        val entries = dao.oldest(limit = 2)

        // Then
        assertEquals(listOf("early", "middle"), entries.map { it.noteId })
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
