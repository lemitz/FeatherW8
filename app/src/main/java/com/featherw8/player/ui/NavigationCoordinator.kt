package com.featherw8.player.ui

import android.app.Activity
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import com.featherw8.player.ui.model.AlbumCollection
import com.featherw8.player.ui.model.ArtistCollection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal enum class AppPage {
    LIBRARY, PLAYLISTS, SETTINGS,
    ALBUMS, ARTISTS, ALBUM_DETAIL, ARTIST_DETAIL, PLAYLIST_DETAIL,

    // Kept only so instance state saved by an older build (which used a separate
    // queue page) can still be restored. Nothing navigates here any more - the queue
    // now lives on the second page of the player pager.
    QUEUE
}

/** Owns app-page navigation, tab/list scroll positions, and root back-stack behavior. */
internal class NavigationCoordinator internal constructor(
    private val pageState: MutableState<AppPage>,
    private val returnFromSettingsState: MutableState<AppPage>,
    private val returnFromDetailState: MutableState<AppPage>,
    val tabPagerState: PagerState,
    val libraryListState: LazyListState,
    val playlistsListState: LazyListState,
    val albumsGridState: LazyGridState,
    val artistsGridState: LazyGridState,
    val detailListState: LazyListState,
    private val selectedAlbumKeyState: MutableState<String?>,
    private val selectedArtistKeyState: MutableState<String?>,
    private val scope: CoroutineScope,
    private val onQueryReset: () -> Unit,
    private val onClearLibrarySelection: () -> Unit
) {
    var page by pageState
    private var returnFromSettings by returnFromSettingsState
    private var returnFromDetail by returnFromDetailState
    var selectedAlbumKey by selectedAlbumKeyState
    var selectedArtistKey by selectedArtistKeyState
    private var lastRootBackAt by mutableStateOf(0L)

    fun resetRootBackConfirmation() {
        lastRootBackAt = 0L
    }

    fun openSettings() {
        returnFromSettings = page
        resetRootBackConfirmation()
        page = AppPage.SETTINGS
    }

    fun openLibrary() {
        resetRootBackConfirmation()
        page = AppPage.LIBRARY
    }

    fun openPlaylists() {
        onQueryReset()
        onClearLibrarySelection()
        resetRootBackConfirmation()
        page = AppPage.PLAYLISTS
    }

    fun openAlbums() {
        resetRootBackConfirmation()
        page = AppPage.ALBUMS
    }

    fun openArtists() {
        resetRootBackConfirmation()
        page = AppPage.ARTISTS
    }

    fun openAlbum(album: AlbumCollection) {
        selectedAlbumKey = album.key
        returnFromDetail = AppPage.ALBUMS
        page = AppPage.ALBUM_DETAIL
    }

    fun openArtist(artist: ArtistCollection) {
        selectedArtistKey = artist.key
        returnFromDetail = AppPage.ARTISTS
        page = AppPage.ARTIST_DETAIL
    }

    fun openPlaylistDetail() {
        returnFromDetail = AppPage.PLAYLISTS
        resetRootBackConfirmation()
        page = AppPage.PLAYLIST_DETAIL
    }

    fun goBackFromPage() {
        resetRootBackConfirmation()
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

    internal fun notePlayerBack() {
        lastRootBackAt = SystemClock.elapsedRealtime()
    }

    internal fun rootBackIsConfirmed(now: Long): Boolean =
        lastRootBackAt > 0L && now - lastRootBackAt <= 2_000L

    internal fun markRootBack(now: Long) {
        lastRootBackAt = now
    }
}

@Composable
internal fun rememberNavigationCoordinator(
    onQueryReset: () -> Unit,
    selectedLibraryTrackIds: Set<String>,
    onClearLibrarySelection: () -> Unit,
    playerOpen: Boolean,
    onBackFromPlayer: () -> Unit
): NavigationCoordinator {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestQueryReset = rememberUpdatedState(onQueryReset)
    val latestClearSelection = rememberUpdatedState(onClearLibrarySelection)
    val latestPlayerOpen = rememberUpdatedState(playerOpen)
    val latestBackFromPlayer = rememberUpdatedState(onBackFromPlayer)
    val pageState = rememberSaveable { mutableStateOf(AppPage.LIBRARY) }
    val returnFromSettingsState = rememberSaveable { mutableStateOf(AppPage.LIBRARY) }
    val returnFromDetailState = rememberSaveable { mutableStateOf(AppPage.PLAYLISTS) }
    val libraryListState = rememberLazyListState()
    val playlistsListState = rememberLazyListState()
    val albumsGridState = rememberLazyGridState()
    val artistsGridState = rememberLazyGridState()
    val detailListState = rememberLazyListState()
    val selectedAlbumKeyState = remember { mutableStateOf<String?>(null) }
    val selectedArtistKeyState = remember { mutableStateOf<String?>(null) }
    val tabPagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coordinator = remember {
        NavigationCoordinator(
            pageState = pageState,
            returnFromSettingsState = returnFromSettingsState,
            returnFromDetailState = returnFromDetailState,
            tabPagerState = tabPagerState,
            libraryListState = libraryListState,
            playlistsListState = playlistsListState,
            albumsGridState = albumsGridState,
            artistsGridState = artistsGridState,
            detailListState = detailListState,
            selectedAlbumKeyState = selectedAlbumKeyState,
            selectedArtistKeyState = selectedArtistKeyState,
            scope = scope,
            onQueryReset = { latestQueryReset.value() },
            onClearLibrarySelection = { latestClearSelection.value() }
        )
    }
    val latestAppPage by rememberUpdatedState(coordinator.page)
    val latestSelection by rememberUpdatedState(selectedLibraryTrackIds)

    LaunchedEffect(tabPagerState) {
        var previousSettledPage = tabPagerState.settledPage
        snapshotFlow { tabPagerState.settledPage }.collect { settledPage ->
            if (settledPage == previousSettledPage) return@collect
            previousSettledPage = settledPage
            val nextPage = if (settledPage == 0) AppPage.LIBRARY else AppPage.PLAYLISTS
            if ((latestAppPage == AppPage.LIBRARY || latestAppPage == AppPage.PLAYLISTS) && latestAppPage != nextPage) {
                if (nextPage == AppPage.PLAYLISTS) latestQueryReset.value()
                coordinator.resetRootBackConfirmation()
                coordinator.page = nextPage
            }
        }
    }

    LaunchedEffect(Unit) {
        if (coordinator.page == AppPage.QUEUE) coordinator.page = AppPage.LIBRARY
    }

    LaunchedEffect(coordinator.page) {
        if (coordinator.page != AppPage.LIBRARY) latestClearSelection.value()
        val targetPage = when (coordinator.page) {
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

    BackHandler(enabled = true) {
        if (latestPlayerOpen.value) {
            latestBackFromPlayer.value()
            coordinator.notePlayerBack()
        } else if (coordinator.page == AppPage.LIBRARY && latestSelection.isNotEmpty()) {
            latestClearSelection.value()
            coordinator.resetRootBackConfirmation()
        } else {
            val now = SystemClock.elapsedRealtime()
            when (coordinator.page) {
                AppPage.SETTINGS, AppPage.ALBUMS, AppPage.ARTISTS,
                AppPage.ALBUM_DETAIL, AppPage.ARTIST_DETAIL, AppPage.PLAYLIST_DETAIL,
                AppPage.QUEUE -> coordinator.goBackFromPage()
                AppPage.LIBRARY, AppPage.PLAYLISTS -> {
                    if (coordinator.rootBackIsConfirmed(now)) {
                        (context as? Activity)?.finish()
                    } else {
                        coordinator.markRootBack(now)
                        Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    return coordinator
}
