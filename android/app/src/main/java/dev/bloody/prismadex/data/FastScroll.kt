package dev.bloody.prismadex.data

import kotlin.math.roundToInt

/**
 * Pure fast-scroll math for the Pokédex grid/list scrollbar (11:46).
 *
 * The thumb and the bubble share ONE fraction: 0 = first list item at the
 * top of the viewport, 1 = last list item reached. Dragging DOWN must
 * always increase the fraction (forward through the list), dragging UP
 * must decrease it. Both functions are monotonic by construction and are
 * covered by FastScrollTest.
 *
 * Progress uses item-space counts from the lazy layout (first visible
 * item / total items / visible item count), which is monotonic in both
 * list (1 column) and grid (2+ columns) layouts. The bubble label is
 * always the item the fraction points at in the CURRENT list order, so
 * it can never run ahead of, behind, or inverted against the on-screen
 * scroll position.
 */

/** Scroll progress from the lazy layout: 0 at the top, 1 when the last items are visible. */
internal fun fastScrollProgress(firstVisibleIndex: Int, totalItems: Int, visibleItemCount: Int): Float {
    val span = (totalItems - visibleItemCount).coerceAtLeast(1)
    return (firstVisibleIndex.coerceIn(0, span).toFloat() / span).coerceIn(0f, 1f)
}

/** List index for a drag fraction. 0 -> first item, 1 -> last item. */
internal fun fastScrollTargetIndex(fraction: Float, listSize: Int): Int {
    if (listSize <= 0) return 0
    return (fraction.coerceIn(0f, 1f) * (listSize - 1)).roundToInt().coerceIn(0, listSize - 1)
}

/** Bubble label for the target item: first letter in A-Z sort, #number otherwise. */
internal fun fastScrollBubbleLabel(target: Pokemon, alphabetical: Boolean): String =
    if (alphabetical) target.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?" else "#${target.baseId}"
