package com.hossam.carenote.core.testing.repository

import com.hossam.carenote.core.domain.repository.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow

/** Online until a test says otherwise. */
class FakeNetworkMonitor : NetworkMonitor {
    override val isOnline = MutableStateFlow(true)

    fun setOnline(online: Boolean) {
        isOnline.value = online
    }
}
