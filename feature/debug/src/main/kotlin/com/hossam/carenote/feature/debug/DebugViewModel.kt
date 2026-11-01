package com.hossam.carenote.feature.debug

import androidx.lifecycle.viewModelScope
import com.hossam.carenote.core.network.mock.MockServer
import com.hossam.carenote.core.network.mock.MockServerConfig
import com.hossam.carenote.core.network.mock.MockServerSettings
import com.hossam.carenote.core.speech.ActiveSpeechEngine
import com.hossam.carenote.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * The one ViewModel that talks to tools instead of use cases, on purpose: the mock server and the engine
 * name are developer tools, not business rules, and this module exists only in the mock flavor.
 */
@HiltViewModel
class DebugViewModel @Inject constructor(
    private val serverSettings: MockServerSettings,
    mockServer: MockServer,
    activeSpeechEngine: ActiveSpeechEngine,
) : MviViewModel<DebugState, DebugIntent, DebugEffect>(DebugState()) {
    init {
        combine(
            serverSettings.config,
            activeSpeechEngine.activeEngine,
            mockServer.storedNotes,
        ) { config, engine, notes ->
            DebugState(
                serverOffline = config.offline,
                failureRate = FailureRate.entries.firstOrNull { it.share == config.failureRate } ?: FailureRate.None,
                latency = Latency.entries.firstOrNull { it.millis == config.latencyMillis } ?: Latency.Normal,
                speechEngine = engine,
                receivedNoteCount = notes.size,
                recentNotes = notes.takeLast(RECENT_NOTES).reversed().map {
                    ReceivedNote(residentId = it.residentId, createdAt = it.createdAt)
                },
            )
        }.onEach { state -> setState { state } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: DebugIntent) {
        when (intent) {
            is DebugIntent.ServerOfflineChanged -> updateServer { copy(offline = intent.offline) }
            is DebugIntent.FailureRateSelected -> updateServer { copy(failureRate = intent.rate.share) }
            is DebugIntent.LatencySelected -> updateServer { copy(latencyMillis = intent.latency.millis) }
            DebugIntent.BackClicked -> sendEffect(DebugEffect.NavigateBack)
        }
    }

    private fun updateServer(change: MockServerConfig.() -> MockServerConfig) {
        serverSettings.update(change)
    }

    companion object {
        const val RECENT_NOTES = 5
    }
}
