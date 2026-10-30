package dev.bloody.prismadex.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Theme redo 30/10/2026: dark variants must stay true dark graphite
 *  (no sepia cast) and light variants soft warm paper (no glaring
 *  pure white), with every Eeveelution accent readable on both. */
class ThemeContrastTest {
    private fun luminance(c: Color): Double {
        fun lin(v: Float): Double {
            val d = v.toDouble()
            return if (d <= 0.03928) d / 12.92 else Math.pow((d + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * lin(c.red) + 0.7152 * lin(c.green) + 0.0722 * lin(c.blue)
    }

    private fun ratio(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    @Test fun opaqueSchemeTokensAreFullyOpaque() {
        for (theme in AppThemes) {
            for (light in listOf(false, true)) {
                val c = opaqueThemeScheme(theme.colors(light).accent, theme.colors(light).background, theme.colors(light).surface, light)
                for ((name, color) in listOf(
                    "primary" to c.primary, "onPrimary" to c.onPrimary,
                    "primaryContainer" to c.primaryContainer, "onPrimaryContainer" to c.onPrimaryContainer,
                    "background" to c.background, "onBackground" to c.onBackground,
                    "surface" to c.surface, "onSurface" to c.onSurface,
                    "surfaceVariant" to c.surfaceVariant, "onSurfaceVariant" to c.onSurfaceVariant,
                    "surfaceTint" to c.surfaceTint, "outline" to c.outline,
                    "outlineVariant" to c.outlineVariant, "error" to c.error, "onError" to c.onError,
                    "surfaceContainerLowest" to c.surfaceContainerLowest,
                    "surfaceContainerLow" to c.surfaceContainerLow,
                    "surfaceContainer" to c.surfaceContainer,
                    "surfaceContainerHigh" to c.surfaceContainerHigh,
                    "surfaceContainerHighest" to c.surfaceContainerHighest,
                    "inverseSurface" to c.inverseSurface, "inverseOnSurface" to c.inverseOnSurface,
                )) {
                    assertEquals("${theme.key} $light $name alpha", 1f, color.alpha, 0f)
                }
            }
        }
    }

    @Test fun opaqueSchemeDerivedPairsKeepContrast() {
        for (theme in AppThemes) {
            for (light in listOf(false, true)) {
                val t = theme.colors(light)
                val c = opaqueThemeScheme(t.accent, t.background, t.surface, light)
                assertTrue("${theme.key} $light container", ratio(c.primaryContainer, c.onPrimaryContainer) >= 4.5)
                assertTrue("${theme.key} $light variant", ratio(c.surfaceVariant, c.onSurfaceVariant) >= 4.5)
                assertTrue("${theme.key} $light onPrimary", ratio(c.primary, c.onPrimary) >= 4.5)
                assertTrue("${theme.key} $light surface", ratio(c.surface, c.onSurface) >= 4.5)
            }
        }
    }

    @Test fun solidBlendIsOpaqueAndBounded() {
        val blended = solidBlend(Color(0xFFFF0000), Color(0xFF0000FF), 0.5f)
        assertEquals(1f, blended.alpha, 0f)
        assertTrue(blended.red in 0f..1f && blended.green in 0f..1f && blended.blue in 0f..1f)
        assertEquals(Color(0xFFFF0000), solidBlend(Color(0xFFFF0000), Color(0xFF0000FF), 1f))
        assertEquals(Color(0xFF0000FF), solidBlend(Color(0xFFFF0000), Color(0xFF0000FF), 0f))
    }

    @Test fun nineThemesWithUniqueKeys() {
        assertEquals(9, AppThemes.size)
        assertEquals(9, AppThemes.map { it.key }.distinct().size)
    }

    @Test fun accentContrastAtLeast45OnBackgroundAndSurface() {
        for (theme in AppThemes) {
            for ((name, colors) in listOf("dark" to theme.dark, "light" to theme.day)) {
                assertTrue(
                    "${theme.key} $name accent/background ${ratio(colors.accent, colors.background)}",
                    ratio(colors.accent, colors.background) >= 4.5,
                )
                assertTrue(
                    "${theme.key} $name accent/surface ${ratio(colors.accent, colors.surface)}",
                    ratio(colors.accent, colors.surface) >= 4.5,
                )
            }
        }
    }

    @Test fun darkIsTrueDarkAndLightIsSoftPaperWithLayeredSurfaces() {
        for (theme in AppThemes) {
            assertTrue("${theme.key} dark background too light", luminance(theme.dark.background) < 0.03)
            assertTrue("${theme.key} light background too dark", luminance(theme.day.background) > 0.80)
            assertTrue("${theme.key} dark surface not layered", luminance(theme.dark.surface) > luminance(theme.dark.background))
            assertTrue("${theme.key} light surface not layered", luminance(theme.day.surface) > luminance(theme.day.background))
        }
    }
}
