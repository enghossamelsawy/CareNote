package com.hossam.carenote.core.database.dao

import com.hossam.carenote.core.database.CareNoteDatabase
import com.hossam.carenote.core.database.careNote
import com.hossam.carenote.core.database.inMemoryDatabase
import com.hossam.carenote.core.database.model.SyncCursorEntity
import com.hossam.carenote.core.database.outboxEntry
import com.hossam.carenote.core.database.resident
import com.hossam.carenote.core.model.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class NoteFeedDaoTest {
    private val caughtUpAt = Instant.ofEpochSecond(1_700_000_000)
    private lateinit var database: CareNoteDatabase
    private lateinit var dao: NoteFeedDao

    @Before
    fun setUp() = runTest {
        database = inMemoryDatabase()
        dao = database.noteFeedDao()
        database.residentDao().upsertAll(listOf(resident("r1"), resident("r2")))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `given a page of new notes, when storing it, then they are synced, not queued, and the cursor moves`() =
        runTest {
            // Given
            val notes = listOf(pulled("a", "r1"), pulled("b", "r2"))

            // When
            dao.insertPulled(notes, cursor = "c-1", caughtUpAt = caughtUpAt)

            // Then
            assertEquals(notes, listOf("a", "b").map { database.careNoteDao().getById(it) })
            assertEquals(emptyList<Any>(), database.outboxDao().oldest(limit = 10))
            assertEquals(SyncCursorEntity(SyncCursorEntity.NOTES_FEED, "c-1", caughtUpAt), dao.notesCursor())
        }

    @Test
    fun `given a note this phone hasn't sent yet, when the server's copy arrives, then the phone's stays`() = runTest {
        // Given
        database.careNoteDao().insertAndQueue(careNote("a", "r1"), outboxEntry("a"))

        // When
        dao.insertPulled(listOf(pulled("a", "r1")), cursor = "c-1", caughtUpAt = caughtUpAt)

        // Then
        assertEquals(SyncStatus.PENDING, database.careNoteDao().getById("a")?.syncStatus)
        assertEquals(listOf("a"), database.outboxDao().oldest(limit = 10).map { it.noteId })
    }

    @Test
    fun `given a note for an unknown resident, when storing its page, then only it is skipped`() = runTest {
        // When
        val page = dao.insertPulled(
            listOf(pulled("a", "r1"), pulled("b", "unknown")),
            cursor = "c-1",
            caughtUpAt = caughtUpAt,
        )

        // Then
        assertEquals(PulledPage(unknownResident = listOf("b"), droppedEdits = emptyList()), page)
        assertEquals("a", database.careNoteDao().getById("a")?.id)
        assertNull(database.careNoteDao().getById("b"))
        assertEquals("c-1", dao.notesCursor()?.cursor)
    }

    @Test
    fun `given a page that isn't the last, when storing it, then the cursor moves but the caught-up time stays`() =
        runTest {
            // Given
            dao.insertPulled(emptyList(), cursor = "c-1", caughtUpAt = caughtUpAt)

            // When
            dao.insertPulled(listOf(pulled("a", "r1")), cursor = "c-2", caughtUpAt = null)

            // Then
            assertEquals(SyncCursorEntity(SyncCursorEntity.NOTES_FEED, "c-2", caughtUpAt), dao.notesCursor())
        }

    @Test
    fun `given no pull yet, when reading the cursor, then there is none`() = runTest {
        // When
        val cursor = dao.notesCursor()

        // Then
        assertNull(cursor)
    }

    @Test
    fun `given no pull yet, when pages are stored, then the caught-up time appears only after the last`() = runTest {
        // Given
        assertNull(dao.observeNotesCaughtUpAt().first())

        // When
        dao.insertPulled(emptyList(), cursor = "c-1", caughtUpAt = null)
        val afterFirstPage = dao.observeNotesCaughtUpAt().first()
        dao.insertPulled(emptyList(), cursor = "c-2", caughtUpAt = caughtUpAt)
        val afterLastPage = dao.observeNotesCaughtUpAt().first()

        // Then
        assertNull(afterFirstPage)
        assertEquals(caughtUpAt, afterLastPage)
    }

    @Test
    fun `given a version this phone already edited, when another phone's edit of it arrives, then it is dropped`() =
        runTest {
            // Given
            dao.insertPulled(listOf(pulled("a", "r1"), pulled("a2", "r1", revisionOf = "a")), "c-1", caughtUpAt)

            // When
            val page = dao.insertPulled(listOf(pulled("b2", "r1", revisionOf = "a")), cursor = "c-2", caughtUpAt)

            // Then
            assertEquals(PulledPage(unknownResident = emptyList(), droppedEdits = listOf("b2")), page)
            assertNull(database.careNoteDao().getById("b2"))
        }

    @Test
    fun `given a note the phone already has, when it arrives again, then it isn't reported as dropped`() = runTest {
        // Given
        dao.insertPulled(listOf(pulled("a", "r1")), cursor = "c-1", caughtUpAt = caughtUpAt)

        // When
        val page = dao.insertPulled(listOf(pulled("a", "r1")), cursor = "c-2", caughtUpAt = caughtUpAt)

        // Then
        assertEquals(PulledPage(unknownResident = emptyList(), droppedEdits = emptyList()), page)
    }

    private fun pulled(id: String, residentId: String, revisionOf: String? = null) =
        careNote(id, residentId, syncStatus = SyncStatus.SYNCED, revisionOf = revisionOf)
}
