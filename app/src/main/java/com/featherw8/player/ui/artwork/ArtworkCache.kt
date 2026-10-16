package com.featherw8.player.ui.artwork

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import android.util.LruCache
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import com.featherw8.player.data.CoverDiskCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal val ArtworkBitmapCache = object : LruCache<String, Bitmap>(24 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = (value.allocationByteCount / 1024).coerceAtLeast(1)
}

/** Only a few covers are decoded at a time; see TrackArtwork for the queueing. */
internal val ArtworkDecodeSemaphore = Semaphore(3)

/**
 * Loads a cover for the composable that is currently on screen. The work runs on
 * Dispatchers.IO behind [ArtworkDecodeSemaphore] and is cancelled as soon as the row
 * scrolls away, so a fast fling never leaves hundreds of stale decodes in flight.
 */
internal suspend fun loadArtwork(context: Context, uri: Uri, targetPixels: Int, cacheKey: String): Bitmap? {
    ArtworkBitmapCache.get(cacheKey)?.let { return it }
    val onDisk = CoverDiskCache.isCacheable(targetPixels)
    return withContext(Dispatchers.IO) {
        ArtworkBitmapCache.get(cacheKey) ?: ArtworkDecodeSemaphore.withPermit {
            ArtworkBitmapCache.get(cacheKey)
                ?: (if (onDisk) {
                    CoverDiskCache.get(context, cacheKey)?.also { cover -> ArtworkBitmapCache.put(cacheKey, cover) }
                } else {
                    null
                })
                ?: runCatching {
                    decodeSampledArtwork(context, uri, targetPixels)
                }.getOrNull()?.also { bitmap ->
                    ArtworkBitmapCache.put(cacheKey, bitmap)
                    if (onDisk) CoverDiskCache.put(context, cacheKey, bitmap)
                }
        }
    }
}

internal fun decodeSampledArtwork(context: Context, uri: Uri, targetPixels: Int): Bitmap? {
    // On Q+ MediaProvider keeps its own thumbnail cache and returns a bitmap that is
    // already scaled down: one IPC instead of opening the audio file twice.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching {
            context.contentResolver.loadThumbnail(uri, Size(targetPixels, targetPixels), null)
        }.getOrNull()?.let { return it }
    }
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val boundsStream = resolver.openInputStream(uri) ?: return null
    boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val largestDimension = maxOf(bounds.outWidth, bounds.outHeight)
    val ratio = largestDimension.toDouble() / targetPixels.coerceAtLeast(1)
    var sampleSize = 1
    while (sampleSize * 2 <= ratio) sampleSize *= 2
    if (ratio / sampleSize > 1.414) sampleSize *= 2

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}
