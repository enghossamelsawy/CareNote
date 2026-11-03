package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.testing.repository.FakeSyncScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RequestSyncUseCaseTest {
    private val syncScheduler = FakeSyncScheduler()
    private val requestSync = RequestSyncUseCase(syncScheduler)

    @Test
    fun `given the app, when a sync is requested, then the scheduler is asked once`() = runTest {
        // When
        requestSync()

        // Then
        assertEquals(1, syncScheduler.requestCount)
    }
}
