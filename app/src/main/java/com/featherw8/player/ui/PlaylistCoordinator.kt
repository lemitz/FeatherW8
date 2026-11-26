package com.featherw8.player.ui

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.featherw8.player.data.MusicTrack
import com.featherw8.player.ui.model.PlaylistDetailState
import com.featherw8.player.ui.model.UserPlaylist
import com.featherw8.player.ui.model.encodePlaylists
import com.featherw8.player.ui.model.loadFavorites
import com.featherw8.player.ui.model.loadPlaylists
import com.featherw8.player.ui.model.movedOver

internal data class PlaylistCoordinatorState(
    val favoriteIds: Set<String>,
    val playlists: List<UserPlaylist>,
    val tracksToAddToPlaylist: List<MusicTrack>?,
    val showCreatePlaylist: Boolean,
    val pendingPlaylistName: String,
    val tracksForNewPlaylist: List<String>,
    val playlistForActions: UserPlaylist?,
    val playlistForRename: UserPlaylist?,
    val renamePlaylistName: String,
    val playlistForDelete: UserPlaylist?,
    val playlistDetail: PlaylistDetailState?
)

internal data class PlaylistCoordinatorActions(
    val toggleFavorite: (String) -> Unit,
    val setTracksToAddToPlaylist: (List<MusicTrack>?) -> Unit,
    val showCreatePlaylist: () -> Unit,
    val managePlaylist: (UserPlaylist) -> Unit,
    val openPlaylistDetail: (String, List<MusicTrack>, String?) -> Unit,
    val openUserPlaylist: (UserPlaylist, List<MusicTrack>) -> Unit,
    val resolvePlaylistDetail: (List<MusicTrack>) -> PlaylistDetailState?,
    val moveInPlaylist: (String, List<MusicTrack>, Int, Int) -> Unit,
    val removeFromPlaylist: (String, String) -> Unit,
    val choosePlaylist: (List<MusicTrack>, UserPlaylist) -> Unit,
    val createPlaylistFromPicker: (List<MusicTrack>) -> Unit,
    val dismissPlaylistPicker: () -> Unit,
    val changePendingPlaylistName: (String) -> Unit,
    val dismissCreatePlaylist: () -> Unit,
    val createPlaylist: () -> Unit,
    val renamePlaylist: (UserPlaylist) -> Unit,
    val deletePlaylist: (UserPlaylist) -> Unit,
    val dismissPlaylistActions: () -> Unit,
    val changeRenameName: (String) -> Unit,
    val saveRename: (UserPlaylist) -> Unit,
    val dismissRename: () -> Unit,
    val confirmDeletePlaylist: (UserPlaylist) -> Unit,
    val dismissDeletePlaylist: () -> Unit
)

internal data class PlaylistCoordinator(
    val state: PlaylistCoordinatorState,
    val actions: PlaylistCoordinatorActions
)

