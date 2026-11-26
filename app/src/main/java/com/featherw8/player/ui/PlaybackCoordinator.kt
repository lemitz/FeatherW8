package com.featherw8.player.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.featherw8.player.data.MusicTrack
import com.featherw8.player.data.PlayCountStore
import com.featherw8.player.ui.actions.toMediaItem
import com.featherw8.player.ui.model.PlaybackSnapshot
import com.featherw8.player.ui.model.moved
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun rememberPlaybackCoordinator(
    context: Context,
    controller: MediaController?,
    scope: CoroutineScope,
    onPlaybackStarted: () -> Unit,
    onDismissed: () -> Unit,
    onSelectionCleared: () -> Unit
): PlaybackCoordinator {
    val coordinator = remember(context, scope) { PlaybackCoordinator(context, scope) }
    coordinator.update(controller, onPlaybackStarted, onDismissed, onSelectionCleared)
    return coordinator
}

@Composable
internal fun PlaybackCoordinatorEffects(
    coordinator: PlaybackCoordinator,
    tracks: List<MusicTrack>
) {
    val controller = coordinator.controller

    DisposableEffect(controller, coordinator) {
        val activePlayer = controller
        if (activePlayer == null) {
            onDispose {}
        } else {
            val listener = object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) =
                    coordinator.refreshPlaybackSnapshot(activePlayer)

                override fun onIsPlayingChanged(isPlaying: Boolean) =
                    coordinator.refreshPlaybackSnapshot(activePlayer)

                override fun onPlaybackStateChanged(playbackState: Int) =
                    coordinator.refreshPlaybackSnapshot(activePlayer)

                override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) =
                    coordinator.refreshPlaybackSnapshot(activePlayer)

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) = coordinator.refreshPlaybackSnapshot(activePlayer)

                override fun onRepeatModeChanged(repeatMode: Int) =
                    coordinator.refreshPlaybackSnapshot(activePlayer)

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) =
                    coordinator.refreshPlaybackSnapshot(activePlayer)
            }
            activePlayer.addListener(listener)
            coordinator.refreshPlaybackSnapshot(activePlayer)
            onDispose { activePlayer.removeListener(listener) }
        }
    }

    val mediaId = coordinator.playbackSnapshot.mediaId
    LaunchedEffect(controller, mediaId) {
        val activeMediaId = mediaId ?: return@LaunchedEffect
        while (true) {
            coordinator.observePlayCount(PlayCountStore.get(coordinator.context, activeMediaId))
            delay(1_000L)
        }
    }

    LaunchedEffect(tracks, coordinator.playCountRevision) {
        coordinator.playCountValues = withContext(Dispatchers.IO) {
            PlayCountStore.getAll(coordinator.context, tracks.map { it.id })
        }
    }
}

