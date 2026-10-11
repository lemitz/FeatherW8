package com.featherw8.player.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.security.MessageDigest

/**
 * A small JPEG cache of already decoded covers.
 *
 * The in-memory cache dies with the process, so without this every launch asks
 * MediaProvider for the same covers again - an IPC per cover, sometimes an extraction
 * from the audio file. Storing our own thumbnails turns that into a plain file read.
 * It lives in cacheDir, so the system is free to evict it when storage runs low.
 */
object CoverDiskCache {
    private const val DIRECTORY = "covers"
    private const val MAX_TOTAL_BYTES = 96L * 1024 * 1024
    private const val QUALITY = 85
    private const val TRIM_EVERY_WRITES = 25

    @Volatile
    private var writesSinceTrim = 0

    fun contains(context: Context, key: String): Boolean = fileFor(context, key).exists()

    fun get(context: Context, key: String): Bitmap? {
        val file = fileFor(context, key)
        if (!file.exists()) return null
        return runCatching {
            BitmapFactory.decodeFile(file.absolutePath)?.also {
                runCatching { file.setLastModified(System.currentTimeMillis()) }
            }
        }.getOrNull()
    }

    fun put(context: Context, key: String, bitmap: Bitmap) {
        runCatching {
            val directory = File(context.cacheDir, DIRECTORY)
            if (!directory.exists() && !directory.mkdirs()) return
            val file = fileFor(context, key)
            file.outputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, stream)
            }
            if (++writesSinceTrim >= TRIM_EVERY_WRITES) {
                writesSinceTrim = 0
                trim(context)
            }
        }
    }

    private fun fileFor(context: Context, key: String): File =
        File(File(context.cacheDir, DIRECTORY), "${sha1(key)}.jpg")

    private fun sha1(value: String): String = MessageDigest.getInstance("SHA-1")
        .digest(value.toByteArray())
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }

    /** Drops the least recently touched covers until the cache is back under the cap. */
    private fun trim(context: Context) {
        val files = File(context.cacheDir, DIRECTORY).listFiles() ?: return
        var total = files.sumOf { it.length() }
        if (total <= MAX_TOTAL_BYTES) return
        for (file in files.sortedBy { it.lastModified() }) {
            if (total <= MAX_TOTAL_BYTES) break
            val size = file.length()
            if (file.delete()) total -= size
        }
    }
}
