package com.featherw8.player.data

import android.net.Uri

/** A row from the primary (internal shared-storage) MediaStore volume. */
data class MusicTrack(
    val id: String,
    val uri: Uri,
    val displayName: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSeconds: Long,
    val albumId: Long,
    val mimeType: String,
    val relativePath: String = ""
) {
    val extension: String
        get() = displayName.substringAfterLast('.', "").lowercase()

    val isDsd: Boolean
        get() = extension in setOf("dsf", "dff", "dsd")

    /** A conservative local-file estimate, used only to populate the Hi-Res smart list. */
    val isLikelyHiRes: Boolean
        get() = isDsd || (durationMs > 0L && sizeBytes * 1_000L / durationMs >= 200_000L)
}
