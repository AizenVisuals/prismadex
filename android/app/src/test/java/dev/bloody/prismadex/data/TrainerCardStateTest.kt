package dev.bloody.prismadex.data

import org.junit.Assert.*
import org.junit.Test

// NOTE: Store.encode/decode use org.json, which is a stub on the JVM unit
// test classpath ("not mocked"). Persistence round-trips are covered by
// the instrumented TrainerCardPersistenceTest instead; this file covers
// only pure in-memory state logic.

class TrainerCardStateTest {
    private val teamA = SavedTeam("a", "Equipe A", 1, listOf(25, 6))
    private val teamB = SavedTeam("b", "Equipe B", 23, listOf(1))

    @Test
    fun cardTeamFallsBackToActiveWhenNoneChosen() {
        val state = UserState(teams = listOf(teamA, teamB), activeTeamId = "a")
        assertEquals(teamA, state.cardTeam)
        assertEquals(teamA, state.copy(trainerCardTeamId = "").cardTeam)
    }

    @Test
    fun cardTeamResolvesChosenTeamAndUnknownIdFallsBack() {
        val state = UserState(teams = listOf(teamA, teamB), activeTeamId = "a", trainerCardTeamId = "b")
        assertEquals(teamB, state.cardTeam)
        assertEquals(teamA, state.copy(trainerCardTeamId = "missing").cardTeam)
    }

    @Test
    fun toggleCardBadgeAddsAndRemovesOnlyForGamesWithImageSets() {
        val state = UserState(teams = listOf(teamA, teamB), activeTeamId = "a")
        val added = state.toggleCardBadge(1, 3)
        assertEquals(setOf(3), added.badgeSets[1])
        assertEquals(state, added.toggleCardBadge(1, 3))
        // Game 25 (Champions) has no verified badge image set: unchanged.
        assertEquals(state, state.toggleCardBadge(25, 0))
        // Out-of-range index: unchanged.
        assertEquals(state, state.toggleCardBadge(1, 8))
        assertEquals(state, state.toggleCardBadge(1, -1))
        // Toggling one game leaves the other game's record untouched.
        val both = added.toggleCardBadge(23, 0)
        assertEquals(setOf(3), both.badgeSets[1])
        assertEquals(setOf(0), both.badgeSets[23])
    }

    @Test
    fun trainerSpriteKeysAreUniqueAndUseExactFilenames() {
        assertEquals(TrainerSpriteKeys.size, TrainerSpriteKeys.distinct().size)
        assertTrue(TrainerSpriteKeys.containsAll(listOf("florian-s", "juliana-s", "nemona-s", "arven-s", "leaf-gen3")))
        assertFalse(TrainerSpriteKeys.contains("florian"))
        assertFalse(TrainerSpriteKeys.contains("leaf"))
        TrainerSpriteKeys.forEach { key -> assertTrue(key.matches(Regex("[a-z0-9-]+"))) }
    }
}
