package com.hossam.carenote.core.testing.repository

import com.hossam.carenote.core.domain.repository.CurrentTime
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

/** Stays at the time a test sets, so nothing depends on waiting. */
class FakeCurrentTime(start: Instant = Instant.EPOCH) : CurrentTime {
    override val now = MutableStateFlow(start)

    fun set(time: Instant) {
        now.value = time
    }
}
