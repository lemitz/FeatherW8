package com.featherw8.player.ui.artwork

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import com.featherw8.player.data.MusicTrack
import com.featherw8.player.data.CoverDiskCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Generates thumbnails for the library in the background, one cover at a time, so a
 * later launch - and the parts of the list you have not reached yet - is served from
 * [CoverDiskCache] instead of going through MediaProvider. It pauses while the list is
 * scrolling so it never competes with the frames the user is looking at.
 */
internal object CoverWarmer {
    private val warmSemaphore = Semaphore(1)
    private var job: Job? = null

    fun start(scope: CoroutineScope, context: Context, tracks: List<MusicTrack>, isScrolling: () -> Boolean) {
        job?.cancel()
        job = scope.launch(Dispatchers.IO) {
            warmSemaphore.withPermit {
                for (track in tracks) {
                    if (isScrolling()) {
                        delay(500L)
                        continue
                    }
                    val uri = track.artworkUri ?: continue
                    val key = CoverDiskCache.key(uri, CoverDiskCache.THUMB_PIXELS)
                    if (CoverDiskCache.contains(context, key)) continue
                    val cover = runCatching {
                        decodeSampledArtwork(context, uri, CoverDiskCache.THUMB_PIXELS)
                    }.getOrNull()
                    if (cover != null) CoverDiskCache.put(context, key, cover)
                    delay(12L)
                }
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}
