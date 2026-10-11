package com.featherw8.player.data

import android.content.Context

/**
 * Track details that are expensive to read back from a file - currently the bitrate,
 * which otherwise needs a MediaMetadataRetriever (open + parse) every time a track is
 * shown. Values are remembered for good, so the cost is paid once per track, ever.
 *
 * The stored string is prefixed with "~" when the value was estimated from the file
 * size instead of read from the stream.
 */
object TrackInfoStore {
    private const val PREFS = "featherw8_track_info"
    private const val ESTIMATED_PREFIX = "~"
    private fun key(id: String) = "bitrate_$id"

    fun getBitrate(context: Context, id: String): Pair<Long, Boolean>? {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key(id), null) ?: return null
        val estimated = raw.startsWith(ESTIMATED_PREFIX)
        val bitsPerSecond = raw.removePrefix(ESTIMATED_PREFIX).toLongOrNull() ?: return null
        if (bitsPerSecond <= 0L) return null
        return bitsPerSecond to estimated
    }

    fun putBitrate(context: Context, id: String, bitsPerSecond: Long, estimated: Boolean) {
        if (bitsPerSecond <= 0L) return
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(key(id), "${if (estimated) ESTIMATED_PREFIX else ""}$bitsPerSecond")
            .apply()
    }
}
