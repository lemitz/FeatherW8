package com.featherw8.player.playback

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.datasource.DataSourceBitmapLoader
import com.featherw8.player.ui.artwork.loadArtwork
import com.google.common.util.concurrent.AsyncFunction
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Loads the cover that the notification shade, the lock screen and the media widget show.
 *
 * The session only carries a link to the cover (`content://media/.../audio/albumart/<id>`, a user-chosen
 * image, or a file extracted from a DSD tag). Media3's own loader opens that link as a plain stream, which
 * MediaProvider refuses for album-art links on recent Android versions, so the card ended up without a
 * cover while the app itself, which asks MediaProvider for a thumbnail, showed it fine. This loader uses
 * the app's own cover loading (and its caches) first and falls back to Media3's loader.
 */
internal class ArtworkBitmapLoader(private val context: Context) : BitmapLoader {
    private val appContext = context.applicationContext
    private val fallback = DataSourceBitmapLoader.Builder(appContext).build()
    private val executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

    override fun supportsMimeType(mimeType: String): Boolean = fallback.supportsMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = fallback.decodeBitmap(data)

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val own = executor.submit(Callable<Bitmap?> {
            runBlocking { loadArtwork(appContext, uri, SHELL_COVER_PIXELS, "$uri@$SHELL_COVER_PIXELS") }
        })
        return Futures.transformAsync(
            own,
            AsyncFunction<Bitmap?, Bitmap> { bitmap ->
                if (bitmap != null) Futures.immediateFuture(bitmap) else fallback.loadBitmap(uri)
            },
            MoreExecutors.directExecutor()
        )
    }

    private companion object {
        /** Large enough for the media widget and the lock screen; still a cacheable thumbnail size. */
        const val SHELL_COVER_PIXELS = 512
    }
}
