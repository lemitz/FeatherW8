package com.featherw8.player.ui.model

import androidx.compose.foundation.layout.Row
import org.json.JSONArray
import org.json.JSONObject

internal fun loadFavorites(preferences: android.content.SharedPreferences): Set<String> =
    preferences.getStringSet("favorites", emptySet())?.toSet().orEmpty()

internal fun loadPlaylists(preferences: android.content.SharedPreferences): List<UserPlaylist> = runCatching {
    val array = JSONArray(preferences.getString("user_playlists", "[]"))
    buildList {
        for (index in 0 until array.length()) {
            val row = array.getJSONObject(index)
            val ids = row.optJSONArray("trackIds") ?: JSONArray()
            add(UserPlaylist(row.optString("id"), row.optString("name"), buildList {
                for (idIndex in 0 until ids.length()) add(ids.optString(idIndex))
            }))
        }
    }
}.getOrDefault(emptyList())

internal fun encodePlaylists(playlists: List<UserPlaylist>): String {
    val array = JSONArray()
    playlists.forEach { playlist ->
        val ids = JSONArray()
        playlist.trackIds.forEach { ids.put(it) }
        array.put(JSONObject().put("id", playlist.id).put("name", playlist.name).put("trackIds", ids))
    }
    return array.toString()
}
