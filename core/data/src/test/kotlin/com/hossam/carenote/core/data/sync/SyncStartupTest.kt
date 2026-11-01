package com.hossam.carenote.core.data.sync

import com.hossam.carenote.core.testing.repository.FakeNetworkMonitor
import com.hossam.carenote.core.testing.repository.FakeSyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncStartupTest {
    private val networkMonitor = FakeNetworkMonitor()
    private val syncScheduler = FakeSyncScheduler()

    @Test
    fun `given the device is online, when the app starts, then a sync is requested`() = runTest {
        // When
        start()

        // Then
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `given the app started offline, when the device comes online, then a sync is requested`() = runTest {
        // Given
        networkMonitor.setOnline(false)
        start()

        // When
        networkMonitor.setOnline(true)

        // Then
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `given the device goes offline and back, when it reconnects, then another sync is requested`() = runTest {
        // Given
        start()
        networkMonitor.setOnline(false)

        // When
        networkMonitor.setOnline(true)

        // Then
        assertEquals(2, syncScheduler.requestCount)
    }

    /**
     * Collects in [TestScope.backgroundScope], like the app scope: it keeps running until the test ends.
     * Unconfined, so every network change is handled before the next line of the test.
     */
    private fun TestScope.start() {
        val appScope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))
        SyncStartup(networkMonitor, syncScheduler, appScope).start()
    }
}
