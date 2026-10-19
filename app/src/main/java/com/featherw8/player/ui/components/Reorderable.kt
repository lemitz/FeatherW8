package com.featherw8.player.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

/** How far, in pixels, the list scrolls per frame while a row is held near its top or bottom edge. */
private const val AUTO_SCROLL_PX_PER_FRAME = 16f

/** How much bigger the held row is drawn, so it reads as lifted. */
private const val DRAGGED_ROW_SCALE = 1.03f

/**
 * Drag-to-reorder for the rows of a LazyColumn: hold a row, then move it.
 *
 * The row under the finger is not moved in the list while the finger travels; it is only drawn at the
 * finger's position (a translation on top of wherever the list has it). When its centre crosses a
 * neighbour, [onMove] is called with the two row positions and the list is expected to reorder; the
 * translation is measured from the row's new place, so the row stays under the finger across the swap.
 *
 * Positions passed to [onMove] count only the reorderable rows: [firstItemIndex] items before them (a
 * header) are skipped, and [itemCount] says where they end (a trailing button is not one of them).
 */
internal class ReorderState(
    private val listState: LazyListState,
    private val firstItemIndex: Int,
    private val onMove: (from: Int, to: Int) -> Unit
) {
    /** How many reorderable rows there are; refreshed on every composition. */
    var itemCount: Int = 0

    /** Key of the row being held, or null. */
    var draggedKey by mutableStateOf<Any?>(null)
        private set

    /** Top edge of the held row where the finger has put it, in the list's viewport coordinates. */
    private var visualTop by mutableFloatStateOf(0f)

    /**
     * Position of the held row in the list right after a move was requested. The list lays out the move
     * a frame or two later; until it does, asking again would apply the same move twice.
     */
    private var awaitingLayoutAt = -1

    private fun heldRow() = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == draggedKey }

    fun start(key: Any) {
        val row = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return
        draggedKey = key
        visualTop = row.offset.toFloat()
        awaitingLayoutAt = -1
    }

    fun drag(deltaY: Float) {
        if (draggedKey == null) return
        visualTop += deltaY
        moveIfCrossedNeighbour()
    }

    fun stop() {
        draggedKey = null
        awaitingLayoutAt = -1
    }

    /** How far the row with [key] has to be drawn from where the list has it; 0 for every other row. */
    fun translationFor(key: Any): Float {
        if (key != draggedKey) return 0f
        val row = heldRow() ?: return 0f
        return visualTop - row.offset
    }

    private fun moveIfCrossedNeighbour() {
        val rows = listState.layoutInfo.visibleItemsInfo
        val held = rows.firstOrNull { it.key == draggedKey } ?: return
        if (held.index == awaitingLayoutAt) return
        awaitingLayoutAt = -1
        val centre = visualTop + held.size / 2f
        val reorderable = firstItemIndex until firstItemIndex + itemCount
        val target = rows.firstOrNull {
            it.key != draggedKey && it.index in reorderable && centre >= it.offset && centre < it.offset + it.size
        } ?: return
        awaitingLayoutAt = held.index
        onMove(held.index - firstItemIndex, target.index - firstItemIndex)
    }

    /** One frame of edge scrolling: if the held row is near the top or bottom, scroll that way. */
    suspend fun autoScrollStep() {
        val held = heldRow() ?: return
        val layout = listState.layoutInfo
        val edge = held.size * 0.7f
        val step = when {
            visualTop < layout.viewportStartOffset + edge -> -AUTO_SCROLL_PX_PER_FRAME
            visualTop + held.size > layout.viewportEndOffset - edge -> AUTO_SCROLL_PX_PER_FRAME
            else -> 0f
        }
        if (step != 0f) {
            listState.scrollBy(step)
            moveIfCrossedNeighbour()
        }
    }
}

@Composable
internal fun rememberReorderState(
    listState: LazyListState,
    firstItemIndex: Int,
    itemCount: Int,
    onMove: (from: Int, to: Int) -> Unit
): ReorderState {
    val latestOnMove by rememberUpdatedState(onMove)
    val state = remember(listState, firstItemIndex) {
        ReorderState(listState, firstItemIndex) { from, to -> latestOnMove(from, to) }
    }
    state.itemCount = itemCount
    // While a row is held, keep the list scrolling if the finger rests near an edge.
    LaunchedEffect(state.draggedKey) {
        if (state.draggedKey == null) return@LaunchedEffect
        while (true) {
            state.autoScrollStep()
            delay(16L)
        }
    }
    return state
}

/** Draws the row at the finger while it is held (lifted, above its neighbours); a no-op otherwise. */
internal fun Modifier.reorderLayer(state: ReorderState, key: Any): Modifier =
    // Which row is held changes only when a drag starts or ends, so reading it while composing is cheap;
    // the finger position changes every frame and is read in the layer, where it only redraws.
    zIndex(if (state.draggedKey == key) 1f else 0f).graphicsLayer {
        val held = state.draggedKey == key
        translationY = state.translationFor(key)
        scaleX = if (held) DRAGGED_ROW_SCALE else 1f
        scaleY = scaleX
    }

/**
 * Long-press, then drag. Meant to sit *inside* the row's click handling: it then sees the touch first and
 * consumes it, so holding a row and letting go does not also count as a tap that starts playback.
 */
@Composable
internal fun Modifier.reorderGesture(state: ReorderState, key: Any): Modifier {
    val haptics = LocalHapticFeedback.current
    return pointerInput(key) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                state.start(key)
            },
            onDrag = { change, amount ->
                change.consume()
                state.drag(amount.y)
            },
            onDragEnd = { state.stop() },
            onDragCancel = { state.stop() }
        )
    }
}
