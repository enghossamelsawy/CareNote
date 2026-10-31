package com.hossam.carenote.core.database.dao

import android.database.sqlite.SQLiteConstraintException
import app.cash.turbine.test
import com.hossam.carenote.core.database.CareNoteDatabase
import com.hossam.carenote.core.database.careNote
import com.hossam.carenote.core.database.inMemoryDatabase
import com.hossam.carenote.core.database.outboxEntry
import com.hossam.carenote.core.database.resident
import com.hossam.carenote.core.model.SyncStatus
import kotlinx.coroutines.flow.first
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
        assertEquals(mapOf("r1" to 3, "r2" to 1), counts)
    }

    @Test
    fun `given only synced notes, when counting pending, then the map is empty`() = runTest {
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
        assertEquals(listOf(note), dao.observeForResident("r1").first())
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
}
