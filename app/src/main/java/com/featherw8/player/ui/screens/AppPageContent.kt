package com.featherw8.player.ui.screens

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.unit.Dp
import com.featherw8.player.data.MusicTrack
import com.featherw8.player.ui.components.EmptyCard
import com.featherw8.player.ui.model.AlbumCollection
import com.featherw8.player.ui.AppPage
import com.featherw8.player.ui.model.ArtistCollection
import com.featherw8.player.ui.model.PlaylistDetailState
import com.featherw8.player.ui.model.SmartPlaylistCollections
import com.featherw8.player.ui.model.SortMode
import com.featherw8.player.ui.model.TrackAction
import com.featherw8.player.ui.model.UserPlaylist
import com.featherw8.player.ui.theme.DeviceProfile

/** Values owned and computed by the app root, passed to the stateless page renderer. */
internal data class AppPageContentState(
    val page: AppPage,
    val tabPagerState: PagerState,
    val userScrollEnabled: Boolean,
    val visibleTracks: List<MusicTrack>,
    val libraryListState: LazyListState,
    val query: String,
    val sortMode: SortMode,
    val permissionGranted: Boolean,
    val playCount: (String) -> Int,
    val isCurrentlyPlaying: (String) -> Boolean,
    val bottomOverlayPadding: Dp,
    val selectedLibraryTrackIds: Set<String>,
    val playlistsListState: LazyListState,
    val smartPlaylists: SmartPlaylistCollections,
    val playlists: List<UserPlaylist>,
    val albums: List<AlbumCollection>,
    val artists: List<ArtistCollection>,
    val profile: DeviceProfile,
    val minimumSizeKb: Float,
    val minimumDurationSeconds: Float,
    val dotFontEnabled: Boolean,
    val accentId: String,
    val albumsGridState: LazyGridState,
    val artistsGridState: LazyGridState,
    val selectedAlbumKey: String?,
    val selectedAlbum: AlbumCollection?,
    val selectedArtistKey: String?,
    val selectedArtist: ArtistCollection?,
    val resolvedPlaylistDetail: PlaylistDetailState?,
    val detailListState: LazyListState
)

/** UI events are routed to the root, where navigation and persistence remain owned. */
internal data class AppPageContentActions(
    val onQueryChange: (String) -> Unit,
    val onSortChange: (SortMode) -> Unit,
    val onPlayTracks: (List<MusicTrack>, Int) -> Unit,
    val onTrackAction: (MusicTrack, TrackAction) -> Unit,
    val onScan: () -> Unit,
    val onManualScan: () -> Unit,
    val onLongPressTrack: (MusicTrack) -> Unit,
    val onToggleSelection: (MusicTrack) -> Unit,
    val onCancelSelection: () -> Unit,
    val onSelectAll: () -> Unit,
    val onOpenAlbums: () -> Unit,
    val onOpenArtists: () -> Unit,
    val onOpenSmartPlaylist: (String, List<MusicTrack>) -> Unit,
    val onCreatePlaylist: () -> Unit,
    val onManagePlaylist: (UserPlaylist) -> Unit,
    val onOpenUserPlaylist: (UserPlaylist) -> Unit,
    val onMinimumSizeChange: (Float) -> Unit,
    val onMinimumDurationChange: (Float) -> Unit,
    val onDotFontChange: (Boolean) -> Unit,
    val onAccentChange: (String) -> Unit,
    val onOpenAlbum: (AlbumCollection) -> Unit,
    val onOpenArtist: (ArtistCollection) -> Unit,
    val onManageDetailPlaylist: (String) -> Unit,
    val onMoveDetailTrack: (String, List<MusicTrack>, Int, Int) -> Unit,
    val onRemoveDetailTrack: (String, List<MusicTrack>, Int) -> Unit
)

