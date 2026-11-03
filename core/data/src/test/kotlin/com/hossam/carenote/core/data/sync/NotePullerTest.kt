package com.hossam.carenote.core.data.sync

import com.hossam.carenote.core.common.logging.Logger
import com.hossam.carenote.core.data.FakeRemoteDataSource
import com.hossam.carenote.core.data.inMemoryDatabase
import com.hossam.carenote.core.data.repository.OfflineFirstResidentRepository
import com.hossam.carenote.core.database.CareNoteDatabase
import com.hossam.carenote.core.database.model.SyncCursorEntity
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.Resident
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.network.FetchResult
import com.hossam.carenote.core.network.NotePage
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
class NotePullerTest {
    private val now = Instant.ofEpochSecond(1_700_000_000)
    private val remoteDataSource = FakeRemoteDataSource().apply {
        residentsResult = FetchResult.Success(
            listOf(Resident(id = "res-1", firstName = "Anna", lastName = "Becker", room = "101")),
        )
    }
    private val logger = RecordingLogger()
    private lateinit var database: CareNoteDatabase
    private lateinit var puller: NotePuller

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        puller = NotePuller(
            residentRepository = OfflineFirstResidentRepository(database.residentDao(), remoteDataSource),
            remoteDataSource = remoteDataSource,
            noteFeedDao = database.noteFeedDao(),
            clock = Clock.fixed(now, ZoneOffset.UTC),
            logger = logger,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `given two pages on the server, when pulling, then both are stored and the phone is caught up`() = runTest {
        // Given
        remoteDataSource.notePages[null] = page(listOf(note(id = 1)), cursor = "c-1", hasMore = true)
        remoteDataSource.notePages["c-1"] = page(listOf(note(id = 2)), cursor = "c-2", hasMore = false)

        // When
        val outcome = puller.pull()

        // Then
        assertEquals(SyncOutcome.Done, outcome)
        assertEquals(listOf(SyncStatus.SYNCED, SyncStatus.SYNCED), (1..2).map { storedNote(it)?.syncStatus })
        assertEquals(SyncCursorEntity(SyncCursorEntity.NOTES_FEED, "c-2", now), database.noteFeedDao().notesCursor())
    }

    @Test
    fun `given an earlier pull, when pulling again, then it starts where that one stopped`() = runTest {
        // Given
        database.noteFeedDao().insertPulled(emptyList(), cursor = "c-1", caughtUpAt = Instant.EPOCH)
        remoteDataSource.notePages["c-1"] = page(listOf(note(id = 2)), cursor = "c-2", hasMore = false)

        // When
        puller.pull()

        // Then
        assertEquals(listOf("c-1"), remoteDataSource.fetchedCursors)
        assertEquals("res-1", storedNote(2)?.residentId)
    }

    @Test
    fun `given the server goes away after one page, when pulling, then that page is kept and it retries later`() =
        runTest {
            // Given
            remoteDataSource.notePages[null] = page(listOf(note(id = 1)), cursor = "c-1", hasMore = true)
            remoteDataSource.notePages["c-1"] = FetchResult.Unavailable

            // When
            val outcome = puller.pull()

            // Then
            assertEquals(SyncOutcome.RetryLater, outcome)
            assertEquals(SyncStatus.SYNCED, storedNote(1)?.syncStatus)
            assertEquals(
                SyncCursorEntity(SyncCursorEntity.NOTES_FEED, "c-1", caughtUpAt = null),
                database.noteFeedDao().notesCursor(),
            )
        }

    @Test
    fun `given the residents can't be refreshed, when pulling, then no notes are fetched`() = runTest {
        // Given: notes for residents the phone doesn't know yet would be skipped for good
        remoteDataSource.residentsResult = FetchResult.Unavailable

        // When
        val outcome = puller.pull()

        // Then
        assertEquals(SyncOutcome.RetryLater, outcome)
        assertTrue(remoteDataSource.fetchedCursors.isEmpty())
    }

    @Test
    fun `given a server that says there is more without moving on, when pulling, then it stops as failed`() = runTest {
        // Given
        remoteDataSource.notePages[null] = page(emptyList(), cursor = "c-1", hasMore = true)
        remoteDataSource.notePages["c-1"] = page(emptyList(), cursor = "c-1", hasMore = true)

        // When
        val outcome = puller.pull()

        // Then
        assertEquals(SyncOutcome.Failed, outcome)
        assertEquals(listOf(null, "c-1"), remoteDataSource.fetchedCursors)
    }

    @Test
    fun `given a note for an unknown resident, when pulling, then it is skipped and its id logged`() = runTest {
        // Given
        remoteDataSource.notePages[null] =
            page(listOf(note(id = 1), note(id = 2, residentId = "res-unknown")), cursor = "c-1", hasMore = false)

        // When
        val outcome = puller.pull()

        // Then
        assertEquals(SyncOutcome.Done, outcome)
        assertEquals("res-1", storedNote(1)?.residentId)
        assertNull(storedNote(2))
        assertEquals(listOf("Skipped notes for unknown residents: [${UUID(0, 2)}]"), logger.warnings)
    }

    private fun page(notes: List<CareNote>, cursor: String, hasMore: Boolean) =
        FetchResult.Success(NotePage(notes = notes, cursor = cursor, hasMore = hasMore))

    private fun note(id: Int, residentId: String = "res-1") = CareNote(
        id = UUID(0, id.toLong()),
        residentId = residentId,
        transcript = "Pulse 72",
        vitals = Vitals(pulse = 72),
        createdAt = Instant.ofEpochSecond(id.toLong()),
        syncStatus = SyncStatus.SYNCED,
    )

    private suspend fun storedNote(id: Int) = database.careNoteDao().getById(UUID(0, id.toLong()).toString())
}

private class RecordingLogger : Logger {
    val warnings = mutableListOf<String>()

    override fun d(tag: String, message: String) = Unit

    override fun i(tag: String, message: String) = Unit

    override fun w(tag: String, message: String, throwable: Throwable?) {
        warnings += message
    }

    override fun e(tag: String, message: String, throwable: Throwable?) = Unit
}
