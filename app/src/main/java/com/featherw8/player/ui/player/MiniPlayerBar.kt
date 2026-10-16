package com.featherw8.player.ui.player

import android.util.Size
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.session.MediaController
import com.featherw8.player.data.MusicTrack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.featherw8.player.ui.artwork.TrackArtwork
import com.featherw8.player.ui.theme.CardBlack
import com.featherw8.player.ui.theme.FeatherRed
import com.featherw8.player.ui.theme.MutedText

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MiniPlayerBar(
    track: MusicTrack,
    playing: Boolean,
    durationMs: Long,
    initialPositionMs: Long,
    controller: MediaController?,
    isVisible: Boolean,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onDismiss: () -> Unit,
    onDragActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(16.dp)
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 40.dp.toPx() }
    val fallbackHeightPx = with(density) { 64.dp.toPx() }
    val overscrollPx = with(density) { 48.dp.toPx() }
    val scope = rememberCoroutineScope()
    val accentColor = FeatherRed
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val latestOnDragActiveChange by rememberUpdatedState(onDragActiveChange)
    var swipeOffsetPx by remember(track.id) { mutableFloatStateOf(0f) }
    var dismissing by remember(track.id) { mutableStateOf(false) }
    var settling by remember(track.id) { mutableStateOf(false) }
    var positionMs by remember(track.id) { mutableLongStateOf(initialPositionMs) }
    var cardHeightPx by remember { mutableFloatStateOf(0f) }
    // The card's bottom edge sits right on top of the bottom navigation bar, so it is
    // fully hidden (and fully transparent) once it has travelled its own height.
    val hideDistancePx = if (cardHeightPx > 0f) cardHeightPx else fallbackHeightPx
    val maxDragPx = hideDistancePx + overscrollPx

    LaunchedEffect(track.id, playing, durationMs, initialPositionMs, controller, isVisible) {
        positionMs = controller?.currentPosition?.coerceAtLeast(0L) ?: initialPositionMs
        if (isVisible && playing && controller != null) {
            while (true) {
                delay(250L)
                positionMs = controller.currentPosition.coerceAtLeast(0L)
            }
        }
    }

    fun springBack() {
        if (swipeOffsetPx <= 0f) {
            swipeOffsetPx = 0f
            settling = false
            latestOnDragActiveChange(false)
            return
        }
        settling = true
        val start = swipeOffsetPx
        scope.launch {
            animateSwipeOffset(start, 0f) { swipeOffsetPx = it }
            swipeOffsetPx = 0f
            settling = false
            latestOnDragActiveChange(false)
        }
    }

    Box(
        modifier = modifier.fillMaxWidth()
            .onSizeChanged { cardHeightPx = it.height.toFloat() }
            .interactiveVerticalDrag(
                enabled = isVisible && !dismissing && !settling,
                onStart = {
                    if (dismissing || settling) false else {
                        swipeOffsetPx = 0f
                        latestOnDragActiveChange(true)
                        true
                    }
                },
                onDrag = { deltaY ->
                    swipeOffsetPx = (swipeOffsetPx + deltaY).coerceIn(0f, maxDragPx)
                },
                onEnd = {
                    if (swipeOffsetPx >= dismissThresholdPx) {
                        dismissing = true
                        settling = true
                        val start = swipeOffsetPx
                        val target = maxOf(start, hideDistancePx)
                        scope.launch {
                            animateDismissOffset(start, target, durationMillis = 130) { swipeOffsetPx = it }
                            latestOnDragActiveChange(false)
                            latestOnDismiss()
                        }
                    } else {
                        springBack()
                    }
                },
                onCancel = { springBack() }
            )
            .then(if (isVisible) Modifier.systemGestureExclusion() else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .graphicsLayer {
                translationY = swipeOffsetPx
                alpha = 1f - (swipeOffsetPx / hideDistancePx).coerceIn(0f, 1f)
            }
            .clip(cardShape)
            .background(CardBlack)
            .border(1.25.dp, accentColor, cardShape)
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackArtwork(track, 40.dp, RoundedCornerShape(10.dp))
            Column(Modifier.weight(1f).padding(start = 9.dp)) {
                Text(
                    track.title,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, repeatDelayMillis = 0, initialDelayMillis = 0, velocity = 28.dp)
                )
                Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MutedText, fontSize = 9.sp, fontFamily = FontFamily.SansSerif, modifier = Modifier.padding(top = 2.dp))
            }
            IconButton(onClick = onPlayPause, modifier = Modifier.size(38.dp)) {
                Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (playing) "Pause" else "Play", modifier = Modifier.size(19.dp))
            }
        }
        Canvas(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(2.dp)) {
            drawRect(Color.White.copy(alpha = .08f))
            if (durationMs > 0L) {
                val progress = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
                drawRect(
                    color = accentColor,
                    size = androidx.compose.ui.geometry.Size(size.width * progress, size.height)
                )
            }
        }
    }
}

@Composable
internal fun Modifier.interactiveVerticalDrag(
    enabled: Boolean,
    onStart: () -> Boolean,
    onDrag: (Float) -> Unit,
    onEnd: () -> Unit,
    onCancel: () -> Unit
): Modifier {
    if (!enabled) return this
    val latestOnStart by rememberUpdatedState(onStart)
    val latestOnDrag by rememberUpdatedState(onDrag)
    val latestOnEnd by rememberUpdatedState(onEnd)
    val latestOnCancel by rememberUpdatedState(onCancel)
    return pointerInput(enabled) {
        var accepted = false
        detectVerticalDragGestures(
            onDragStart = { accepted = latestOnStart() },
            onVerticalDrag = { change, dragAmount ->
                if (accepted) {
                    latestOnDrag(dragAmount)
                    change.consume()
                }
            },
            onDragEnd = {
                if (accepted) latestOnEnd()
                accepted = false
            },
            onDragCancel = {
                if (accepted) latestOnCancel()
                accepted = false
            }
        )
    }
}

internal suspend fun animateSwipeOffset(start: Float, target: Float, onValue: (Float) -> Unit) {
    animate(
        initialValue = start,
        targetValue = target,
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 520f)
    ) { value, _ -> onValue(value) }
}

internal suspend fun animateDismissOffset(
    start: Float,
    target: Float,
    durationMillis: Int = 170,
    onValue: (Float) -> Unit
) {
    animate(
        initialValue = start,
        targetValue = target,
        animationSpec = tween(durationMillis = durationMillis, easing = FastOutLinearInEasing)
    ) { value, _ -> onValue(value) }
}
