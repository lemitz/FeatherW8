package com.featherw8.player

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.LruCache
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.MutableTransitionState
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.featherw8.player.data.MediaLibraryRepository
import com.featherw8.player.data.MusicTrack
import com.featherw8.player.data.PlayCountStore
import com.featherw8.player.playback.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executor
import kotlin.math.roundToInt

private val AmoledBlack = Color(0xFF000000)
private val SurfaceBlack = Color(0xFF09090B)
private val CardBlack = Color(0xFF111114)
private val DefaultAccent = Color(0xFFF23B50)
private val MiniPlayerContentOverlap = 64.dp
private val LocalFeatherAccent = staticCompositionLocalOf { DefaultAccent }
private val FeatherRed: Color
    @Composable get() = LocalFeatherAccent.current
private val PrimaryText = Color(0xFFF5F5F6)
private val MutedText = Color(0xFFA3A2A5)
private data class AccentOption(val id: String, val label: String, val color: Color)
private val AccentOptions = listOf(
    AccentOption("red", "Crimson", Color(0xFFF23B50)),
    AccentOption("violet", "Violet", Color(0xFFAE80FF)),
    AccentOption("cyan", "Ice blue", Color(0xFF4CC9F0)),
    AccentOption("mint", "Mint", Color(0xFF3DD6A4)),
    AccentOption("amber", "Amber", Color(0xFFFFB84D)),
    AccentOption("rose", "Rose", Color(0xFFFF6FAE))
)
private fun accentFor(id: String): Color = AccentOptions.firstOrNull { it.id == id }?.color ?: DefaultAccent
private const val KNOWN_TRACK_IDS_KEY = "known_track_ids"
private object ArtworkLoader {
    private val bitmapCache = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.allocationByteCount / 1024).coerceAtLeast(1)
    }
    private val pendingLoads = mutableMapOf<String, Deferred<Bitmap?>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val decodeSemaphore = Semaphore(2)

    suspend fun load(context: Context, uri: Uri, targetPixels: Int, cacheKey: String): Bitmap? {
        bitmapCache.get(cacheKey)?.let { return it }
        val load = synchronized(pendingLoads) {
            bitmapCache.get(cacheKey)?.let { return it }
            pendingLoads[cacheKey] ?: scope.async(start = CoroutineStart.LAZY) {
                decodeSemaphore.withPermit {
                    bitmapCache.get(cacheKey) ?: runCatching {
                        decodeSampledArtwork(context, uri, targetPixels)
                    }.getOrNull()?.also { bitmapCache.put(cacheKey, it) }
                }
            }.also { deferred ->
                pendingLoads[cacheKey] = deferred
                deferred.invokeOnCompletion {
                    synchronized(pendingLoads) {
                        if (pendingLoads[cacheKey] === deferred) pendingLoads.remove(cacheKey)
                    }
                }
                deferred.start()
            }
        }
        return load.await()
    }
}

private object CustomArtworkStore {
    private const val PREFIX = "custom_cover_uri_"
    var covers by mutableStateOf<Map<String, Uri>>(emptyMap())
        private set

    fun load(context: Context) {
        val preferences = context.getSharedPreferences("featherw8", Context.MODE_PRIVATE)
        covers = preferences.all.mapNotNull { (key, value) ->
            if (!key.startsWith(PREFIX)) return@mapNotNull null
            val uri = (value as? String)?.let(Uri::parse) ?: return@mapNotNull null
            key.removePrefix(PREFIX) to uri
        }.toMap()
    }

    fun get(trackId: String): Uri? = covers[trackId]

    fun put(context: Context, trackId: String, uri: Uri) {
        covers = covers + (trackId to uri)
        context.getSharedPreferences("featherw8", Context.MODE_PRIVATE).edit()
            .putString(PREFIX + trackId, uri.toString()).apply()
    }

    fun remove(context: Context, trackId: String) {
        covers = covers - trackId
        context.getSharedPreferences("featherw8", Context.MODE_PRIVATE).edit()
            .remove(PREFIX + trackId).apply()
    }
}

private fun buildAlbumCollections(tracks: List<MusicTrack>): List<AlbumCollection> = tracks
    .groupBy { track ->
        val title = track.album.trim().ifBlank { "Unknown Album" }.lowercase()
        val artist = track.artist.trim().ifBlank { "Unknown Artist" }.lowercase()
        if (track.albumId >= 0L) "${track.albumId}|$artist|$title" else "$artist|$title"
    }
    .map { (key, albumTracks) ->
        val sortedTracks = albumTracks.sortedBy { it.title.lowercase() }
        AlbumCollection(key, sortedTracks.first().album, sortedTracks.first().artist, sortedTracks)
    }
    .sortedBy { it.title.lowercase() }

private fun buildArtistCollections(tracks: List<MusicTrack>): List<ArtistCollection> = tracks
    .groupBy { it.artist.trim().ifBlank { "Unknown Artist" }.lowercase() }
    .map { (key, artistTracks) -> ArtistCollection(key, artistTracks.first().artist, artistTracks.sortedBy { it.title.lowercase() }) }
    .sortedBy { it.name.lowercase() }

private fun buildSmartPlaylistCollections(
    tracks: List<MusicTrack>,
    favorites: Set<String>,
    playCount: (String) -> Int
): SmartPlaylistCollections {
    val mostPlayed = tracks.mapNotNull { track ->
        val count = playCount(track.id)
        if (count > 0) track to count else null
    }.sortedByDescending { it.second }.take(10).map { it.first }

    return SmartPlaylistCollections(
        recent = tracks.sortedByDescending { it.dateAddedSeconds }.take(10),
        highResolution = tracks.filter { it.isLikelyHiRes },
        mostPlayed = mostPlayed,
        favorites = tracks.filter { it.id in favorites }
    )
}

private const val PlayerNowPlayingPage = 0
private const val PlayerQueuePage = 1

private enum class DeviceProfile { NOTHING, REALME, GENERIC }
private enum class AppPage {
    LIBRARY, PLAYLISTS, SETTINGS,
    ALBUMS, ARTISTS, ALBUM_DETAIL, ARTIST_DETAIL, PLAYLIST_DETAIL,

    // Kept only so instance state saved by an older build (which used a separate
    // queue page) can still be restored. Nothing navigates here any more - the queue
    // now lives on the second page of the player pager.
    QUEUE
}
private data class AlbumCollection(val key: String, val title: String, val artist: String, val tracks: List<MusicTrack>)
private data class ArtistCollection(val key: String, val name: String, val tracks: List<MusicTrack>)
private data class PlaylistDetailState(val title: String, val tracks: List<MusicTrack>, val userPlaylistId: String? = null)
private enum class SortMode(val label: String) {
    TITLE("Track title"),
    ADDED("Date added"),
    PLAYS("Play count");

