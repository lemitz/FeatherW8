package com.featherw8.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActualPlayCounterTest {
    private val duration = 100_000L

    /** Plays [seconds] seconds from the current position, sampling every [stepMs]; the clock equals the position. */
    private fun ActualPlayCounter.playFor(seconds: Int, startMs: Long, stepMs: Long = 1_000L): Boolean {
        var counted = false
        var position = startMs
        val end = startMs + seconds * 1_000L
        while (position < end) {
            position = minOf(position + stepMs, end)
            if (sample(isPlaying = true, positionMs = position, durationMs = duration, nowMs = position)) counted = true
        }
        return counted
    }

    @Test
    fun countsOnlyAfterMoreThanNinetyPercentActuallyPlayed() {
        val counter = ActualPlayCounter()
        counter.sample(isPlaying = true, positionMs = 0L, durationMs = duration, nowMs = 0L)
        repeat(90) { step ->
            val t = (step + 1L) * 1_000L
            assertFalse(counter.sample(isPlaying = true, positionMs = t, durationMs = duration, nowMs = t))
        }
        assertTrue(counter.sample(isPlaying = true, positionMs = 91_000L, durationMs = duration, nowMs = 91_000L))
        assertFalse(counter.sample(isPlaying = true, positionMs = 92_000L, durationMs = duration, nowMs = 92_000L))
    }

    @Test
    fun pollingFarApartStillCountsEverything() {
        // A phone that slows the app down with the screen off polls every ~5 s, not every 250 ms. The old
        // counter threw away every step longer than 1.5 s and never counted such a track.
        val counter = ActualPlayCounter()
        counter.sample(isPlaying = true, positionMs = 0L, durationMs = duration, nowMs = 0L)
        assertTrue(counter.playFor(seconds = 100, startMs = 0L, stepMs = 5_000L))
        assertEquals(100_000L, counter.playedMs)
    }

    @Test
    fun aSingleHugeGapStillCountsWhenTheClockAgrees() {
        val counter = ActualPlayCounter()
        counter.sample(isPlaying = true, positionMs = 0L, durationMs = duration, nowMs = 0L)
        assertTrue(counter.sample(isPlaying = true, positionMs = 95_000L, durationMs = duration, nowMs = 95_000L))
    }

    @Test
    fun aJumpNobodyReportedIsNotCredited() {
        // The position leaps 80 s while only a second of clock time passes: that is a seek, not listening.
        val counter = ActualPlayCounter()
        counter.sample(isPlaying = true, positionMs = 0L, durationMs = duration, nowMs = 0L)
        counter.sample(isPlaying = true, positionMs = 1_000L, durationMs = duration, nowMs = 1_000L)
        val counted = counter.sample(isPlaying = true, positionMs = 81_000L, durationMs = duration, nowMs = 2_000L)
        assertFalse(counted)
        assertTrue("credited ${counter.playedMs} ms", counter.playedMs <= 2_000L + 750L)
    }

    @Test
    fun seekJumpDoesNotContributeSkippedTime() {
        val counter = ActualPlayCounter()
        counter.sample(isPlaying = true, positionMs = 0L, durationMs = duration, nowMs = 0L)
        counter.sample(isPlaying = true, positionMs = 2_000L, durationMs = duration, nowMs = 2_000L)
        counter.onSeek(fromMs = 2_000L, toMs = 90_000L, isPlaying = true, durationMs = duration, nowMs = 2_000L)
        assertFalse(counter.sample(isPlaying = true, positionMs = 91_000L, durationMs = duration, nowMs = 3_000L))
        assertEquals(3_000L, counter.playedMs) // 2 s before the seek + 1 s after it
    }

    @Test
    fun timePlayedBeforeASeekCounts() {
        val counter = ActualPlayCounter()
        counter.sample(isPlaying = true, positionMs = 0L, durationMs = duration, nowMs = 0L)
        // 60 s are played with no poll in between, then the user seeks back to the start.
        counter.onSeek(fromMs = 60_000L, toMs = 0L, isPlaying = true, durationMs = duration, nowMs = 60_000L)
        assertEquals(60_000L, counter.playedMs)
        // Listening to the first 30 s again is not yet enough (90 s of 100 s) ...
        assertFalse(counter.sample(isPlaying = true, positionMs = 30_000L, durationMs = duration, nowMs = 90_000L))
        // ... one more second is: 91 s in total, however the 60 s before the seek were interrupted.
        assertTrue(counter.sample(isPlaying = true, positionMs = 31_000L, durationMs = duration, nowMs = 91_000L))
    }

    @Test
    fun seekWhilePausedStartsNothingUntilPlayResumes() {
        val counter = ActualPlayCounter()
        counter.sample(isPlaying = true, positionMs = 0L, durationMs = duration, nowMs = 0L)
        counter.sample(isPlaying = true, positionMs = 1_000L, durationMs = duration, nowMs = 1_000L)
        counter.sample(isPlaying = false, positionMs = 1_000L, durationMs = duration, nowMs = 1_000L)
        counter.onSeek(fromMs = 1_000L, toMs = 50_000L, isPlaying = false, durationMs = duration, nowMs = 5_000L)
        counter.sample(isPlaying = true, positionMs = 50_000L, durationMs = duration, nowMs = 6_000L)
        counter.sample(isPlaying = true, positionMs = 52_000L, durationMs = duration, nowMs = 8_000L)
        assertEquals(3_000L, counter.playedMs)
    }

    @Test
    fun pausedTimeIsNotCounted() {
        val counter = ActualPlayCounter()
        counter.sample(isPlaying = true, positionMs = 0L, durationMs = 10_000L, nowMs = 0L)
        counter.sample(isPlaying = true, positionMs = 1_000L, durationMs = 10_000L, nowMs = 1_000L)
        counter.sample(isPlaying = false, positionMs = 1_000L, durationMs = 10_000L, nowMs = 1_000L)
        // Eight seconds of clock pass while paused; the position does not move.
        counter.sample(isPlaying = true, positionMs = 1_000L, durationMs = 10_000L, nowMs = 9_000L)
        assertEquals(1_000L, counter.playedMs)
    }

    @Test
    fun aLatePauseSampleStillBanksTheWholeStretch() {
        // The pause is noticed seconds after it happened: the position stood still, the clock kept going.
        val counter = ActualPlayCounter()
        counter.sample(isPlaying = true, positionMs = 0L, durationMs = duration, nowMs = 0L)
        counter.sample(isPlaying = true, positionMs = 10_000L, durationMs = duration, nowMs = 10_000L)
        counter.sample(isPlaying = false, positionMs = 10_000L, durationMs = duration, nowMs = 25_000L)
        assertEquals(10_000L, counter.playedMs)
    }

    @Test
    fun countsOncePerTrackAndNeverWithoutADuration() {
        val counter = ActualPlayCounter()
        var counts = 0
        var position = 0L
        repeat(120) {
            position += 1_000L
            if (counter.sample(true, position, duration, position)) counts++
        }
        assertEquals(1, counts)

        val unknown = ActualPlayCounter()
        position = 0L
        repeat(200) {
            position += 1_000L
            assertFalse(unknown.sample(true, position, durationMs = 0L, nowMs = position))
        }
    }

    @Test
    fun finishingAtTheEndOfATrackCountsThePartNeverPolled() {
        // The track ends between two polls; the service samples once more at its end position.
        val counter = ActualPlayCounter()
        counter.sample(isPlaying = true, positionMs = 0L, durationMs = duration, nowMs = 0L)
        counter.sample(isPlaying = true, positionMs = 40_000L, durationMs = duration, nowMs = 40_000L)
        assertTrue(counter.sample(isPlaying = true, positionMs = 100_000L, durationMs = duration, nowMs = 100_000L))
    }
}
