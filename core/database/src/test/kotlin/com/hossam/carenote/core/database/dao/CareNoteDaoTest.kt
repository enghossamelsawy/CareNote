package com.hossam.carenote.core.database.dao

import android.database.sqlite.SQLiteConstraintException
import app.cash.turbine.test
import com.hossam.carenote.core.database.CareNoteDatabase
import com.hossam.carenote.core.database.careNote
import com.hossam.carenote.core.database.inMemoryDatabase
import com.hossam.carenote.core.database.model.PendingCount
import com.hossam.carenote.core.database.outboxEntry
import com.hossam.carenote.core.database.resident
import com.hossam.carenote.core.model.SyncStatus
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
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class CareNoteDaoTest {
    private lateinit var database: CareNoteDatabase
    private lateinit var dao: CareNoteDao

    @Before
    fun setUp() = runTest {
        database = inMemoryDatabase()
        dao = database.careNoteDao()
        database.residentDao().upsertAll(listOf(resident("r1"), resident("r2")))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `given three notes, when they are saved and queued, then they are numbered in save order`() = runTest {
        // When
        listOf("a", "b", "c").forEach { dao.insertAndQueue(careNote(it, "r1"), outboxEntry(it)) }

        // Then
        assertEquals(listOf(1L, 2L, 3L), listOf("a", "b", "c").map { dao.getById(it)?.saveOrder })
    }

    @Test
    fun `given notes for two residents, when observing one, then only theirs come newest first`() = runTest {
        // Given
        dao.insert(careNote("old", "r1", createdAt = Instant.ofEpochSecond(1)))
        dao.insert(careNote("new", "r1", createdAt = Instant.ofEpochSecond(2)))
        dao.insert(careNote("other", "r2", createdAt = Instant.ofEpochSecond(3)))

        // When
        val notes = dao.observeForResident("r1").first()

        // Then
        assertEquals(listOf("new", "old"), notes.map { it.id })
    }

    @Test
    fun `given notes for two residents, when observing all, then both residents' notes come newest first`() = runTest {
        // Given
        dao.insert(careNote("old", "r1", createdAt = Instant.ofEpochSecond(1)))
        dao.insert(careNote("other", "r2", createdAt = Instant.ofEpochSecond(2)))
        dao.insert(careNote("new", "r1", createdAt = Instant.ofEpochSecond(3)))

        // When
        val notes = dao.observeAll().first()

        // Then
        assertEquals(listOf("new", "other", "old"), notes.map { it.id })
    }

    @Test
    fun `given notes in every status, when counting pending, then all but synced count per resident`() = runTest {
        // Given
        dao.insert(careNote("1", "r1", syncStatus = SyncStatus.PENDING))
        dao.insert(careNote("2", "r1", syncStatus = SyncStatus.SYNCING))
        dao.insert(careNote("3", "r1", syncStatus = SyncStatus.FAILED))
        dao.insert(careNote("4", "r1", syncStatus = SyncStatus.SYNCED))
        dao.insert(careNote("5", "r2", syncStatus = SyncStatus.PENDING))

        // When
        val counts = dao.observePendingCounts().first()

        // Then
        assertEquals(listOf(PendingCount("r1", 3), PendingCount("r2", 1)), counts)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun `given an edited note, when inserting a second edit of it, then it throws`() = runTest {
        // Given
        dao.insert(careNote("original", "r1"))
        dao.insert(careNote("edit", "r1", revisionOf = "original"))

        // When
        dao.insert(careNote("other edit", "r1", revisionOf = "original"))

        // Then: SQLiteConstraintException
    }

    @Test
    fun `given a note and an edit of it, when checking for revisions, then only the original has one`() = runTest {
        // Given
        dao.insert(careNote("original", "r1"))
        dao.insert(careNote("edit", "r1", revisionOf = "original"))

        // When
        val originalHasRevision = dao.hasRevision("original")
        val editHasRevision = dao.hasRevision("edit")

        // Then
        assertTrue(originalHasRevision)
        assertFalse(editHasRevision)
    }

    @Test
    fun `given notes in every status, when resetting interrupted syncs, then only syncing ones wait again`() = runTest {
        // Given
        dao.insert(careNote("1", "r1", syncStatus = SyncStatus.SYNCING))
        dao.insert(careNote("2", "r1", syncStatus = SyncStatus.SYNCED))
        dao.insert(careNote("3", "r1", syncStatus = SyncStatus.FAILED))

        // When
        dao.resetSyncing()

        // Then
        val statuses = listOf("1", "2", "3").map { dao.getById(it)?.syncStatus }
        assertEquals(listOf(SyncStatus.PENDING, SyncStatus.SYNCED, SyncStatus.FAILED), statuses)
    }

    @Test
    fun `given only synced notes, when counting pending, then nothing is counted`() = runTest {
        // Given
        dao.insert(careNote("1", "r1", syncStatus = SyncStatus.SYNCED))

        // When
        val counts = dao.observePendingCounts().first()

        // Then
        assertTrue(counts.isEmpty())
    }

    @Test(expected = SQLiteConstraintException::class)
    fun `given a saved note, when inserting the same id again, then it throws`() = runTest {
        // Given
        dao.insert(careNote("1", "r1"))

        // When
        dao.insert(careNote("1", "r1"))

        // Then: SQLiteConstraintException
    }

    @Test
    fun `given a note, when inserting and queuing it, then the note and its outbox entry are saved`() = runTest {
        // Given
        val note = careNote("1", "r1")

        // When
        dao.insertAndQueue(note, outboxEntry("1"))

        // Then
        // The first note saved is numbered 1.
        assertEquals(listOf(note.copy(saveOrder = 1)), dao.observeForResident("r1").first())
        assertEquals(listOf(outboxEntry("1")), database.outboxDao().oldest(limit = 10))
    }

    @Test
    fun `given a note, when updating its sync status, then the new status is emitted`() = runTest {
        // Given
        dao.insert(careNote("1", "r1", syncStatus = SyncStatus.PENDING))

        dao.observeForResident("r1").test {
            assertEquals(SyncStatus.PENDING, awaitItem().single().syncStatus)

            // When
            dao.updateSyncStatus("1", SyncStatus.SYNCED)

            // Then
            assertEquals(SyncStatus.SYNCED, awaitItem().single().syncStatus)
        }
    }

    @Test
    fun `given a failed note, when requeuing it, then it is pending with a fresh outbox entry`() = runTest {
        // Given
        dao.insertAndQueue(careNote("1", "r1"), outboxEntry("1"))
        dao.updateSyncStatus("1", SyncStatus.FAILED)
        database.outboxDao().recordAttempt("1", at = Instant.ofEpochSecond(10), error = "HTTP 500")

        // When
        val queued = dao.requeue(outboxEntry("1", enqueuedAt = Instant.ofEpochSecond(20)))

        // Then
        assertTrue(queued)
        assertEquals(SyncStatus.PENDING, dao.observeForResident("r1").first().single().syncStatus)
        assertEquals(
            listOf(outboxEntry("1", enqueuedAt = Instant.ofEpochSecond(20))),
            database.outboxDao().oldest(limit = 10),
        )
    }

    @Test
    fun `given a synced note, when requeuing it, then it stays synced and off the queue`() = runTest {
        // Given
        dao.insertAndQueue(careNote("1", "r1"), outboxEntry("1"))
        dao.markSynced("1")

        // When
        val queued = dao.requeue(outboxEntry("1", enqueuedAt = Instant.ofEpochSecond(20)))

        // Then
        assertFalse(queued)
        assertEquals(SyncStatus.SYNCED, dao.getById("1")?.syncStatus)
        assertTrue(database.outboxDao().oldest(limit = 10).isEmpty())
    }

    @Test
    fun `given a waiting note, when requeuing it, then its queue entry is left as it was`() = runTest {
        // Given
        dao.insertAndQueue(careNote("1", "r1"), outboxEntry("1"))
        database.outboxDao().recordAttempt("1", at = Instant.ofEpochSecond(10), error = "HTTP 500")
        val entryBefore = database.outboxDao().oldest(limit = 10)

        // When
        val queued = dao.requeue(outboxEntry("1", enqueuedAt = Instant.ofEpochSecond(20)))

        // Then
        assertFalse(queued)
        assertEquals(SyncStatus.PENDING, dao.getById("1")?.syncStatus)
        assertEquals(entryBefore, database.outboxDao().oldest(limit = 10))
    }

    @Test
    fun `given a queued note, when the server accepts it, then it is synced and off the queue`() = runTest {
        // Given
        dao.insertAndQueue(careNote("1", "r1"), outboxEntry("1"))

        // When
        dao.markSynced("1")

        // Then
        assertEquals(SyncStatus.SYNCED, dao.getById("1")?.syncStatus)
        assertTrue(database.outboxDao().oldest(limit = 10).isEmpty())
    }

    @Test
    fun `given a queued note, when the server refuses it, then it is failed and off the queue`() = runTest {
        // Given
        dao.insertAndQueue(careNote("1", "r1"), outboxEntry("1"))

        // When
        dao.markFailed("1")

        // Then
        assertEquals(SyncStatus.FAILED, dao.getById("1")?.syncStatus)
        assertTrue(database.outboxDao().oldest(limit = 10).isEmpty())
    }
}
