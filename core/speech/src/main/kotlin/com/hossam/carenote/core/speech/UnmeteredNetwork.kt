package com.hossam.carenote.core.speech

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** A connection that isn't paid by the megabyte, such as Wi-Fi, for downloads too large for mobile data. */
internal class UnmeteredNetwork @Inject constructor(@ApplicationContext context: Context) {
    private val connectivityManager = checkNotNull(context.getSystemService(ConnectivityManager::class.java))

    /** Returns at once on such a connection, otherwise when the phone joins one. */
    suspend fun await() {
        callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    trySend(capabilities.isUnmetered())
                }
            }
            // The callback only reports changes, so start with the current state.
            val current = connectivityManager.activeNetwork?.let(connectivityManager::getNetworkCapabilities)
            trySend(current?.isUnmetered() == true)
            connectivityManager.registerDefaultNetworkCallback(callback)
            awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
        }.first { it }
    }

    private fun NetworkCapabilities.isUnmetered(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
}
