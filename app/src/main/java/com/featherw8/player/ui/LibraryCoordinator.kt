package com.featherw8.player.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.featherw8.player.MainActivity
import com.featherw8.player.data.MediaLibraryRepository
import com.featherw8.player.data.MusicTrack
import com.featherw8.player.ui.actions.audioPermission
import com.featherw8.player.ui.artwork.CoverWarmer
import com.featherw8.player.ui.model.AlbumCollection
import com.featherw8.player.ui.model.ArtistCollection
import com.featherw8.player.ui.model.SmartPlaylistCollections
import com.featherw8.player.ui.model.SortMode
import com.featherw8.player.ui.model.buildAlbumCollections
import com.featherw8.player.ui.model.buildArtistCollections
import com.featherw8.player.ui.model.buildSmartPlaylistCollections
import com.featherw8.player.ui.model.filterAndSortTracks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal const val KNOWN_TRACK_IDS_KEY = "known_track_ids"

internal data class LibraryCoordinatorState(
    val tracks: List<MusicTrack>,
    val permissionGranted: Boolean,
    val showScanProgress: Boolean,
    val albums: List<AlbumCollection>,
    val artists: List<ArtistCollection>,
    val smartPlaylists: SmartPlaylistCollections,
    val visibleTracks: List<MusicTrack>
)

internal data class LibraryCoordinatorActions(
    val start: () -> Unit,
    val scan: (showProgress: Boolean, manualScan: Boolean) -> Unit,
    val requestOrScan: (manualScan: Boolean) -> Unit
)

internal data class LibraryCoordinator(
    val state: LibraryCoordinatorState,
    val actions: LibraryCoordinatorActions
)

@Composable
internal fun rememberLibraryCoordinator(
    context: Context,
    repository: MediaLibraryRepository,
    preferences: android.content.SharedPreferences,
    libraryListState: LazyListState,
    selectedTrackIds: Set<String>,
    onSelectedTrackIdsChange: (Set<String>) -> Unit,
    query: String,
    sortMode: SortMode,
    minimumSizeKb: Float,
    minimumDurationSeconds: Float,
    favoriteIds: Set<String>,
    playCounts: Map<String, Int>
): LibraryCoordinator {
    val scope = rememberCoroutineScope()
    var tracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var permissionGranted by remember {
        mutableStateOf(context.checkSelfPermission(audioPermission()) == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }
    var scanning by remember { mutableStateOf(false) }
    var showScanProgress by remember { mutableStateOf(false) }
    var pendingManualScan by remember { mutableStateOf(false) }
    val latestSelectedTrackIds by rememberUpdatedState(selectedTrackIds)
    val latestOnSelectedTrackIdsChange by rememberUpdatedState(onSelectedTrackIdsChange)

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
                // Avoid rebuilding the library UI and restarting cover warming for an unchanged scan.
                if (scannedTracks != tracks) {
                    tracks = scannedTracks
                    CoverWarmer.start(scope, context.applicationContext, scannedTracks) {
                        libraryListState.isScrollInProgress
                    }
                }
                latestOnSelectedTrackIdsChange(latestSelectedTrackIds.intersect(currentIds))
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

    val latestAppEntryScan by rememberUpdatedState {
        val hasAudioPermission = context.checkSelfPermission(audioPermission()) == android.content.pm.PackageManager.PERMISSION_GRANTED
        permissionGranted = hasAudioPermission
        if (hasAudioPermission) scanLibrary(showProgress = false)
    }

    DisposableEffect(context) {
        onDispose { CoverWarmer.cancel() }
    }
    DisposableEffect(context) {
        val activity = context as? MainActivity
        val callback: () -> Unit = { latestAppEntryScan() }
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

    fun requestOrScan(manualScan: Boolean = false) {
        if (permissionGranted) {
            scanLibrary(showProgress = true, manualScan = manualScan)
        } else {
            pendingManualScan = manualScan
            permissionLauncher.launch(audioPermission())
        }
    }

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

    var smartPlaylists by remember { mutableStateOf(SmartPlaylistCollections()) }
    LaunchedEffect(tracks, favoriteIds, playCounts) {
        val trackSnapshot = tracks
        val favoritesSnapshot = favoriteIds
        val countsSnapshot = playCounts
        smartPlaylists = withContext(Dispatchers.Default) {
            buildSmartPlaylistCollections(trackSnapshot, favoritesSnapshot) { id -> countsSnapshot[id] ?: 0 }
        }
    }

    val playCountsForSorting = if (sortMode == SortMode.PLAYS) playCounts else emptyMap()
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
        val normalizedQuery = query.trim()
        val sortModeSnapshot = sortMode
        if (normalizedQuery.isNotBlank()) delay(80L)
        visibleTracks = withContext(Dispatchers.Default) {
            filterAndSortTracks(
                tracks = trackSnapshot,
                minimumSizeBytes = minimumSizeBytes,
                minimumDurationSeconds = minDurationSeconds,
                query = normalizedQuery,
                sortMode = sortModeSnapshot,
                playCounts = playCountsForSorting,
                id = MusicTrack::id,
                title = MusicTrack::title,
                artist = MusicTrack::artist,
                album = MusicTrack::album,
                mimeType = MusicTrack::mimeType,
                sizeBytes = MusicTrack::sizeBytes,
                durationMs = MusicTrack::durationMs,
                dateAddedSeconds = MusicTrack::dateAddedSeconds
            )
        }
    }

    fun start() {
        if (permissionGranted) scanLibrary(showProgress = false)
        else permissionLauncher.launch(audioPermission())
    }

    return LibraryCoordinator(
        state = LibraryCoordinatorState(
            tracks = tracks,
            permissionGranted = permissionGranted,
            showScanProgress = showScanProgress,
            albums = albums,
            artists = artists,
            smartPlaylists = smartPlaylists,
            visibleTracks = visibleTracks
        ),
        actions = LibraryCoordinatorActions(
            start = { start() },
            scan = { showProgress, manualScan -> scanLibrary(showProgress, manualScan) },
            requestOrScan = { manualScan -> requestOrScan(manualScan) }
        )
    )
}
