package dev.bloody.prismadex.ui

import org.junit.Assert.*
import org.junit.Test

class AppIconGroupsTest {
    @Test fun groupsCoverEveryOptionExactlyOnce() {
        val grouped = AppIconGroups.flatMap { it.options.map { o -> o.key } }
        assertEquals(AppIconOptions.map { it.key }.sorted(), grouped.sorted())
        assertEquals(grouped.size, grouped.toSet().size)
    }

    @Test fun ballsGroupHoldsOnlyBallsAndDevice() {
        val balls = AppIconGroups.first { it.kind == AppIconGroupKind.BALLS }
        assertEquals("Pokébolas", balls.title)
        assertEquals(setOf("ultraball", "greatball", "masterball", "pokeball", "pokedex"), balls.options.map { it.key }.toSet())
    }

    @Test fun pokemonGroupHoldsPrismaFirstAndFourteenOptions() {
        val pokemon = AppIconGroups.first { it.kind == AppIconGroupKind.POKEMON }
        assertEquals("Pokémon", pokemon.title)
        assertEquals(14, pokemon.options.size)
        assertEquals("prisma", pokemon.options.first().key)
    }

    @Test fun groupOfResolvesCurrentIconGroup() {
        assertEquals(AppIconGroupKind.BALLS, appIconGroupOf("masterball"))
        assertEquals(AppIconGroupKind.BALLS, appIconGroupOf("pokedex"))
        assertEquals(AppIconGroupKind.POKEMON, appIconGroupOf("prisma"))
        assertEquals(AppIconGroupKind.POKEMON, appIconGroupOf("aggron"))
        assertEquals(AppIconGroupKind.POKEMON, appIconGroupOf("unknown-key"))
    }
}
