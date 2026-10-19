package com.featherw8.player.ui.screens

import android.util.Size
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.featherw8.player.data.MusicTrack
import com.featherw8.player.ui.artwork.TrackArtwork
import com.featherw8.player.ui.components.EmptyCard
import com.featherw8.player.ui.components.rememberReorderState
import com.featherw8.player.ui.components.SectionLabel
import com.featherw8.player.ui.components.TrackRow
import com.featherw8.player.ui.model.AlbumCollection
import com.featherw8.player.ui.model.ArtistCollection
import com.featherw8.player.ui.model.TrackAction
import com.featherw8.player.ui.theme.CardBlack
import com.featherw8.player.ui.theme.MutedText

@Composable
internal fun AlbumsScreen(
    albums: List<AlbumCollection>,
    listState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onOpenAlbum: (AlbumCollection) -> Unit,
    bottomOverlayPadding: Dp = 0.dp
) {
    if (albums.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) {
            EmptyCard("No albums yet", "Album cards appear when your tracks include album metadata.")
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 20.dp + bottomOverlayPadding),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(albums, key = { it.key }) { album ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenAlbum(album) },
                    colors = CardDefaults.cardColors(containerColor = CardBlack),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(8.dp)) {
                        album.tracks.firstOrNull()?.let { cover ->
                            TrackArtwork(cover, Modifier.fillMaxWidth().aspectRatio(1f), RoundedCornerShape(11.dp))
                        }
                        Text(album.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif, modifier = Modifier.padding(top = 8.dp))
                        Text(album.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp, color = MutedText, fontFamily = FontFamily.SansSerif, modifier = Modifier.padding(top = 3.dp))
                        Text("${album.tracks.size} tracks", fontSize = 9.sp, color = MutedText, modifier = Modifier.padding(top = 5.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun ArtistsScreen(
    artists: List<ArtistCollection>,
    listState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onOpenArtist: (ArtistCollection) -> Unit,
    bottomOverlayPadding: Dp = 0.dp
) {
    if (artists.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) {
            EmptyCard("No artists yet", "Artist cards appear when your tracks include artist metadata.")
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 20.dp + bottomOverlayPadding),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(artists, key = { it.key }) { artist ->
                val albumCount = artist.tracks.map { it.albumId.takeIf { id -> id >= 0L }?.toString() ?: it.album.lowercase() }.distinct().size
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenArtist(artist) },
                    colors = CardDefaults.cardColors(containerColor = CardBlack),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(8.dp)) {
                        artist.tracks.firstOrNull()?.let { cover ->
                            TrackArtwork(cover, Modifier.fillMaxWidth().aspectRatio(1f), RoundedCornerShape(11.dp))
                        }
                        Text(artist.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif, modifier = Modifier.padding(top = 8.dp))
                        Text("$albumCount albums · ${artist.tracks.size} tracks", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 9.sp, color = MutedText, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TrackCollectionDetailScreen(
    title: String,
    subtitle: String,
    heroTrack: MusicTrack?,
    tracks: List<MusicTrack>,
    listState: LazyListState,
    onPlayAll: () -> Unit,
    onPlayTrack: (Int) -> Unit,
    onTrackAction: (MusicTrack, TrackAction) -> Unit,
    onManage: (() -> Unit)? = null,
    /** Reordering and removing tracks (hold and drag, cross to remove); only given for a playlist the user made. */
    onMoveTrack: ((from: Int, to: Int) -> Unit)? = null,
    onRemoveTrack: ((index: Int) -> Unit)? = null,
    bottomOverlayPadding: Dp = 0.dp
) {
    // The hero card and the "Tracks" label come before the first track.
    val reorder = if (onMoveTrack != null) {
        rememberReorderState(listState, firstItemIndex = 2, itemCount = tracks.size, onMove = onMoveTrack)
    } else {
        null
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 22.dp + bottomOverlayPadding),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardBlack), shape = RoundedCornerShape(17.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    heroTrack?.let { TrackArtwork(it, 70.dp, RoundedCornerShape(12.dp)) }
                    Column(Modifier.weight(1f).padding(start = 11.dp)) {
                        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
                        Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MutedText, fontSize = 10.sp, fontFamily = FontFamily.SansSerif, modifier = Modifier.padding(top = 4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onPlayAll, enabled = tracks.isNotEmpty()) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Text("Play all", modifier = Modifier.padding(start = 4.dp))
                            }
                            if (onManage != null) TextButton(onClick = onManage) { Text("Manage") }
                        }
                    }
                }
            }
        }
        item { SectionLabel("Tracks") }
        if (tracks.isEmpty()) item { EmptyCard("No tracks", "Add tracks from Library or scan internal storage.") }
        itemsIndexed(tracks, key = { _, track -> track.id }, contentType = { _, _ -> "track" }) { index, track ->
            TrackRow(
                track = track,
                plays = 0,
                playing = false,
                showPlays = false, // no counts here; a constant "0x" on every row was wrong
                onPlay = { onPlayTrack(index) },
                onAction = { onTrackAction(track, it) },
                // The row being held is drawn at the finger; animating its placement as well would fight that.
                modifier = if (reorder?.draggedKey == track.id) Modifier else Modifier.animateItem(),
                reorder = reorder,
                onRemove = if (reorder != null && onRemoveTrack != null) ({ onRemoveTrack(index) }) else null
            )
        }
    }
}
