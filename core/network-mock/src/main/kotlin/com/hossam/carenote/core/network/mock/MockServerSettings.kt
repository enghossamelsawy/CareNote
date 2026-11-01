package com.hossam.carenote.core.network.mock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** How the mock server behaves; changed at runtime from the debug menu to show offline and retry handling. */
data class MockServerConfig(
    /** Each answer takes a random time in this range, like a real mobile network. */
    val latencyMillis: LongRange = 300L..1200L,
    /** Share of requests answered with 503 Service Unavailable, from 0.0 (none) to 1.0 (all). */
    val failureRate: Double = 0.0,
    /** Every request fails as if the phone had no network. */
    val offline: Boolean = false,
)

@Singleton
class MockServerSettings @Inject constructor() {
    private val _config = MutableStateFlow(MockServerConfig())
    val config: StateFlow<MockServerConfig> = _config.asStateFlow()

    fun update(change: MockServerConfig.() -> MockServerConfig) {
        _config.update(change)
    }
}
