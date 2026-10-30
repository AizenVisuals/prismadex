package dev.bloody.prismadex.data
import org.junit.Assert.*
import org.junit.Test

/** Fast-scroll mapping (11:46): fraction, thumb and bubble must move in the
 *  same direction as the finger, in the current list order, with no inversion
 *  or lag. These tests pin the pure math; the Compose gesture layer feeds it
 *  drag deltas only (down = fraction up, up = fraction down). */
class FastScrollTest {
    private fun p(id: Int, name: String = "Species $id") =
        Pokemon(id, id, name, listOf("normal"), listOf(50, 50, 50, 50, 50, 50), listOf("run-away"), 1.0, 1.0, true, false, generation = 1, games = listOf(23))

    @Test fun progressIsZeroAtTopAndOneAtBottom() {
        assertEquals(0f, fastScrollProgress(0, 100, 10), 0f)
        assertEquals(1f, fastScrollProgress(90, 100, 10), 0f)
        assertEquals(1f, fastScrollProgress(200, 100, 10), 0f)
        assertEquals(0f, fastScrollProgress(0, 5, 10), 0f)
        assertEquals(0f, fastScrollProgress(0, 1, 1), 0f)
    }

    @Test fun progressGrowsMonotonicallyWithFirstVisibleIndex() {
        val values = (0..90 step 10).map { fastScrollProgress(it, 100, 10) }
        assertEquals(values.sorted(), values)
        assertTrue(values.zipWithNext().all { (a, b) -> b > a })
    }

    @Test fun targetIndexMapsFractionToFirstMiddleLast() {
        assertEquals(0, fastScrollTargetIndex(0f, 1357))
        assertEquals(1356, fastScrollTargetIndex(1f, 1357))
        assertEquals(678, fastScrollTargetIndex(0.5f, 1357))
        assertEquals(0, fastScrollTargetIndex(-0.5f, 10))
        assertEquals(9, fastScrollTargetIndex(1.5f, 10))
        assertEquals(0, fastScrollTargetIndex(0.5f, 0))
        assertEquals(0, fastScrollTargetIndex(0.5f, 1))
    }

    @Test fun targetIndexIsMonotonicOverTheWholeTrack() {
        val targets = (0..100).map { fastScrollTargetIndex(it / 100f, 1357) }
        assertEquals(targets.sorted(), targets)
        assertEquals(0, targets.first())
        assertEquals(1356, targets.last())
    }

    @Test fun draggingDownMovesForwardThroughNumberOrder() {
        // The bubble label is the item at the fraction in list order: as the
        // fraction grows, the shown #number never decreases (NUMBER sort
        // orders by baseId, so the label is non-decreasing by construction).
        val list = (1..50).map { p(it) }
        val labels = (0..100).map { fastScrollBubbleLabel(list[fastScrollTargetIndex(it / 100f, list.size)], false) }
        val numbers = labels.map { it.removePrefix("#").toInt() }
        assertEquals(numbers.sorted(), numbers)
    }

    @Test fun draggingDownMovesForwardThroughNameOrder() {
        val list = listOf("Abra", "Bulbasaur", "Charmander", "Diglett", "Eevee").mapIndexed { i, n -> p(i + 1, n) }
        val labels = (0..100).map { fastScrollBubbleLabel(list[fastScrollTargetIndex(it / 100f, list.size)], true) }
        val letters = labels.map { it.single() }
        assertEquals(letters.sorted(), letters)
        assertEquals('A', letters.first())
        assertEquals('E', letters.last())
    }

    @Test fun bubbleLabelUsesLetterOrNumber() {
        assertEquals("B", fastScrollBubbleLabel(p(1, "Bulbasaur"), true))
        assertEquals("#25", fastScrollBubbleLabel(p(25, "Pikachu"), false))
    }
}
