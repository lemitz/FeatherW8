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
 * A copy of the ids with [first] and [second] exchanging places. Playlist edits go by id because the
 * list on screen leaves out tracks whose file is gone: two neighbours on screen need not be neighbours in
 * the stored playlist, but exchanging them keeps every other track where it is and puts the two in the
 * order that was asked for. Unchanged when either id is missing.
 */
internal fun List<String>.swapped(first: String, second: String): List<String> {
    val a = indexOf(first)
    val b = indexOf(second)
    if (a < 0 || b < 0 || a == b) return this
    return toMutableList().apply {
        this[a] = second
        this[b] = first
    }
}
