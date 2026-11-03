package com.hossam.carenote.core.domain.usecase

import app.cash.turbine.test
import com.hossam.carenote.core.domain.sync.Freshness
import com.hossam.carenote.core.testing.repository.FakeCurrentTime
import com.hossam.carenote.core.testing.repository.FakeNoteFeedRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ObserveFreshnessUseCaseTest {
    private val noteFeedRepository = FakeNoteFeedRepository()
    private val currentTime = FakeCurrentTime()
    private val observeFreshness = ObserveFreshnessUseCase(noteFeedRepository, currentTime)

    private val caughtUpAt = Instant.ofEpochSecond(1_000)
    private val outOfDateAt = caughtUpAt.plus(ObserveFreshnessUseCase.OUT_OF_DATE_AFTER)

    @Test
    fun `given no pull has reached the end, when observing, then the phone was never updated`() = runTest {
        // When
        val freshness = observeFreshness().first()

        // Then
        assertEquals(Freshness.NeverUpdated, freshness)
    }

    @Test
    fun `given a pull just under 30 minutes ago, when observing, then the phone is updated`() = runTest {
        // Given
        noteFeedRepository.caughtUpAt.value = caughtUpAt
        currentTime.set(outOfDateAt.minusSeconds(60))

        // When
        val freshness = observeFreshness().first()

        // Then
        assertEquals(Freshness.Updated(at = caughtUpAt), freshness)
    }

    @Test
    fun `given a pull exactly 30 minutes ago, when observing, then the phone is out of date`() = runTest {
        // Given
        noteFeedRepository.caughtUpAt.value = caughtUpAt
        currentTime.set(outOfDateAt)

        // When
        val freshness = observeFreshness().first()

        // Then
        assertEquals(Freshness.OutOfDate(since = caughtUpAt), freshness)
    }

    @Test
    fun `given an updated phone, when 30 minutes pass without a pull, then it turns out of date`() = runTest {
        // Given
        noteFeedRepository.caughtUpAt.value = caughtUpAt
        currentTime.set(caughtUpAt)

        observeFreshness().test {
            assertEquals(Freshness.Updated(at = caughtUpAt), awaitItem())

            // When
            currentTime.set(outOfDateAt.minusSeconds(60))
            currentTime.set(outOfDateAt)

            // Then: the minute that changed nothing isn't emitted
            assertEquals(Freshness.OutOfDate(since = caughtUpAt), awaitItem())
        }
    }

    @Test
    fun `given an out of date phone, when a pull reaches the end, then it is updated again`() = runTest {
        // Given
        noteFeedRepository.caughtUpAt.value = caughtUpAt
        currentTime.set(outOfDateAt)

        observeFreshness().test {
            assertEquals(Freshness.OutOfDate(since = caughtUpAt), awaitItem())

            // When
            noteFeedRepository.caughtUpAt.value = outOfDateAt

            // Then
            assertEquals(Freshness.Updated(at = outOfDateAt), awaitItem())
        }
    }
}
