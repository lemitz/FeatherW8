package com.featherw8.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.featherw8.player.data.MusicTrack
import com.featherw8.player.ui.components.EmptyCard
import com.featherw8.player.ui.components.TrackRow
import com.featherw8.player.ui.theme.AmoledBlack
import com.featherw8.player.ui.theme.MutedText

@Composable
internal fun QueueScreen(
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
        items(queue, key = { it.id }, contentType = { "track" }) { track ->
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
