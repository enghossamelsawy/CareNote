package com.hossam.carenote.core.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.hossam.carenote.core.common.coroutines.ApplicationScope
import com.hossam.carenote.core.domain.repository.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton

/** Follows the device's default network. Shared, so every collector uses one system callback. */
@Singleton
internal class ConnectivityManagerNetworkMonitor @Inject constructor(
    @ApplicationContext context: Context,
    @ApplicationScope applicationScope: CoroutineScope,
) : NetworkMonitor {
    private val connectivityManager = checkNotNull(context.getSystemService(ConnectivityManager::class.java))

    override val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        // The callback only reports changes, so start with the current state.
        trySend(isOnlineNow())
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
        .distinctUntilChanged()
        .shareIn(applicationScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

    private fun isOnlineNow(): Boolean = connectivityManager.activeNetwork
        ?.let(connectivityManager::getNetworkCapabilities)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

    private companion object {
        /** Keeps the callback through a screen rotation instead of re-registering it. */
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
