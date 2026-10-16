package com.featherw8.player.ui.model

import android.os.Build
import android.provider.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import com.featherw8.player.data.MusicTrack

internal const val PlayerNowPlayingPage = 0
internal const val PlayerQueuePage = 1

internal enum class AppPage {
    LIBRARY, PLAYLISTS, SETTINGS,
    ALBUMS, ARTISTS, ALBUM_DETAIL, ARTIST_DETAIL, PLAYLIST_DETAIL,

    // Kept only so instance state saved by an older build (which used a separate
    // queue page) can still be restored. Nothing navigates here any more - the queue
    // now lives on the second page of the player pager.
    QUEUE
}
internal data class AlbumCollection(val key: String, val title: String, val artist: String, val tracks: List<MusicTrack>)
internal data class ArtistCollection(val key: String, val name: String, val tracks: List<MusicTrack>)
internal data class PlaylistDetailState(val title: String, val tracks: List<MusicTrack>, val userPlaylistId: String? = null)
internal enum class SortMode(val label: String) {
    TITLE("Track title"),
    ADDED("Date added"),
    PLAYS("Play count");

    companion object {
        fun from(value: String?): SortMode = entries.firstOrNull { it.name == value } ?: TITLE
    }
}
internal enum class TrackAction { ADD_TO_PLAYLIST, INFO, RINGTONE, SHARE, DELETE }
internal data class UserPlaylist(val id: String, val name: String, val trackIds: List<String>)
internal data class SmartPlaylistCollections(
    val recent: List<MusicTrack> = emptyList(),
    val highResolution: List<MusicTrack> = emptyList(),
    val mostPlayed: List<MusicTrack> = emptyList(),
    val favorites: List<MusicTrack> = emptyList()
)
internal data class PlaybackSnapshot(
    val mediaId: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playing: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleModeEnabled: Boolean = false
)
