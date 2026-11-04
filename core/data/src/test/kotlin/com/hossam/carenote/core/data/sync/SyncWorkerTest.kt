package com.hossam.carenote.core.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.hossam.carenote.core.data.FakeRemoteDataSource
import com.hossam.carenote.core.data.inMemoryDatabase
import com.hossam.carenote.core.data.mapper.asEntity
import com.hossam.carenote.core.data.repository.OfflineFirstCareNoteRepository
import com.hossam.carenote.core.data.repository.OfflineFirstResidentRepository
import com.hossam.carenote.core.database.CareNoteDatabase
import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.Resident
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.network.FetchResult
import com.hossam.carenote.core.network.SendResult
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class SyncWorkerTest {
    private val clock = Clock.fixed(Instant.ofEpochSecond(1_700_000_000), ZoneOffset.UTC)
    private val anna = Resident(id = "res-1", firstName = "Anna", lastName = "Becker", room = "101")
    private val remoteDataSource = FakeRemoteDataSource().apply { residentsResult = FetchResult.Success(listOf(anna)) }
    private val logger = RecordingLogger()
    private lateinit var database: CareNoteDatabase
    private lateinit var worker: SyncWorker

    @Before
    fun setUp() = runTest {
        database = inMemoryDatabase()
        database.residentDao().upsertAll(listOf(anna.asEntity()))
        OfflineFirstCareNoteRepository(database.careNoteDao(), clock).save(note)
        val context = ApplicationProvider.getApplicationContext<Context>()
        worker = TestListenableWorkerBuilder<SyncWorker>(context).setWorkerFactory(SyncWorkerFactory()).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `given the server is reachable, when the worker runs, then it sends, then pulls, and succeeds`() = runTest {
        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(listOf(note.id), remoteDataSource.sentNotes.map { it.id })
        assertEquals(listOf<String?>(null), remoteDataSource.fetchedCursors)
    }

    @Test
    fun `given sending can't reach the server, when the worker runs, then it retries without pulling`() = runTest {
        // Given
        remoteDataSource.sendResults[note.id] = SendResult.RetryLater

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.retry(), result)
        assertEquals(emptyList<String?>(), remoteDataSource.fetchedCursors)
    }

    @Test
    fun `given the notes were sent, when pulling can't reach the server, then the worker retries`() = runTest {
        // Given
        remoteDataSource.notePages[null] = FetchResult.Unavailable

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `given the notes were sent, when pulling fails, then it is logged and the worker still succeeds`() = runTest {
        // Given
        remoteDataSource.notePages[null] = FetchResult.Failed

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(listOf("Pulling notes failed; they are pulled again on the next sync"), logger.errors)
    }

    private inner class SyncWorkerFactory : WorkerFactory() {
        override fun createWorker(context: Context, workerClassName: String, params: WorkerParameters) = SyncWorker(
            context = context,
            params = params,
            noteSyncer = NoteSyncer(database.careNoteDao(), database.outboxDao(), remoteDataSource, clock),
            notePuller = NotePuller(
                residentRepository = OfflineFirstResidentRepository(database.residentDao(), remoteDataSource),
                remoteDataSource = remoteDataSource,
                noteFeedDao = database.noteFeedDao(),
                clock = clock,
                logger = logger,
            ),
            logger = logger,
        )
    }

    private companion object {
        val note = CareNote(
            id = UUID(0, 1),
            residentId = "res-1",
            transcript = "Pulse 72",
            vitals = Vitals(pulse = 72),
            createdAt = Instant.ofEpochSecond(10),
        )
    }
}
