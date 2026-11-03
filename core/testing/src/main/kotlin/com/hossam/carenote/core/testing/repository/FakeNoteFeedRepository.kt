package com.hossam.carenote.core.testing.repository

import com.hossam.carenote.core.domain.repository.NoteFeedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

/** Never caught up until a test sets [caughtUpAt]. */
class FakeNoteFeedRepository : NoteFeedRepository {
    val caughtUpAt = MutableStateFlow<Instant?>(null)

    override fun observeCaughtUpAt() = caughtUpAt
}