    companion object {
        fun from(value: String?): SortMode = entries.firstOrNull { it.name == value } ?: TITLE
    }
}
private enum class TrackAction { ADD_TO_PLAYLIST, INFO, RINGTONE, SHARE, DELETE }
private data class UserPlaylist(val id: String, val name: String, val trackIds: List<String>)
private data class SmartPlaylistCollections(
    val recent: List<MusicTrack> = emptyList(),
    val highResolution: List<MusicTrack> = emptyList(),
    val mostPlayed: List<MusicTrack> = emptyList(),
    val favorites: List<MusicTrack> = emptyList()
)
private data class PlaybackSnapshot(
    val mediaId: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playing: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleModeEnabled: Boolean = false
)

class MainActivity : ComponentActivity() {
    var onAppEntry: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CustomArtworkStore.load(this)
        request120HzIfAvailable()
        setContent {
            val profile = remember { detectDeviceProfile() }
            val preferences = remember { getSharedPreferences("featherw8", Context.MODE_PRIVATE) }
            var dotFontEnabled by rememberSaveable {
                mutableStateOf(profile == DeviceProfile.NOTHING && preferences.getBoolean("nothing_dot_font_enabled", true))
            }
            var accentId by rememberSaveable {
                val saved = preferences.getString("accent_color", "red") ?: "red"
                mutableStateOf(AccentOptions.firstOrNull { it.id == saved }?.id ?: "red")
            }
            FeatherW8Theme(profile, dotFontEnabled, accentId) {
                FeatherW8App(
                    profile = profile,
                    dotFontEnabled = dotFontEnabled,
                    onDotFontChange = { enabled ->
                        dotFontEnabled = enabled
                        preferences.edit().putBoolean("nothing_dot_font_enabled", enabled).apply()
                    },
                    accentId = accentId,
                    onAccentChange = { id ->
                        accentId = id
                        preferences.edit().putString("accent_color", id).apply()
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        onAppEntry?.invoke()
    }

    override fun onResume() {
        super.onResume()
        window.decorView.post { request120HzIfAvailable(nothingOnly = true) }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) window.decorView.post { request120HzIfAvailable(nothingOnly = true) }
    }

    @Suppress("DEPRECATION")
    private fun request120HzIfAvailable(nothingOnly: Boolean = false) {
        if (nothingOnly && detectDeviceProfile() != DeviceProfile.NOTHING) return
        val display = windowManager.defaultDisplay
        val currentMode = display.mode
        val sameResolutionModes = display.supportedModes.filter {
            it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight
        }
        val preferredMode = sameResolutionModes
            .filter { it.refreshRate >= 90f }
            .minByOrNull { kotlin.math.abs(it.refreshRate - 120f) }
        val attributes = window.attributes
        attributes.preferredRefreshRate = preferredMode?.refreshRate ?: 120f
        if (preferredMode != null) attributes.preferredDisplayModeId = preferredMode.modeId
        window.attributes = attributes
    }
}

private fun detectDeviceProfile(): DeviceProfile {
    val device = "${Build.MANUFACTURER} ${Build.BRAND} ${Build.MODEL}".lowercase()
    return when {
        "nothing" in device || "a142" in device || "phone 3a" in device -> DeviceProfile.NOTHING
        "realme" in device || "rmx3853" in device || "gt 6t" in device -> DeviceProfile.REALME
        else -> DeviceProfile.GENERIC
    }
}

@Composable
private fun FeatherW8Theme(
    profile: DeviceProfile,
    dotFontEnabled: Boolean,
    accentId: String,
    content: @Composable () -> Unit
) {
    val accent = remember(accentId) { accentFor(accentId) }
    val typeface = remember(profile, dotFontEnabled) {
        if (profile == DeviceProfile.NOTHING && dotFontEnabled) Typeface.create("NDot55", Typeface.NORMAL) else Typeface.DEFAULT
    }
    val family = remember(typeface) { FontFamily(typeface) }
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = Color.White,
        secondary = accent.copy(alpha = .84f),
        background = AmoledBlack,
        onBackground = PrimaryText,
        surface = SurfaceBlack,
        onSurface = PrimaryText,
        surfaceVariant = CardBlack,
        onSurfaceVariant = MutedText,
        error = Color(0xFFFF5268)
    )
    MaterialTheme(colorScheme = scheme) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalFeatherAccent provides accent,
            androidx.compose.material3.LocalContentColor provides PrimaryText,
            androidx.compose.material3.LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(fontFamily = family),
            content = content
        )
    }
}

