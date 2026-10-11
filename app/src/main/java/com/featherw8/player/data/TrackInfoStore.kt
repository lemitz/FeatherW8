package com.featherw8.player.data

import android.content.Context

/**
 * Track details that are expensive to read back from a file - currently the bitrate,
 * which otherwise needs a MediaMetadataRetriever (open + parse) every time a track is
 * shown. Values are remembered for good, so the cost is paid once per track, ever.
 *
 * A stored value is `"[~]<bitsPerSecond>|<sizeBytes>"`: a "~" prefix marks a value that
 * was estimated from the file size instead of read from the stream, and the size it was
 * measured from is kept so a replaced or re-encoded track is measured again.
 */
object TrackInfoStore {
    private const val PREFS = "featherw8_track_info"
    private const val ESTIMATED_PREFIX = "~"
    private const val SEPARATOR = "|"
    private const val UNKNOWN_SIZE = -1L

    private fun key(id: String) = "bitrate_$id"

    /** Null when this track has not been measured yet; the flag is true when estimated. */
    fun getBitrate(context: Context, id: String, sizeBytes: Long): Pair<Long, Boolean>? {
        val raw = runCatching { preferences(context).getString(key(id), null) }.getOrNull() ?: return null
        val storedSize = raw.substringAfterLast(SEPARATOR, "").toLongOrNull() ?: UNKNOWN_SIZE
        if (storedSize != UNKNOWN_SIZE && storedSize != sizeBytes) return null
        val value = raw.substringBeforeLast(SEPARATOR, raw)
        val estimated = value.startsWith(ESTIMATED_PREFIX)
        val bitsPerSecond = value.removePrefix(ESTIMATED_PREFIX).toLongOrNull() ?: return null
        if (bitsPerSecond < 0L) return null
        return bitsPerSecond to estimated
    }

    /**
     * Zero [bitsPerSecond] means "measured, but the file reports none" - it is stored as
     * well so tracks that cannot answer (DSD among them) are not re-opened every visit.
     */
    fun putBitrate(context: Context, id: String, bitsPerSecond: Long, estimated: Boolean, sizeBytes: Long) {
        if (bitsPerSecond < 0L) return
        val value = "${if (estimated) ESTIMATED_PREFIX else ""}$bitsPerSecond$SEPARATOR$sizeBytes"
        runCatching { preferences(context).edit().putString(key(id), value).apply() }
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
