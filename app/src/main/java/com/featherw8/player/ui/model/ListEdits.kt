package com.featherw8.player.ui.model

/**
 * A copy of the list with the element at [from] moved to position [to] (the position it has afterwards).
 * Returns the list itself when nothing would change or an index is outside the list.
 */
internal fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from == to || from !in indices || to !in indices) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

/**
 * A copy of the ids with [id] moved to the place [targetId] has. Playlist edits go by id because the list
 * on screen leaves out tracks whose file is gone: a position on screen is not a position in the stored
 * playlist. Moving over the id the user dragged past keeps every other track where it is. Unchanged when
 * either id is missing.
 */
internal fun List<String>.movedOver(id: String, targetId: String): List<String> =
    moved(indexOf(id), indexOf(targetId))
