package com.hossam.carenote.core.data.sync

import com.hossam.carenote.core.data.FakeRemoteDataSource
import com.hossam.carenote.core.data.inMemoryDatabase
import com.hossam.carenote.core.data.repository.OfflineFirstCareNoteRepository
import com.hossam.carenote.core.database.CareNoteDatabase
import com.hossam.carenote.core.database.model.ResidentEntity
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.network.SendResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
class NoteSyncerTest {
    private val now = Instant.ofEpochSecond(1_700_000_000)
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val remoteDataSource = FakeRemoteDataSource()
    private lateinit var database: CareNoteDatabase
    private lateinit var repository: OfflineFirstCareNoteRepository
    private lateinit var syncer: NoteSyncer

    @Before
    fun setUp() = runTest {
        database = inMemoryDatabase()
        repository = OfflineFirstCareNoteRepository(database.careNoteDao(), clock)
        syncer = NoteSyncer(database.careNoteDao(), database.outboxDao(), remoteDataSource, clock)
        database.residentDao().upsertAll(
            listOf(ResidentEntity(id = "res-1", firstName = "Anna", lastName = "Becker", room = "101")),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `given queued notes, when syncing, then all are sent oldest first and marked synced`() = runTest {
        // Given
        repository.save(note(id = 1, createdAt = 10))
        repository.save(note(id = 2, createdAt = 20))

        // When
        val outcome = syncer.syncPending()

        // Then
        assertEquals(SyncOutcome.Done, outcome)
        assertEquals(listOf(UUID(0, 1), UUID(0, 2)), remoteDataSource.sentNotes.map { it.id })
        assertEquals(listOf(SyncStatus.SYNCED, SyncStatus.SYNCED), statuses())
        assertTrue(database.outboxDao().oldest(limit = 10).isEmpty())
    }

    @Test
    fun `given the server is unreachable, when syncing, then the note stays queued and sync stops`() = runTest {
        // Given
        repository.save(note(id = 1, createdAt = 10))
        repository.save(note(id = 2, createdAt = 20))
        remoteDataSource.sendResults[UUID(0, 1)] = SendResult.RetryLater

        // When
        val outcome = syncer.syncPending()

        // Then
        assertEquals(SyncOutcome.RetryLater, outcome)
        assertEquals(listOf(UUID(0, 1)), remoteDataSource.sentNotes.map { it.id })
        assertEquals(listOf(SyncStatus.PENDING, SyncStatus.PENDING), statuses())
        val entry = database.outboxDao().oldest(limit = 10).first()
        assertEquals(1, entry.attempts)
        assertEquals(now, entry.lastAttemptAt)
    }

    @Test
    fun `given the server refuses one note, when syncing, then it fails and the others still sync`() = runTest {
        // Given
        repository.save(note(id = 1, createdAt = 10))
        repository.save(note(id = 2, createdAt = 20))
        remoteDataSource.sendResults[UUID(0, 1)] = SendResult.Rejected(400)

        // When
        val outcome = syncer.syncPending()

        // Then
        assertEquals(SyncOutcome.Done, outcome)
        assertEquals(listOf(SyncStatus.FAILED, SyncStatus.SYNCED), statuses())
        assertTrue(database.outboxDao().oldest(limit = 10).isEmpty())
    }

    @Test
    fun `given a failed note that was retried, when syncing, then it is sent again and synced`() = runTest {
        // Given
        repository.save(note(id = 1, createdAt = 10))
        remoteDataSource.sendResults[UUID(0, 1)] = SendResult.Rejected(400)
        syncer.syncPending()
        remoteDataSource.sendResults.clear()
        repository.retry(UUID(0, 1))

        // When
        syncer.syncPending()

        // Then
        assertEquals(listOf(SyncStatus.SYNCED), statuses())
        assertEquals(2, remoteDataSource.sentNotes.size)
    }

    @Test
    fun `given an empty queue, when syncing, then nothing is sent`() = runTest {
        // When
        val outcome = syncer.syncPending()

        // Then
        assertEquals(SyncOutcome.Done, outcome)
        assertTrue(remoteDataSource.sentNotes.isEmpty())
    }

    @Test
    fun `given a send in flight, when the sync is cancelled, then the note is waiting and still queued`() = runTest {
        // Given
        repository.save(note(id = 1, createdAt = 10))
        remoteDataSource.hangSends = true
        val sync = launch { syncer.syncPending() }
        remoteDataSource.sendStarted.await()

        // When
        sync.cancelAndJoin()

        // Then
        assertEquals(listOf(SyncStatus.PENDING), statuses())
        assertEquals(1, database.outboxDao().oldest(limit = 10).size)
    }

    @Test
    fun `given a note left syncing by a killed app, when syncing, then it is sent and synced`() = runTest {
        // Given
        repository.save(note(id = 1, createdAt = 10))
        database.careNoteDao().updateSyncStatus(UUID(0, 1).toString(), SyncStatus.SYNCING)

        // When
        syncer.syncPending()

        // Then
        assertEquals(listOf(UUID(0, 1)), remoteDataSource.sentNotes.map { it.id })
        assertEquals(listOf(SyncStatus.SYNCED), statuses())
    }

    /** Sync status of every note, oldest first. */
    private suspend fun statuses() = repository.observeNotes("res-1").first().reversed().map { it.syncStatus }

    private fun note(id: Int, createdAt: Long) = CareNote(
        id = UUID(0, id.toLong()),
        residentId = "res-1",
        transcript = "Pulse 72",
        vitals = Vitals(pulse = 72),
        createdAt = Instant.ofEpochSecond(createdAt),
    )
}
