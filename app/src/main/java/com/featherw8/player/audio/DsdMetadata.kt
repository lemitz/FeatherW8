package com.featherw8.player.audio

import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.media3.common.DataReader
import androidx.media3.extractor.DefaultExtractorInput
import androidx.media3.extractor.metadata.id3.ApicFrame
import androidx.media3.extractor.metadata.id3.Id3Decoder
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SeekableByteChannel
import kotlin.math.min

/** What the container of a DSF / DSDIFF file says about itself (cover bytes are still unprocessed). */
internal class DsdMetadata(
    val channels: Int,
    val sampleRate: Int,
    val durationMs: Long,
    val title: String?,
    val artist: String?,
    val album: String?,
    val coverBytes: ByteArray?
) {
    /** Raw DSD data rate, e.g. 11 289 600 for stereo DSD128. */
    val bitsPerSecond: Long get() = sampleRate.toLong() * channels
    val rateLabel: String get() = dsdRateLabel(sampleRate)
}

/** "DSD64", "DSD128", ... from the 1-bit sample rate (44.1 kHz and 48 kHz families). */
internal fun dsdRateLabel(sampleRate: Int): String = when {
    sampleRate <= 0 -> "DSD"
    sampleRate % 44_100 == 0 -> "DSD${sampleRate / 44_100}"
    sampleRate % 48_000 == 0 -> "DSD${sampleRate / 48_000}"
    else -> "DSD ${"%.2f".format(sampleRate / 1_000_000.0)} MHz"
}

/**
 * Reads the stream parameters and the embedded tags of a DSF or DSDIFF file.
 *
 * MediaStore does not understand DSD tags (it reports the folder as the album and nothing as the
 * duration), so the container is read directly: DSF keeps an ID3v2 tag at the offset stored in its
 * header, DSDIFF has an `ID3 ` chunk and, as a fallback, `DIIN` title/artist chunks.
 */
internal object DsdMetadataReader {
    private const val MAX_TAG_BYTES = 32L * 1024L * 1024L

    private class Tags(
        var title: String? = null,
        var artist: String? = null,
        var album: String? = null,
        var cover: ByteArray? = null
    )

