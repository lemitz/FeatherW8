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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.featherw8.player.ui.components.rememberReorderState
import com.featherw8.player.ui.components.TrackRow
import com.featherw8.player.ui.theme.AmoledBlack
import com.featherw8.player.ui.theme.MutedText

/** The "Playback queue" header is the first list item; the tracks follow it. */
private const val QUEUE_HEADER_ITEMS = 1

/** How many tracks before the current one stay visible above it, so it is not glued to the top edge. */
private const val QUEUE_CONTEXT_ROWS = 2

@Composable
internal fun QueueScreen(
    queue: List<MusicTrack>,
    currentId: String?,
    onPlay: (Int) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (index: Int) -> Unit,
    onOpenPlayer: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentIndex = queue.indexOfFirst { it.id == currentId }
    // The page is created each time the queue is opened, so starting the list at the current track
    // here means it appears already there, instead of at the top and then jumping.
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (currentIndex < 0) 0 else {
            (currentIndex + QUEUE_HEADER_ITEMS - QUEUE_CONTEXT_ROWS).coerceAtLeast(0)
        }
    )
    // While the queue stays open the player moves on to the next track: follow it, but only when it
    // has left the screen, so a track you are looking at does not get scrolled under your finger.
    LaunchedEffect(currentId) {
        if (currentIndex < 0) return@LaunchedEffect
        val visible = listState.layoutInfo.visibleItemsInfo
        if (visible.isEmpty()) return@LaunchedEffect // not measured yet: the initial index covers this
        val target = currentIndex + QUEUE_HEADER_ITEMS
        if (visible.none { it.index == target }) {
            listState.animateScrollToItem((target - QUEUE_CONTEXT_ROWS).coerceAtLeast(0))
        }
    }
    val reorder = rememberReorderState(
        listState = listState,
        firstItemIndex = QUEUE_HEADER_ITEMS,
        itemCount = queue.size,
        onMove = onMove
    )
    LazyColumn(
        state = listState,
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
        itemsIndexed(queue, key = { _, track -> track.id }, contentType = { _, _ -> "track" }) { index, track ->
            // The current track stays marked while paused too: this is "where am I in the queue".
            val isCurrent = track.id == currentId
            TrackRow(
                track = track,
                plays = 0,
                playing = isCurrent,
                highlighted = isCurrent,
                showPlays = false, // the queue has no counts to show; a constant "0x" on every row was wrong
                onPlay = { onPlay(index) },
                onAction = {},
                // The row being held is drawn at the finger; animating its placement as well would fight that.
                modifier = if (reorder.draggedKey == track.id) Modifier else Modifier.animateItem(),
                // Hold and drag to reorder; the cross takes the track out. The library's track menu
                // (playlist, ringtone, share, delete) did nothing here and is gone.
                reorder = reorder,
                onRemove = { onRemove(index) }
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
