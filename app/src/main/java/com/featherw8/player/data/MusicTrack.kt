package com.featherw8.player.data

import android.net.Uri
import android.os.Build
import android.provider.MediaStore

private val DsdExtensions = setOf("dsf", "dff", "dsd")

/**
 * A row from the primary (internal shared-storage) MediaStore volume.
 *
 * Everything a list row or the player shows repeatedly is derived once: either lazily
 * on first use, or eagerly through [precompute], which the scanner calls while it is
 * already off the main thread. Deriving these in a composable would repeat the work on
 * every recomposition while scrolling.
 */
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
    val relativePath: String = "",
    /** A cover that was extracted from the file itself (DSD tags); wins over the media index. */
    val coverUri: Uri? = null,
    /** Exact bit rate when the file format states it (DSD); 0 when it has to be measured. */
    val bitrateBps: Long = 0L,
    /** Extra format details shown next to [formatLabel], e.g. "DSD128 · stereo". */
    val formatDetail: String = ""
) {
    val extension: String by lazy { displayName.substringAfterLast('.', "").lowercase() }

    val isDsd: Boolean by lazy { extension in DsdExtensions }

    /** A conservative local-file estimate, used only to populate the Hi-Res smart list. */
    val isLikelyHiRes: Boolean by lazy {
        isDsd || (durationMs > 0L && sizeBytes * 1_000L / durationMs >= 200_000L)
    }

    val artworkUri: Uri? by lazy {
        if (coverUri != null) {
            coverUri
        } else if (albumId < 0L) {
            null
        } else {
            val volume = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            } else {
                "external"
            }
            Uri.parse("content://media/$volume/audio/albumart/$albumId")
        }
    }

    val formatLabel: String by lazy {
        extension.takeIf { it.isNotBlank() }?.uppercase()
            ?: mimeType.substringAfter('/').takeIf { it.isNotBlank() }?.uppercase()
            ?: "AUDIO"
    }

    val subtitle: String by lazy { "$artist · ${formatDuration(durationMs)}" }

    val sizeLabel: String by lazy { formatSize(sizeBytes) }

    /** Touches every derived value; called by the scanner on its background thread. */
    fun precompute() {
        extension
        isDsd
        isLikelyHiRes
        artworkUri
        formatLabel
        subtitle
        sizeLabel
    }
}
