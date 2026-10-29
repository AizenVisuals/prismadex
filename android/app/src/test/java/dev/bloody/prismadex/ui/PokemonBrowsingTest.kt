package dev.bloody.prismadex.ui

import org.junit.Assert.*
import org.junit.Test

class PokemonBrowsingTest {
    @Test fun followsSourceOrderingIncludingFormsWithoutWrapping(){
        val filtered=listOf(6,10034,10035,9)
        assertEquals(10034,adjacentPokemonId(filtered,6,1))
        assertEquals(10035,adjacentPokemonId(filtered,9,-1))
        assertNull(adjacentPokemonId(filtered,6,-1))
        assertNull(adjacentPokemonId(filtered,9,1))
        assertNull(adjacentPokemonId(filtered,25,1))
        assertNull(adjacentPokemonId(emptyList(),6,1))
        assertNull(adjacentPokemonId(filtered,6,2))
    }
}
