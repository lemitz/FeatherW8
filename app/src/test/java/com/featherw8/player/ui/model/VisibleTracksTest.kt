package com.featherw8.player.ui.model

import org.junit.Assert.assertEquals
import org.junit.Test

class VisibleTracksTest {
    private data class Track(
        val id: String,
        val title: String,
        val artist: String = "Artist",
        val album: String = "Album",
        val mimeType: String = "audio/mpeg",
        val sizeBytes: Long = 1_000L,
        val durationMs: Long = 60_000L,
        val dateAddedSeconds: Long = 0L
    )

    private fun visible(
        tracks: List<Track>,
        minimumSizeBytes: Long = 0L,
        minimumDurationSeconds: Float = 0f,
        query: String = "",
        sortMode: SortMode = SortMode.TITLE,
        playCounts: Map<String, Int> = emptyMap()
    ) = filterAndSortTracks(
        tracks = tracks,
        minimumSizeBytes = minimumSizeBytes,
        minimumDurationSeconds = minimumDurationSeconds,
        query = query,
        sortMode = sortMode,
        playCounts = playCounts,
        id = Track::id,
        title = Track::title,
        artist = Track::artist,
        album = Track::album,
        mimeType = Track::mimeType,
        sizeBytes = Track::sizeBytes,
        durationMs = Track::durationMs,
        dateAddedSeconds = Track::dateAddedSeconds
    )

    @Test
    fun unknownDurationPassesDurationFilterAndKnownDurationUsesWholeSeconds() {
        val unknown = Track("unknown", "Unknown", durationMs = 0L)
        val knownAtBoundary = Track("boundary", "Boundary", durationMs = 5_999L)
        val belowBoundary = Track("short", "Short", durationMs = 4_999L)

        assertEquals(
            listOf("Boundary", "Unknown"),
            visible(listOf(unknown, knownAtBoundary, belowBoundary), minimumDurationSeconds = 5f)
                .map { it.title }
        )
    }

    @Test
    fun filtersByInclusiveSizeAndSearchesAllVisibleTextFieldsIgnoringCase() {
        val sizeBoundary = Track("one", "One", sizeBytes = 500L, artist = "The Vocalists")
        val tooSmall = Track("two", "Two", sizeBytes = 499L, mimeType = "audio/flac")
        val mimeMatch = Track("three", "Three", sizeBytes = 600L, mimeType = "audio/FLAC")
        val albumMatch = Track("four", "Four", sizeBytes = 700L, album = "Blue Note")

        assertEquals(
            listOf("One"),
            visible(listOf(sizeBoundary, tooSmall, mimeMatch, albumMatch), minimumSizeBytes = 500L, query = "vOcAl")
                .map { it.title }
        )
        assertEquals(
            listOf("Three"),
            visible(listOf(sizeBoundary, tooSmall, mimeMatch, albumMatch), minimumSizeBytes = 500L, query = "flac")
                .map { it.title }
        )
        assertEquals(
            listOf("Four"),
            visible(listOf(sizeBoundary, tooSmall, mimeMatch, albumMatch), minimumSizeBytes = 500L, query = "blue note")
                .map { it.title }
        )
    }

    @Test
    fun sortsByTitleAddedAndPlayCountDescendingWithStableTies() {
        val first = Track("first", "beta", dateAddedSeconds = 10L)
        val second = Track("second", "Alpha", dateAddedSeconds = 20L)
        val third = Track("third", "alpha", dateAddedSeconds = 20L)

        assertEquals(listOf("Alpha", "alpha", "beta"), visible(listOf(first, second, third)).map { it.title })
        assertEquals(
            listOf("second", "third", "first"),
            visible(listOf(first, second, third), sortMode = SortMode.ADDED).map { it.id }
        )
        assertEquals(
            listOf("first", "third", "second"),
            visible(
                listOf(first, second, third),
                sortMode = SortMode.PLAYS,
                playCounts = mapOf("first" to 4, "third" to 4, "second" to 1)
            ).map { it.id }
        )
    }
}
