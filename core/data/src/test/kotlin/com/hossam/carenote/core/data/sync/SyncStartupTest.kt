package com.hossam.carenote.core.data.sync

import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.core.testing.repository.FakeCareNoteRepository
import com.hossam.carenote.core.testing.repository.FakeNetworkMonitor
import com.hossam.carenote.core.testing.repository.FakeSyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class SyncStartupTest {
    private val networkMonitor = FakeNetworkMonitor()
    private val syncScheduler = FakeSyncScheduler()
    private val careNoteRepository = FakeCareNoteRepository()

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

    @Test
    fun `given a note left syncing by a killed app, when the app starts offline, then it shows as waiting`() = runTest {
        // Given
        val note = CareNote(
            id = UUID(0, 1),
            residentId = "res-1",
            transcript = "Pulse 72",
            vitals = Vitals(pulse = 72),
            createdAt = Instant.EPOCH,
            syncStatus = SyncStatus.SYNCING,
        )
        careNoteRepository.save(note)
        networkMonitor.setOnline(false)

        // When
        start()

        // Then
        assertEquals(SyncStatus.PENDING, careNoteRepository.getNote(note.id)?.syncStatus)
    }

    /**
     * Collects in [TestScope.backgroundScope], like the app scope: it keeps running until the test ends.
     * Unconfined, so every network change is handled before the next line of the test.
     */
    private fun TestScope.start() {
        val appScope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))
        SyncStartup(networkMonitor, syncScheduler, careNoteRepository, appScope).start()
    }
}