    fun read(resolver: ContentResolver, uri: Uri): DsdMetadata? = runCatching {
        val descriptor = resolver.openFileDescriptor(uri, "r") ?: return null
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream -> parse(stream.channel) }
    }.getOrNull()

    /**
     * Throws when the file is not a supported DSD container. A damaged or unsupported tag never fails
     * the whole read (the stream parameters are still valid) unless [strictTags] is set, which the
     * tests use so that a tag problem is reported instead of silently producing no tags.
     */
    fun parse(channel: SeekableByteChannel, strictTags: Boolean = false): DsdMetadata {
        val fileSize = channel.size()
        channel.position(0L)
        val reader = DataReader { buffer, offset, length ->
            if (length == 0) 0 else channel.read(ByteBuffer.wrap(buffer, offset, length))
        }
        val info = DsdFileInfo.parse(DefaultExtractorInput(reader, 0L, fileSize))
        val readTags = {
            when (info.container) {
                Container.DSF -> readDsfTags(channel, fileSize)
                Container.DSDIFF -> readDsdiffTags(channel, fileSize)
            }
        }
        val tags = if (strictTags) readTags() else runCatching(readTags).getOrDefault(Tags())
        return DsdMetadata(
            channels = info.channels,
            sampleRate = info.sampleRate,
            durationMs = info.durationUs / 1_000L,
            title = tags.title,
            artist = tags.artist,
            album = tags.album,
            coverBytes = tags.cover
        )
    }

    private fun readDsfTags(channel: SeekableByteChannel, fileSize: Long): Tags {
        // DSD chunk: "DSD " (4), chunk size (8), total size (8), pointer to the metadata chunk (8).
        val pointer = readLong(channel, 20L, bigEndian = false)
        if (pointer <= 0L || pointer >= fileSize) return Tags()
        return decodeId3(readBytes(channel, pointer, min(fileSize - pointer, MAX_TAG_BYTES)))
    }

    private fun readDsdiffTags(channel: SeekableByteChannel, fileSize: Long): Tags {
        val formEnd = min(12L + readLong(channel, 4L, bigEndian = true), fileSize)
        val fallback = Tags()
        var id3: Tags? = null
        var position = 16L // "FRM8" (4), size (8), form type (4)
        while (position + 12L <= formEnd) {
            val header = readBytes(channel, position, 12L)
            if (header.size < 12) break
            val id = String(header, 0, 4, Charsets.US_ASCII)
            val size = ByteBuffer.wrap(header, 4, 8).order(ByteOrder.BIG_ENDIAN).long
            if (size < 0L) break
            val contentStart = position + 12L
            when (id) {
                "ID3 " -> id3 = decodeId3(readBytes(channel, contentStart, min(size, MAX_TAG_BYTES)))
                "DIIN" -> readDiin(channel, contentStart, min(contentStart + size, formEnd), fallback)
            }
            position = contentStart + size + (size and 1L)
        }
        val tags = id3 ?: return fallback
        return Tags(
            title = tags.title ?: fallback.title,
            artist = tags.artist ?: fallback.artist,
            album = tags.album,
            cover = tags.cover
        )
    }

    /** DIIN holds DITI (title) and DIAR (artist) sub-chunks: a 4-byte length followed by the text. */
    private fun readDiin(channel: SeekableByteChannel, start: Long, end: Long, into: Tags) {
        var position = start
        while (position + 12L <= end) {
            val header = readBytes(channel, position, 12L)
            if (header.size < 12) return
            val id = String(header, 0, 4, Charsets.US_ASCII)
            val size = ByteBuffer.wrap(header, 4, 8).order(ByteOrder.BIG_ENDIAN).long
            if (size < 0L) return
            val contentStart = position + 12L
            if ((id == "DITI" || id == "DIAR") && size in 4L..65_536L) {
                val content = readBytes(channel, contentStart, size)
                if (content.size >= 4) {
                    val length = ByteBuffer.wrap(content, 0, 4).order(ByteOrder.BIG_ENDIAN).int
                    val textLength = length.coerceIn(0, content.size - 4)
                    val text = String(content, 4, textLength, Charsets.UTF_8).trim().takeIf { it.isNotEmpty() }
                    if (id == "DITI") into.title = text else into.artist = text
                }
            }
            position = contentStart + size + (size and 1L)
        }
    }

    private fun decodeId3(bytes: ByteArray): Tags {
        val tags = Tags()
        if (bytes.size < 10) return tags
        val metadata = Id3Decoder().decode(bytes, bytes.size) ?: return tags
        for (index in 0 until metadata.length()) {
            when (val entry = metadata[index]) {
                is TextInformationFrame -> {
                    val text = entry.values.map { it.trim() }.filter { it.isNotEmpty() }
                        .joinToString(", ").takeIf { it.isNotEmpty() }
                    when (entry.id) {
                        "TIT2" -> tags.title = text
                        "TPE1" -> tags.artist = text
                        "TPE2" -> if (tags.artist == null) tags.artist = text
                        "TALB" -> tags.album = text
                    }
                }
                is ApicFrame -> {
                    // Prefer the front cover (type 3) over any other picture.
                    if (tags.cover == null || entry.pictureType == 3) tags.cover = entry.pictureData
                }
            }
        }
        return tags
    }

    private fun readBytes(channel: SeekableByteChannel, position: Long, length: Long): ByteArray {
        if (length <= 0L) return ByteArray(0)
        val buffer = ByteBuffer.allocate(length.toInt())
        channel.position(position)
        while (buffer.hasRemaining()) {
            if (channel.read(buffer) < 0) break
        }
        return buffer.array().copyOf(buffer.position())
    }

    private fun readLong(channel: SeekableByteChannel, position: Long, bigEndian: Boolean): Long {
        val bytes = readBytes(channel, position, 8L)
        if (bytes.size < 8) return 0L
        return ByteBuffer.wrap(bytes).order(if (bigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN).long
    }
}
