package com.featherw8.player.ui.player

import android.content.ComponentName
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.featherw8.player.playback.PlaybackService
import java.util.concurrent.Executor

@Composable
internal fun rememberMediaController(): MediaController? {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    val future = remember(context) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        MediaController.Builder(context, token).buildAsync()
    }
    DisposableEffect(future) {
        val mainHandler = Handler(Looper.getMainLooper())
        val mainExecutor = Executor { command -> mainHandler.post(command) }
        future.addListener({ controller = runCatching { future.get() }.getOrNull() }, mainExecutor)
        onDispose { MediaController.releaseFuture(future) }
    }
    return controller
}
