package com.featherw8.player.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import com.featherw8.player.data.MusicTrack
import com.featherw8.player.ui.model.PlaybackSnapshot
import com.featherw8.player.ui.model.TrackAction

/**
 * The two layers that sit above the screens: the mini player pinned above the bottom bar and the
 * full player that slides over everything. Both are stateless; the app owns the state and decides
 * what each callback does.
 */
@Composable
internal fun BoxScope.PlayerOverlays(
    currentTrack: MusicTrack?,
    snapshot: PlaybackSnapshot,
    controller: MediaController?,
    queue: List<MusicTrack>,
    favoriteIds: Set<String>,
    playerOpen: Boolean,
    playerClosedBySwipe: Boolean,
    onOpenPlayer: () -> Unit,
    onPlayPause: () -> Unit,
    onDismissMiniPlayer: () -> Unit,
    onMiniDragActiveChange: (Boolean) -> Unit,
    onClosePlayer: () -> Unit,
    onSwipeClosePlayer: () -> Unit,
    onPlayQueueTrack: (Int) -> Unit,
    onMoveQueueTrack: (from: Int, to: Int) -> Unit,
    onRemoveQueueTrack: (index: Int) -> Unit,
    onClearQueue: (MusicTrack) -> Unit,
    onArtworkClick: (MusicTrack) -> Unit,
    onTrackInfo: (MusicTrack) -> Unit,
    onToggleFavorite: (MusicTrack) -> Unit,
    onTrackAction: (MusicTrack, TrackAction) -> Unit
) {
    currentTrack?.let { miniTrack ->
        MiniPlayerBar(
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 80.dp),
            track = miniTrack,
            playing = snapshot.playing,
            durationMs = snapshot.durationMs,
            initialPositionMs = snapshot.positionMs,
            controller = controller,
            isVisible = !playerOpen,
            onOpen = onOpenPlayer,
            onPlayPause = onPlayPause,
            onDismiss = onDismissMiniPlayer,
            onDragActiveChange = onMiniDragActiveChange
        )
    }

    AnimatedVisibility(
        visible = playerOpen && currentTrack != null,
        modifier = Modifier.fillMaxSize(),
        enter = slideInVertically(animationSpec = tween(190)) { it } + fadeIn(animationSpec = tween(120)),
        exit = if (playerClosedBySwipe) {
            ExitTransition.None
        } else {
            slideOutVertically(animationSpec = tween(190)) { it } + fadeOut(animationSpec = tween(120))
        }
    ) {
        currentTrack?.let { activeTrack ->
            FullPlayerScreen(
                track = activeTrack,
                queue = queue,
                currentId = snapshot.mediaId,
                isFavorite = activeTrack.id in favoriteIds,
                snapshot = snapshot,
                controller = controller,
                onClose = onClosePlayer,
                onSwipeClose = onSwipeClosePlayer,
                onPlayQueueTrack = onPlayQueueTrack,
                onMoveQueueTrack = onMoveQueueTrack,
                onRemoveQueueTrack = onRemoveQueueTrack,
                onClearQueue = { onClearQueue(activeTrack) },
                onArtworkClick = onArtworkClick,
                onTrackInfo = onTrackInfo,
                onToggleFavorite = { onToggleFavorite(activeTrack) },
                onTrackAction = { onTrackAction(activeTrack, it) }
            )
        }
    }
}
