package com.featherw8.player.data

import android.content.Context

object PlayCountStore {
    private const val PREFS = "featherw8_play_counts"
    private fun key(id: String) = "full_plays_$id"

    fun get(context: Context, id: String): Int =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(key(id), 0)

    fun increment(context: Context, id: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(key(id), prefs.getInt(key(id), 0) + 1).apply()
    }
}
