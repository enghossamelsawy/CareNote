package com.hossam.carenote.core.domain.repository

import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** How far this phone has read the server's notes, the ones other phones wrote included. */
interface NoteFeedRepository {
    /** When the phone last had every note the server had; null until a pull first reaches the end. */
    fun observeCaughtUpAt(): Flow<Instant?>
}
