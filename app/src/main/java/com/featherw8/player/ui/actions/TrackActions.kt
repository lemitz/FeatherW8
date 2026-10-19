package com.featherw8.player.ui.actions

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Size
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.featherw8.player.audio.DsdLibrary
import com.featherw8.player.audio.withDsdInfo
import com.featherw8.player.data.MusicTrack
import kotlinx.coroutines.launch
import com.featherw8.player.ui.artwork.CustomArtworkStore

internal fun audioPermission(): String = if (Build.VERSION.SDK_INT >= 33) {
    Manifest.permission.READ_MEDIA_AUDIO
} else {
    Manifest.permission.READ_EXTERNAL_STORAGE
}

/**
 * Maps a file opened from another app to a track. A file that is already in the library keeps its
 * library identity (play counts, cover); anything else becomes a standalone track.
 */
internal fun resolveExternalTrack(context: Context, uri: Uri, library: List<MusicTrack>): MusicTrack {
    library.firstOrNull { it.uri == uri }?.let { return it }
    if (uri.authority == MediaStore.AUTHORITY) {
        runCatching { ContentUris.parseId(uri).toString() }.getOrNull()
            ?.let { id -> library.firstOrNull { it.id == id } }
            ?.let { return it }
    }

    var displayName: String? = null
    var sizeBytes = 0L
    runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameColumn >= 0) displayName = cursor.getString(nameColumn)
                val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) sizeBytes = cursor.getLong(sizeColumn)
            }
        }
    }
    val name = displayName?.takeIf { it.isNotBlank() }
        ?: uri.lastPathSegment?.substringAfterLast('/')
        ?: "Audio"
    var track = MusicTrack(
        id = "ext:$uri",
        uri = uri,
        displayName = name,
        title = name.substringBeforeLast('.', name).replace('_', ' '),
        artist = "Unknown Artist",
        album = "Unknown Album",
        durationMs = 0L,
        sizeBytes = sizeBytes.coerceAtLeast(0L),
        dateAddedSeconds = System.currentTimeMillis() / 1_000L,
        albumId = -1L,
        mimeType = context.contentResolver.getType(uri).orEmpty()
    )
    if (track.isDsd) {
        track = track.withDsdInfo(DsdLibrary.info(context, uri, "ext:$uri:${track.sizeBytes}"))
    }
    track.precompute()
    return track
}

internal fun MusicTrack.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id)
    .setUri(uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(CustomArtworkStore.get(id) ?: artworkUri)
            // For the play counter's 90 % rule when the player cannot tell the length of a file itself.
            .setDurationMs(durationMs.takeIf { it > 0L })
            .build()
    )
    .build()

internal fun shareTrack(context: Context, track: MusicTrack) {
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = track.mimeType.ifBlank { "audio/*" }
            putExtra(Intent.EXTRA_STREAM, track.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share track"))
    }.onFailure { Toast.makeText(context, "Couldn't share this track", Toast.LENGTH_SHORT).show() }
}

internal fun shareTracks(context: Context, tracks: List<MusicTrack>) {
    if (tracks.isEmpty()) return
    if (tracks.size == 1) {
        shareTrack(context, tracks.first())
        return
    }
    runCatching {
        val streams = ArrayList<Uri>(tracks.size).apply { tracks.forEach { add(it.uri) } }
        val clipData = ClipData.newUri(context.contentResolver, "Audio files", streams.first()).apply {
            streams.drop(1).forEach { addItem(ClipData.Item(it)) }
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "audio/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, streams)
            this.clipData = clipData
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share files"))
    }.onFailure {
        Toast.makeText(context, "Couldn't share these files", Toast.LENGTH_SHORT).show()
    }
}

internal fun setAsRingtone(context: Context, track: MusicTrack) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")))
        }
        Toast.makeText(context, "Allow ringtone access, then try again", Toast.LENGTH_LONG).show()
        return
    }
    runCatching {
        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, track.uri)
        Toast.makeText(context, "Ringtone updated", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, "Couldn't set this ringtone", Toast.LENGTH_SHORT).show()
    }
}

internal fun deleteFromMediaStore(
    context: Context,
    tracks: List<MusicTrack>,
    launcher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>,
    onDeleted: () -> Unit
) {
    if (tracks.isEmpty()) return
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val request = MediaStore.createDeleteRequest(context.contentResolver, tracks.map { it.uri })
            launcher.launch(IntentSenderRequest.Builder(request.intentSender).build())
        } else {
            tracks.forEach { track -> context.contentResolver.delete(track.uri, null, null) }
            onDeleted()
        }
    }.onFailure {
        val message = if (tracks.size == 1) "Android couldn't delete this file" else "Android couldn't delete these files"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
