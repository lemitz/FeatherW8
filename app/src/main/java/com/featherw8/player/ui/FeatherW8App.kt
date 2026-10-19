package com.featherw8.player.ui

import com.featherw8.player.MainActivity
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Size
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.featherw8.player.data.MediaLibraryRepository
import com.featherw8.player.data.MusicTrack
import com.featherw8.player.data.PlayCountStore
import com.featherw8.player.playback.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import com.featherw8.player.ui.actions.audioPermission
import com.featherw8.player.ui.actions.deleteFromMediaStore
import com.featherw8.player.ui.actions.resolveExternalTrack
import com.featherw8.player.ui.actions.setAsRingtone
import com.featherw8.player.ui.actions.shareTrack
import com.featherw8.player.ui.actions.shareTracks
import com.featherw8.player.ui.actions.toMediaItem
import com.featherw8.player.ui.artwork.CoverWarmer
import com.featherw8.player.ui.artwork.CustomArtworkStore
import com.featherw8.player.ui.components.AppToolbar
import com.featherw8.player.ui.components.CreatePlaylistDialog
import com.featherw8.player.ui.components.EmptyCard
import com.featherw8.player.ui.components.LibrarySelectionActionBar
import com.featherw8.player.ui.components.PlaylistPickerDialog
import com.featherw8.player.ui.components.ScanDialog
import com.featherw8.player.ui.components.TrackInfoDialog
import com.featherw8.player.ui.model.AlbumCollection
import com.featherw8.player.ui.model.AppPage
import com.featherw8.player.ui.model.ArtistCollection
import com.featherw8.player.ui.model.buildAlbumCollections
import com.featherw8.player.ui.model.SmartPlaylistCollections
import com.featherw8.player.ui.model.buildSmartPlaylistCollections
import com.featherw8.player.ui.model.buildArtistCollections
import com.featherw8.player.ui.model.encodePlaylists
import com.featherw8.player.ui.model.loadFavorites
import com.featherw8.player.ui.model.loadPlaylists
import com.featherw8.player.ui.model.PlaybackSnapshot
import com.featherw8.player.ui.model.PlaylistDetailState
import com.featherw8.player.ui.model.SortMode
import com.featherw8.player.ui.model.TrackAction
import com.featherw8.player.ui.model.UserPlaylist
import com.featherw8.player.ui.screens.AlbumsScreen
import com.featherw8.player.ui.screens.ArtistsScreen
import com.featherw8.player.ui.screens.CollectionPageEntrance
import com.featherw8.player.ui.screens.LibraryScreen
import com.featherw8.player.ui.screens.PlaylistsScreen
import com.featherw8.player.ui.screens.SettingsScreen
import com.featherw8.player.ui.screens.snapToStep
import com.featherw8.player.ui.screens.TrackCollectionDetailScreen
import com.featherw8.player.ui.theme.AmoledBlack
import com.featherw8.player.ui.theme.DeviceProfile
import com.featherw8.player.ui.theme.MiniPlayerContentOverlap
import com.featherw8.player.ui.theme.navColors
import com.featherw8.player.ui.components.ChangeCoverDialog
import com.featherw8.player.ui.components.DeletePlaylistDialog
import com.featherw8.player.ui.components.DeleteTracksDialog
import com.featherw8.player.ui.model.moved
import com.featherw8.player.ui.model.movedOver
import com.featherw8.player.ui.components.PlaylistActionsDialog
import com.featherw8.player.ui.components.RenamePlaylistDialog
import com.featherw8.player.ui.player.PlayerOverlays

internal const val KNOWN_TRACK_IDS_KEY = "known_track_ids"

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

