package com.hossam.carenote.core.domain.repository

import kotlinx.coroutines.flow.Flow

/** Whether the device has a network right now. Says nothing about whether the server is reachable. */
interface NetworkMonitor {
    /** Emits the current state first, then every change. */
    val isOnline: Flow<Boolean>
}
