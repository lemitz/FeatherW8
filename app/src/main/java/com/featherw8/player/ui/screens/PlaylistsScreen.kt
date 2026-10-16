package com.featherw8.player.ui.screens

import android.util.Size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.featherw8.player.data.MusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.featherw8.player.ui.components.EmptyCard
import com.featherw8.player.ui.components.SectionLabel
import com.featherw8.player.ui.model.AlbumCollection
import com.featherw8.player.ui.model.ArtistCollection
import com.featherw8.player.ui.model.buildSmartPlaylistCollections
import com.featherw8.player.ui.model.SmartPlaylistCollections
import com.featherw8.player.ui.model.UserPlaylist
import com.featherw8.player.ui.theme.CardBlack
import com.featherw8.player.ui.theme.FeatherRed
import com.featherw8.player.ui.theme.MutedText

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PlaylistsScreen(
    tracks: List<MusicTrack>,
    listState: LazyListState,
    playlists: List<UserPlaylist>,
    favorites: Set<String>,
    playCount: (String) -> Int,
    albums: List<AlbumCollection>,
    artists: List<ArtistCollection>,
    onOpenAlbums: () -> Unit,
    onOpenArtists: () -> Unit,
    onOpenSmartPlaylist: (String, List<MusicTrack>) -> Unit,
    onCreate: () -> Unit,
    onManagePlaylist: (UserPlaylist) -> Unit,
    onOpenUserPlaylist: (UserPlaylist) -> Unit,
    bottomOverlayPadding: Dp = 0.dp
) {
    var smartPlaylists by remember { mutableStateOf(SmartPlaylistCollections()) }
    LaunchedEffect(tracks, favorites, playCount) {
        val trackSnapshot = tracks
        val favoritesSnapshot = favorites
        val playCountSnapshot = playCount
        smartPlaylists = withContext(Dispatchers.Default) {
            buildSmartPlaylistCollections(trackSnapshot, favoritesSnapshot, playCountSnapshot)
        }
    }
    val recent = smartPlaylists.recent
    val highResolution = smartPlaylists.highResolution
    val mostPlayed = smartPlaylists.mostPlayed
    val favoritesList = smartPlaylists.favorites
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 9.dp, bottom = 20.dp + bottomOverlayPadding),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        item { Text("Playlists", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp)) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CollectionEntryCard("Albums", "${albums.size} albums", Icons.Filled.LibraryMusic, onOpenAlbums, Modifier.weight(1f))
                CollectionEntryCard("Artists", "${artists.size} artists", Icons.Filled.Waves, onOpenArtists, Modifier.weight(1f))
            }
        }
        item { SectionLabel("Smart playlists") }
        item { SmartPlaylistRow("Recently added", "Newest tracks", recent.size, Icons.AutoMirrored.Filled.Sort) { onOpenSmartPlaylist("Recently added", recent) } }
        item { SmartPlaylistRow("Hi-Res", "High-resolution files", highResolution.size, Icons.Filled.Waves) { onOpenSmartPlaylist("Hi-Res", highResolution) } }
        item { SmartPlaylistRow("Most played", "Sorted by full plays", mostPlayed.size, Icons.Filled.Repeat) { onOpenSmartPlaylist("Most played", mostPlayed) } }
        item { SmartPlaylistRow("Favorites", "Your liked tracks", favoritesList.size, Icons.Filled.Favorite) { onOpenSmartPlaylist("Favorites", favoritesList) } }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                SectionLabel("My playlists")
                TextButton(onClick = onCreate) { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null); Text("New", modifier = Modifier.padding(start = 4.dp)) }
            }
        }
        if (playlists.isEmpty()) {
            item { EmptyCard("No playlists yet", "Create a playlist and add tracks from Library.") }
        } else {
            items(playlists, key = { it.id }) { playlist ->
                Card(
                    modifier = Modifier.fillMaxWidth().combinedClickable(
                        onClick = { onOpenUserPlaylist(playlist) },
                        onLongClick = { onManagePlaylist(playlist) }
                    ),
                    colors = CardDefaults.cardColors(containerColor = CardBlack),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = FeatherRed, modifier = Modifier.size(24.dp))
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(playlist.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${playlist.trackIds.size} tracks", color = MutedText, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CollectionEntryCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBlack),
        shape = RoundedCornerShape(15.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 12.dp)) {
            Icon(icon, contentDescription = null, tint = FeatherRed, modifier = Modifier.size(21.dp))
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 9.dp))
            Text(subtitle, fontSize = 9.sp, color = MutedText, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
internal fun SmartPlaylistRow(title: String, subtitle: String, count: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBlack),
        shape = RoundedCornerShape(15.dp)
    ) {
        Row(Modifier.padding(horizontal = 11.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(FeatherRed.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = FeatherRed, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 9.sp, color = MutedText, modifier = Modifier.padding(top = 3.dp))
            }
            Text(count.toString(), fontSize = 10.sp, color = MutedText)
        }
    }
}

@Composable
internal fun CollectionPageEntrance(key: Any, content: @Composable () -> Unit) {
    val visibility = remember(key) {
        MutableTransitionState(false).apply { targetState = true }
    }
    AnimatedVisibility(
        visibleState = visibility,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(animationSpec = tween(170)) +
            slideInVertically(animationSpec = tween(210)) { it / 24 },
        exit = ExitTransition.None
    ) {
        Box(Modifier.fillMaxSize()) { content() }
    }
}
