package com.featherw8.player.data

/**
 * Counts only continuously advanced playback time. Seek discontinuities reset the
 * sampling anchor, so a seek cannot contribute the time jumped over.
 */
class ActualPlayCounter {
    var playedMs: Long = 0L
        private set

    private var lastPositionMs: Long? = null
    private var counted = false

    fun onSeek() {
        lastPositionMs = null
    }

    fun sample(isPlaying: Boolean, positionMs: Long, durationMs: Long): Boolean {
        if (!isPlaying) {
            lastPositionMs = null
            return false
        }

        val previous = lastPositionMs
        if (previous != null) {
            val delta = positionMs - previous
            // The service samples frequently. A large discontinuity is never credited.
            if (delta in 1L..1_500L) playedMs += delta
        }
        lastPositionMs = positionMs

        if (!counted && durationMs > 0L && playedMs * 10L > durationMs * 9L) {
            counted = true
            return true
        }
        return false
    }
}
