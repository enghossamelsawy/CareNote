package com.hossam.carenote.core.domain.usecase

import com.hossam.carenote.core.domain.repository.CurrentTime
import com.hossam.carenote.core.domain.repository.NoteFeedRepository
import com.hossam.carenote.core.domain.sync.Freshness
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Duration
import javax.inject.Inject

/**
 * How fresh this phone's copy of other phones' notes is. Emits again after each pull that reaches the end, and
 * as time passes, but only when the result changes: "Updated" turns "Out of date" by itself after
 * [OUT_OF_DATE_AFTER] without a pull.
 */
class ObserveFreshnessUseCase @Inject constructor(
    private val noteFeedRepository: NoteFeedRepository,
    private val currentTime: CurrentTime,
) {
    operator fun invoke(): Flow<Freshness> =
        combine(noteFeedRepository.observeCaughtUpAt(), currentTime.now) { caughtUpAt, now ->
            when {
                caughtUpAt == null -> Freshness.NeverUpdated
                Duration.between(caughtUpAt, now) >= OUT_OF_DATE_AFTER -> Freshness.OutOfDate(since = caughtUpAt)
                else -> Freshness.Updated(at = caughtUpAt)
            }
        }.distinctUntilChanged()

    companion object {
        /** A demo baseline: long enough not to nag during a quiet half hour, short enough to matter on a shift. */
        val OUT_OF_DATE_AFTER: Duration = Duration.ofMinutes(30)
    }
}
