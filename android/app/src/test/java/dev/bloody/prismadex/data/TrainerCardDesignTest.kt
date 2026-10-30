package dev.bloody.prismadex.data

import org.junit.Assert.*
import org.junit.Test

class TrainerCardDesignTest {
    @Test
    fun stylesAndBackgroundsHaveUniqueNonBlankKeys() {
        assertTrue(TrainerCardStyles.isNotEmpty())
        assertEquals(TrainerCardStyles.size, TrainerCardStyles.map { it.key }.distinct().size)
        assertTrue(TrainerCardStyles.all { it.name.isNotBlank() && it.detail.isNotBlank() })
        assertTrue(TrainerCardBackgrounds.isNotEmpty())
        assertEquals(TrainerCardBackgrounds.size, TrainerCardBackgrounds.map { it.key }.distinct().size)
        assertTrue(TrainerCardBackgrounds.all { it.name.isNotBlank() })
        assertTrue("classico" in TrainerCardStyleKeys)
        assertTrue("padrao" in TrainerCardBackgroundKeys)
    }

    @Test
    fun backgroundColorsFallBackToDefaultForUnknownKey() {
        val (top, bottom, ink) = trainerCardBackgroundColors("inexistente")
        val (defTop, defBottom, defInk) = trainerCardBackgroundColors("padrao")
        assertEquals(defTop, top)
        assertEquals(defBottom, bottom)
        assertEquals(defInk, ink)
        // All colours carry a full alpha channel.
        TrainerCardBackgrounds.forEach {
            assertEquals(0xFF000000L, it.top and 0xFF000000L)
            assertEquals(0xFF000000L, it.bottom and 0xFF000000L)
            assertEquals(0xFF000000L, it.ink and 0xFF000000L)
        }
    }

    @Test
    fun typeBackgroundTintsWithFirstTypeColour() {
        val (tinted, _, _) = trainerCardBackgroundColors("tipo", 0xFFF5A17DL)
        val (plain, _, _) = trainerCardBackgroundColors("tipo", null)
        assertNotEquals(plain, tinted)
        assertEquals(0xFF000000L, tinted and 0xFF000000L)
    }

    @Test
    fun spriteDisplayNameFormatsKeysWithoutInventingNames() {
        assertEquals("Acerola Masters 2", trainerSpriteDisplayName("acerola-masters2"))
        assertEquals("Leaf Gen 3", trainerSpriteDisplayName("leaf-gen3"))
        assertEquals("Florian S", trainerSpriteDisplayName("florian-s"))
        assertEquals("Acetrainer Gen 1 RB", trainerSpriteDisplayName("acetrainer-gen1rb"))
        assertEquals("", trainerSpriteDisplayName(""))
        // No raw key separator survives formatting.
        assertFalse(trainerSpriteDisplayName("bugcatcher-gen3").contains("-"))
    }

    @Test
    fun spriteSearchTextMatchesNameAndKey() {
        val text = trainerSpriteSearchText("acerola-masters2")
        assertTrue(text.contains("acerola"))
        assertTrue(text.contains("masters 2"))
        assertTrue(text.contains("acerola-masters2"))
        assertEquals(text, text.lowercase())
    }

    @Test
    fun userStateDefaultsKeepPreviousCardLook() {
        val state = UserState()
        assertEquals("classico", state.trainerCardStyle)
        assertEquals("padrao", state.trainerCardBackground)
    }

    @Test
    fun everySpriteKeyHasANonBlankDisplayName() {
        assertTrue(TrainerSpriteKeys.isNotEmpty())
        TrainerSpriteKeys.forEach { key ->
            assertTrue("empty name for $key", trainerSpriteDisplayName(key).isNotBlank())
        }
    }
}
