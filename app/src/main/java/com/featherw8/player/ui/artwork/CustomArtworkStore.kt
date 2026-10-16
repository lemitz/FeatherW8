package com.featherw8.player.ui.artwork

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal object CustomArtworkStore {
    private const val PREFIX = "custom_cover_uri_"
    var covers by mutableStateOf<Map<String, Uri>>(emptyMap())
        private set

    fun load(context: Context) {
        val preferences = context.getSharedPreferences("featherw8", Context.MODE_PRIVATE)
        covers = preferences.all.mapNotNull { (key, value) ->
            if (!key.startsWith(PREFIX)) return@mapNotNull null
            val uri = (value as? String)?.let(Uri::parse) ?: return@mapNotNull null
            key.removePrefix(PREFIX) to uri
        }.toMap()
    }

    fun get(trackId: String): Uri? = covers[trackId]

    fun put(context: Context, trackId: String, uri: Uri) {
        covers = covers + (trackId to uri)
        context.getSharedPreferences("featherw8", Context.MODE_PRIVATE).edit()
            .putString(PREFIX + trackId, uri.toString()).apply()
    }

    fun remove(context: Context, trackId: String) {
        covers = covers - trackId
        context.getSharedPreferences("featherw8", Context.MODE_PRIVATE).edit()
            .remove(PREFIX + trackId).apply()
    }
}