@Composable
internal fun AppPageContent(
    state: AppPageContentState,
    actions: AppPageContentActions,
    pagerModifier: Modifier
) {
    when (state.page) {
        AppPage.LIBRARY, AppPage.PLAYLISTS -> HorizontalPager(
            state = state.tabPagerState,
            // Keep the neighbouring tab composed: otherwise every switch throws one page away
            // and builds the other from scratch in the middle of the swipe animation.
            beyondViewportPageCount = 1,
            modifier = pagerModifier,
            userScrollEnabled = state.userScrollEnabled
        ) { tabIndex ->
            when (tabIndex) {
                0 -> LibraryScreen(
                    tracks = state.visibleTracks,
                    listState = state.libraryListState,
                    query = state.query,
                    onQueryChange = actions.onQueryChange,
                    sortMode = state.sortMode,
                    onSortChange = actions.onSortChange,
                    onPlay = { index -> actions.onPlayTracks(state.visibleTracks, index) },
                    onTrackAction = actions.onTrackAction,
                    onScan = actions.onScan,
                    permissionGranted = state.permissionGranted,
                    playCount = state.playCount,
                    isCurrentlyPlaying = state.isCurrentlyPlaying,
                    bottomOverlayPadding = state.bottomOverlayPadding,
                    selectedTrackIds = state.selectedLibraryTrackIds,
                    onLongPressTrack = actions.onLongPressTrack,
                    onToggleSelection = actions.onToggleSelection,
                    onCancelSelection = actions.onCancelSelection,
                    onSelectAll = actions.onSelectAll
                )
                else -> PlaylistsScreen(
                    smartPlaylists = state.smartPlaylists,
                    listState = state.playlistsListState,
                    playlists = state.playlists,
                    albums = state.albums,
                    artists = state.artists,
                    onOpenAlbums = actions.onOpenAlbums,
                    onOpenArtists = actions.onOpenArtists,
                    onOpenSmartPlaylist = actions.onOpenSmartPlaylist,
                    onCreate = actions.onCreatePlaylist,
                    onManagePlaylist = actions.onManagePlaylist,
                    onOpenUserPlaylist = actions.onOpenUserPlaylist,
                    bottomOverlayPadding = state.bottomOverlayPadding
                )
            }
        }
        AppPage.SETTINGS -> SettingsScreen(
            profile = state.profile,
            minimumSizeKb = state.minimumSizeKb,
            onMinimumSizeChange = actions.onMinimumSizeChange,
            minimumDurationSeconds = state.minimumDurationSeconds,
            onMinimumDurationChange = actions.onMinimumDurationChange,
            onScan = actions.onManualScan,
            dotFontEnabled = state.dotFontEnabled,
            onDotFontChange = actions.onDotFontChange,
            accentId = state.accentId,
            onAccentChange = actions.onAccentChange,
            bottomOverlayPadding = state.bottomOverlayPadding
        )
        AppPage.ALBUMS -> CollectionPageEntrance(AppPage.ALBUMS) {
            AlbumsScreen(
                albums = state.albums,
                listState = state.albumsGridState,
                onOpenAlbum = actions.onOpenAlbum,
                bottomOverlayPadding = state.bottomOverlayPadding
            )
        }
        AppPage.ARTISTS -> CollectionPageEntrance(AppPage.ARTISTS) {
            ArtistsScreen(
                artists = state.artists,
                listState = state.artistsGridState,
                onOpenArtist = actions.onOpenArtist,
                bottomOverlayPadding = state.bottomOverlayPadding
            )
        }
        AppPage.ALBUM_DETAIL -> CollectionPageEntrance(state.selectedAlbumKey ?: AppPage.ALBUM_DETAIL) {
            state.selectedAlbum?.let { album ->
                TrackCollectionDetailScreen(
                    title = album.title,
                    subtitle = "${album.artist} · ${album.tracks.size} tracks",
                    heroTrack = album.tracks.firstOrNull(),
                    tracks = album.tracks,
                    listState = state.detailListState,
                    onPlayAll = { actions.onPlayTracks(album.tracks, 0) },
                    onPlayTrack = { index -> actions.onPlayTracks(album.tracks, index) },
                    onTrackAction = actions.onTrackAction,
                    bottomOverlayPadding = state.bottomOverlayPadding
                )
            } ?: EmptyCard("Album unavailable", "It may have been removed by a library scan.")
        }
        AppPage.ARTIST_DETAIL -> CollectionPageEntrance(state.selectedArtistKey ?: AppPage.ARTIST_DETAIL) {
            state.selectedArtist?.let { artist ->
                val albumCount = artist.tracks.map {
                    it.albumId.takeIf { id -> id >= 0L }?.toString() ?: it.album.lowercase()
                }.distinct().size
                TrackCollectionDetailScreen(
                    title = artist.name,
                    subtitle = "${albumCount} albums · ${artist.tracks.size} tracks",
                    heroTrack = artist.tracks.firstOrNull(),
                    tracks = artist.tracks,
                    listState = state.detailListState,
                    onPlayAll = { actions.onPlayTracks(artist.tracks, 0) },
                    onPlayTrack = { index -> actions.onPlayTracks(artist.tracks, index) },
                    onTrackAction = actions.onTrackAction,
                    bottomOverlayPadding = state.bottomOverlayPadding
                )
            } ?: EmptyCard("Artist unavailable", "They may no longer be in the library.")
        }
        AppPage.PLAYLIST_DETAIL -> CollectionPageEntrance(
            state.resolvedPlaylistDetail?.userPlaylistId
                ?: state.resolvedPlaylistDetail?.title
                ?: AppPage.PLAYLIST_DETAIL
        ) {
            state.resolvedPlaylistDetail?.let { detail ->
                TrackCollectionDetailScreen(
                    title = detail.title,
                    subtitle = "${detail.tracks.size} tracks",
                    heroTrack = detail.tracks.firstOrNull(),
                    tracks = detail.tracks,
                    listState = state.detailListState,
                    onPlayAll = { actions.onPlayTracks(detail.tracks, 0) },
                    onPlayTrack = { index -> actions.onPlayTracks(detail.tracks, index) },
                    onTrackAction = actions.onTrackAction,
                    bottomOverlayPadding = state.bottomOverlayPadding,
                    onManage = detail.userPlaylistId?.let { id -> { actions.onManageDetailPlaylist(id) } },
                    // Only a playlist the user made can be reordered; albums, artists and the
                    // smart playlists are computed from the library.
                    onMoveTrack = detail.userPlaylistId?.let { id ->
                        { from, to -> actions.onMoveDetailTrack(id, detail.tracks, from, to) }
                    },
                    onRemoveTrack = detail.userPlaylistId?.let { id ->
                        { index -> actions.onRemoveDetailTrack(id, detail.tracks, index) }
                    }
                )
            } ?: EmptyCard("Playlist unavailable", "It may have been removed.")
        }
        // Legacy page restored from old saved state; LaunchedEffect(Unit) moves it to LIBRARY.
        AppPage.QUEUE -> Unit
    }
}
