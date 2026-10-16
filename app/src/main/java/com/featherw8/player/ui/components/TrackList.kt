package com.featherw8.player.ui.components

import android.util.Size
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.featherw8.player.data.MusicTrack
import com.featherw8.player.ui.artwork.TrackArtwork
import com.featherw8.player.ui.model.TrackAction
import com.featherw8.player.ui.theme.CardBlack
import com.featherw8.player.ui.theme.FeatherRed
import com.featherw8.player.ui.theme.MutedText
import com.featherw8.player.ui.theme.PrimaryText

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TrackRow(
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
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                color = if (playing) FeatherRed else PrimaryText
            )
            Text(
                track.subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
                color = MutedText,
                modifier = Modifier.padding(top = 3.dp)
            )
            Text(track.sizeLabel, maxLines = 1, fontSize = 9.sp, color = MutedText, modifier = Modifier.padding(top = 3.dp))
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
internal fun SelectionCheckBox(checked: Boolean, modifier: Modifier = Modifier) {
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
internal fun LibrarySelectionActionBar(
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
internal fun SelectionActionButton(
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
internal fun TrackOptionsDropdown(
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
