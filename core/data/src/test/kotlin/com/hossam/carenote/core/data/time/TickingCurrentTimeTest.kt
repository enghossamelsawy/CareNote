package com.hossam.carenote.core.data.time

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class TickingCurrentTimeTest {
    private val now = Instant.ofEpochSecond(1_700_000_000)
    private val tickingTime = TickingCurrentTime(Clock.fixed(now, ZoneOffset.UTC))

    @Test
    fun `given the clock, when observing, then the time comes at once and again a minute later`() = runTest {
        // When
        val times = tickingTime.now.take(2).toList()

        // Then
        assertEquals(listOf(now, now), times)
        assertEquals(TickingCurrentTime.TICK.inWholeMilliseconds, currentTime)
    }
}
