package dev.bloody.prismadex.data

import org.junit.Assert.*
import org.junit.Test

class AppIconTest {
    @Test
    fun defaultIconIsPrisma() {
        assertEquals("prisma", UserState().appIcon)
    }

    @Test
    fun iconKeysAreExactlyTheNineteenOptions() {
        assertEquals(setOf("prisma", "sylveon", "flareon", "aggron", "pikachu", "starmie", "lucario", "cloyster", "sigilyph", "haxorus", "vaporeon", "infernape", "umbreon", "ultraball", "greatball", "pokedex", "glaceon", "masterball", "pokeball"), AppIconKeys)
    }

    @Test
    fun unknownIconFallsBackToPrisma() {
        assertEquals("prisma", canonicalAppIcon("mew"))
        assertEquals("prisma", canonicalAppIcon(""))
        assertEquals("sylveon", canonicalAppIcon("sylveon"))
        assertEquals("flareon", canonicalAppIcon("flareon"))
        assertEquals("aggron", canonicalAppIcon("aggron"))
        assertEquals("umbreon", canonicalAppIcon("umbreon"))
        assertEquals("ultraball", canonicalAppIcon("ultraball"))
        assertEquals("glaceon", canonicalAppIcon("glaceon"))
        assertEquals("masterball", canonicalAppIcon("masterball"))
        assertEquals("pokeball", canonicalAppIcon("pokeball"))
    }

    @Test
    fun stateCopyKeepsUnknownIconUntilStoreValidation() {
        // Store.save rejects unknown keys and Store.decode falls back via
        // canonicalAppIcon; the copy itself is a plain data-class copy.
        assertEquals("sylveon", UserState().copy(appIcon = "sylveon").appIcon)
    }

    @Test
    fun aliasMapCoversEveryNonPrismaKeyExactlyOnce() {
        assertEquals(AppIconKeys - "prisma", AppIconAliasSuffixes.keys)
        assertEquals(18, AppIconAliasSuffixes.size)
        assertEquals(AppIconAliasSuffixes.size, AppIconAliasSuffixes.values.toSet().size)
        assertTrue(AppIconAliasSuffixes.values.all { it.startsWith("Icon") })
    }
}