@Composable
internal fun FeatherW8App(
    profile: DeviceProfile,
    dotFontEnabled: Boolean,
    onDotFontChange: (Boolean) -> Unit,
    accentId: String,
    onAccentChange: (String) -> Unit,
    openUri: Uri?,
    onOpenUriHandled: () -> Unit,
    openPlayerRequested: Boolean,
    onOpenPlayerHandled: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = rememberMediaController()
    val repository = remember(context) { MediaLibraryRepository(context) }
    val preferences = remember(context) { context.getSharedPreferences("featherw8", Context.MODE_PRIVATE) }

    var page by rememberSaveable { mutableStateOf(AppPage.LIBRARY) }
    var returnFromSettings by rememberSaveable { mutableStateOf(AppPage.LIBRARY) }
    var returnFromDetail by rememberSaveable { mutableStateOf(AppPage.PLAYLISTS) }
    val libraryListState = rememberLazyListState()
    val playlistsListState = rememberLazyListState()
    val albumsGridState = rememberLazyGridState()
    val artistsGridState = rememberLazyGridState()
    val detailListState = rememberLazyListState()
    var lastRootBackAt by remember { mutableStateOf(0L) }
    var tracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var queue by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var selectedLibraryTrackIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var query by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf(SortMode.from(preferences.getString("sort_mode", "TITLE"))) }
    var minimumSizeKb by rememberSaveable {
        val savedKb = preferences.getFloat("minimum_size_kb", Float.NaN)
        val initialKb = if (savedKb.isNaN()) preferences.getFloat("minimum_size_mb", 0f) * 1024f else savedKb
        mutableFloatStateOf(snapToStep(initialKb, 0f..1500f, 50f))
    }
    var minimumDurationSeconds by rememberSaveable {
        mutableFloatStateOf(snapToStep(preferences.getFloat("minimum_duration_seconds", 0f), 0f..40f, 5f))
    }
    var permissionGranted by remember {
        mutableStateOf(context.checkSelfPermission(audioPermission()) == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }
    var scanning by remember { mutableStateOf(false) }
    var showScanProgress by remember { mutableStateOf(false) }
    var pendingManualScan by remember { mutableStateOf(false) }
    var playbackSnapshot by remember { mutableStateOf(PlaybackSnapshot()) }
    var playCountRevision by remember { mutableIntStateOf(0) }
    var lastObservedPlayCount by remember { mutableIntStateOf(-1) }
    var playerOpen by rememberSaveable { mutableStateOf(false) }
    var playerClosedBySwipe by remember { mutableStateOf(false) }
    var miniPlayerDragActive by remember { mutableStateOf(false) }
    var favoriteIds by remember { mutableStateOf(loadFavorites(preferences)) }
    var userPlaylists by remember { mutableStateOf(loadPlaylists(preferences)) }
    var playlistForActions by remember { mutableStateOf<UserPlaylist?>(null) }
    var playlistForRename by remember { mutableStateOf<UserPlaylist?>(null) }
    var playlistForDelete by remember { mutableStateOf<UserPlaylist?>(null) }
    var renamePlaylistName by rememberSaveable { mutableStateOf("") }
    var infoTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var tracksToAddToPlaylist by remember { mutableStateOf<List<MusicTrack>?>(null) }
    var deleteTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var tracksPendingDelete by remember { mutableStateOf<List<MusicTrack>?>(null) }
    var tracksForNewPlaylist by remember { mutableStateOf<List<String>>(emptyList()) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var pendingPlaylistName by remember { mutableStateOf("") }
    var playlistDetail by remember { mutableStateOf<PlaylistDetailState?>(null) }
    var selectedAlbumKey by remember { mutableStateOf<String?>(null) }
    var selectedArtistKey by remember { mutableStateOf<String?>(null) }
    var artworkActionTrack by remember { mutableStateOf<MusicTrack?>(null) }
    val tabPagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    var albums by remember { mutableStateOf<List<AlbumCollection>>(emptyList()) }
    var artists by remember { mutableStateOf<List<ArtistCollection>>(emptyList()) }
    LaunchedEffect(tracks) {
        val trackSnapshot = tracks
        val (nextAlbums, nextArtists) = withContext(Dispatchers.Default) {
            buildAlbumCollections(trackSnapshot) to buildArtistCollections(trackSnapshot)
        }
        albums = nextAlbums
        artists = nextArtists
    }
    val latestAppPage by rememberUpdatedState(page)

    LaunchedEffect(tabPagerState) {
        var previousSettledPage = tabPagerState.settledPage
        snapshotFlow { tabPagerState.settledPage }.collect { settledPage ->
            if (settledPage == previousSettledPage) return@collect
            previousSettledPage = settledPage
            val nextPage = if (settledPage == 0) AppPage.LIBRARY else AppPage.PLAYLISTS
            if ((latestAppPage == AppPage.LIBRARY || latestAppPage == AppPage.PLAYLISTS) && latestAppPage != nextPage) {
                if (nextPage == AppPage.PLAYLISTS) query = ""
                lastRootBackAt = 0L
                page = nextPage
            }
        }
    }

    LaunchedEffect(Unit) {
        if (page == AppPage.QUEUE) page = AppPage.LIBRARY
    }

    LaunchedEffect(page) {
        if (page != AppPage.LIBRARY) selectedLibraryTrackIds = emptySet()
        val targetPage = when (page) {
            AppPage.LIBRARY -> 0
            AppPage.PLAYLISTS -> 1
            else -> null
        }
        // Compare with where the pager is heading, not with the last page it settled on: a quick tap
        // back while it is still travelling has to be able to reverse the animation. (Comparing with
        // settledPage dropped that tap and left the pager to drift, then fight the selected tab.)
        if (targetPage != null && tabPagerState.targetPage != targetPage) {
            tabPagerState.animateScrollToPage(targetPage)
        }
    }

    fun scanLibrary(showProgress: Boolean = false, manualScan: Boolean = false) {
        if (scanning) return
        scanning = true
        showScanProgress = showProgress
        scope.launch {
            try {
                val scannedTracks = repository.scanInternalStorage()
                val previouslyKnown = withContext(Dispatchers.IO) {
                    preferences.getStringSet(KNOWN_TRACK_IDS_KEY, null)?.toSet()
                }
                val (currentIds, newTrackCount) = withContext(Dispatchers.Default) {
                    val ids = scannedTracks.mapTo(mutableSetOf()) { it.id }
                    val count = if (previouslyKnown == null) {
                        ids.size
                    } else {
                        ids.count { it !in previouslyKnown }
                    }
                    ids to count
                }
                withContext(Dispatchers.IO) {
                    preferences.edit().putStringSet(KNOWN_TRACK_IDS_KEY, currentIds).apply()
                }
                // The scan runs on every return to the app. Replacing the list with an equal one would
                // still re-filter, re-sort and regroup the whole library and restart the cover warmer.
                if (scannedTracks != tracks) {
                    tracks = scannedTracks
                    CoverWarmer.start(scope, context.applicationContext, scannedTracks) {
                        libraryListState.isScrollInProgress
                    }
                }
                selectedLibraryTrackIds = selectedLibraryTrackIds.intersect(currentIds)
                if (manualScan) {
                    val message = if (newTrackCount == 0) "No new tracks found" else {
                        val unit = if (newTrackCount == 1) "track" else "tracks"
                        "Found $newTrackCount new $unit"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                } else if (newTrackCount > 0) {
                    val unit = if (newTrackCount == 1) "track" else "tracks"
                    Toast.makeText(context, "Found $newTrackCount new $unit", Toast.LENGTH_SHORT).show()
                }
            } catch (_: SecurityException) {
                Toast.makeText(context, "Audio access is needed to scan internal storage", Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Library scan failed", Toast.LENGTH_SHORT).show()
            } finally {
                scanning = false
                showScanProgress = false
            }
        }
    }

    val scanOnAppEntry by rememberUpdatedState {
        val hasAudioPermission = context.checkSelfPermission(audioPermission()) == android.content.pm.PackageManager.PERMISSION_GRANTED
        permissionGranted = hasAudioPermission
        if (hasAudioPermission) scanLibrary(showProgress = false)
    }
    DisposableEffect(context) {
        onDispose { CoverWarmer.cancel() }
    }

    DisposableEffect(context) {
        val activity = context as? MainActivity
        val callback: () -> Unit = { scanOnAppEntry() }
        activity?.onAppEntry = callback
        onDispose {
            if (activity?.onAppEntry === callback) activity.onAppEntry = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        val requestedManually = pendingManualScan
        pendingManualScan = false
        if (granted) {
            scanLibrary(showProgress = requestedManually, manualScan = requestedManually)
        } else {
            Toast.makeText(context, "Audio access is needed to scan internal storage", Toast.LENGTH_SHORT).show()
        }
    }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedLibraryTrackIds = emptySet()
            scanLibrary()
        }
    }
    val artworkPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = artworkActionTrack
        if (uri != null && target != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            CustomArtworkStore.put(context, target.id, uri)
            Toast.makeText(context, "Cover updated", Toast.LENGTH_SHORT).show()
        }
        artworkActionTrack = null
    }

    fun requestOrScan(manualScan: Boolean = false) {
        if (permissionGranted) {
            scanLibrary(showProgress = true, manualScan = manualScan)
        } else {
            pendingManualScan = manualScan
            permissionLauncher.launch(audioPermission())
        }
    }

    fun persistFavorites(next: Set<String>) {
        favoriteIds = next
        preferences.edit().putStringSet("favorites", next).apply()
    }

    fun persistPlaylists(next: List<UserPlaylist>) {
        userPlaylists = next
        preferences.edit().putString("user_playlists", encodePlaylists(next)).apply()
    }

    // Playlist edits go by track id, not by position: the list on screen skips tracks whose file is
    // gone, so a position on screen is not a position in the stored playlist.
    fun moveInPlaylist(playlistId: String, shown: List<MusicTrack>, from: Int, to: Int) {
        val movingId = shown.getOrNull(from)?.id ?: return
        val overId = shown.getOrNull(to)?.id ?: return
        persistPlaylists(userPlaylists.map {
            if (it.id == playlistId) it.copy(trackIds = it.trackIds.movedOver(movingId, overId)) else it
        })
    }

    fun removeFromPlaylist(playlistId: String, trackId: String) {
        persistPlaylists(userPlaylists.map {
            if (it.id == playlistId) it.copy(trackIds = it.trackIds - trackId) else it
        })
    }

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
        // Show the now playing screen right away with the track we are about to play:
        // the real snapshot follows a few frames later with the media item transition.
        // The playlist itself is built off the main thread - a few hundred MediaItem and
        // MediaMetadata allocations plus a Uri.parse each are enough to starve the
        // slide-in animation on the frame the user taps.
        playbackSnapshot = playbackSnapshot.copy(
            mediaId = items[index].id,
            durationMs = items[index].durationMs,
            positionMs = 0L
        )
        lastRootBackAt = 0L
        playerClosedBySwipe = false
        playerOpen = true
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
        selectedLibraryTrackIds = emptySet()
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
        playerOpen = false
        miniPlayerDragActive = false
        lastRootBackAt = 0L
    }

    // The queue the screen shows mirrors the player's playlist, so every edit is made on both.
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
            // Removing the last track is the same as closing the player.
            dismissMiniPlayer()
            return
        }
        // Removing the track that is playing makes the player continue with the next one.
        activePlayer.removeMediaItem(index)
        queue = queue.toMutableList().apply { removeAt(index) }
    }

    fun handleTrackAction(track: MusicTrack, action: TrackAction) {
        when (action) {
            TrackAction.ADD_TO_PLAYLIST -> tracksToAddToPlaylist = listOf(track)
            TrackAction.INFO -> infoTrack = track
            TrackAction.RINGTONE -> setAsRingtone(context, track)
            TrackAction.SHARE -> shareTrack(context, track)
            TrackAction.DELETE -> deleteTrack = track
        }
    }

    // "Open with FeatherW8": play the requested file once the player service is connected.
    LaunchedEffect(openUri, controller) {
        val uri = openUri ?: return@LaunchedEffect
        if (controller == null) return@LaunchedEffect
        val library = tracks
        val track = withContext(Dispatchers.IO) { resolveExternalTrack(context, uri, library) }
        playTracks(listOf(track), 0)
        onOpenUriHandled()
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { PlayCountStore.getAll(context, emptyList()) }
        if (permissionGranted) scanLibrary(showProgress = false)
        else permissionLauncher.launch(audioPermission())
    }

    LaunchedEffect(playerClosedBySwipe) {
        if (playerClosedBySwipe) {
            delay(48L)
            playerClosedBySwipe = false
        }
    }

    fun refreshPlaybackSnapshot(activePlayer: MediaController) {
        val mediaId = activePlayer.currentMediaItem?.mediaId
        val durationMs = activePlayer.duration.takeIf { it > 0L } ?: 0L
        val positionMs = activePlayer.currentPosition.coerceAtLeast(0L)
        val activePlayCount = mediaId?.let { PlayCountStore.get(context, it) } ?: 0
        if (activePlayCount != lastObservedPlayCount) {
            lastObservedPlayCount = activePlayCount
            playCountRevision++
        }
        playbackSnapshot = PlaybackSnapshot(
            mediaId = mediaId,
            positionMs = positionMs,
            durationMs = durationMs,
            playing = activePlayer.isPlaying,
            repeatMode = activePlayer.repeatMode,
            shuffleModeEnabled = activePlayer.shuffleModeEnabled
        )
    }

    DisposableEffect(controller) {
        val activePlayer = controller
        if (activePlayer == null) {
            onDispose {}
        } else {
            val listener = object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) =
                    refreshPlaybackSnapshot(activePlayer)

                override fun onIsPlayingChanged(isPlaying: Boolean) =
                    refreshPlaybackSnapshot(activePlayer)

                override fun onPlaybackStateChanged(playbackState: Int) =
                    refreshPlaybackSnapshot(activePlayer)

                override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) =
                    refreshPlaybackSnapshot(activePlayer)

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) = refreshPlaybackSnapshot(activePlayer)

                override fun onRepeatModeChanged(repeatMode: Int) =
                    refreshPlaybackSnapshot(activePlayer)

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) =
                    refreshPlaybackSnapshot(activePlayer)
            }
            activePlayer.addListener(listener)
            refreshPlaybackSnapshot(activePlayer)
            onDispose { activePlayer.removeListener(listener) }
        }
    }

    LaunchedEffect(controller, playbackSnapshot.mediaId) {
        val mediaId = playbackSnapshot.mediaId ?: return@LaunchedEffect
        while (true) {
            val activePlayCount = PlayCountStore.get(context, mediaId)
            if (activePlayCount != lastObservedPlayCount) {
                lastObservedPlayCount = activePlayCount
                playCountRevision++
            }
            delay(1_000L)
        }
    }

    val currentTrack = queue.firstOrNull { it.id == playbackSnapshot.mediaId }
        ?: tracks.firstOrNull { it.id == playbackSnapshot.mediaId }

    // A tap on the player card in the notification shade: show the full player once the track it is
    // about is known. After a cold start that is only after the library scan, so the request waits;
    // if nothing is playing there is nothing to show and the request is dropped.
    LaunchedEffect(openPlayerRequested, currentTrack, controller, playbackSnapshot.mediaId) {
        if (!openPlayerRequested) return@LaunchedEffect
        when {
            currentTrack != null -> {
                selectedLibraryTrackIds = emptySet()
                miniPlayerDragActive = false
                playerClosedBySwipe = false
                lastRootBackAt = 0L
                playerOpen = true
                onOpenPlayerHandled()
            }
            controller != null && playbackSnapshot.mediaId == null -> onOpenPlayerHandled()
        }
    }
    // While the now playing screen covers everything, keep the content padding and the
    // "currently playing" marker frozen. Starting playback fires several player events
    // (media item, buffering, playing, timeline), and each of them would otherwise
    // re-layout the list behind the slide-in animation.
    val bottomOverlayPadding = if (currentTrack != null && !playerOpen) MiniPlayerContentOverlap else 0.dp
    val activeMediaId = playbackSnapshot.mediaId
    val isActuallyPlaying = playbackSnapshot.playing && !playerOpen
    var playCountValues by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    LaunchedEffect(tracks, playCountRevision) {
        val trackSnapshot = tracks
        playCountValues = withContext(Dispatchers.IO) {
            PlayCountStore.getAll(context, trackSnapshot.map { it.id })
        }
    }
    val playCountReader: (String) -> Int = remember(playCountValues) {
        { id -> playCountValues[id] ?: 0 }
    }
    // Computed here, once per change of its inputs, instead of inside the Playlists page: the page
    // is created and torn down while switching tabs and would start empty each time.
    var smartPlaylists by remember { mutableStateOf(SmartPlaylistCollections()) }
    LaunchedEffect(tracks, favoriteIds, playCountValues) {
        val trackSnapshot = tracks
        val favoritesSnapshot = favoriteIds
        val countsSnapshot = playCountValues
        smartPlaylists = withContext(Dispatchers.Default) {
            buildSmartPlaylistCollections(trackSnapshot, favoritesSnapshot) { id -> countsSnapshot[id] ?: 0 }
        }
    }
    val currentlyPlaying: (String) -> Boolean = remember(activeMediaId, isActuallyPlaying) {
        { id -> id == activeMediaId && isActuallyPlaying }
    }
    val playCountsForSorting: Map<String, Int> =
        if (sortMode == SortMode.PLAYS) playCountValues else emptyMap()
    var visibleTracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    LaunchedEffect(
        tracks,
        minimumSizeKb,
        minimumDurationSeconds,
        query,
        sortMode,
        playCountsForSorting
    ) {
        val trackSnapshot = tracks
        val minimumSizeBytes = (minimumSizeKb * 1024f).toLong()
        val minDurationSeconds = minimumDurationSeconds
        val minimumWholeSeconds = minDurationSeconds.toLong()
        val normalizedQuery = query.trim()
        val sortModeSnapshot = sortMode
        if (normalizedQuery.isNotBlank()) delay(80L)
        visibleTracks = withContext(Dispatchers.Default) {
            val filtered = trackSnapshot.filter { track ->
                track.sizeBytes >= minimumSizeBytes &&
                    // A track whose duration is unknown (0) is never hidden by this filter.
                    (minDurationSeconds <= 0f || track.durationMs <= 0L ||
                        track.durationMs / 1_000L >= minimumWholeSeconds) &&
                    (normalizedQuery.isBlank() ||
                        track.title.contains(normalizedQuery, ignoreCase = true) ||
                        track.artist.contains(normalizedQuery, ignoreCase = true) ||
                        track.album.contains(normalizedQuery, ignoreCase = true) ||
                        track.mimeType.contains(normalizedQuery, ignoreCase = true))
            }
            when (sortModeSnapshot) {
                SortMode.TITLE -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                SortMode.ADDED -> filtered.sortedByDescending { it.dateAddedSeconds }
                SortMode.PLAYS -> filtered.sortedByDescending { playCountsForSorting[it.id] ?: 0 }
            }
        }
    }

    val selectedLibraryTracks = visibleTracks.filter { it.id in selectedLibraryTrackIds }

    fun toggleLibraryTrackSelection(track: MusicTrack) {
        selectedLibraryTrackIds = if (track.id in selectedLibraryTrackIds) {
            selectedLibraryTrackIds - track.id
        } else {
            selectedLibraryTrackIds + track.id
        }
        lastRootBackAt = 0L
    }

    fun selectAllVisibleLibraryTracks() {
        val visibleIds = visibleTracks.mapTo(linkedSetOf()) { it.id }
        selectedLibraryTrackIds = if (visibleIds.isNotEmpty() && visibleIds.all { it in selectedLibraryTrackIds }) {
            emptySet()
        } else {
            visibleIds
        }
        lastRootBackAt = 0L
    }

    val selectedAlbum = albums.firstOrNull { it.key == selectedAlbumKey }
    val selectedArtist = artists.firstOrNull { it.key == selectedArtistKey }
    val resolvedPlaylistDetail = playlistDetail?.let { detail ->
        if (detail.userPlaylistId == null) detail else {
            val userPlaylist = userPlaylists.firstOrNull { it.id == detail.userPlaylistId }
            detail.copy(
                title = userPlaylist?.name ?: detail.title,
                tracks = userPlaylist?.trackIds?.mapNotNull { id -> tracks.firstOrNull { it.id == id } }.orEmpty()
            )
        }
    }

    fun openPlaylistDetail(title: String, items: List<MusicTrack>, userPlaylistId: String? = null) {
        playlistDetail = PlaylistDetailState(title, items, userPlaylistId)
        returnFromDetail = AppPage.PLAYLISTS
        lastRootBackAt = 0L
        page = AppPage.PLAYLIST_DETAIL
    }

    fun goBackFromPage() {
        lastRootBackAt = 0L
        val destination = when (page) {
            AppPage.SETTINGS -> returnFromSettings
            AppPage.ALBUMS, AppPage.ARTISTS -> AppPage.PLAYLISTS
            AppPage.ALBUM_DETAIL, AppPage.ARTIST_DETAIL, AppPage.PLAYLIST_DETAIL -> returnFromDetail
            else -> AppPage.LIBRARY
        }
        if (destination == AppPage.LIBRARY && tabPagerState.currentPage != 0) {
            scope.launch { tabPagerState.scrollToPage(0) }
        } else if (destination == AppPage.PLAYLISTS && tabPagerState.currentPage != 1) {
            scope.launch { tabPagerState.scrollToPage(1) }
        }
        page = destination
    }

    BackHandler(enabled = true) {
        if (playerOpen) {
            playerClosedBySwipe = false
            playerOpen = false
            lastRootBackAt = SystemClock.elapsedRealtime()
        } else if (page == AppPage.LIBRARY && selectedLibraryTrackIds.isNotEmpty()) {
            selectedLibraryTrackIds = emptySet()
            lastRootBackAt = 0L
        } else {
            val now = SystemClock.elapsedRealtime()
            when (page) {
                AppPage.SETTINGS, AppPage.ALBUMS, AppPage.ARTISTS,
                AppPage.ALBUM_DETAIL, AppPage.ARTIST_DETAIL, AppPage.PLAYLIST_DETAIL,
                AppPage.QUEUE -> goBackFromPage()
                AppPage.LIBRARY, AppPage.PLAYLISTS -> {
                    if (lastRootBackAt > 0L && now - lastRootBackAt <= 2_000L) {
                        (context as? Activity)?.finish()
                    } else {
                        lastRootBackAt = now
                        Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(AmoledBlack)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = AmoledBlack,
            bottomBar = {
                if (page == AppPage.LIBRARY && selectedLibraryTrackIds.isNotEmpty()) {
                    LibrarySelectionActionBar(
                        onDelete = { if (selectedLibraryTracks.isNotEmpty()) tracksPendingDelete = selectedLibraryTracks },
                        onAddToPlaylist = { if (selectedLibraryTracks.isNotEmpty()) tracksToAddToPlaylist = selectedLibraryTracks },
                        onAddToQueue = { if (selectedLibraryTracks.isNotEmpty()) addTracksToQueue(selectedLibraryTracks) },
                        onShare = {
                            if (selectedLibraryTracks.isNotEmpty()) {
                                shareTracks(context, selectedLibraryTracks)
                                selectedLibraryTrackIds = emptySet()
                            }
                        }
                    )
                } else {
                    Column {
                        NavigationBar(containerColor = AmoledBlack, tonalElevation = 0.dp) {
                            NavigationBarItem(
                                selected = page == AppPage.LIBRARY,
                                onClick = { lastRootBackAt = 0L; page = AppPage.LIBRARY },
                                icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = null) },
                                label = { Text("Library") },
                                colors = navColors()
                            )
                            NavigationBarItem(
                                selected = page in setOf(
                                    AppPage.PLAYLISTS, AppPage.ALBUMS, AppPage.ARTISTS,
                                    AppPage.ALBUM_DETAIL, AppPage.ARTIST_DETAIL, AppPage.PLAYLIST_DETAIL
                                ),
                                onClick = {
                                    query = ""
                                    selectedLibraryTrackIds = emptySet()
                                    lastRootBackAt = 0L
                                    page = AppPage.PLAYLISTS
                                },
                                icon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) },
                                label = { Text("Playlists") },
                                colors = navColors()
                            )
                        }
                    }
                }
            }
        ) { padding ->
            val layoutDirection = LocalLayoutDirection.current
            val contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection),
                top = padding.calculateTopPadding(),
                end = padding.calculateEndPadding(layoutDirection),
                bottom = padding.calculateBottomPadding()
            )
            Column(Modifier.fillMaxSize().background(AmoledBlack).padding(contentPadding)) {
                if (page != AppPage.LIBRARY || selectedLibraryTrackIds.isEmpty()) {
                    AppToolbar(
                        page = page,
                        title = when (page) {
                            AppPage.SETTINGS -> "Settings"
                            AppPage.ALBUMS -> "Albums"
                            AppPage.ARTISTS -> "Artists"
                            AppPage.ALBUM_DETAIL -> selectedAlbum?.title ?: "Album"
                            AppPage.ARTIST_DETAIL -> selectedArtist?.name ?: "Artist"
                            AppPage.PLAYLIST_DETAIL -> resolvedPlaylistDetail?.title ?: "Playlist"
                            else -> null
                        },
                        onSettings = {
                            returnFromSettings = page
                            lastRootBackAt = 0L
                            page = AppPage.SETTINGS
                        },
                        onBack = { goBackFromPage() }
                    )
                }
                when (page) {
                    AppPage.LIBRARY, AppPage.PLAYLISTS -> HorizontalPager(
                        state = tabPagerState,
                        // Keep the neighbouring tab composed: otherwise every switch throws one page away
                        // and builds the other from scratch in the middle of the swipe animation.
                        beyondViewportPageCount = 1,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        userScrollEnabled = !playerOpen && !playerClosedBySwipe && !miniPlayerDragActive && selectedLibraryTrackIds.isEmpty()
                    ) { tabIndex ->
                        when (tabIndex) {
                            0 -> LibraryScreen(
                                tracks = visibleTracks,
                                listState = libraryListState,
                                query = query,
                                onQueryChange = { query = it },
                                sortMode = sortMode,
                                onSortChange = {
                                    sortMode = it
                                    preferences.edit().putString("sort_mode", it.name).apply()
                                },
                                onPlay = { index -> playTracks(visibleTracks, index) },
                                onTrackAction = { track, action -> handleTrackAction(track, action) },
                                onScan = { requestOrScan() },
                                permissionGranted = permissionGranted,
                                playCount = playCountReader,
                                isCurrentlyPlaying = currentlyPlaying,
                                bottomOverlayPadding = bottomOverlayPadding,
                                selectedTrackIds = selectedLibraryTrackIds,
                                onLongPressTrack = { track -> toggleLibraryTrackSelection(track) },
                                onToggleSelection = { track -> toggleLibraryTrackSelection(track) },
                                onCancelSelection = {
                                    selectedLibraryTrackIds = emptySet()
                                    lastRootBackAt = 0L
                                },
                                onSelectAll = { selectAllVisibleLibraryTracks() }
                            )
                            else -> PlaylistsScreen(
                                smartPlaylists = smartPlaylists,
                                listState = playlistsListState,
                                playlists = userPlaylists,
                                albums = albums,
                                artists = artists,
                                onOpenAlbums = { lastRootBackAt = 0L; page = AppPage.ALBUMS },
                                onOpenArtists = { lastRootBackAt = 0L; page = AppPage.ARTISTS },
                                onOpenSmartPlaylist = { title, items -> openPlaylistDetail(title, items) },
                                onCreate = { showCreatePlaylist = true },
                                onManagePlaylist = { playlistForActions = it },
                                onOpenUserPlaylist = { playlist ->
                                    val items = playlist.trackIds.mapNotNull { id -> tracks.firstOrNull { it.id == id } }
                                    openPlaylistDetail(playlist.name, items, playlist.id)
                                },
                                bottomOverlayPadding = bottomOverlayPadding
                            )
                        }
                    }
                    AppPage.SETTINGS -> SettingsScreen(
                        profile = profile,
                        minimumSizeKb = minimumSizeKb,
                        onMinimumSizeChange = {
                            minimumSizeKb = it
                            preferences.edit().putFloat("minimum_size_kb", it).remove("minimum_size_mb").apply()
                        },
                        minimumDurationSeconds = minimumDurationSeconds,
                        onMinimumDurationChange = {
                            minimumDurationSeconds = it
                            preferences.edit().putFloat("minimum_duration_seconds", it).apply()
                        },
                        onScan = { requestOrScan(manualScan = true) },
                        dotFontEnabled = dotFontEnabled,
                        onDotFontChange = onDotFontChange,
                        accentId = accentId,
                        onAccentChange = onAccentChange,
                        bottomOverlayPadding = bottomOverlayPadding
                    )
                    AppPage.ALBUMS -> CollectionPageEntrance(AppPage.ALBUMS) {
                        AlbumsScreen(
                            albums = albums,
                            listState = albumsGridState,
                            onOpenAlbum = { album ->
                                selectedAlbumKey = album.key
                                returnFromDetail = AppPage.ALBUMS
                                page = AppPage.ALBUM_DETAIL
                            },
                            bottomOverlayPadding = bottomOverlayPadding
                        )
                    }
                    AppPage.ARTISTS -> CollectionPageEntrance(AppPage.ARTISTS) {
                        ArtistsScreen(
                            artists = artists,
                            listState = artistsGridState,
                            onOpenArtist = { artist ->
                                selectedArtistKey = artist.key
                                returnFromDetail = AppPage.ARTISTS
                                page = AppPage.ARTIST_DETAIL
                            },
                            bottomOverlayPadding = bottomOverlayPadding
                        )
                    }
                    AppPage.ALBUM_DETAIL -> CollectionPageEntrance(selectedAlbumKey ?: AppPage.ALBUM_DETAIL) {
                        selectedAlbum?.let { album ->
                            TrackCollectionDetailScreen(
                                title = album.title,
                                subtitle = "${album.artist} · ${album.tracks.size} tracks",
                                heroTrack = album.tracks.firstOrNull(),
                                tracks = album.tracks,
                                listState = detailListState,
                                onPlayAll = { playTracks(album.tracks, 0) },
                                onPlayTrack = { index -> playTracks(album.tracks, index) },
                                onTrackAction = { track, action -> handleTrackAction(track, action) },
                                bottomOverlayPadding = bottomOverlayPadding
                            )
                        } ?: EmptyCard("Album unavailable", "It may have been removed by a library scan.")
                    }
                    AppPage.ARTIST_DETAIL -> CollectionPageEntrance(selectedArtistKey ?: AppPage.ARTIST_DETAIL) {
                        selectedArtist?.let { artist ->
                            val albumCount = artist.tracks.map { it.albumId.takeIf { id -> id >= 0L }?.toString() ?: it.album.lowercase() }.distinct().size
                            TrackCollectionDetailScreen(
                                title = artist.name,
                                subtitle = "${albumCount} albums · ${artist.tracks.size} tracks",
                                heroTrack = artist.tracks.firstOrNull(),
                                tracks = artist.tracks,
                                listState = detailListState,
                                onPlayAll = { playTracks(artist.tracks, 0) },
                                onPlayTrack = { index -> playTracks(artist.tracks, index) },
                                onTrackAction = { track, action -> handleTrackAction(track, action) },
                                bottomOverlayPadding = bottomOverlayPadding
                            )
                        } ?: EmptyCard("Artist unavailable", "They may no longer be in the library.")
                    }
                    AppPage.PLAYLIST_DETAIL -> CollectionPageEntrance(
                        resolvedPlaylistDetail?.userPlaylistId ?: resolvedPlaylistDetail?.title ?: AppPage.PLAYLIST_DETAIL
                    ) {
                        resolvedPlaylistDetail?.let { detail ->
                            TrackCollectionDetailScreen(
                                title = detail.title,
                                subtitle = "${detail.tracks.size} tracks",
                                heroTrack = detail.tracks.firstOrNull(),
                                tracks = detail.tracks,
                                listState = detailListState,
                                onPlayAll = { playTracks(detail.tracks, 0) },
                                onPlayTrack = { index -> playTracks(detail.tracks, index) },
                                onTrackAction = { track, action -> handleTrackAction(track, action) },
                                bottomOverlayPadding = bottomOverlayPadding,
                                onManage = detail.userPlaylistId?.let { id ->
                                    { userPlaylists.firstOrNull { it.id == id }?.let { playlistForActions = it } }
                                },
                                // Only a playlist the user made can be reordered; albums, artists and the
                                // smart playlists are computed from the library.
                                onMoveTrack = detail.userPlaylistId?.let { playlistId ->
                                    { from, to -> moveInPlaylist(playlistId, detail.tracks, from, to) }
                                },
                                onRemoveTrack = detail.userPlaylistId?.let { playlistId ->
                                    { index -> detail.tracks.getOrNull(index)?.let { removeFromPlaylist(playlistId, it.id) } }
                                }
                            )
                        } ?: EmptyCard("Playlist unavailable", "It may have been removed.")
                    }
                    // Legacy page restored from old saved state; LaunchedEffect(Unit) moves it to LIBRARY.
                    AppPage.QUEUE -> Unit
                }
            }
        }

        PlayerOverlays(
            currentTrack = currentTrack,
            snapshot = playbackSnapshot,
            controller = controller,
            queue = queue,
            favoriteIds = favoriteIds,
            playerOpen = playerOpen,
            playerClosedBySwipe = playerClosedBySwipe,
            onOpenPlayer = {
                selectedLibraryTrackIds = emptySet()
                miniPlayerDragActive = false
                playerClosedBySwipe = false
                lastRootBackAt = 0L
                playerOpen = true
            },
            onPlayPause = { if (playbackSnapshot.playing) controller?.pause() else controller?.play() },
            onDismissMiniPlayer = { dismissMiniPlayer() },
            onMiniDragActiveChange = { miniPlayerDragActive = it },
            onClosePlayer = {
                playerClosedBySwipe = false
                playerOpen = false
            },
            onSwipeClosePlayer = {
                playerClosedBySwipe = true
                playerOpen = false
            },
            onPlayQueueTrack = { index -> playTracks(queue, index) },
            onMoveQueueTrack = { from, to -> moveQueueTrack(from, to) },
            onRemoveQueueTrack = { index -> removeQueueTrack(index) },
            onClearQueue = { activeTrack ->
                queue = listOf(activeTrack)
                controller?.clearMediaItems()
                playTracks(listOf(activeTrack), 0)
            },
            onArtworkClick = { artworkActionTrack = it },
            onTrackInfo = { infoTrack = it },
            onToggleFavorite = { activeTrack ->
                val next = favoriteIds.toMutableSet().apply {
                    if (activeTrack.id in this) remove(activeTrack.id) else add(activeTrack.id)
                }
                persistFavorites(next)
            },
            onTrackAction = { track, action -> handleTrackAction(track, action) }
        )
    }

    if (showScanProgress) ScanDialog()
    artworkActionTrack?.let { track ->
        ChangeCoverDialog(
            hasCustomCover = CustomArtworkStore.get(track.id) != null,
            onChooseImage = { artworkPickerLauncher.launch(arrayOf("image/*")) },
            onUseOriginal = {
                CustomArtworkStore.remove(context, track.id)
                artworkActionTrack = null
            },
            onDismiss = { artworkActionTrack = null }
        )
    }
    infoTrack?.let { track ->
        val plays = remember(track.id, playCountRevision) { PlayCountStore.get(context, track.id) }
        TrackInfoDialog(track, plays, onDismiss = { infoTrack = null })
    }
    tracksToAddToPlaylist?.let { selectedTracks ->
        PlaylistPickerDialog(
            tracks = selectedTracks,
            playlists = userPlaylists,
            onChoose = { playlist ->
                val selectedIds = selectedTracks.map { it.id }
                persistPlaylists(userPlaylists.map {
                    if (it.id == playlist.id) it.copy(trackIds = (it.trackIds + selectedIds).distinct()) else it
                })
                tracksToAddToPlaylist = null
                selectedLibraryTrackIds = emptySet()
                val countLabel = if (selectedIds.size == 1) "track" else "tracks"
                Toast.makeText(context, "Added ${selectedIds.size} $countLabel to ${playlist.name}", Toast.LENGTH_SHORT).show()
            },
            onCreate = {
                tracksForNewPlaylist = selectedTracks.map { it.id }
                pendingPlaylistName = ""
                showCreatePlaylist = true
                tracksToAddToPlaylist = null
            },
            onDismiss = { tracksToAddToPlaylist = null }
        )
    }
    if (showCreatePlaylist) {
        CreatePlaylistDialog(
            name = pendingPlaylistName,
            onNameChange = { pendingPlaylistName = it },
            onDismiss = {
                showCreatePlaylist = false
                tracksForNewPlaylist = emptyList()
            },
            onCreate = {
                val cleanName = pendingPlaylistName.trim()
                if (cleanName.isNotEmpty()) {
                    val playlist = UserPlaylist(
                        id = System.currentTimeMillis().toString(),
                        name = cleanName,
                        trackIds = tracksForNewPlaylist
                    )
                    persistPlaylists(userPlaylists + playlist)
                    tracksForNewPlaylist = emptyList()
                    selectedLibraryTrackIds = emptySet()
                    showCreatePlaylist = false
                }
            }
        )
    }
    playlistForActions?.let { playlist ->
        PlaylistActionsDialog(
            playlist = playlist,
            onRename = {
                renamePlaylistName = playlist.name
                playlistForRename = playlist
                playlistForActions = null
            },
            onDelete = {
                playlistForDelete = playlist
                playlistForActions = null
            },
            onDismiss = { playlistForActions = null }
        )
    }
    playlistForRename?.let { playlist ->
        RenamePlaylistDialog(
            name = renamePlaylistName,
            onNameChange = { renamePlaylistName = it },
            onSave = {
                val cleanName = renamePlaylistName.trim()
                persistPlaylists(userPlaylists.map { if (it.id == playlist.id) it.copy(name = cleanName) else it })
                playlistForRename = null
            },
            onDismiss = { playlistForRename = null }
        )
    }
    playlistForDelete?.let { playlist ->
        DeletePlaylistDialog(
            playlist = playlist,
            onConfirm = {
                persistPlaylists(userPlaylists.filterNot { it.id == playlist.id })
                playlistForDelete = null
            },
            onDismiss = { playlistForDelete = null }
        )
    }

    deleteTrack?.let { track ->
        DeleteTracksDialog(
            tracks = listOf(track),
            onConfirm = {
                deleteTrack = null
                deleteFromMediaStore(context, listOf(track), deleteLauncher) { requestOrScan() }
            },
            onDismiss = { deleteTrack = null }
        )
    }
    tracksPendingDelete?.let { selectedTracks ->
        DeleteTracksDialog(
            tracks = selectedTracks,
            onConfirm = {
                tracksPendingDelete = null
                deleteFromMediaStore(context, selectedTracks, deleteLauncher) {
                    selectedLibraryTrackIds = emptySet()
                    requestOrScan()
                }
            },
            onDismiss = { tracksPendingDelete = null }
        )
    }
}
