package com.featherw8.player.audio

import android.content.ContentResolver
import android.net.Uri
import androidx.media3.common.DataReader
import androidx.media3.extractor.DefaultExtractorInput

/**
 * Reads a DSF / DSDIFF duration straight from the container header.
 *
 * MediaStore does not index DSD files as audio, so it reports no duration for them. The header
 * is tiny (a few dozen bytes) and is parsed by the same code the playback extractor uses.
 */
internal object DsdDuration {
    /** Duration in milliseconds, or 0 when the file cannot be opened or is not a supported DSD file. */
    fun readDurationMs(resolver: ContentResolver, uri: Uri, sizeBytes: Long): Long = runCatching {
        resolver.openInputStream(uri)?.use { stream ->
            val reader = DataReader { buffer, offset, length -> stream.read(buffer, offset, length) }
            val length = if (sizeBytes > 0L) sizeBytes else -1L
            DsdFileInfo.parse(DefaultExtractorInput(reader, 0L, length)).durationUs / 1_000L
        } ?: 0L
    }.getOrDefault(0L)
}
