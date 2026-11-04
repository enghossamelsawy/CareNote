package com.hossam.carenote.core.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Syncs need a network, which the test WorkManager never reports, so every requested sync stays waiting
 * until a test runs it. A run does [work] instead of a real sync.
 */
@RunWith(RobolectricTestRunner::class)
class WorkManagerSyncSchedulerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val logger = RecordingLogger()
    private var work: suspend () -> ListenableWorker.Result = { ListenableWorker.Result.success() }
    private lateinit var workManager: WorkManager
    private lateinit var scheduler: WorkManagerSyncScheduler

    @Before
    fun setUp() {
        val configuration = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .setWorkerFactory(StubWorkerFactory { work() })
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
        workManager = WorkManager.getInstance(context)
        scheduler = WorkManagerSyncScheduler(workManager = { workManager }, logger = logger)
    }

    @Test
    fun `given no sync, when one is requested, then it waits for a network`() = runTest {
        // When
        scheduler.requestSync()

        // Then
        assertEquals(listOf(WorkInfo.State.ENQUEUED), syncStates())
    }

    @Test
    fun `given a sync waiting for a network, when more are requested, then it stays the only one`() = runTest {
        // Given
        scheduler.requestSync()

        // When
        scheduler.requestSync()
        scheduler.requestSync()

        // Then
        assertEquals(listOf(WorkInfo.State.ENQUEUED), syncStates())
    }

    @Test
    fun `given a running sync, when more are requested, then one more waits behind it`() = runTest {
        // Given
        val finish = CompletableDeferred<Unit>()
        work = {
            finish.await()
            ListenableWorker.Result.success()
        }
        scheduler.requestSync()
        runWaitingSync()
        awaitSync { it.state == WorkInfo.State.RUNNING }

        // When
        scheduler.requestSync()
        scheduler.requestSync()

        // Then
        assertEquals(listOf(WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED), syncStates().sorted())
        finish.complete(Unit)
    }

    @Test
    fun `given a sync waiting out its backoff, when one is requested, then a new one replaces it`() = runTest {
        // Given
        work = { ListenableWorker.Result.retry() }
        scheduler.requestSync()
        val retried = runWaitingSync()
        awaitSync { it.state == WorkInfo.State.ENQUEUED && it.runAttemptCount == 1 }

        // When
        scheduler.requestSync()

        // Then
        val sync = syncs().single()
        assertNotEquals(retried, sync.id)
        assertEquals(WorkInfo.State.ENQUEUED, sync.state)
        assertEquals(0, sync.runAttemptCount)
    }

    @Test
    fun `given WorkManager fails, when a sync is requested, then the error is logged instead of thrown`() = runTest {
        // Given
        val failing = WorkManagerSyncScheduler(workManager = { error("WorkManager isn't ready") }, logger = logger)

        // When
        failing.requestSync()

        // Then
        assertEquals(listOf("Couldn't request a sync"), logger.errors)
    }

    /** Lets the waiting sync start, as if the device came online, and returns its id. */
    private suspend fun runWaitingSync() = syncs().single().id.also { id ->
        WorkManagerTestInitHelper.getTestDriver(context)!!.setAllConstraintsMet(id)
    }

    private suspend fun awaitSync(predicate: (WorkInfo) -> Boolean) {
        syncsFlow().first { syncs -> syncs.any(predicate) }
    }

    private fun syncsFlow() = workManager.getWorkInfosForUniqueWorkFlow(WorkManagerSyncScheduler.WORK_NAME)

    private suspend fun syncs(): List<WorkInfo> = syncsFlow().first()

    private suspend fun syncStates(): List<WorkInfo.State> = syncs().map { it.state }
}

/** Creates a worker that runs [work], whatever worker class was requested. */
private class StubWorkerFactory(private val work: suspend () -> ListenableWorker.Result) : WorkerFactory() {
    override fun createWorker(context: Context, workerClassName: String, params: WorkerParameters) =
        StubWorker(context, params, work)
}

private class StubWorker(
    context: Context,
    params: WorkerParameters,
    private val work: suspend () -> ListenableWorker.Result,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = work()
}