@Composable
private fun rememberMediaController(): MediaController? {
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
private fun FeatherW8App(
    profile: DeviceProfile,
    dotFontEnabled: Boolean,
    onDotFontChange: (Boolean) -> Unit,
    accentId: String,
    onAccentChange: (String) -> Unit
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
        if (targetPage != null && tabPagerState.settledPage != targetPage) {
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
                tracks = scannedTracks
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
        queue = items
        val mediaItems = items.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setArtworkUri(CustomArtworkStore.get(track.id) ?: track.albumArtUri())
                        .build()
                )
                .build()
        }
        activePlayer.setMediaItems(mediaItems, startIndex.coerceIn(items.indices), 0L)
        activePlayer.prepare()
        activePlayer.play()
        lastRootBackAt = 0L
        playerClosedBySwipe = false
        playerOpen = true
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
        val mediaItems = additions.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setArtworkUri(CustomArtworkStore.get(track.id) ?: track.albumArtUri())
                        .build()
                )
                .build()
        }
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

    fun handleTrackAction(track: MusicTrack, action: TrackAction) {
        when (action) {
            TrackAction.ADD_TO_PLAYLIST -> tracksToAddToPlaylist = listOf(track)
            TrackAction.INFO -> infoTrack = track
            TrackAction.RINGTONE -> setAsRingtone(context, track)
            TrackAction.SHARE -> shareTrack(context, track)
            TrackAction.DELETE -> deleteTrack = track
        }
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
    val bottomOverlayPadding = if (currentTrack != null) MiniPlayerContentOverlap else 0.dp
    val activeMediaId = playbackSnapshot.mediaId
    val isActuallyPlaying = playbackSnapshot.playing
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
                    (minDurationSeconds <= 0f ||
                        (track.durationMs > 0L && track.durationMs / 1_000L >= minimumWholeSeconds)) &&
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
                AppPage.ALBUM_DETAIL, AppPage.ARTIST_DETAIL, AppPage.PLAYLIST_DETAIL -> goBackFromPage()
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
                                tracks = tracks,
                                listState = playlistsListState,
                                playlists = userPlaylists,
                                favorites = favoriteIds,
                                playCount = playCountReader,
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
                                }
                            )
                        } ?: EmptyCard("Playlist unavailable", "It may have been removed.")
                    }
                }
            }
        }

        currentTrack?.let { miniTrack ->
            MiniPlayerBar(
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 80.dp),
                track = miniTrack,
                playing = playbackSnapshot.playing,
                durationMs = playbackSnapshot.durationMs,
                initialPositionMs = playbackSnapshot.positionMs,
                controller = controller,
                isVisible = !playerOpen,
                onOpen = {
                    selectedLibraryTrackIds = emptySet()
                    miniPlayerDragActive = false
                    playerClosedBySwipe = false
                    lastRootBackAt = 0L
                    playerOpen = true
                },
                onPlayPause = { if (playbackSnapshot.playing) controller?.pause() else controller?.play() },
                onDismiss = { dismissMiniPlayer() },
                onDragActiveChange = { miniPlayerDragActive = it }
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
                    currentId = playbackSnapshot.mediaId,
                    isFavorite = activeTrack.id in favoriteIds,
                    snapshot = playbackSnapshot,
                    controller = controller,
                    onClose = {
                        playerClosedBySwipe = false
                        playerOpen = false
                    },
                    onSwipeClose = {
                        playerClosedBySwipe = true
                        playerOpen = false
                    },
                    onPlayQueueTrack = { index -> playTracks(queue, index) },
                    onClearQueue = {
                        queue = listOf(activeTrack)
                        controller?.clearMediaItems()
                        playTracks(listOf(activeTrack), 0)
                    },
                    onArtworkClick = { artworkActionTrack = it },
                    onTrackInfo = { infoTrack = it },
                    onToggleFavorite = {
                        val next = favoriteIds.toMutableSet().apply {
                            if (activeTrack.id in this) remove(activeTrack.id) else add(activeTrack.id)
                        }
                        persistFavorites(next)
                    },
                    onTrackAction = { handleTrackAction(activeTrack, it) }
                )
            }
        }
    }

    if (showScanProgress) ScanDialog()
    artworkActionTrack?.let { track ->
        AlertDialog(
            onDismissRequest = { artworkActionTrack = null },
            title = { Text("Change cover") },
            text = {
                Column {
                    Text("Choose an image file to use as this track's cover.", color = MutedText)
                    TextButton(
                        onClick = { artworkPickerLauncher.launch(arrayOf("image/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Choose image") }
                    if (CustomArtworkStore.get(track.id) != null) {
                        TextButton(
                            onClick = {
                                CustomArtworkStore.remove(context, track.id)
                                artworkActionTrack = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Use original cover", color = MutedText) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { artworkActionTrack = null }) { Text("Cancel") } },
            containerColor = CardBlack
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
        AlertDialog(
            onDismissRequest = { playlistForActions = null },
            title = { Text(playlist.name) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            renamePlaylistName = playlist.name
                            playlistForRename = playlist
                            playlistForActions = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Rename playlist") }
                    TextButton(
                        onClick = {
                            playlistForDelete = playlist
                            playlistForActions = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Delete playlist", color = FeatherRed) }
                }
            },
            confirmButton = { TextButton(onClick = { playlistForActions = null }) { Text("Cancel") } },
            containerColor = CardBlack
        )
    }
    playlistForRename?.let { playlist ->
        AlertDialog(
            onDismissRequest = { playlistForRename = null },
            title = { Text("Rename playlist") },
            text = {
                OutlinedTextField(
                    value = renamePlaylistName,
                    onValueChange = { renamePlaylistName = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = renamePlaylistName.trim().isNotEmpty(),
                    onClick = {
                        val cleanName = renamePlaylistName.trim()
                        persistPlaylists(userPlaylists.map {
                            if (it.id == playlist.id) it.copy(name = cleanName) else it
                        })
                        playlistForRename = null
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { playlistForRename = null }) { Text("Cancel") } },
            containerColor = CardBlack
        )
    }
    playlistForDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { playlistForDelete = null },
            title = { Text("Delete playlist?") },
            text = { Text("Delete “${playlist.name}”? Its tracks will remain in your library.") },
            confirmButton = {
                TextButton(onClick = {
                    persistPlaylists(userPlaylists.filterNot { it.id == playlist.id })
                    playlistForDelete = null
                }) { Text("Delete", color = FeatherRed) }
            },
            dismissButton = { TextButton(onClick = { playlistForDelete = null }) { Text("Cancel") } },
            containerColor = CardBlack
        )
    }

    deleteTrack?.let { track ->
        AlertDialog(
            onDismissRequest = { deleteTrack = null },
            title = { Text("Delete file?") },
            text = { Text(track.title, fontFamily = FontFamily.SansSerif) },
            confirmButton = {
                TextButton(onClick = {
                    deleteTrack = null
                    deleteFromMediaStore(context, listOf(track), deleteLauncher) { requestOrScan() }
                }) { Text("Delete file", color = FeatherRed) }
            },
            dismissButton = { TextButton(onClick = { deleteTrack = null }) { Text("Cancel") } },
            containerColor = CardBlack
        )
    }
    tracksPendingDelete?.let { selectedTracks ->
        AlertDialog(
            onDismissRequest = { tracksPendingDelete = null },
            title = { Text(if (selectedTracks.size == 1) "Delete file?" else "Delete files?") },
            text = {
                Text(
                    if (selectedTracks.size == 1) selectedTracks.first().title
                    else "Delete ${selectedTracks.size} selected audio files? This can't be undone.",
                    fontFamily = FontFamily.SansSerif
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    tracksPendingDelete = null
                    deleteFromMediaStore(context, selectedTracks, deleteLauncher) {
                        selectedLibraryTrackIds = emptySet()
                        requestOrScan()
                    }
                }) { Text(if (selectedTracks.size == 1) "Delete file" else "Delete files", color = FeatherRed) }
            },
            dismissButton = { TextButton(onClick = { tracksPendingDelete = null }) { Text("Cancel") } },
            containerColor = CardBlack
        )
    }
}

private fun audioPermission(): String = if (Build.VERSION.SDK_INT >= 33) {
    Manifest.permission.READ_MEDIA_AUDIO
} else {
    Manifest.permission.READ_EXTERNAL_STORAGE
}

@Composable
private fun AppToolbar(page: AppPage, title: String?, onSettings: () -> Unit, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (page == AppPage.LIBRARY || page == AppPage.PLAYLISTS) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ImageIconLogo()
                Text("FeatherW8", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text(title.orEmpty(), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.size(48.dp))
        }
    }
}

@Composable
private fun ImageIconLogo() {
    androidx.compose.foundation.Image(
        painter = painterResource(R.drawable.featherw8_icon),
        contentDescription = "FeatherW8",
        modifier = Modifier.size(30.dp).clip(CircleShape)
    )
}

@Composable
private fun LibraryScreen(
    tracks: List<MusicTrack>,
    listState: LazyListState,
    query: String,
    onQueryChange: (String) -> Unit,
    sortMode: SortMode,
    onSortChange: (SortMode) -> Unit,
    onPlay: (Int) -> Unit,
    onTrackAction: (MusicTrack, TrackAction) -> Unit,
    onScan: () -> Unit,
    permissionGranted: Boolean,
    playCount: (String) -> Int,
    isCurrentlyPlaying: (String) -> Boolean,
    bottomOverlayPadding: Dp = 0.dp,
    selectedTrackIds: Set<String> = emptySet(),
    onLongPressTrack: (MusicTrack) -> Unit = {},
    onToggleSelection: (MusicTrack) -> Unit = {},
    onCancelSelection: () -> Unit = {},
    onSelectAll: () -> Unit = {}
) {
    var sortExpanded by remember { mutableStateOf(false) }
    val selectionMode = selectedTrackIds.isNotEmpty()
    val allVisibleSelected = tracks.isNotEmpty() && tracks.all { it.id in selectedTrackIds }
    Column(Modifier.fillMaxSize()) {
        if (selectionMode) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, top = 14.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancelSelection, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 6.dp)) {
                    Text("Cancel", color = FeatherRed, fontSize = 15.sp)
                }
                TextButton(onClick = onSelectAll, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 6.dp)) {
                    Text(if (allVisibleSelected) "Deselect All" else "Select All", color = FeatherRed, fontSize = 15.sp)
                }
            }
            Text(
                "${selectedTrackIds.size} selected",
                modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 12.dp),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text("Library", modifier = Modifier.padding(start = 20.dp, top = 9.dp, bottom = 11.dp), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = MutedText, modifier = Modifier.padding(start = 12.dp).size(19.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = PrimaryText),
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 13.dp),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) Text("Search tracks, artists or albums", color = MutedText, fontSize = 12.sp)
                            inner()
                        }
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${tracks.size} tracks", color = MutedText, fontSize = 11.sp)
                Box {
                    IconButton(onClick = { sortExpanded = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort tracks", tint = PrimaryText, modifier = Modifier.size(19.dp))
                    }
                    DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }, containerColor = CardBlack) {
                        SortMode.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = { sortExpanded = false; onSortChange(option) },
                                trailingIcon = { if (sortMode == option) Text("✓", color = FeatherRed, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }
        }
        if (tracks.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.LibraryMusic, contentDescription = null, tint = MutedText, modifier = Modifier.size(38.dp))
                Spacer(Modifier.height(12.dp))
                Text(if (permissionGranted) "No audio tracks found" else "Audio access needed", fontWeight = FontWeight.SemiBold)
                Text(
                    if (permissionGranted) "Scan internal storage to find music." else "Allow access to scan audio on this phone.",
                    color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)
                )
                Button(onClick = onScan, modifier = Modifier.padding(top = 16.dp)) { Text("Scan Library") }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 12.dp + bottomOverlayPadding),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(tracks, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        plays = playCount(track.id),
                        playing = isCurrentlyPlaying(track.id),
                        onPlay = { onPlay(tracks.indexOf(track)) },
                        onAction = { onTrackAction(track, it) },
                        selectionMode = selectionMode,
                        selected = track.id in selectedTrackIds,
                        onLongClick = { onLongPressTrack(track) },
                        onToggleSelection = { onToggleSelection(track) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackRow(
    track: MusicTrack,
    plays: Int,
    playing: Boolean,
    onPlay: () -> Unit,
    onAction: (TrackAction) -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onToggleSelection: () -> Unit = {}
) {
    var menuExpanded by remember(track.id) { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).combinedClickable(
            onClick = { if (selectionMode) onToggleSelection() else onPlay() },
            onLongClick = onLongClick
        ).padding(horizontal = 5.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackArtwork(track, 48.dp, RoundedCornerShape(12.dp))
        Column(Modifier.weight(1f).padding(start = 10.dp, end = 4.dp)) {
            Text(
                track.title,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                color = if (playing) FeatherRed else PrimaryText,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, repeatDelayMillis = 0, initialDelayMillis = 0, velocity = 30.dp)
            )
            Text(
                "${track.artist} · ${formatDuration(track.durationMs)}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
                color = MutedText,
                modifier = Modifier.padding(top = 3.dp)
            )
            Text(formatSize(track.sizeBytes), maxLines = 1, fontSize = 9.sp, color = MutedText, modifier = Modifier.padding(top = 3.dp))
        }
        if (selectionMode) {
            SelectionCheckBox(checked = selected, modifier = Modifier.padding(horizontal = 5.dp))
        } else {
            Text("${plays}×", color = FeatherRed.copy(alpha = .8f), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 2.dp))
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Track options", tint = MutedText, modifier = Modifier.size(18.dp))
                }
                TrackOptionsDropdown(expanded = menuExpanded, onDismiss = { menuExpanded = false }) { action ->
                    menuExpanded = false
                    onAction(action)
                }
            }
        }
    }
}

