package com.featherw8.player.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ListEditsTest {
    private val list = listOf("a", "b", "c", "d")

    @Test
    fun movesAnElementUpAndDown() {
        assertEquals(listOf("a", "c", "b", "d"), list.moved(2, 1)) // up
        assertEquals(listOf("a", "c", "b", "d"), list.moved(1, 2)) // down
    }

    @Test
    fun movesToTheEnds() {
        assertEquals(listOf("d", "a", "b", "c"), list.moved(3, 0))
        assertEquals(listOf("b", "c", "d", "a"), list.moved(0, 3))
    }

    @Test
    fun movesSeveralPlacesAtOnce() {
        // A fast drag can cross more than one row between two frames.
        assertEquals(listOf("c", "a", "b", "d"), list.moved(2, 0))
        assertEquals(listOf("a", "d", "b", "c"), list.moved(3, 1))
    }

    @Test
    fun leavesTheListAloneForNoOpsAndBadIndices() {
        assertSame(list, list.moved(1, 1))
        assertSame(list, list.moved(-1, 2))
        assertSame(list, list.moved(0, 4))
        assertSame(list, list.moved(4, 0))
    }

    @Test
    fun doesNotModifyTheOriginal() {
        list.moved(0, 3)
        assertEquals(listOf("a", "b", "c", "d"), list)
    }

    @Test
    fun movesAnIdOverAnother() {
        assertEquals(listOf("a", "c", "b", "d"), list.movedOver("c", "b"))
        assertEquals(listOf("a", "c", "b", "d"), list.movedOver("b", "c"))
        assertEquals(listOf("d", "a", "b", "c"), list.movedOver("d", "a"))
    }

    @Test
    fun movesIdsThatAreNotNeighboursInTheStoredList() {
        // "b" and "c" look adjacent on screen when "x" (a deleted file) sits between them in storage.
        val stored = listOf("a", "b", "x", "c", "d")
        assertEquals(listOf("a", "c", "b", "x", "d"), stored.movedOver("c", "b"))
        assertEquals(listOf("a", "x", "c", "b", "d"), stored.movedOver("b", "c"))
    }

    @Test
    fun ignoresMissingIdsAndSelfMoves() {
        assertSame(list, list.movedOver("a", "zzz"))
        assertSame(list, list.movedOver("zzz", "a"))
        assertSame(list, list.movedOver("a", "a"))
    }
}
