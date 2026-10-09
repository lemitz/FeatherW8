package com.featherw8.player.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans only the primary shared-storage volume (the phone's internal storage).
 * It never enumerates removable volumes and never offers a source picker.
 */
class MediaLibraryRepository(context: Context) {
    private val resolver: ContentResolver = context.applicationContext.contentResolver

    suspend fun scanInternalStorage(): List<MusicTrack> = withContext(Dispatchers.IO) {
        val rows = LinkedHashMap<String, MusicTrack>()
        val audioCollection = primaryAudioCollection()
        val audioProjection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.MIME_TYPE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.MediaColumns.RELATIVE_PATH)
        }.toTypedArray()

        resolver.query(
            audioCollection,
            audioProjection,
            null,
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val pathCol = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val displayName = cursor.getString(nameCol).orEmpty().ifBlank { "Audio $id" }
                val title = cursor.getString(titleCol).orEmpty()
                    .takeUnless { it.isBlank() || it == "<unknown>" }
                    ?: displayName.substringBeforeLast('.', displayName).replace('_', ' ')
                val artist = cursor.getString(artistCol).orEmpty()
                    .takeUnless { it.isBlank() || it == "<unknown>" }
                    ?: "Unknown Artist"
                val album = cursor.getString(albumCol).orEmpty()
                    .takeUnless { it.isBlank() || it == "<unknown>" }
                    ?: "Unknown Album"
                val albumId = cursor.getLong(albumIdCol)
                val uri = ContentUris.withAppendedId(audioCollection, id)
                rows[id.toString()] = MusicTrack(
                    id = id.toString(),
                    uri = uri,
                    displayName = displayName,
                    title = title,
                    artist = artist,
                    album = album,
                    durationMs = cursor.getLong(durationCol).coerceAtLeast(0L),
                    sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L),
                    dateAddedSeconds = cursor.getLong(addedCol),
                    albumId = albumId,
                    mimeType = cursor.getString(mimeCol).orEmpty(),
                    relativePath = if (pathCol >= 0) cursor.getString(pathCol).orEmpty() else ""
                )
            }
        }

        // Android's media indexer may not classify DSD extensions as audio. Include
        // DSF/DFF/DSD rows from the same primary volume so they remain visible in Library.
        val filesCollection = primaryFilesCollection()
        val fileProjection = buildList {
            add(MediaStore.Files.FileColumns._ID)
            add(MediaStore.Files.FileColumns.DISPLAY_NAME)
            add(MediaStore.Files.FileColumns.MIME_TYPE)
            add(MediaStore.Files.FileColumns.SIZE)
            add(MediaStore.Files.FileColumns.DATE_ADDED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.MediaColumns.RELATIVE_PATH)
        }.toTypedArray()
        val nameColumn = MediaStore.Files.FileColumns.DISPLAY_NAME
        val dsdSelection = "LOWER($nameColumn) LIKE ? OR LOWER($nameColumn) LIKE ? OR LOWER($nameColumn) LIKE ?"
        val dsdArgs = arrayOf("%.dsf", "%.dff", "%.dsd")
        resolver.query(
            filesCollection,
            fileProjection,
            dsdSelection,
            dsdArgs,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val pathCol = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val key = id.toString()
                if (key in rows) continue
                val displayName = cursor.getString(nameCol).orEmpty().ifBlank { "DSD audio $id" }
                val title = displayName.substringBeforeLast('.', displayName).replace('_', ' ')
                rows[key] = MusicTrack(
                    id = key,
                    uri = ContentUris.withAppendedId(filesCollection, id),
                    displayName = displayName,
                    title = title,
                    artist = "Unknown Artist",
                    album = "Unknown Album",
                    durationMs = 0L,
                    sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L),
                    dateAddedSeconds = cursor.getLong(addedCol),
                    albumId = -1L,
                    mimeType = cursor.getString(mimeCol).orEmpty().ifBlank { "audio/x-dsd" },
                    relativePath = if (pathCol >= 0) cursor.getString(pathCol).orEmpty() else ""
                )
            }
        }

        rows.values.sortedByDescending { it.dateAddedSeconds }
    }

    private fun primaryAudioCollection(): Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    private fun primaryFilesCollection(): Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Files.getContentUri("external")
    }
}