@Composable
private fun SelectionCheckBox(checked: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier.size(23.dp)
            .clip(shape)
            .background(if (checked) FeatherRed else Color.Transparent)
            .border(2.dp, if (checked) FeatherRed else Color(0xFF65656A), shape),
        contentAlignment = Alignment.Center
    ) {
        if (checked) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun LibrarySelectionActionBar(
    onDelete: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(Color(0xFF151517))
            .navigationBarsPadding()
            .padding(horizontal = 5.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SelectionActionButton(Icons.Filled.Delete, "Delete", FeatherRed, onDelete, Modifier.weight(1f))
        SelectionActionButton(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to Playlist", PrimaryText, onAddToPlaylist, Modifier.weight(1f))
        SelectionActionButton(Icons.AutoMirrored.Filled.QueueMusic, "Add to play queue", PrimaryText, onAddToQueue, Modifier.weight(1f))
        SelectionActionButton(Icons.Filled.Share, "Share file", PrimaryText, onShare, Modifier.weight(1f))
    }
}

@Composable
private fun SelectionActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(23.dp))
        Text(label, color = PrimaryText, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TrackOptionsDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    showTrackInfo: Boolean = false,
    onAction: (TrackAction) -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, containerColor = CardBlack) {
        DropdownMenuItem(text = { Text("Add to playlist") }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) }, onClick = { onAction(TrackAction.ADD_TO_PLAYLIST) })
        if (showTrackInfo) DropdownMenuItem(text = { Text("Track info") }, leadingIcon = { Icon(Icons.Filled.Info, null) }, onClick = { onAction(TrackAction.INFO) })
        DropdownMenuItem(text = { Text("Set as ringtone") }, leadingIcon = { Icon(Icons.Filled.Waves, null) }, onClick = { onAction(TrackAction.RINGTONE) })
        DropdownMenuItem(text = { Text("Share") }, leadingIcon = { Icon(Icons.Filled.Share, null) }, onClick = { onAction(TrackAction.SHARE) })
        DropdownMenuItem(text = { Text("Delete file", color = FeatherRed) }, leadingIcon = { Icon(Icons.Filled.Delete, null, tint = FeatherRed) }, onClick = { onAction(TrackAction.DELETE) })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistsScreen(
    tracks: List<MusicTrack>,
    listState: LazyListState,
    playlists: List<UserPlaylist>,
    favorites: Set<String>,
    playCount: (String) -> Int,
    albums: List<AlbumCollection>,
    artists: List<ArtistCollection>,
    onOpenAlbums: () -> Unit,
    onOpenArtists: () -> Unit,
    onOpenSmartPlaylist: (String, List<MusicTrack>) -> Unit,
    onCreate: () -> Unit,
    onManagePlaylist: (UserPlaylist) -> Unit,
    onOpenUserPlaylist: (UserPlaylist) -> Unit,
    bottomOverlayPadding: Dp = 0.dp
) {
    var smartPlaylists by remember { mutableStateOf(SmartPlaylistCollections()) }
    LaunchedEffect(tracks, favorites, playCount) {
        val trackSnapshot = tracks
        val favoritesSnapshot = favorites
        val playCountSnapshot = playCount
        smartPlaylists = withContext(Dispatchers.Default) {
            buildSmartPlaylistCollections(trackSnapshot, favoritesSnapshot, playCountSnapshot)
        }
    }
    val recent = smartPlaylists.recent
    val highResolution = smartPlaylists.highResolution
    val mostPlayed = smartPlaylists.mostPlayed
    val favoritesList = smartPlaylists.favorites
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 9.dp, bottom = 20.dp + bottomOverlayPadding),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        item { Text("Playlists", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp)) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CollectionEntryCard("Albums", "${albums.size} albums", Icons.Filled.LibraryMusic, onOpenAlbums, Modifier.weight(1f))
                CollectionEntryCard("Artists", "${artists.size} artists", Icons.Filled.Waves, onOpenArtists, Modifier.weight(1f))
            }
        }
        item { SectionLabel("Smart playlists") }
        item { SmartPlaylistRow("Recently added", "Newest tracks", recent.size, Icons.AutoMirrored.Filled.Sort) { onOpenSmartPlaylist("Recently added", recent) } }
        item { SmartPlaylistRow("Hi-Res", "High-resolution files", highResolution.size, Icons.Filled.Waves) { onOpenSmartPlaylist("Hi-Res", highResolution) } }
        item { SmartPlaylistRow("Most played", "Sorted by full plays", mostPlayed.size, Icons.Filled.Repeat) { onOpenSmartPlaylist("Most played", mostPlayed) } }
        item { SmartPlaylistRow("Favorites", "Your liked tracks", favoritesList.size, Icons.Filled.Favorite) { onOpenSmartPlaylist("Favorites", favoritesList) } }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                SectionLabel("My playlists")
                TextButton(onClick = onCreate) { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null); Text("New", modifier = Modifier.padding(start = 4.dp)) }
            }
        }
        if (playlists.isEmpty()) {
            item { EmptyCard("No playlists yet", "Create a playlist and add tracks from Library.") }
        } else {
            items(playlists, key = { it.id }) { playlist ->
                Card(
                    modifier = Modifier.fillMaxWidth().combinedClickable(
                        onClick = { onOpenUserPlaylist(playlist) },
                        onLongClick = { onManagePlaylist(playlist) }
                    ),
                    colors = CardDefaults.cardColors(containerColor = CardBlack),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = FeatherRed, modifier = Modifier.size(24.dp))
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(playlist.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${playlist.trackIds.size} tracks", color = MutedText, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionEntryCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBlack),
        shape = RoundedCornerShape(15.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 12.dp)) {
            Icon(icon, contentDescription = null, tint = FeatherRed, modifier = Modifier.size(21.dp))
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 9.dp))
            Text(subtitle, fontSize = 9.sp, color = MutedText, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun SmartPlaylistRow(title: String, subtitle: String, count: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBlack),
        shape = RoundedCornerShape(15.dp)
    ) {
        Row(Modifier.padding(horizontal = 11.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(FeatherRed.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = FeatherRed, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 9.sp, color = MutedText, modifier = Modifier.padding(top = 3.dp))
            }
            Text(count.toString(), fontSize = 10.sp, color = MutedText)
        }
    }
}