internal class PlaybackCoordinator(
    internal val context: Context,
    private val scope: CoroutineScope
) {
    internal var controller: MediaController? = null
        private set
    var queue by mutableStateOf<List<MusicTrack>>(emptyList())
        private set
    var playbackSnapshot by mutableStateOf(PlaybackSnapshot())
        private set
    var playCountRevision by mutableIntStateOf(0)
        private set
    var playCountValues by mutableStateOf<Map<String, Int>>(emptyMap())
        internal set

    internal var tracks: List<MusicTrack> = emptyList()
        private set
    private var lastObservedPlayCount = -1
    private var onPlaybackStarted: () -> Unit = {}
    private var onDismissed: () -> Unit = {}
    private var onSelectionCleared: () -> Unit = {}

    val currentTrack: MusicTrack?
        get() = queue.firstOrNull { it.id == playbackSnapshot.mediaId }
            ?: tracks.firstOrNull { it.id == playbackSnapshot.mediaId }

    fun update(
        controller: MediaController?,
        onPlaybackStarted: () -> Unit,
        onDismissed: () -> Unit,
        onSelectionCleared: () -> Unit
    ) {
        this.controller = controller
        this.onPlaybackStarted = onPlaybackStarted
        this.onDismissed = onDismissed
        this.onSelectionCleared = onSelectionCleared
    }

    fun updateTracks(tracks: List<MusicTrack>) {
        this.tracks = tracks
    }

    fun playCount(id: String): Int = playCountValues[id] ?: 0

    fun playTracks(items: List<MusicTrack>, startIndex: Int) {
        if (items.isEmpty()) {
            Toast.makeText(context, "No tracks match this list", Toast.LENGTH_SHORT).show()
            return
        }
        val activePlayer = controller
        if (activePlayer == null) {
            Toast.makeText(context, "Player is starting. Try again in a moment.", Toast.LENGTH_SHORT).show()
            return
        }
        val index = startIndex.coerceIn(items.indices)
        queue = items
        // Optimistically publish the selected track before MediaController reports its transition.
        playbackSnapshot = playbackSnapshot.copy(
            mediaId = items[index].id,
            durationMs = items[index].durationMs,
            positionMs = 0L
        )
        onPlaybackStarted()
        scope.launch {
            val mediaItems = withContext(Dispatchers.Default) { items.map { it.toMediaItem() } }
            activePlayer.setMediaItems(mediaItems, index, 0L)
            activePlayer.prepare()
            activePlayer.play()
        }
    }

    fun addTracksToQueue(selectedTracks: List<MusicTrack>) {
        val activePlayer = controller
        if (activePlayer == null) {
            Toast.makeText(context, "Player is starting. Try again in a moment.", Toast.LENGTH_SHORT).show()
            return
        }
        val hasExistingQueue = activePlayer.mediaItemCount > 0 && queue.isNotEmpty()
        val queuedIds = mutableSetOf<String>()
        if (hasExistingQueue) queuedIds.addAll(queue.map { it.id })
        val additions = selectedTracks.filter { queuedIds.add(it.id) }
        if (additions.isEmpty()) {
            Toast.makeText(context, "Selected tracks are already in the play queue", Toast.LENGTH_SHORT).show()
            return
        }
        val mediaItems = additions.map { it.toMediaItem() }
        if (hasExistingQueue) {
            activePlayer.addMediaItems(mediaItems)
            queue = queue + additions
        } else {
            activePlayer.setMediaItems(mediaItems)
            activePlayer.prepare()
            queue = additions
        }
        onSelectionCleared()
        val countLabel = if (additions.size == 1) "track" else "tracks"
        Toast.makeText(context, "Added ${additions.size} $countLabel to the play queue", Toast.LENGTH_SHORT).show()
    }

    fun dismissMiniPlayer() {
        controller?.let { activePlayer ->
            activePlayer.stop()
            activePlayer.clearMediaItems()
        }
        queue = emptyList()
        playbackSnapshot = PlaybackSnapshot()
        onDismissed()
    }

    fun moveQueueTrack(from: Int, to: Int) {
        val activePlayer = controller ?: return
        if (from == to || from !in queue.indices || to !in queue.indices) return
        if (from >= activePlayer.mediaItemCount || to >= activePlayer.mediaItemCount) return
        activePlayer.moveMediaItem(from, to)
        queue = queue.moved(from, to)
    }

    fun removeQueueTrack(index: Int) {
        val activePlayer = controller ?: return
        if (index !in queue.indices || index >= activePlayer.mediaItemCount) return
        if (queue.size == 1) {
            dismissMiniPlayer()
            return
        }
        activePlayer.removeMediaItem(index)
        queue = queue.toMutableList().apply { removeAt(index) }
    }

    fun clearQueueAndPlay(activeTrack: MusicTrack) {
        queue = listOf(activeTrack)
        controller?.clearMediaItems()
        playTracks(listOf(activeTrack), 0)
    }

    internal fun refreshPlaybackSnapshot(activePlayer: MediaController) {
        val mediaId = activePlayer.currentMediaItem?.mediaId
        val durationMs = activePlayer.duration.takeIf { it > 0L } ?: 0L
        val positionMs = activePlayer.currentPosition.coerceAtLeast(0L)
        val activePlayCount = mediaId?.let { PlayCountStore.get(context, it) } ?: 0
        observePlayCount(activePlayCount)
        playbackSnapshot = PlaybackSnapshot(
            mediaId = mediaId,
            positionMs = positionMs,
            durationMs = durationMs,
            playing = activePlayer.isPlaying,
            repeatMode = activePlayer.repeatMode,
            shuffleModeEnabled = activePlayer.shuffleModeEnabled
        )
    }

    internal fun observePlayCount(playCount: Int) {
        if (playCount != lastObservedPlayCount) {
            lastObservedPlayCount = playCount
            playCountRevision++
        }
    }
}
