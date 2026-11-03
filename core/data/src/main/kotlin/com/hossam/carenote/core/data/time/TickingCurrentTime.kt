package com.hossam.carenote.core.data.time

import com.hossam.carenote.core.domain.repository.CurrentTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

/** Reads [clock] once a minute, for as long as someone collects; collect it only while a screen is shown. */
internal class TickingCurrentTime @Inject constructor(private val clock: Clock) : CurrentTime {
    override val now: Flow<Instant> = flow {
        while (true) {
            emit(clock.instant())
            delay(TICK)
        }
    }

    companion object {
        val TICK = 1.minutes
    }
}