@Composable
internal fun rememberPlaylistCoordinator(
    context: Context,
    preferences: SharedPreferences,
    onClearLibrarySelection: () -> Unit
): PlaylistCoordinator {
    var favoriteIds by remember(preferences) { mutableStateOf(loadFavorites(preferences)) }
    var playlists by remember(preferences) { mutableStateOf(loadPlaylists(preferences)) }
    var tracksToAddToPlaylist by remember { mutableStateOf<List<MusicTrack>?>(null) }
    var playlistForActions by remember { mutableStateOf<UserPlaylist?>(null) }
    var playlistForRename by remember { mutableStateOf<UserPlaylist?>(null) }
    var playlistForDelete by remember { mutableStateOf<UserPlaylist?>(null) }
    var renamePlaylistName by remember { mutableStateOf("") }
    var tracksForNewPlaylist by remember { mutableStateOf<List<String>>(emptyList()) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var pendingPlaylistName by remember { mutableStateOf("") }
    var playlistDetail by remember { mutableStateOf<PlaylistDetailState?>(null) }

    fun persistFavorites(next: Set<String>) {
        favoriteIds = next
        preferences.edit().putStringSet("favorites", next).apply()
    }

    fun persistPlaylists(next: List<UserPlaylist>) {
        playlists = next
        preferences.edit().putString("user_playlists", encodePlaylists(next)).apply()
    }

    return PlaylistCoordinator(
        state = PlaylistCoordinatorState(
            favoriteIds = favoriteIds,
            playlists = playlists,
            tracksToAddToPlaylist = tracksToAddToPlaylist,
            showCreatePlaylist = showCreatePlaylist,
            pendingPlaylistName = pendingPlaylistName,
            tracksForNewPlaylist = tracksForNewPlaylist,
            playlistForActions = playlistForActions,
            playlistForRename = playlistForRename,
            renamePlaylistName = renamePlaylistName,
            playlistForDelete = playlistForDelete,
            playlistDetail = playlistDetail
        ),
        actions = PlaylistCoordinatorActions(
            toggleFavorite = { trackId ->
                persistFavorites(favoriteIds.toMutableSet().apply {
                    if (trackId in this) remove(trackId) else add(trackId)
                })
            },
            setTracksToAddToPlaylist = { tracksToAddToPlaylist = it },
            showCreatePlaylist = { showCreatePlaylist = true },
            managePlaylist = { playlistForActions = it },
            openPlaylistDetail = { title, items, userPlaylistId ->
                playlistDetail = PlaylistDetailState(title, items, userPlaylistId)
            },
            openUserPlaylist = { playlist, tracks ->
                val items = playlist.trackIds.mapNotNull { id -> tracks.firstOrNull { it.id == id } }
                playlistDetail = PlaylistDetailState(playlist.name, items, playlist.id)
            },
            resolvePlaylistDetail = { tracks ->
                playlistDetail?.let { detail ->
                    if (detail.userPlaylistId == null) detail else {
                        val userPlaylist = playlists.firstOrNull { it.id == detail.userPlaylistId }
                        detail.copy(
                            title = userPlaylist?.name ?: detail.title,
                            tracks = userPlaylist?.trackIds?.mapNotNull { id -> tracks.firstOrNull { it.id == id } }.orEmpty()
                        )
                    }
                }
            },
            moveInPlaylist = { playlistId, shown, from, to ->
                // Visible lists omit missing files, so reordering must use track IDs rather than
                // interpreting the visible positions as stored playlist positions.
                val movingId = shown.getOrNull(from)?.id
                val overId = shown.getOrNull(to)?.id
                if (movingId != null && overId != null) {
                    persistPlaylists(playlists.map {
                        if (it.id == playlistId) it.copy(trackIds = it.trackIds.movedOver(movingId, overId)) else it
                    })
                }
            },
            removeFromPlaylist = { playlistId, trackId ->
                persistPlaylists(playlists.map {
                    if (it.id == playlistId) it.copy(trackIds = it.trackIds - trackId) else it
                })
            },
            choosePlaylist = { selectedTracks, playlist ->
                val selectedIds = selectedTracks.map { it.id }
                persistPlaylists(playlists.map {
                    if (it.id == playlist.id) it.copy(trackIds = (it.trackIds + selectedIds).distinct()) else it
                })
                tracksToAddToPlaylist = null
                onClearLibrarySelection()
                val countLabel = if (selectedIds.size == 1) "track" else "tracks"
                Toast.makeText(
                    context,
                    "Added ${selectedIds.size} $countLabel to ${playlist.name}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            createPlaylistFromPicker = { selectedTracks ->
                tracksForNewPlaylist = selectedTracks.map { it.id }
                pendingPlaylistName = ""
                showCreatePlaylist = true
                tracksToAddToPlaylist = null
            },
            dismissPlaylistPicker = { tracksToAddToPlaylist = null },
            changePendingPlaylistName = { pendingPlaylistName = it },
            dismissCreatePlaylist = {
                showCreatePlaylist = false
                tracksForNewPlaylist = emptyList()
            },
            createPlaylist = {
                val cleanName = pendingPlaylistName.trim()
                if (cleanName.isNotEmpty()) {
                    val playlist = UserPlaylist(
                        id = System.currentTimeMillis().toString(),
                        name = cleanName,
                        trackIds = tracksForNewPlaylist
                    )
                    persistPlaylists(playlists + playlist)
                    tracksForNewPlaylist = emptyList()
                    onClearLibrarySelection()
                    showCreatePlaylist = false
                }
            },
            renamePlaylist = { playlist ->
                renamePlaylistName = playlist.name
                playlistForRename = playlist
                playlistForActions = null
            },
            deletePlaylist = { playlist ->
                playlistForDelete = playlist
                playlistForActions = null
            },
            dismissPlaylistActions = { playlistForActions = null },
            changeRenameName = { renamePlaylistName = it },
            saveRename = { playlist ->
                val cleanName = renamePlaylistName.trim()
                persistPlaylists(playlists.map { if (it.id == playlist.id) it.copy(name = cleanName) else it })
                playlistForRename = null
            },
            dismissRename = { playlistForRename = null },
            confirmDeletePlaylist = { playlist ->
                persistPlaylists(playlists.filterNot { it.id == playlist.id })
                playlistForDelete = null
            },
            dismissDeletePlaylist = { playlistForDelete = null }
        )
    )
}
