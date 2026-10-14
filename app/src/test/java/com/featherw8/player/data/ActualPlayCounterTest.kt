package com.featherw8.player.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActualPlayCounterTest {
    @Test
    fun countsOnlyAfterMoreThanNinetyPercentActuallyPlayed() {
        val counter = ActualPlayCounter()
        counter.sample(isPlaying = true, positionMs = 0L, durationMs = 100_000L)
        repeat(90) { step ->
            assertFalse(counter.sample(isPlaying = true, positionMs = (step + 1L) * 1_000L, durationMs = 100_000L))
        }
        assertTrue(counter.sample(isPlaying = true, positionMs = 91_000L, durationMs = 100_000L))
        assertFalse(counter.sample(isPlaying = true, positionMs = 92_000L, durationMs = 100_000L))
    }

    @Test
    fun seekJumpDoesNotContributeSkippedTime() {
        val counter = ActualPlayCounter()
        counter.sample(isPlaying = true, positionMs = 0L, durationMs = 100_000L)
        counter.sample(isPlaying = true, positionMs = 1_000L, durationMs = 100_000L)
        counter.sample(isPlaying = true, positionMs = 2_000L, durationMs = 100_000L)
        counter.onSeek()
        assertFalse(counter.sample(isPlaying = true, positionMs = 90_000L, durationMs = 100_000L))
        assertFalse(counter.sample(isPlaying = true, positionMs = 91_000L, durationMs = 100_000L))
    }

    @Test
    fun pausedTimeIsNotCounted() {
        val counter = ActualPlayCounter()
        counter.sample(isPlaying = true, positionMs = 0L, durationMs = 10_000L)
        counter.sample(isPlaying = true, positionMs = 1_000L, durationMs = 10_000L)
        counter.sample(isPlaying = false, positionMs = 8_000L, durationMs = 10_000L)
        counter.sample(isPlaying = true, positionMs = 8_000L, durationMs = 10_000L)
        assertTrue(counter.playedMs == 1_000L)
    }
}
