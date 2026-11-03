package com.hossam.carenote.core.domain.sync

import java.time.Instant

/**
 * Whether this phone has the notes other phones wrote, as far as it can know: when it last read the server's
 * notes to the end. Says nothing about this phone's own notes, which are always here.
 */
sealed interface Freshness {
    /** No pull has reached the end yet, such as on first launch: colleagues' notes may not be here at all. */
    data object NeverUpdated : Freshness

    /** Caught up at [at], recently enough to rely on. */
    data class Updated(val at: Instant) : Freshness

    /** Last caught up at [since], long enough ago that colleagues' newer notes may be missing. */
    data class OutOfDate(val since: Instant) : Freshness
}
