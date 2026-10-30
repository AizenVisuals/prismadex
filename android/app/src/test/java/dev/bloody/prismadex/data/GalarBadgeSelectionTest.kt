package dev.bloody.prismadex.data

import org.junit.Assert.*
import org.junit.Test

class GalarBadgeSelectionTest {
    @Test fun swordAndShieldEarnedBadgesStayIndependentAndOtherGamesRemainUntouched() {
        val initial = UserState(badgeSets = mapOf(1 to setOf(2, 5)))
        val sword = initial.toggleCardBadge(20, 3)
        assertEquals("galar-sword", sword.cardBadgeSet(20)!!.prefix)
        assertEquals(setOf(3), sword.cardBadgeIndices(20))
        val shield = sword.copy(galarVersion = "shield").toggleCardBadge(20, 5)
        assertEquals("galar-shield", shield.cardBadgeSet(20)!!.prefix)
        assertEquals(setOf(5), shield.cardBadgeIndices(20))
        assertEquals(setOf(3), shield.copy(galarVersion = "sword").cardBadgeIndices(20))
        assertEquals(initial.badgeSets, shield.badgeSets)
        val removed = shield.toggleCardBadge(20, 5)
        assertTrue(removed.cardBadgeIndices(20).isEmpty())
        assertEquals(setOf(3), removed.copy(galarVersion = "sword").cardBadgeIndices(20))
    }

    @Test fun unsupportedGamesAndInvalidIndicesDoNotCreateBadges() {
        val state = UserState()
        listOf(12,13,17,18,22,24,25).forEach { game ->
            assertNull(state.cardBadgeSet(game))
            assertEquals(state, state.toggleCardBadge(game, 0))
        }
        assertEquals(state, state.toggleCardBadge(20, -1))
        assertEquals(state, state.toggleCardBadge(20, 8))
        assertEquals(state, state.toggleCardBadge(1, 8))
    }

    @Test fun remakesAndSequelsUseTheirActualGymOrderMetadata() {
        val state = UserState()
        assertEquals("sinnoh-dp", state.cardBadgeSet(8)!!.prefix)
        assertEquals("sinnoh-dp", state.cardBadgeSet(21)!!.prefix)
        assertEquals("sinnoh", state.cardBadgeSet(9)!!.prefix)
        assertEquals("unova", state.cardBadgeSet(11)!!.prefix)
        assertEquals("unova2", state.cardBadgeSet(14)!!.prefix)
        assertEquals("kalos", state.cardBadgeSet(15)!!.prefix)
        assertEquals(1500, TrainerSpriteKeys.size)
        assertEquals(TrainerSpriteKeys.size, TrainerSpriteKeys.distinct().size)
        assertEquals("red", TrainerSpriteKeys.first())
    }
    @Test fun johtoKantoPostgameBadgesAreSeparateFromTheFirstEightAndOtherVersions() {
        val initial=UserState(badgeSets=mapOf(3 to setOf(0,1)),galarBadgeSets=mapOf("shield" to setOf(4)))
        val gold=initial.toggleKantoPostgameBadge(3,7)
        assertEquals(setOf(7),gold.kantoPostgameBadges[3])
        assertEquals(initial.badgeSets,gold.badgeSets)
        assertEquals(initial.galarBadgeSets,gold.galarBadgeSets)
        assertTrue(gold.kantoPostgameBadges[4].orEmpty().isEmpty())
        assertTrue(gold.kantoPostgameBadges[10].orEmpty().isEmpty())
    }

    @Test fun johtoPostgameToggleCanRemoveOnlyTheTargetGameBadge() {
        val initial=UserState(kantoPostgameBadges=mapOf(3 to setOf(2),4 to setOf(4),10 to setOf(6)))
        val next=initial.toggleKantoPostgameBadge(4,4)
        assertFalse(4 in next.kantoPostgameBadges)
        assertEquals(setOf(2),next.kantoPostgameBadges[3])
        assertEquals(setOf(6),next.kantoPostgameBadges[10])
    }

    @Test fun otherGamesCannotInventKantoPostgameBadges() {
        val state=UserState()
        listOf(1,2,5,8,15,17,20,23,25).forEach { assertEquals(state,state.toggleKantoPostgameBadge(it,0)) }
        listOf(3,4,10).forEach { game ->
            assertEquals(state,state.toggleKantoPostgameBadge(game,-1))
            assertEquals(state,state.toggleKantoPostgameBadge(game,8))
        }
    }
}
