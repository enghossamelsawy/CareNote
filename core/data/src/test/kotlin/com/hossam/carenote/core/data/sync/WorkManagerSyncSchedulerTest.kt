package com.hossam.carenote.core.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Syncs need a network, which the test WorkManager never reports, so every requested sync stays waiting. */
@RunWith(RobolectricTestRunner::class)
class WorkManagerSyncSchedulerTest {
    private lateinit var workManager: WorkManager
    private lateinit var scheduler: WorkManagerSyncScheduler

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
        workManager = WorkManager.getInstance(context)
        scheduler = WorkManagerSyncScheduler { workManager }
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

    private suspend fun syncStates(): List<WorkInfo.State> =
        workManager.getWorkInfosForUniqueWorkFlow(WorkManagerSyncScheduler.WORK_NAME).first().map { it.state }
}
