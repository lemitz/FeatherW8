package com.featherw8.player.ui.screens

import android.util.Size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.featherw8.player.data.MusicTrack
import com.featherw8.player.ui.components.TrackRow
import com.featherw8.player.ui.model.SortMode
import com.featherw8.player.ui.model.TrackAction
import com.featherw8.player.ui.theme.CardBlack
import com.featherw8.player.ui.theme.FeatherRed
import com.featherw8.player.ui.theme.MutedText
import com.featherw8.player.ui.theme.PrimaryText

@Composable
internal fun LibraryScreen(
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
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 2.dp),
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
                items(tracks, key = { it.id }, contentType = { "track" }) { track ->
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
