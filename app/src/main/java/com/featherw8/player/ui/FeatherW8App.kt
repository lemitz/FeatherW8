package com.featherw8.player.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.featherw8.player.data.MediaLibraryRepository
import com.featherw8.player.data.MusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.featherw8.player.ui.actions.deleteFromMediaStore
import com.featherw8.player.ui.actions.resolveExternalTrack
import com.featherw8.player.ui.actions.setAsRingtone
import com.featherw8.player.ui.actions.shareTrack
import com.featherw8.player.ui.actions.shareTracks
import com.featherw8.player.ui.artwork.CustomArtworkStore
import com.featherw8.player.ui.components.AppDialogsHost
import com.featherw8.player.ui.components.AppToolbar
import com.featherw8.player.ui.components.LibrarySelectionActionBar
import com.featherw8.player.ui.model.SortMode
import com.featherw8.player.ui.model.TrackAction
import com.featherw8.player.ui.screens.AppPageContent
import com.featherw8.player.ui.screens.AppPageContentActions
import com.featherw8.player.ui.screens.AppPageContentState
import com.featherw8.player.ui.screens.snapToStep
import com.featherw8.player.ui.theme.AmoledBlack
import com.featherw8.player.ui.theme.DeviceProfile
import com.featherw8.player.ui.theme.MiniPlayerContentOverlap
import com.featherw8.player.ui.theme.navColors
import com.featherw8.player.ui.player.PlayerOverlays
import com.featherw8.player.ui.player.rememberMediaController

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

    var selectedLibraryTrackIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val playlistCoordinator = rememberPlaylistCoordinator(
        context = context,
        preferences = preferences,
        onClearLibrarySelection = { selectedLibraryTrackIds = emptySet() }
    )
    val playlistState = playlistCoordinator.state
    val playlistActions = playlistCoordinator.actions
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
    var playerOpen by rememberSaveable { mutableStateOf(false) }
    var playerClosedBySwipe by remember { mutableStateOf(false) }
    var miniPlayerDragActive by remember { mutableStateOf(false) }
    val navigation = rememberNavigationCoordinator(
        onQueryReset = { query = "" },
        selectedLibraryTrackIds = selectedLibraryTrackIds,
        onClearLibrarySelection = { selectedLibraryTrackIds = emptySet() },
        playerOpen = playerOpen,
        onBackFromPlayer = {
            playerClosedBySwipe = false
            playerOpen = false
        }
    )
    var infoTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var deleteTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var tracksPendingDelete by remember { mutableStateOf<List<MusicTrack>?>(null) }
    var artworkActionTrack by remember { mutableStateOf<MusicTrack?>(null) }
    val playbackCoordinator = rememberPlaybackCoordinator(
        context = context,
        controller = controller,
        scope = scope,
        onPlaybackStarted = {
            navigation.resetRootBackConfirmation()
            playerClosedBySwipe = false
            playerOpen = true
        },
        onDismissed = {
            playerOpen = false
            miniPlayerDragActive = false
            navigation.resetRootBackConfirmation()
        },
        onSelectionCleared = { selectedLibraryTrackIds = emptySet() }
    )
    val libraryCoordinator = rememberLibraryCoordinator(
        context = context,
        repository = repository,
        preferences = preferences,
        libraryListState = navigation.libraryListState,
        selectedTrackIds = selectedLibraryTrackIds,
        onSelectedTrackIdsChange = { selectedLibraryTrackIds = it },
        query = query,
        sortMode = sortMode,
        minimumSizeKb = minimumSizeKb,
        minimumDurationSeconds = minimumDurationSeconds,
        favoriteIds = playlistState.favoriteIds,
        playCounts = playbackCoordinator.playCountValues
    )
    val libraryState = libraryCoordinator.state
    val tracks = libraryState.tracks
    playbackCoordinator.updateTracks(tracks)
    PlaybackCoordinatorEffects(playbackCoordinator, tracks)
    val permissionGranted = libraryState.permissionGranted
    val showScanProgress = libraryState.showScanProgress
    val albums = libraryState.albums
    val artists = libraryState.artists
    val smartPlaylists = libraryState.smartPlaylists
    val visibleTracks = libraryState.visibleTracks
    fun scanLibrary(showProgress: Boolean = false, manualScan: Boolean = false) {
        libraryCoordinator.actions.scan(showProgress, manualScan)
    }

    fun requestOrScan(manualScan: Boolean = false) {
        libraryCoordinator.actions.requestOrScan(manualScan)
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

    fun playTracks(items: List<MusicTrack>, startIndex: Int) = playbackCoordinator.playTracks(items, startIndex)

    fun addTracksToQueue(selectedTracks: List<MusicTrack>) = playbackCoordinator.addTracksToQueue(selectedTracks)

    fun dismissMiniPlayer() = playbackCoordinator.dismissMiniPlayer()

    fun moveQueueTrack(from: Int, to: Int) = playbackCoordinator.moveQueueTrack(from, to)

    fun removeQueueTrack(index: Int) = playbackCoordinator.removeQueueTrack(index)

    fun handleTrackAction(track: MusicTrack, action: TrackAction) {
        when (action) {
            TrackAction.ADD_TO_PLAYLIST -> playlistActions.setTracksToAddToPlaylist(listOf(track))
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
        libraryCoordinator.actions.start()
    }

    LaunchedEffect(playerClosedBySwipe) {
        if (playerClosedBySwipe) {
            delay(48L)
            playerClosedBySwipe = false
        }
    }

    val queue = playbackCoordinator.queue
    val playbackSnapshot = playbackCoordinator.playbackSnapshot
    val playCountRevision = playbackCoordinator.playCountRevision
    val currentTrack = playbackCoordinator.currentTrack

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
                navigation.resetRootBackConfirmation()
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
    val playCountValues = playbackCoordinator.playCountValues
    val playCountReader: (String) -> Int = remember(playCountValues) {
        { id -> playCountValues[id] ?: 0 }
    }
    val currentlyPlaying: (String) -> Boolean = remember(activeMediaId, isActuallyPlaying) {
        { id -> id == activeMediaId && isActuallyPlaying }
    }
    val selectedLibraryTracks = visibleTracks.filter { it.id in selectedLibraryTrackIds }

    fun toggleLibraryTrackSelection(track: MusicTrack) {
        selectedLibraryTrackIds = if (track.id in selectedLibraryTrackIds) {
            selectedLibraryTrackIds - track.id
        } else {
            selectedLibraryTrackIds + track.id
        }
        navigation.resetRootBackConfirmation()
    }

    fun selectAllVisibleLibraryTracks() {
        val visibleIds = visibleTracks.mapTo(linkedSetOf()) { it.id }
        selectedLibraryTrackIds = if (visibleIds.isNotEmpty() && visibleIds.all { it in selectedLibraryTrackIds }) {
            emptySet()
        } else {
            visibleIds
        }
        navigation.resetRootBackConfirmation()
    }

    val selectedAlbum = albums.firstOrNull { it.key == navigation.selectedAlbumKey }
    val selectedArtist = artists.firstOrNull { it.key == navigation.selectedArtistKey }
    val resolvedPlaylistDetail = playlistActions.resolvePlaylistDetail(tracks)

    fun openPlaylistDetail(title: String, items: List<MusicTrack>, userPlaylistId: String? = null) {
        playlistActions.openPlaylistDetail(title, items, userPlaylistId)
        navigation.openPlaylistDetail()
    }

    Box(Modifier.fillMaxSize().background(AmoledBlack)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = AmoledBlack,
            bottomBar = {
                if (navigation.page == AppPage.LIBRARY && selectedLibraryTrackIds.isNotEmpty()) {
                    LibrarySelectionActionBar(
                        onDelete = { if (selectedLibraryTracks.isNotEmpty()) tracksPendingDelete = selectedLibraryTracks },
                        onAddToPlaylist = { if (selectedLibraryTracks.isNotEmpty()) playlistActions.setTracksToAddToPlaylist(selectedLibraryTracks) },
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
                                selected = navigation.page == AppPage.LIBRARY,
                                onClick = { navigation.openLibrary() },
                                icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = null) },
                                label = { Text("Library") },
                                colors = navColors()
                            )
                            NavigationBarItem(
                                selected = navigation.page in setOf(
                                    AppPage.PLAYLISTS, AppPage.ALBUMS, AppPage.ARTISTS,
                                    AppPage.ALBUM_DETAIL, AppPage.ARTIST_DETAIL, AppPage.PLAYLIST_DETAIL
                                ),
                                onClick = { navigation.openPlaylists() },
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
                if (navigation.page != AppPage.LIBRARY || selectedLibraryTrackIds.isEmpty()) {
                    AppToolbar(
                        page = navigation.page,
                        title = when (navigation.page) {
                            AppPage.SETTINGS -> "Settings"
                            AppPage.ALBUMS -> "Albums"
                            AppPage.ARTISTS -> "Artists"
                            AppPage.ALBUM_DETAIL -> selectedAlbum?.title ?: "Album"
                            AppPage.ARTIST_DETAIL -> selectedArtist?.name ?: "Artist"
                            AppPage.PLAYLIST_DETAIL -> resolvedPlaylistDetail?.title ?: "Playlist"
                            else -> null
                        },
                        onSettings = { navigation.openSettings() },
                        onBack = { navigation.goBackFromPage() }
                    )
                }
                AppPageContent(
                    state = AppPageContentState(
                        page = navigation.page,
                        tabPagerState = navigation.tabPagerState,
                        userScrollEnabled = !playerOpen && !playerClosedBySwipe && !miniPlayerDragActive && selectedLibraryTrackIds.isEmpty(),
                        visibleTracks = visibleTracks,
                        libraryListState = navigation.libraryListState,
                        query = query,
                        sortMode = sortMode,
                        permissionGranted = permissionGranted,
                        playCount = playCountReader,
                        isCurrentlyPlaying = currentlyPlaying,
                        bottomOverlayPadding = bottomOverlayPadding,
                        selectedLibraryTrackIds = selectedLibraryTrackIds,
                        playlistsListState = navigation.playlistsListState,
                        smartPlaylists = smartPlaylists,
                        playlists = playlistState.playlists,
                        albums = albums,
                        artists = artists,
                        profile = profile,
                        minimumSizeKb = minimumSizeKb,
                        minimumDurationSeconds = minimumDurationSeconds,
                        dotFontEnabled = dotFontEnabled,
                        accentId = accentId,
                        albumsGridState = navigation.albumsGridState,
                        artistsGridState = navigation.artistsGridState,
                        selectedAlbumKey = navigation.selectedAlbumKey,
                        selectedAlbum = selectedAlbum,
                        selectedArtistKey = navigation.selectedArtistKey,
                        selectedArtist = selectedArtist,
                        resolvedPlaylistDetail = resolvedPlaylistDetail,
                        detailListState = navigation.detailListState
                    ),
                    actions = AppPageContentActions(
                        onQueryChange = { query = it },
                        onSortChange = {
                            sortMode = it
                            preferences.edit().putString("sort_mode", it.name).apply()
                        },
                        onPlayTracks = { items, index -> playTracks(items, index) },
                        onTrackAction = { track, action -> handleTrackAction(track, action) },
                        onScan = { requestOrScan() },
                        onManualScan = { requestOrScan(manualScan = true) },
                        onLongPressTrack = { track -> toggleLibraryTrackSelection(track) },
                        onToggleSelection = { track -> toggleLibraryTrackSelection(track) },
                        onCancelSelection = {
                            selectedLibraryTrackIds = emptySet()
                            navigation.resetRootBackConfirmation()
                        },
                        onSelectAll = { selectAllVisibleLibraryTracks() },
                        onOpenAlbums = { navigation.openAlbums() },
                        onOpenArtists = { navigation.openArtists() },
                        onOpenSmartPlaylist = { title, items -> openPlaylistDetail(title, items) },
                        onCreatePlaylist = { playlistActions.showCreatePlaylist() },
                        onManagePlaylist = { playlistActions.managePlaylist(it) },
                        onOpenUserPlaylist = { playlist ->
                            playlistActions.openUserPlaylist(playlist, tracks)
                            navigation.openPlaylistDetail()
                        },
                        onMinimumSizeChange = {
                            minimumSizeKb = it
                            preferences.edit().putFloat("minimum_size_kb", it).remove("minimum_size_mb").apply()
                        },
                        onMinimumDurationChange = {
                            minimumDurationSeconds = it
                            preferences.edit().putFloat("minimum_duration_seconds", it).apply()
                        },
                        onDotFontChange = onDotFontChange,
                        onAccentChange = onAccentChange,
                        onOpenAlbum = { album -> navigation.openAlbum(album) },
                        onOpenArtist = { artist -> navigation.openArtist(artist) },
                        onManageDetailPlaylist = { id ->
                            playlistState.playlists.firstOrNull { it.id == id }?.let { playlistActions.managePlaylist(it) }
                        },
                        onMoveDetailTrack = { playlistId, shown, from, to ->
                            playlistActions.moveInPlaylist(playlistId, shown, from, to)
                        },
                        onRemoveDetailTrack = { playlistId, shown, index ->
                            shown.getOrNull(index)?.let { playlistActions.removeFromPlaylist(playlistId, it.id) }
                        }
                    ),
                    pagerModifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }

        PlayerOverlays(
            currentTrack = currentTrack,
            snapshot = playbackSnapshot,
            controller = controller,
            queue = queue,
            favoriteIds = playlistState.favoriteIds,
            playerOpen = playerOpen,
            playerClosedBySwipe = playerClosedBySwipe,
            onOpenPlayer = {
                selectedLibraryTrackIds = emptySet()
                miniPlayerDragActive = false
                playerClosedBySwipe = false
                navigation.resetRootBackConfirmation()
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
            onClearQueue = { activeTrack -> playbackCoordinator.clearQueueAndPlay(activeTrack) },
            onArtworkClick = { artworkActionTrack = it },
            onTrackInfo = { infoTrack = it },
            onToggleFavorite = { activeTrack -> playlistActions.toggleFavorite(activeTrack.id) },
            onTrackAction = { track, action -> handleTrackAction(track, action) }
        )
    }

    AppDialogsHost(
        showScanProgress = showScanProgress,
        artworkTrack = artworkActionTrack,
        hasCustomArtwork = artworkActionTrack?.let { CustomArtworkStore.get(it.id) != null } ?: false,
        infoTrack = infoTrack,
        infoPlayCount = infoTrack?.let { track ->
            remember(track.id, playCountRevision) { playbackCoordinator.playCount(track.id) }
        } ?: 0,
        tracksToAddToPlaylist = playlistState.tracksToAddToPlaylist,
        playlists = playlistState.playlists,
        showCreatePlaylist = playlistState.showCreatePlaylist,
        pendingPlaylistName = playlistState.pendingPlaylistName,
        playlistForActions = playlistState.playlistForActions,
        playlistForRename = playlistState.playlistForRename,
        renamePlaylistName = playlistState.renamePlaylistName,
        playlistForDelete = playlistState.playlistForDelete,
        deleteTrack = deleteTrack,
        tracksPendingDelete = tracksPendingDelete,
        onChooseArtwork = { artworkPickerLauncher.launch(arrayOf("image/*")) },
        onUseOriginalArtwork = {
            artworkActionTrack?.let { CustomArtworkStore.remove(context, it.id) }
            artworkActionTrack = null
        },
        onDismissArtwork = { artworkActionTrack = null },
        onDismissInfo = { infoTrack = null },
        onChoosePlaylist = { selectedTracks, playlist -> playlistActions.choosePlaylist(selectedTracks, playlist) },
        onCreatePlaylistFromPicker = { playlistActions.createPlaylistFromPicker(it) },
        onDismissPlaylistPicker = { playlistActions.dismissPlaylistPicker() },
        onPendingPlaylistNameChange = { playlistActions.changePendingPlaylistName(it) },
        onDismissCreatePlaylist = { playlistActions.dismissCreatePlaylist() },
        onCreatePlaylist = { playlistActions.createPlaylist() },
        onRenamePlaylist = { playlistActions.renamePlaylist(it) },
        onDeletePlaylist = { playlistActions.deletePlaylist(it) },
        onDismissPlaylistActions = { playlistActions.dismissPlaylistActions() },
        onRenameNameChange = { playlistActions.changeRenameName(it) },
        onSaveRename = { playlistActions.saveRename(it) },
        onDismissRename = { playlistActions.dismissRename() },
        onConfirmDeletePlaylist = { playlistActions.confirmDeletePlaylist(it) },
        onDismissDeletePlaylist = { playlistActions.dismissDeletePlaylist() },
        onConfirmDeleteTrack = { track ->
            deleteTrack = null
            deleteFromMediaStore(context, listOf(track), deleteLauncher) { requestOrScan() }
        },
        onDismissDeleteTrack = { deleteTrack = null },
        onConfirmDeleteSelectedTracks = { selectedTracks ->
            tracksPendingDelete = null
            deleteFromMediaStore(context, selectedTracks, deleteLauncher) {
                selectedLibraryTrackIds = emptySet()
                requestOrScan()
            }
        },
        onDismissDeleteSelectedTracks = { tracksPendingDelete = null }
    )
}
