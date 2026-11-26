package com.featherw8.player.ui.model

/**
 * Filters and sorts tracks for the Library. Selectors keep this helper independent of Android-backed
 * [com.featherw8.player.data.MusicTrack], so the transformation can be tested on the JVM.
 */
internal fun <T> filterAndSortTracks(
    tracks: List<T>,
    minimumSizeBytes: Long,
    minimumDurationSeconds: Float,
    query: String,
    sortMode: SortMode,
    playCounts: Map<String, Int>,
    id: (T) -> String,
    title: (T) -> String,
    artist: (T) -> String,
    album: (T) -> String,
    mimeType: (T) -> String,
    sizeBytes: (T) -> Long,
    durationMs: (T) -> Long,
    dateAddedSeconds: (T) -> Long
): List<T> {
    val minimumWholeSeconds = minimumDurationSeconds.toLong()
    val filtered = tracks.filter { track ->
        sizeBytes(track) >= minimumSizeBytes &&
            // A track whose duration is unknown (0) is never hidden by this filter.
            (minimumDurationSeconds <= 0f || durationMs(track) <= 0L ||
                durationMs(track) / 1_000L >= minimumWholeSeconds) &&
            (query.isBlank() ||
                title(track).contains(query, ignoreCase = true) ||
                artist(track).contains(query, ignoreCase = true) ||
                album(track).contains(query, ignoreCase = true) ||
                mimeType(track).contains(query, ignoreCase = true))
    }
    return when (sortMode) {
        SortMode.TITLE -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, title))
        SortMode.ADDED -> filtered.sortedByDescending(dateAddedSeconds)
        SortMode.PLAYS -> filtered.sortedByDescending { playCounts[id(it)] ?: 0 }
    }
}
