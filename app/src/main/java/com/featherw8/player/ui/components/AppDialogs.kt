package com.featherw8.player.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.featherw8.player.data.MusicTrack
import com.featherw8.player.ui.model.UserPlaylist
import com.featherw8.player.ui.theme.CardBlack
import com.featherw8.player.ui.theme.FeatherRed
import com.featherw8.player.ui.theme.MutedText

// Stateless dialogs of the app. The state that decides whether one is shown, and what happens when
// it is confirmed, stays with the caller (FeatherW8App); these only draw the dialog and report taps.

@Composable
internal fun ChangeCoverDialog(
    hasCustomCover: Boolean,
    onChooseImage: () -> Unit,
    onUseOriginal: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change cover") },
        text = {
            Column {
                Text("Choose an image file to use as this track's cover.", color = MutedText)
                TextButton(onClick = onChooseImage, modifier = Modifier.fillMaxWidth()) { Text("Choose image") }
                if (hasCustomCover) {
                    TextButton(onClick = onUseOriginal, modifier = Modifier.fillMaxWidth()) {
                        Text("Use original cover", color = MutedText)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = CardBlack
    )
}

@Composable
internal fun PlaylistActionsDialog(
    playlist: UserPlaylist,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(playlist.name) },
        text = {
            Column {
                TextButton(onClick = onRename, modifier = Modifier.fillMaxWidth()) { Text("Rename playlist") }
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete playlist", color = FeatherRed)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = CardBlack
    )
}

@Composable
internal fun RenamePlaylistDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Playlist name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(enabled = name.trim().isNotEmpty(), onClick = onSave) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = CardBlack
    )
}

@Composable
internal fun DeletePlaylistDialog(playlist: UserPlaylist, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete playlist?") },
        text = { Text("Delete “${playlist.name}”? Its tracks will remain in your library.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete", color = FeatherRed) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = CardBlack
    )
}

/** Confirms deleting audio files from the device; one file shows its title, several show a count. */
@Composable
internal fun DeleteTracksDialog(tracks: List<MusicTrack>, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val single = tracks.size == 1
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (single) "Delete file?" else "Delete files?") },
        text = {
            Text(
                if (single) tracks.first().title else "Delete ${tracks.size} selected audio files? This can't be undone.",
                fontFamily = FontFamily.SansSerif
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(if (single) "Delete file" else "Delete files", color = FeatherRed) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = CardBlack
    )
}