@Composable
private fun CollectionPageEntrance(key: Any, content: @Composable () -> Unit) {
    val visibility = remember(key) {
        MutableTransitionState(false).apply { targetState = true }
    }
    AnimatedVisibility(
        visibleState = visibility,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(animationSpec = tween(170)) +
            slideInVertically(animationSpec = tween(210)) { it / 24 },
        exit = ExitTransition.None
    ) {
        Box(Modifier.fillMaxSize()) { content() }
    }
}

@Composable
private fun AlbumsScreen(
    albums: List<AlbumCollection>,
    listState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onOpenAlbum: (AlbumCollection) -> Unit,
    bottomOverlayPadding: Dp = 0.dp
) {
    if (albums.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) {
            EmptyCard("No albums yet", "Album cards appear when your tracks include album metadata.")
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 20.dp + bottomOverlayPadding),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(albums, key = { it.key }) { album ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenAlbum(album) },
                    colors = CardDefaults.cardColors(containerColor = CardBlack),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(8.dp)) {
                        album.tracks.firstOrNull()?.let { cover ->
                            TrackArtwork(cover, Modifier.fillMaxWidth().aspectRatio(1f), RoundedCornerShape(11.dp))
                        }
                        Text(album.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif, modifier = Modifier.padding(top = 8.dp))
                        Text(album.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp, color = MutedText, fontFamily = FontFamily.SansSerif, modifier = Modifier.padding(top = 3.dp))
                        Text("${album.tracks.size} tracks", fontSize = 9.sp, color = MutedText, modifier = Modifier.padding(top = 5.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistsScreen(
    artists: List<ArtistCollection>,
    listState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onOpenArtist: (ArtistCollection) -> Unit,
    bottomOverlayPadding: Dp = 0.dp
) {
    if (artists.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) {
            EmptyCard("No artists yet", "Artist cards appear when your tracks include artist metadata.")
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 20.dp + bottomOverlayPadding),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(artists, key = { it.key }) { artist ->
                val albumCount = artist.tracks.map { it.albumId.takeIf { id -> id >= 0L }?.toString() ?: it.album.lowercase() }.distinct().size
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenArtist(artist) },
                    colors = CardDefaults.cardColors(containerColor = CardBlack),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(8.dp)) {
                        artist.tracks.firstOrNull()?.let { cover ->
                            TrackArtwork(cover, Modifier.fillMaxWidth().aspectRatio(1f), RoundedCornerShape(11.dp))
                        }
                        Text(artist.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif, modifier = Modifier.padding(top = 8.dp))
                        Text("$albumCount albums · ${artist.tracks.size} tracks", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 9.sp, color = MutedText, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackCollectionDetailScreen(
    title: String,
    subtitle: String,
    heroTrack: MusicTrack?,
    tracks: List<MusicTrack>,
    listState: LazyListState,
    onPlayAll: () -> Unit,
    onPlayTrack: (Int) -> Unit,
    onTrackAction: (MusicTrack, TrackAction) -> Unit,
    onManage: (() -> Unit)? = null,
    bottomOverlayPadding: Dp = 0.dp
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 22.dp + bottomOverlayPadding),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardBlack), shape = RoundedCornerShape(17.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    heroTrack?.let { TrackArtwork(it, 70.dp, RoundedCornerShape(12.dp)) }
                    Column(Modifier.weight(1f).padding(start = 11.dp)) {
                        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
                        Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MutedText, fontSize = 10.sp, fontFamily = FontFamily.SansSerif, modifier = Modifier.padding(top = 4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onPlayAll, enabled = tracks.isNotEmpty()) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Text("Play all", modifier = Modifier.padding(start = 4.dp))
                            }
                            if (onManage != null) TextButton(onClick = onManage) { Text("Manage") }
                        }
                    }
                }
            }
        }
        item { SectionLabel("Tracks") }
        if (tracks.isEmpty()) item { EmptyCard("No tracks", "Add tracks from Library or scan internal storage.") }
        items(tracks, key = { it.id }) { track ->
            TrackRow(
                track = track,
                plays = 0,
                playing = false,
                onPlay = { onPlayTrack(tracks.indexOf(track)) },
                onAction = { onTrackAction(track, it) }
            )
        }
    }
}

