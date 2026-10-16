package com.featherw8.player.audio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.featherw8.player.data.MusicTrack
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.Locale

/** Everything the UI needs to show a DSD file like any other track. */
internal class DsdTrackInfo(
    val channels: Int,
    val sampleRate: Int,
    val durationMs: Long,
    val title: String?,
    val artist: String?,
    val album: String?,
    val coverFile: File?
) {
    val bitsPerSecond: Long get() = sampleRate.toLong() * channels
    val rateLabel: String get() = dsdRateLabel(sampleRate)
    val channelLabel: String
        get() = when (channels) {
            1 -> "mono"
            2 -> "stereo"
            else -> "$channels ch"
        }
}

/**
 * Remembers what was read from DSD files. Reading a file's header and tag costs an open and some
 * I/O, and the cover has to be decoded and shrunk, so each file is processed once and the result
 * is kept in cacheDir (the system may drop it; the next scan simply rebuilds it).
 */
internal object DsdLibrary {
    private const val DIRECTORY = "dsd-meta"
    private const val VERSION = 1
    private const val COVER_MAX_PIXELS = 1024
    private const val COVER_KEEP_BYTES = 600 * 1024
    private const val COVER_QUALITY = 88

    /**
     * [stableKey] must change when the file changes (callers use "id:size"). Returns null for a file
     * that is not a readable DSD container; that outcome is remembered as well.
     */
    fun info(context: Context, uri: Uri, stableKey: String): DsdTrackInfo? {
        val directory = File(context.cacheDir, DIRECTORY)
        val name = sha1(stableKey)
        val jsonFile = File(directory, "$name.json")
        val coverFile = File(directory, "$name.jpg")

        readCached(jsonFile, coverFile)?.let { cached -> return cached.info }

        val metadata = DsdMetadataReader.read(context.contentResolver, uri)
        if (metadata == null) {
            writeJson(directory, jsonFile, JSONObject().put("v", VERSION).put("ok", false))
            return null
        }
        var storedCover: File? = null
        metadata.coverBytes?.let { raw ->
            encodeCover(raw)?.let { jpeg ->
                runCatching {
                    directory.mkdirs()
                    coverFile.writeBytes(jpeg)
                    storedCover = coverFile
                }
            }
        }
        val json = JSONObject()
            .put("v", VERSION)
            .put("ok", true)
            .put("channels", metadata.channels)
            .put("rate", metadata.sampleRate)
            .put("durationMs", metadata.durationMs)
            .put("title", metadata.title ?: "")
            .put("artist", metadata.artist ?: "")
            .put("album", metadata.album ?: "")
            .put("cover", storedCover != null)
        writeJson(directory, jsonFile, json)
        return DsdTrackInfo(
            metadata.channels, metadata.sampleRate, metadata.durationMs,
            metadata.title, metadata.artist, metadata.album, storedCover
        )
    }

    /** A cache hit; [info] is null for a remembered "not a readable DSD file". */
    private class Cached(val info: DsdTrackInfo?)

    /** Null means "not cached" (missing, unreadable or written by another version). */
    private fun readCached(jsonFile: File, coverFile: File): Cached? {
        if (!jsonFile.exists()) return null
        return runCatching {
            val json = JSONObject(jsonFile.readText())
            when {
                json.optInt("v") != VERSION -> null
                !json.optBoolean("ok") -> Cached(null)
                else -> Cached(
                    DsdTrackInfo(
                        channels = json.getInt("channels"),
                        sampleRate = json.getInt("rate"),
                        durationMs = json.getLong("durationMs"),
                        title = json.optString("title").takeIf { it.isNotBlank() },
                        artist = json.optString("artist").takeIf { it.isNotBlank() },
                        album = json.optString("album").takeIf { it.isNotBlank() },
                        coverFile = coverFile.takeIf { json.optBoolean("cover") && it.exists() }
                    )
                )
            }
        }.getOrNull()
    }

    private fun writeJson(directory: File, target: File, json: JSONObject) {
        runCatching {
            directory.mkdirs()
            // Write beside the target and rename so a killed process never leaves a half-written file.
            val temporary = File(directory, "${target.name}.tmp")
            temporary.writeText(json.toString())
            if (!temporary.renameTo(target)) temporary.delete()
        }
    }

    /** Shrinks a tag picture so a library of large covers does not fill the cache. */
    private fun encodeCover(raw: ByteArray): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val largest = maxOf(bounds.outWidth, bounds.outHeight)
        if (largest <= COVER_MAX_PIXELS && raw.size <= COVER_KEEP_BYTES) return raw

        var sample = 1
        while (largest / (sample * 2) >= COVER_MAX_PIXELS) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.size, options) ?: return null
        return try {
            ByteArrayOutputStream().also { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, COVER_QUALITY, out) }
                .toByteArray()
        } finally {
            bitmap.recycle()
        }
    }

    private fun sha1(value: String): String = MessageDigest.getInstance("SHA-1")
        .digest(value.toByteArray())
        .joinToString("") { String.format(Locale.ROOT, "%02x", it.toInt() and 0xFF) }
}

/** Replaces what MediaStore guessed about a DSD file with what the file itself says. */
internal fun MusicTrack.withDsdInfo(info: DsdTrackInfo?): MusicTrack {
    if (info == null) return this
    return copy(
        title = info.title ?: title,
        artist = info.artist ?: "Unknown Artist",
        album = info.album ?: "Unknown Album",
        durationMs = info.durationMs.takeIf { it > 0L } ?: durationMs,
        // MediaStore's album id is derived from the folder name; group by the real tags instead.
        albumId = -1L,
        coverUri = info.coverFile?.let { Uri.fromFile(it) },
        bitrateBps = info.bitsPerSecond,
        formatDetail = "${info.rateLabel} · ${info.channelLabel}"
    )
}
