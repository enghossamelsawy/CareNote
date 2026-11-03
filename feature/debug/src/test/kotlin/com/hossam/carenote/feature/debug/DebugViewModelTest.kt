package com.hossam.carenote.feature.debug

import app.cash.turbine.test
import com.hossam.carenote.core.domain.usecase.RequestSyncUseCase
import com.hossam.carenote.core.network.mock.MockServer
import com.hossam.carenote.core.network.mock.MockServerSettings
import com.hossam.carenote.core.speech.ActiveSpeechEngine
import com.hossam.carenote.core.testing.MainDispatcherRule
import com.hossam.carenote.core.testing.repository.FakeSyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock

class DebugViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = MockServerSettings()
    private val speechEngine = object : ActiveSpeechEngine {
        override val activeEngine = MutableStateFlow<String?>(null)
    }

    private val mockServer = MockServer(Clock.systemUTC())
    private val syncScheduler = FakeSyncScheduler()

    // Lazy, so it's created inside each test, after the rule has replaced the main dispatcher.
    private val viewModel by lazy {
        DebugViewModel(settings, mockServer, speechEngine, RequestSyncUseCase(syncScheduler))
    }

    @Test
    fun `given the debug menu, when server offline is switched on, then the mock server goes offline`() = runTest {
        // When
        viewModel.onIntent(DebugIntent.ServerOfflineChanged(offline = true))

        // Then
        assertTrue(settings.config.value.offline)
        assertTrue(viewModel.state.value.serverOffline)
    }

    @Test
    fun `given the debug menu, when a failure rate is chosen, then the mock server uses it`() = runTest {
        // When
        viewModel.onIntent(DebugIntent.FailureRateSelected(FailureRate.Half))

        // Then
        assertEquals(0.5, settings.config.value.failureRate, 0.0)
        assertEquals(FailureRate.Half, viewModel.state.value.failureRate)
    }

    @Test
    fun `given the debug menu, when a latency is chosen, then the mock server uses it`() = runTest {
        // When
        viewModel.onIntent(DebugIntent.LatencySelected(Latency.Slow))

        // Then
        assertEquals(Latency.Slow.millis, settings.config.value.latencyMillis)
        assertEquals(Latency.Slow, viewModel.state.value.latency)
    }

    @Test
    fun `given a recording happened, when the engine is known, then the menu shows it`() = runTest {
        // When
        speechEngine.activeEngine.value = "ML Kit"

        // Then
        assertEquals("ML Kit", viewModel.state.value.speechEngine)
    }

    @Test
    fun `given the debug menu, when a note from another phone is added, then the server has it and a sync runs`() =
        runTest {
            // When
            viewModel.onIntent(DebugIntent.NoteFromAnotherPhoneClicked)

            // Then
            assertEquals(1, mockServer.storedNotes.value.size)
            assertEquals(1, viewModel.state.value.receivedNoteCount)
            assertEquals(1, syncScheduler.requestCount)
        }

    @Test
    fun `given the debug menu, when back is clicked, then it closes`() = runTest {
        // When
        viewModel.onIntent(DebugIntent.BackClicked)

        // Then
        viewModel.effects.test {
            assertEquals(DebugEffect.NavigateBack, awaitItem())
        }
    }
}
