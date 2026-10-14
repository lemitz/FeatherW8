package com.featherw8.player.data

/** Shared between the library scan and the UI so list rows never format on the fly. */
fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "—:—"
    val totalSeconds = durationMs / 1_000L
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}

fun formatSize(sizeBytes: Long): String = when {
    sizeBytes <= 0L -> "Unknown size"
    sizeBytes >= 1024L * 1024L -> {
        val tenths = (sizeBytes * 10L + 524_288L) / 1_048_576L
        "${tenths / 10L}.${tenths % 10L} MB"
    }
    else -> "${(sizeBytes + 512L) / 1024L} KB"
}
