package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.repository.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** True while the device is online; screens use it to explain that notes will sync later. */
class ObserveNetworkStatusUseCase @Inject constructor(private val networkMonitor: NetworkMonitor) {
    operator fun invoke(): Flow<Boolean> = networkMonitor.isOnline
}