@Composable
private fun QueueScreen(
    queue: List<MusicTrack>,
    currentId: String?,
    playing: Boolean,
    onPlay: (Int) -> Unit,
    onOpenPlayer: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(AmoledBlack),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Playback queue", color = MutedText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                TextButton(
                    onClick = onClear,
                    modifier = Modifier.heightIn(min = 44.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Clear", color = MutedText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        if (queue.isEmpty()) item { EmptyCard("Queue is empty", "Play a track from Library to start a queue.") }
        items(queue, key = { it.id }) { track ->
            TrackRow(
                track = track,
                plays = 0,
                playing = track.id == currentId && playing,
                onPlay = { onPlay(queue.indexOf(track)) },
                onAction = {}
            )
        }
        item {
            if (currentId != null) TextButton(onClick = onOpenPlayer, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PlayArrow, null)
                Text("Back to player", modifier = Modifier.padding(start = 5.dp))
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    profile: DeviceProfile,
    dotFontEnabled: Boolean,
    onDotFontChange: (Boolean) -> Unit,
    accentId: String,
    onAccentChange: (String) -> Unit,
    minimumSizeKb: Float,
    onMinimumSizeChange: (Float) -> Unit,
    minimumDurationSeconds: Float,
    onMinimumDurationChange: (Float) -> Unit,
    onScan: () -> Unit,
    bottomOverlayPadding: Dp = 0.dp
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(bottom = bottomOverlayPadding)) {
        Text("Library filters", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = CardBlack), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                RangeSetting("Minimum file size", minimumSizeKb, "KB", 0f..1500f, 50f, onMinimumSizeChange)
                HorizontalDivider(color = Color.White.copy(alpha = .06f))
                RangeSetting("Minimum duration", minimumDurationSeconds, "sec", 0f..40f, 5f, onMinimumDurationChange)
            }
        }

        Text("Accent color", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 19.dp, bottom = 8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = CardBlack), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 13.dp)) {
                Text("Choose an accent", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                AccentColorPicker(accentId = accentId, onAccentChange = onAccentChange, modifier = Modifier.padding(top = 12.dp))
            }
        }

        Text("Library scan", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 19.dp, bottom = 8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = CardBlack), shape = RoundedCornerShape(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onScan).padding(horizontal = 13.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .06f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = PrimaryText, modifier = Modifier.size(17.dp))
                }
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("Scan Library", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text("Search audio across internal storage", color = MutedText, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }

        Text("System", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 19.dp, bottom = 8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = CardBlack), shape = RoundedCornerShape(16.dp)) {
            Column {
                if (profile == DeviceProfile.NOTHING) {
                    SettingToggleRow(
                        title = "Nothing dot font",
                        subtitle = "Use the dotted typeface across the app",
                        checked = dotFontEnabled,
                        onCheckedChange = onDotFontChange
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = .06f))
                    SettingInfoRow("Text style", if (dotFontEnabled) "NDot55 system typeface" else "System sans-serif", Icons.Filled.Settings)
                } else {
                    SettingInfoRow("Typeface", "System default", Icons.Filled.Settings)
                }
                if (profile == DeviceProfile.REALME) {
                    HorizontalDivider(color = Color.White.copy(alpha = .06f))
                    SettingInfoRow("Mini Capsule", "Automatic on this Realme device", Icons.Filled.Waves, badge = "AUTO")
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun AccentColorPicker(accentId: String, onAccentChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AccentOptions.forEach { option ->
            val selected = option.id == accentId
            Column(
                modifier = Modifier.width(58.dp).clickable { onAccentChange(option.id) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(option.color)
                        .border(2.dp, if (selected) Color.White else Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) Text("✓", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text(option.label, fontSize = 9.sp, color = if (selected) PrimaryText else MutedText, modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}

@Composable
private fun SettingToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MutedText, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun snapToStep(value: Float, range: ClosedFloatingPointRange<Float>, stepSize: Float): Float {
    val safeValue = value.takeIf { it.isFinite() } ?: range.start
    val clamped = safeValue.coerceIn(range)
    if (stepSize <= 0f) return clamped
    val stepIndex = ((clamped - range.start) / stepSize).roundToInt()
    return (range.start + stepIndex * stepSize).coerceIn(range)
}

@Composable
private fun RangeSetting(
    label: String,
    value: Float,
    unit: String,
    range: ClosedFloatingPointRange<Float>,
    stepSize: Float,
    onChange: (Float) -> Unit
) {
    val currentValue = snapToStep(value, range, stepSize)
    val rangeWidth = (range.endInclusive - range.start).coerceAtLeast(1f)
    val divisions = ((range.endInclusive - range.start) / stepSize).roundToInt().coerceAtLeast(1)
    val discreteSteps = (divisions - 1).coerceAtLeast(0)
    val progress = ((currentValue - range.start) / rangeWidth).coerceIn(0f, 1f)

    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("${currentValue.toInt()} $unit", fontSize = 10.sp, color = FeatherRed)
        }
        PlaybackProgressBar(
            value = progress,
            enabled = true,
            modifier = Modifier.fillMaxWidth(),
            steps = discreteSteps,
            emitDuringDrag = true,
            onSeek = { fraction ->
                val rawValue = range.start + fraction.coerceIn(0f, 1f) * (range.endInclusive - range.start)
                onChange(snapToStep(rawValue, range, stepSize))
            }
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 1.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${range.start.toInt()} $unit", color = MutedText, fontSize = 9.sp)
            Text("${range.endInclusive.toInt()} $unit", color = MutedText, fontSize = 9.sp)
        }
    }
}

@Composable
private fun SettingInfoRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, badge: String? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .06f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = PrimaryText, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MutedText, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
        }
        if (badge != null) Text(badge, color = FeatherRed, fontWeight = FontWeight.Bold, fontSize = 9.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FullPlayerScreen(
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
                        "${formatTrackFormat(track)}  ·  ${formatSize(track.sizeBytes)}  ·  ${formatBitrate(bitrateBps)}",
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
private fun PlayerProgress(track: MusicTrack, snapshot: PlaybackSnapshot, controller: MediaController?) {
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

private fun snapProgressFraction(value: Float, steps: Int): Float {
    val clamped = value.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    if (steps <= 0) return clamped
    val divisions = steps + 1
    return (clamped * divisions).roundToInt().coerceIn(0, divisions) / divisions.toFloat()
}

@Composable
private fun PlaybackProgressBar(
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MiniPlayerBar(
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
private fun Modifier.interactiveVerticalDrag(
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

private suspend fun animateSwipeOffset(start: Float, target: Float, onValue: (Float) -> Unit) {
    animate(
        initialValue = start,
        targetValue = target,
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 520f)
    ) { value, _ -> onValue(value) }
}

private suspend fun animateDismissOffset(
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

@Composable
private fun TrackArtwork(track: MusicTrack, size: Dp, shape: RoundedCornerShape, large: Boolean = false) {
    TrackArtwork(track, Modifier.size(size), shape, large)
}

private fun decodeSampledArtwork(context: Context, uri: Uri, targetPixels: Int): Bitmap? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val boundsStream = resolver.openInputStream(uri) ?: return null
    boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val largestDimension = maxOf(bounds.outWidth, bounds.outHeight)
    val ratio = largestDimension.toDouble() / targetPixels.coerceAtLeast(1)
    var sampleSize = 1
    while (sampleSize * 2 <= ratio) sampleSize *= 2
    if (ratio / sampleSize > 1.414) sampleSize *= 2

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}

@Composable
private fun TrackArtwork(track: MusicTrack, modifier: Modifier, shape: RoundedCornerShape, large: Boolean = false) {
    val context = LocalContext.current
    val targetPixels = if (large) 720 else 192
    val customArtworkUri = CustomArtworkStore.get(track.id)
    val defaultArtworkUri = remember(track.albumId) { if (track.albumId >= 0L) track.albumArtUri() else null }
    val artworkUri = customArtworkUri ?: defaultArtworkUri
    val cacheKey = artworkUri?.let { "$it@$targetPixels" }
    var bitmap by remember(track.id, artworkUri, targetPixels) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(cacheKey) {
        if (artworkUri == null || cacheKey == null) {
            bitmap = null
        } else {
            bitmap = ArtworkLoader.load(context.applicationContext, artworkUri, targetPixels, cacheKey)
        }
    }
    Box(modifier.clip(shape).background(Brush.linearGradient(listOf(Color(0xFF522129), Color(0xFF111114)))), contentAlignment = Alignment.Center) { 
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

@Composable
private fun ScanDialog() {
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Surface(shape = RoundedCornerShape(22.dp), color = CardBlack) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = FeatherRed, strokeWidth = 3.dp)
                Text("Scanning internal storage…", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp))
                Text("Searching for audio files", fontSize = 11.sp, color = MutedText, modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}

private fun readTrackBitrate(context: Context, uri: Uri): Long? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()?.takeIf { it > 0L }
    } catch (_: Exception) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

private data class TrackBitrate(val bitsPerSecond: Long, val estimated: Boolean)

@Composable
private fun rememberTrackBitrate(track: MusicTrack): TrackBitrate? {
    val context = LocalContext.current
    var bitrate by remember(track.id, track.uri) { mutableStateOf<TrackBitrate?>(null) }
    LaunchedEffect(track.id, track.uri, track.durationMs, track.sizeBytes) {
        bitrate = withContext(Dispatchers.IO) {
            val embeddedRate = readTrackBitrate(context, track.uri)
            when {
                embeddedRate != null -> TrackBitrate(embeddedRate, estimated = false)
                track.durationMs > 0L && track.sizeBytes > 0L ->
                    TrackBitrate(track.sizeBytes * 8_000L / track.durationMs, estimated = true)
                else -> null
            }
        }
    }
    return bitrate
}

private fun formatTrackFormat(track: MusicTrack): String =
    track.extension.takeIf { it.isNotBlank() }?.uppercase()
        ?: track.mimeType.substringAfter('/').takeIf { it.isNotBlank() }?.uppercase()
        ?: "AUDIO"

private fun formatBitrate(bitrate: TrackBitrate?): String =
    bitrate?.takeIf { it.bitsPerSecond > 0L }?.let {
        "${if (it.estimated) "~" else ""}${(it.bitsPerSecond + 500L) / 1_000L} kbps"
    } ?: "— kbps"

@Composable
private fun TrackInfoDialog(track: MusicTrack, plays: Int, onDismiss: () -> Unit) {
    val bitrateBps = rememberTrackBitrate(track)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Track info") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoLine("Artist", track.artist)
                InfoLine("Album", track.album)
                InfoLine("File", track.displayName)
                InfoLine("Format", formatTrackFormat(track))
                InfoLine("Bitrate", formatBitrate(bitrateBps))
                InfoLine("Duration", formatDuration(track.durationMs))
                InfoLine("File size", formatSize(track.sizeBytes))
                InfoLine("Full plays", plays.toString())
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        containerColor = CardBlack
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, color = MutedText, fontSize = 11.sp)
        Text(value, fontSize = 11.sp, fontFamily = FontFamily.SansSerif, modifier = Modifier.padding(start = 12.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PlaylistPickerDialog(
    tracks: List<MusicTrack>,
    playlists: List<UserPlaylist>,
    onChoose: (UserPlaylist) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tracks.size == 1) "Add to playlist" else "Add ${tracks.size} tracks to playlist") },
        text = {
            Column {
                if (playlists.isEmpty()) Text("Create a playlist first.", color = MutedText)
                playlists.forEach { playlist ->
                    TextButton(onClick = { onChoose(playlist) }, modifier = Modifier.fillMaxWidth()) {
                        Text(playlist.name, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCreate) { Text("New playlist") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = CardBlack
    )
}

@Composable
private fun CreatePlaylistDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist") },
        text = {
            OutlinedTextField(value = name, onValueChange = onNameChange, singleLine = true, label = { Text("Name") })
        },
        confirmButton = { TextButton(onClick = onCreate) { Text("Create", color = FeatherRed) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = CardBlack
    )
}

@Composable
private fun SectionLabel(label: String) {
    Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun EmptyCard(title: String, message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBlack), shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(17.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(message, color = MutedText, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun navColors() = androidx.compose.material3.NavigationBarItemDefaults.colors(
    selectedIconColor = FeatherRed,
    selectedTextColor = FeatherRed,
    indicatorColor = FeatherRed.copy(alpha = .10f),
    unselectedIconColor = MutedText,
    unselectedTextColor = MutedText
)

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "—:—"
    val totalSeconds = durationMs / 1_000L
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}

private fun formatSize(sizeBytes: Long): String = when {
    sizeBytes <= 0L -> "Unknown size"
    sizeBytes >= 1024L * 1024L -> String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0))
    else -> String.format("%.0f KB", sizeBytes / 1024.0)
}

private fun MusicTrack.albumArtUri(): Uri? {
    if (albumId < 0L) return null
    val volume = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.VOLUME_EXTERNAL_PRIMARY else "external"
    return Uri.parse("content://media/$volume/audio/albumart/$albumId")
}

private fun loadFavorites(preferences: android.content.SharedPreferences): Set<String> =
    preferences.getStringSet("favorites", emptySet())?.toSet().orEmpty()

private fun loadPlaylists(preferences: android.content.SharedPreferences): List<UserPlaylist> = runCatching {
    val array = JSONArray(preferences.getString("user_playlists", "[]"))
    buildList {
        for (index in 0 until array.length()) {
            val row = array.getJSONObject(index)
            val ids = row.optJSONArray("trackIds") ?: JSONArray()
            add(UserPlaylist(row.optString("id"), row.optString("name"), buildList {
                for (idIndex in 0 until ids.length()) add(ids.optString(idIndex))
            }))
        }
    }
}.getOrDefault(emptyList())

private fun encodePlaylists(playlists: List<UserPlaylist>): String {
    val array = JSONArray()
    playlists.forEach { playlist ->
        val ids = JSONArray()
        playlist.trackIds.forEach { ids.put(it) }
        array.put(JSONObject().put("id", playlist.id).put("name", playlist.name).put("trackIds", ids))
    }
    return array.toString()
}

private fun shareTrack(context: Context, track: MusicTrack) {
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = track.mimeType.ifBlank { "audio/*" }
            putExtra(Intent.EXTRA_STREAM, track.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share track"))
    }.onFailure { Toast.makeText(context, "Couldn't share this track", Toast.LENGTH_SHORT).show() }
}

private fun shareTracks(context: Context, tracks: List<MusicTrack>) {
    if (tracks.isEmpty()) return
    if (tracks.size == 1) {
        shareTrack(context, tracks.first())
        return
    }
    runCatching {
        val streams = ArrayList<Uri>(tracks.size).apply { tracks.forEach { add(it.uri) } }
        val clipData = ClipData.newUri(context.contentResolver, "Audio files", streams.first()).apply {
            streams.drop(1).forEach { addItem(ClipData.Item(it)) }
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "audio/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, streams)
            this.clipData = clipData
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share files"))
    }.onFailure {
        Toast.makeText(context, "Couldn't share these files", Toast.LENGTH_SHORT).show()
    }
}

private fun setAsRingtone(context: Context, track: MusicTrack) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")))
        }
        Toast.makeText(context, "Allow ringtone access, then try again", Toast.LENGTH_LONG).show()
        return
    }
    runCatching {
        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, track.uri)
        Toast.makeText(context, "Ringtone updated", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, "Couldn't set this ringtone", Toast.LENGTH_SHORT).show()
    }
}

private fun deleteFromMediaStore(
    context: Context,
    tracks: List<MusicTrack>,
    launcher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>,
    onDeleted: () -> Unit
) {
    if (tracks.isEmpty()) return
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val request = MediaStore.createDeleteRequest(context.contentResolver, tracks.map { it.uri })
            launcher.launch(IntentSenderRequest.Builder(request.intentSender).build())
        } else {
            tracks.forEach { track -> context.contentResolver.delete(track.uri, null, null) }
            onDeleted()
        }
    }.onFailure {
        val message = if (tracks.size == 1) "Android couldn't delete this file" else "Android couldn't delete these files"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
