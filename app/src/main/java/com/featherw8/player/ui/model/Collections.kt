package com.featherw8.player.ui.model

import com.featherw8.player.data.MusicTrack

internal fun buildAlbumCollections(tracks: List<MusicTrack>): List<AlbumCollection> = tracks
    .groupBy { track ->
        val title = track.album.trim().ifBlank { "Unknown Album" }.lowercase()
        val artist = track.artist.trim().ifBlank { "Unknown Artist" }.lowercase()
        if (track.albumId >= 0L) "${track.albumId}|$artist|$title" else "$artist|$title"
    }
    .map { (key, albumTracks) ->
        val sortedTracks = albumTracks.sortedBy { it.title.lowercase() }
        AlbumCollection(key, sortedTracks.first().album, sortedTracks.first().artist, sortedTracks)
    }
    .sortedBy { it.title.lowercase() }

internal fun buildArtistCollections(tracks: List<MusicTrack>): List<ArtistCollection> = tracks
    .groupBy { it.artist.trim().ifBlank { "Unknown Artist" }.lowercase() }
    .map { (key, artistTracks) -> ArtistCollection(key, artistTracks.first().artist, artistTracks.sortedBy { it.title.lowercase() }) }
    .sortedBy { it.name.lowercase() }

internal fun buildSmartPlaylistCollections(
    tracks: List<MusicTrack>,
    favorites: Set<String>,
    playCount: (String) -> Int
): SmartPlaylistCollections {
    val mostPlayed = tracks.mapNotNull { track ->
        val count = playCount(track.id)
        if (count > 0) track to count else null
    }.sortedByDescending { it.second }.take(10).map { it.first }

    return SmartPlaylistCollections(
        recent = tracks.sortedByDescending { it.dateAddedSeconds }.take(10),
        highResolution = tracks.filter { it.isLikelyHiRes },
        mostPlayed = mostPlayed,
        favorites = tracks.filter { it.id in favorites }
    )
}
