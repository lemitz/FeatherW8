package com.featherw8.player.ui.components

import androidx.compose.runtime.Composable
import com.featherw8.player.data.MusicTrack
import com.featherw8.player.ui.model.UserPlaylist

/** Renders the app's dialogs; state and all effects remain owned by [com.featherw8.player.ui.FeatherW8App]. */
@Composable
internal fun AppDialogsHost(
    showScanProgress: Boolean,
    artworkTrack: MusicTrack?,
    hasCustomArtwork: Boolean,
    infoTrack: MusicTrack?,
    infoPlayCount: Int,
    tracksToAddToPlaylist: List<MusicTrack>?,
    playlists: List<UserPlaylist>,
    showCreatePlaylist: Boolean,
    pendingPlaylistName: String,
    playlistForActions: UserPlaylist?,
    playlistForRename: UserPlaylist?,
    renamePlaylistName: String,
    playlistForDelete: UserPlaylist?,
    deleteTrack: MusicTrack?,
    tracksPendingDelete: List<MusicTrack>?,
    onChooseArtwork: () -> Unit,
    onUseOriginalArtwork: () -> Unit,
    onDismissArtwork: () -> Unit,
    onDismissInfo: () -> Unit,
    onChoosePlaylist: (List<MusicTrack>, UserPlaylist) -> Unit,
    onCreatePlaylistFromPicker: (List<MusicTrack>) -> Unit,
    onDismissPlaylistPicker: () -> Unit,
    onPendingPlaylistNameChange: (String) -> Unit,
    onDismissCreatePlaylist: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onRenamePlaylist: (UserPlaylist) -> Unit,
    onDeletePlaylist: (UserPlaylist) -> Unit,
    onDismissPlaylistActions: () -> Unit,
    onRenameNameChange: (String) -> Unit,
    onSaveRename: (UserPlaylist) -> Unit,
    onDismissRename: () -> Unit,
    onConfirmDeletePlaylist: (UserPlaylist) -> Unit,
    onDismissDeletePlaylist: () -> Unit,
    onConfirmDeleteTrack: (MusicTrack) -> Unit,
    onDismissDeleteTrack: () -> Unit,
    onConfirmDeleteSelectedTracks: (List<MusicTrack>) -> Unit,
    onDismissDeleteSelectedTracks: () -> Unit
) {
    if (showScanProgress) ScanDialog()
    artworkTrack?.let {
        ChangeCoverDialog(
            hasCustomCover = hasCustomArtwork,
            onChooseImage = onChooseArtwork,
            onUseOriginal = onUseOriginalArtwork,
            onDismiss = onDismissArtwork
        )
    }
    infoTrack?.let { TrackInfoDialog(it, infoPlayCount, onDismiss = onDismissInfo) }
    tracksToAddToPlaylist?.let { selectedTracks ->
        PlaylistPickerDialog(
            tracks = selectedTracks,
            playlists = playlists,
            onChoose = { playlist -> onChoosePlaylist(selectedTracks, playlist) },
            onCreate = { onCreatePlaylistFromPicker(selectedTracks) },
            onDismiss = onDismissPlaylistPicker
        )
    }
    if (showCreatePlaylist) {
        CreatePlaylistDialog(
            name = pendingPlaylistName,
            onNameChange = onPendingPlaylistNameChange,
            onDismiss = onDismissCreatePlaylist,
            onCreate = onCreatePlaylist
        )
    }
    playlistForActions?.let {
        PlaylistActionsDialog(
            playlist = it,
            onRename = { onRenamePlaylist(it) },
            onDelete = { onDeletePlaylist(it) },
            onDismiss = onDismissPlaylistActions
        )
    }
    playlistForRename?.let {
        RenamePlaylistDialog(
            name = renamePlaylistName,
            onNameChange = onRenameNameChange,
            onSave = { onSaveRename(it) },
            onDismiss = onDismissRename
        )
    }
    playlistForDelete?.let {
        DeletePlaylistDialog(
            playlist = it,
            onConfirm = { onConfirmDeletePlaylist(it) },
            onDismiss = onDismissDeletePlaylist
        )
    }
    deleteTrack?.let {
        DeleteTracksDialog(
            tracks = listOf(it),
            onConfirm = { onConfirmDeleteTrack(it) },
            onDismiss = onDismissDeleteTrack
        )
    }
    tracksPendingDelete?.let {
        DeleteTracksDialog(
            tracks = it,
            onConfirm = { onConfirmDeleteSelectedTracks(it) },
            onDismiss = onDismissDeleteSelectedTracks
        )
    }
}
