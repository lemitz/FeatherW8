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
    fun swapsTwoIds() {
        assertEquals(listOf("a", "c", "b", "d"), list.swapped("b", "c"))
        assertEquals(listOf("a", "c", "b", "d"), list.swapped("c", "b"))
    }

    @Test
    fun swapsIdsThatAreNotNeighboursInTheStoredList() {
        // "b" and "c" look adjacent on screen when "x" (a deleted file) sits between them in storage.
        val stored = listOf("a", "b", "x", "c", "d")
        assertEquals(listOf("a", "c", "x", "b", "d"), stored.swapped("b", "c"))
    }

    @Test
    fun ignoresMissingIdsAndSelfSwaps() {
        assertSame(list, list.swapped("a", "zzz"))
        assertSame(list, list.swapped("zzz", "a"))
        assertSame(list, list.swapped("a", "a"))
    }
}
