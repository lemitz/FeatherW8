package com.featherw8.player.data

/**
 * Counts how long a track was really listened to, and says when that passed 90 % of it.
 *
 * Playback is split into segments: from the moment the player starts (or resumes, or is moved by a seek)
 * to the moment it pauses, is seeked or the track ends. What a segment is worth is read from the player's
 * position when it is closed, not summed up from small steps, so it does not matter how often the
 * counter is asked. That matters on phones that slow an app down in the background: with the screen
 * off the polling can run seconds apart, and counting small steps would throw all that time away.
 *
 * Two things keep it honest. A seek ends the segment where it was and starts a new one where the seek
 * landed, so the time jumped over is never credited. And a segment can never be worth more than the
 * clock says has passed since it began, so a position jump nobody reported is not credited either.
 */
class ActualPlayCounter {
    var playedMs: Long = 0L
        private set

    private var bankedMs = 0L
    private var segmentStartPositionMs = NO_SEGMENT
    private var segmentStartClockMs = 0L
    private var counted = false

    /**
     * Updates the count from the player's state. [nowMs] is a monotonic clock (elapsedRealtime) and has
     * to be the same clock on every call. Returns true exactly once: when more than 90 % was played.
     */
    fun sample(isPlaying: Boolean, positionMs: Long, durationMs: Long, nowMs: Long): Boolean {
        if (isPlaying) {
            if (segmentStartPositionMs == NO_SEGMENT) {
                segmentStartPositionMs = positionMs
                segmentStartClockMs = nowMs
            }
            playedMs = bankedMs + openSegmentWorth(positionMs, nowMs)
        } else {
            closeSegment(positionMs, nowMs)
        }
        return reachedThreshold(durationMs)
    }

    /**
     * The position jumped from [fromMs] to [toMs] on purpose (a seek). What was played up to the jump
     * counts, the jump does not, and counting resumes from where it landed if the player is playing.
     */
    fun onSeek(fromMs: Long, toMs: Long, isPlaying: Boolean, durationMs: Long, nowMs: Long): Boolean {
        closeSegment(fromMs, nowMs)
        if (isPlaying) {
            segmentStartPositionMs = toMs
            segmentStartClockMs = nowMs
        }
        return reachedThreshold(durationMs)
    }

    private fun openSegmentWorth(positionMs: Long, nowMs: Long): Long {
        if (segmentStartPositionMs == NO_SEGMENT) return 0L
        val advanced = positionMs - segmentStartPositionMs
        val elapsed = nowMs - segmentStartClockMs
        return advanced.coerceIn(0L, elapsed + CLOCK_TOLERANCE_MS)
    }

    private fun closeSegment(positionMs: Long, nowMs: Long) {
        if (segmentStartPositionMs == NO_SEGMENT) return
        bankedMs += openSegmentWorth(positionMs, nowMs)
        playedMs = bankedMs
        segmentStartPositionMs = NO_SEGMENT
    }

    private fun reachedThreshold(durationMs: Long): Boolean {
        if (counted || durationMs <= 0L || playedMs * 10L <= durationMs * 9L) return false
        counted = true
        return true
    }

    private companion object {
        const val NO_SEGMENT = -1L

        /** The audio clock and the system clock drift apart by a few milliseconds per second. */
        const val CLOCK_TOLERANCE_MS = 750L
    }
}
