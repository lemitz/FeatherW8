package com.featherw8.player.ui.artwork

import com.featherw8.player.R
import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.featherw8.player.data.MusicTrack
import com.featherw8.player.data.CoverDiskCache
import kotlinx.coroutines.delay
import com.featherw8.player.ui.theme.ArtworkPlaceholder

@Composable
internal fun TrackArtwork(track: MusicTrack, size: Dp, shape: RoundedCornerShape, large: Boolean = false) {
    TrackArtwork(track, Modifier.size(size), shape, large)
}

@Composable
internal fun TrackArtwork(track: MusicTrack, modifier: Modifier, shape: RoundedCornerShape, large: Boolean = false) {
    val context = LocalContext.current
    val targetPixels = if (large) 720 else CoverDiskCache.THUMB_PIXELS
    val customArtworkUri = CustomArtworkStore.get(track.id)
    val defaultArtworkUri = remember(track.id, track.albumId, track.coverUri) { track.artworkUri }
    val artworkUri = customArtworkUri ?: defaultArtworkUri
    val cacheKey = artworkUri?.let { "$it@$targetPixels" }
    var bitmap by remember(cacheKey) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(cacheKey) {
        if (artworkUri == null || cacheKey == null) {
            bitmap = null
        } else {
            // The full screen cover is a few megabytes: uploading it mid-animation
            // costs a frame, so it is loaded once the screen has settled - unless it
            // is already in the cache (e.g. coming back from the queue page).
            if (large && ArtworkBitmapCache.get(cacheKey) == null) delay(180L)
            bitmap = loadArtwork(context.applicationContext, artworkUri, targetPixels, cacheKey)
        }
    }
    Box(modifier.clip(shape).background(ArtworkPlaceholder), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap!!.asImageBitmap(),
                contentDescription = "Album cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (large) {
            androidx.compose.foundation.Image(
                painterResource(R.drawable.featherw8_icon),
                contentDescription = "FeatherW8 artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Filled.LibraryMusic, contentDescription = null, tint = Color.White.copy(alpha = .72f), modifier = Modifier.size(if (large) 68.dp else 21.dp))
        }
    }
}
