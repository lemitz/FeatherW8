package com.featherw8.player.ui.components

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.Player
import com.featherw8.player.data.MusicTrack
import com.featherw8.player.data.TrackInfoStore
import com.featherw8.player.data.formatDuration
import com.featherw8.player.data.formatSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.featherw8.player.ui.model.UserPlaylist
import com.featherw8.player.ui.theme.CardBlack
import com.featherw8.player.ui.theme.FeatherRed
import com.featherw8.player.ui.theme.MutedText

@Composable
internal fun ScanDialog() {
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

internal fun readTrackBitrate(context: Context, uri: Uri): Long? {
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

internal data class TrackBitrate(val bitsPerSecond: Long, val estimated: Boolean)

@Composable
internal fun rememberTrackBitrate(track: MusicTrack): TrackBitrate? {
    val context = LocalContext.current
    var bitrate by remember(track.id, track.uri) { mutableStateOf<TrackBitrate?>(null) }
    LaunchedEffect(track.id, track.uri, track.durationMs, track.sizeBytes) {
        // The retriever opens and parses the file, which can take a moment; do not let
        // the result recompose the player while it is still sliding in.
        delay(220L)
        bitrate = withContext(Dispatchers.IO) {
            val remembered = TrackInfoStore.getBitrate(context, track.id, track.sizeBytes)
            if (remembered != null) {
                TrackBitrate(remembered.first, estimated = remembered.second)
            } else {
                val embeddedRate = readTrackBitrate(context, track.uri)
                val resolved = when {
                    embeddedRate != null -> TrackBitrate(embeddedRate, estimated = false)
                    track.durationMs > 0L && track.sizeBytes > 0L ->
                        TrackBitrate(track.sizeBytes * 8_000L / track.durationMs, estimated = true)
                    else -> null
                }
                // Misses are stored as 0 bps as well, so files that cannot answer - DSD
                // among them - are not parsed again every time the screen is opened.
                TrackInfoStore.putBitrate(
                    context,
                    track.id,
                    resolved?.bitsPerSecond ?: 0L,
                    resolved?.estimated ?: false,
                    track.sizeBytes
                )
                resolved
            }
        }
    }
    return bitrate
}

internal fun formatBitrate(bitrate: TrackBitrate?): String =
    bitrate?.takeIf { it.bitsPerSecond > 0L }?.let {
        "${if (it.estimated) "~" else ""}${(it.bitsPerSecond + 500L) / 1_000L} kbps"
    } ?: "— kbps"

@Composable
internal fun TrackInfoDialog(track: MusicTrack, plays: Int, onDismiss: () -> Unit) {
    val bitrateBps = rememberTrackBitrate(track)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Track info") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoLine("Artist", track.artist)
                InfoLine("Album", track.album)
                InfoLine("File", track.displayName)
                InfoLine("Format", track.formatLabel)
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
internal fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, color = MutedText, fontSize = 11.sp)
        Text(value, fontSize = 11.sp, fontFamily = FontFamily.SansSerif, modifier = Modifier.padding(start = 12.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun PlaylistPickerDialog(
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
internal fun CreatePlaylistDialog(
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
