package com.featherw8.player.ui.player

import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.featherw8.player.data.MusicTrack
import com.featherw8.player.data.formatDuration
import com.featherw8.player.data.formatSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.featherw8.player.ui.artwork.TrackArtwork
import com.featherw8.player.ui.components.formatBitrate
import com.featherw8.player.ui.components.rememberTrackBitrate
import com.featherw8.player.ui.components.TrackOptionsDropdown
import com.featherw8.player.ui.model.PlaybackSnapshot
import com.featherw8.player.ui.model.PlayerNowPlayingPage
import com.featherw8.player.ui.model.PlayerQueuePage
import com.featherw8.player.ui.model.TrackAction
import com.featherw8.player.ui.screens.QueueScreen
import com.featherw8.player.ui.theme.AmoledBlack
import com.featherw8.player.ui.theme.CardBlack
import com.featherw8.player.ui.theme.FeatherRed
import com.featherw8.player.ui.theme.MutedText
import com.featherw8.player.ui.theme.PrimaryText

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FullPlayerScreen(
    track: MusicTrack,
    queue: List<MusicTrack>,
    currentId: String?,
    isFavorite: Boolean,
    snapshot: PlaybackSnapshot,
    controller: MediaController?,
    onClose: () -> Unit,
    onSwipeClose: () -> Unit,
    onPlayQueueTrack: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onArtworkClick: (MusicTrack) -> Unit,
    onTrackInfo: (MusicTrack) -> Unit,
    onToggleFavorite: () -> Unit,
    onTrackAction: (TrackAction) -> Unit
) {
    var modeMenu by remember { mutableStateOf(false) }
    var trackMenu by remember { mutableStateOf(false) }
    var swipeOffsetPx by remember(track.id) { mutableFloatStateOf(0f) }
    var playerHeightPx by remember(track.id) { mutableIntStateOf(0) }
    var settling by remember(track.id) { mutableStateOf(false) }
    var dismissing by remember(track.id) { mutableStateOf(false) }
    var verticalDragActive by remember(track.id) { mutableStateOf(false) }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val latestOnSwipeClose by rememberUpdatedState(onSwipeClose)
    val playerPagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val closeThresholdPx = maxOf(with(density) { 112.dp.toPx() }, playerHeightPx * .2f)
    val exitDistancePx = playerHeightPx.toFloat() + with(density) { 48.dp.toPx() }
    val bitrateBps = rememberTrackBitrate(track)
    val albumTitle = track.album.takeUnless { it.isBlank() || it.equals("Unknown Album", ignoreCase = true) }

    LaunchedEffect(track.id) {
        if (playerPagerState.currentPage != PlayerNowPlayingPage) {
            playerPagerState.animateScrollToPage(PlayerNowPlayingPage)
        }
    }
    BackHandler(enabled = playerPagerState.currentPage == PlayerQueuePage) {
        scope.launch { playerPagerState.animateScrollToPage(PlayerNowPlayingPage) }
    }

    fun springBack() {
        if (swipeOffsetPx <= 0f) {
            swipeOffsetPx = 0f
            settling = false
            verticalDragActive = false
            return
        }
        settling = true
        val start = swipeOffsetPx
        scope.launch {
            animateSwipeOffset(start, 0f) { swipeOffsetPx = it }
            swipeOffsetPx = 0f
            settling = false
            verticalDragActive = false
        }
    }

    fun dismissBySwipe() {
        if (dismissing || settling) return
        dismissing = true
        settling = true
        verticalDragActive = false
        val start = swipeOffsetPx
        scope.launch {
            animateDismissOffset(start, exitDistancePx) { swipeOffsetPx = it }
            latestOnSwipeClose()
        }
    }

    val repeatMode = snapshot.repeatMode
    val shuffled = snapshot.shuffleModeEnabled
    val repeatLabel = when {
        shuffled -> "Shuffle"
        repeatMode == Player.REPEAT_MODE_ONE -> "Repeat track"
        repeatMode == Player.REPEAT_MODE_ALL -> "Repeat all"
        else -> "Repeat off"
    }
    val repeatIcon = when {
        shuffled -> Icons.Filled.Shuffle
        repeatMode == Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
        else -> Icons.Filled.Repeat
    }
    fun cycleRepeatMode() {
        controller?.let {
            when {
                it.shuffleModeEnabled -> {
                    it.shuffleModeEnabled = false
                    it.repeatMode = Player.REPEAT_MODE_OFF
                }
                it.repeatMode == Player.REPEAT_MODE_OFF -> it.repeatMode = Player.REPEAT_MODE_ALL
                it.repeatMode == Player.REPEAT_MODE_ALL -> it.repeatMode = Player.REPEAT_MODE_ONE
                else -> {
                    it.repeatMode = Player.REPEAT_MODE_OFF
                    it.shuffleModeEnabled = true
                }
            }
        }
    }

    HorizontalPager(
        state = playerPagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = !verticalDragActive,
        beyondViewportPageCount = 0
    ) { pagerPage ->
        if (pagerPage == 0) {
            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize()
                        .graphicsLayer {
                            translationY = swipeOffsetPx
                            alpha = if (playerHeightPx > 0) 1f - .08f * (swipeOffsetPx / playerHeightPx).coerceIn(0f, 1f) else 1f
                        }
                        .onSizeChanged { playerHeightPx = it.height }
                        .interactiveVerticalDrag(
                            enabled = !dismissing && !settling,
                            onStart = {
                                if (dismissing || settling) false else {
                                    swipeOffsetPx = 0f
                                    verticalDragActive = true
                                    true
                                }
                            },
                            onDrag = { deltaY ->
                                swipeOffsetPx = (swipeOffsetPx + deltaY).coerceIn(0f, exitDistancePx)
                            },
                            onEnd = { if (swipeOffsetPx >= closeThresholdPx) dismissBySwipe() else springBack() },
                            onCancel = { springBack() }
                        )
                        .background(Brush.verticalGradient(listOf(Color(0xFF100307), AmoledBlack, AmoledBlack)))
                        .systemBarsPadding()
                        .padding(horizontal = 22.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(Modifier.fillMaxWidth().height(38.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close player") }
                        Text("NOW PLAYING", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Box {
                            IconButton(onClick = { trackMenu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Track options") }
                            TrackOptionsDropdown(expanded = trackMenu, onDismiss = { trackMenu = false }, showTrackInfo = true) { action -> trackMenu = false; onTrackAction(action) }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    TrackArtwork(
                        track,
                        Modifier.fillMaxWidth().aspectRatio(1f).clickable { onArtworkClick(track) },
                        RoundedCornerShape(26.dp),
                        large = true
                    )

                    Box(Modifier.fillMaxWidth().heightIn(min = if (albumTitle == null) 78.dp else 96.dp).padding(top = 12.dp)) {
                        Column(
                            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 62.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                track.title,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = PrimaryText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                                    .basicMarquee(iterations = Int.MAX_VALUE, repeatDelayMillis = 0, initialDelayMillis = 0, velocity = 34.dp)
                                    .clickable { onTrackInfo(track) }
                            )
                            Text(
                                track.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = MutedText,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            if (albumTitle != null) {
                                Text(
                                    albumTitle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MutedText.copy(alpha = .8f),
                                    modifier = Modifier.padding(top = 3.dp)
                                )
                            }
                        }
                        IconButton(onClick = onToggleFavorite, modifier = Modifier.align(Alignment.CenterEnd).size(46.dp)) {
                            Icon(
                                if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (isFavorite) FeatherRed else Color.White,
                                modifier = Modifier.size(27.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(23.dp))
                    PlayerProgress(track = track, snapshot = snapshot, controller = controller)

                    Spacer(Modifier.height(22.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Box {
                            Box(
                                modifier = Modifier.size(48.dp).combinedClickable(
                                    onClick = { cycleRepeatMode() },
                                    onLongClick = { modeMenu = true }
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(repeatIcon, contentDescription = "Playback mode: $repeatLabel", tint = if (repeatMode != Player.REPEAT_MODE_OFF || shuffled) FeatherRed else Color.White)
                            }
                            DropdownMenu(expanded = modeMenu, onDismissRequest = { modeMenu = false }, containerColor = CardBlack) {
                                listOf("Repeat off", "Repeat all", "Repeat track", "Shuffle").forEach { label ->
                                    DropdownMenuItem(
                                        text = { Text(label, color = if (repeatLabel == label) FeatherRed else PrimaryText) },
                                        onClick = {
                                            controller?.let {
                                                when (label) {
                                                    "Repeat off" -> { it.shuffleModeEnabled = false; it.repeatMode = Player.REPEAT_MODE_OFF }
                                                    "Repeat all" -> { it.shuffleModeEnabled = false; it.repeatMode = Player.REPEAT_MODE_ALL }
                                                    "Repeat track" -> { it.shuffleModeEnabled = false; it.repeatMode = Player.REPEAT_MODE_ONE }
                                                    else -> { it.repeatMode = Player.REPEAT_MODE_OFF; it.shuffleModeEnabled = true }
                                                }
                                            }
                                            modeMenu = false
                                        },
                                        trailingIcon = { if (repeatLabel == label) Text("✓", color = FeatherRed) }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { controller?.seekToPreviousMediaItem() }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous track", modifier = Modifier.size(27.dp))
                        }
                        Surface(
                            modifier = Modifier.size(70.dp).clickable {
                                if (snapshot.playing) controller?.pause() else controller?.play()
                            },
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(if (snapshot.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (snapshot.playing) "Pause" else "Play", modifier = Modifier.size(31.dp))
                            }
                        }
                        IconButton(onClick = { controller?.seekToNextMediaItem() }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.SkipNext, contentDescription = "Next track", modifier = Modifier.size(27.dp))
                        }
                        IconButton(
                            onClick = { scope.launch { playerPagerState.animateScrollToPage(PlayerQueuePage) } },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Open playback queue", modifier = Modifier.size(24.dp))
                        }
                    }
                    Text(
                        listOf(
                            track.formatLabel,
                            track.formatDetail.substringBefore(" · "),
                            formatSize(track.sizeBytes),
                            formatBitrate(bitrateBps)
                        ).filter { it.isNotBlank() }.joinToString("  ·  "),
                        color = MutedText,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
                if (dismissing) {
                    Box(
                        Modifier.fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            )
                    )
                }
            }
        } else {
            QueueScreen(
                queue = queue,
                currentId = currentId,
                playing = snapshot.playing,
                onPlay = onPlayQueueTrack,
                onOpenPlayer = { scope.launch { playerPagerState.animateScrollToPage(PlayerNowPlayingPage) } },
                onClear = onClearQueue,
                modifier = Modifier.systemBarsPadding()
            )
        }
    }
}

@Composable
internal fun PlayerProgress(track: MusicTrack, snapshot: PlaybackSnapshot, controller: MediaController?) {
    var positionMs by remember(track.id) { mutableLongStateOf(snapshot.positionMs) }
    LaunchedEffect(track.id, snapshot.playing, snapshot.positionMs, controller) {
        positionMs = controller?.currentPosition?.coerceAtLeast(0L) ?: snapshot.positionMs
        if (snapshot.playing && controller != null) {
            while (true) {
                delay(33L)
                positionMs = controller.currentPosition.coerceAtLeast(0L)
            }
        }
    }

    val seekDurationMs = snapshot.durationMs
    val displayDurationMs = seekDurationMs.takeIf { it > 0L } ?: track.durationMs
    val fraction = if (seekDurationMs > 0L) (positionMs.toFloat() / seekDurationMs).coerceIn(0f, 1f) else 0f
    PlaybackProgressBar(
        value = fraction,
        enabled = seekDurationMs > 0L,
        modifier = Modifier.fillMaxWidth(),
        onSeek = { value ->
            if (seekDurationMs > 0L) controller?.seekTo((seekDurationMs * value).toLong())
        }
    )
    Row(Modifier.fillMaxWidth().padding(horizontal = 1.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatDuration(positionMs), color = MutedText, fontSize = 10.sp)
        Text(formatDuration(displayDurationMs), color = MutedText, fontSize = 10.sp)
    }
}

internal fun snapProgressFraction(value: Float, steps: Int): Float {
    val clamped = value.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    if (steps <= 0) return clamped
    val divisions = steps + 1
    return (clamped * divisions).roundToInt().coerceIn(0, divisions) / divisions.toFloat()
}

@Composable
internal fun PlaybackProgressBar(
    value: Float,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    emitDuringDrag: Boolean = false,
    onSeek: (Float) -> Unit
) {
    val density = LocalDensity.current
    val thumbRadiusPx = with(density) { 6.dp.toPx() }
    val insetPx = thumbRadiusPx
    val trackHeightPx = with(density) { 4.dp.toPx() }
    val accentColor = FeatherRed
    val latestOnSeek by rememberUpdatedState(onSeek)
    val latestValue by rememberUpdatedState(value)
    var displayedValue by remember(steps) { mutableFloatStateOf(snapProgressFraction(value, steps)) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(value, steps) {
        if (!isDragging) displayedValue = snapProgressFraction(value, steps)
    }

    val gestureAndSemantics = Modifier
        .pointerInput(enabled, insetPx, steps) {
            detectTapGestures { offset ->
                if (enabled) {
                    val usableWidth = (size.width - 2f * insetPx).coerceAtLeast(1f)
                    val raw = ((offset.x - insetPx) / usableWidth).coerceIn(0f, 1f)
                    val next = snapProgressFraction(raw, steps)
                    displayedValue = next
                    latestOnSeek(next)
                }
            }
        }
        .pointerInput(enabled, insetPx, steps, emitDuringDrag) {
            detectDragGestures(
                onDragStart = { offset ->
                    if (enabled) {
                        isDragging = true
                        val usableWidth = (size.width - 2f * insetPx).coerceAtLeast(1f)
                        val raw = ((offset.x - insetPx) / usableWidth).coerceIn(0f, 1f)
                        val next = snapProgressFraction(raw, steps)
                        if (next != displayedValue) {
                            displayedValue = next
                            if (emitDuringDrag) latestOnSeek(next)
                        }
                    }
                },
                onDragEnd = {
                    if (enabled && isDragging && !emitDuringDrag) latestOnSeek(displayedValue)
                    isDragging = false
                },
                onDragCancel = {
                    isDragging = false
                    displayedValue = snapProgressFraction(latestValue, steps)
                },
                onDrag = { change, _ ->
                    if (enabled) {
                        change.consume()
                        val usableWidth = (size.width - 2f * insetPx).coerceAtLeast(1f)
                        val raw = ((change.position.x - insetPx) / usableWidth).coerceIn(0f, 1f)
                        val next = snapProgressFraction(raw, steps)
                        if (next != displayedValue) {
                            displayedValue = next
                            if (emitDuringDrag) latestOnSeek(next)
                        }
                    }
                }
            )
        }
        .semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(displayedValue, 0f..1f, steps)
            if (enabled) {
                setProgress { requested ->
                    val next = snapProgressFraction(requested, steps)
                    displayedValue = next
                    latestOnSeek(next)
                    true
                }
            }
        }

    Canvas(modifier.fillMaxWidth().height(34.dp).then(gestureAndSemantics)) {
        val centerY = size.height / 2f
        val startX = insetPx
        val endX = (size.width - insetPx).coerceAtLeast(startX)
        val progressX = startX + (endX - startX) * displayedValue.coerceIn(0f, 1f)
        drawLine(
            color = Color(0xFF4A4654),
            start = Offset(startX, centerY),
            end = Offset(endX, centerY),
            strokeWidth = trackHeightPx,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accentColor,
            start = Offset(startX, centerY),
            end = Offset(progressX, centerY),
            strokeWidth = trackHeightPx,
            cap = StrokeCap.Round
        )
        drawCircle(color = accentColor, radius = thumbRadiusPx, center = Offset(progressX, centerY))
    }
}
