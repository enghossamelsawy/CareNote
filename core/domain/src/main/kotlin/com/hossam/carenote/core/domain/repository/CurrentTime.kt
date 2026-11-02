package com.hossam.carenote.core.domain.repository

import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * The time now, for what changes on screen as time passes with nothing else happening, such as a resident
 * becoming overdue for a note. A flow instead of a clock, so tests set the time instead of waiting for it.
 */
interface CurrentTime {
    /** Emits the time now first, then again every minute. */
    val now: Flow<Instant>
}
