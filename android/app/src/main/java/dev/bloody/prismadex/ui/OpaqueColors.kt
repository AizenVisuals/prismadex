package dev.bloody.prismadex.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Opaque theme colors (30/10/2026): every derived token is a solid
 * pre-blend (alpha 1) — never a translucent fill over the white/black
 * background. [solidBlend] reproduces the old translucent look over a
 * KNOWN underlying color, baked into one opaque color.
 */
internal fun solidBlend(fg: Color, bg: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = fg.red * f + bg.red * (1f - f),
        green = fg.green * f + bg.green * (1f - f),
        blue = fg.blue * f + bg.blue * (1f - f),
        alpha = 1f,
    )
}

private fun contrastRatio(a: Color, b: Color): Double {
    val la = a.luminance().toDouble()
    val lb = b.luminance().toDouble()
    return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}

/** Whichever of white / dark ink reads better on [background]; always opaque. */
internal fun bestOnColor(background: Color): Color {
    val white = Color.White
    val ink = Color(0xFF28263A)
    return if (contrastRatio(white, background) >= contrastRatio(ink, background)) white else ink
}

/** Full explicit ColorScheme from the three Eeveelution tokens. All tokens
 *  opaque; surfaceTint equals surface so tonal elevation cannot tint a
 *  surface with a translucent accent wash. */
internal fun opaqueThemeScheme(accent: Color, bg: Color, surface: Color, light: Boolean): ColorScheme {
    val onSurface = if (light) Color(0xFF28263A) else Color(0xFFF2F0FA)
    val onSurfaceVariant = if (light) Color(0xFF625A75) else Color(0xFFB5B1C5)
    val surfaceVariant = solidBlend(onSurface, surface, if (light) 0.08f else 0.12f)
    val primaryContainer = solidBlend(accent, bg, if (light) 0.22f else 0.38f)
    val onPrimaryContainer = bestOnColor(primaryContainer)
    val onPrimary = bestOnColor(accent)
    val outline = solidBlend(onSurfaceVariant, bg, 0.45f)
    val outlineVariant = solidBlend(onSurfaceVariant, bg, 0.25f)
    val error = if (light) Color(0xFFBA1A1A) else Color(0xFFFFB4AB)
    val errorContainer = solidBlend(error, bg, 0.18f)
    val surfaceContainerLow = solidBlend(surface, bg, 0.45f)
    val surfaceContainerHigh = solidBlend(onSurface, surface, 0.06f)
    return if (light) {
        lightColorScheme(
            primary = accent, onPrimary = onPrimary,
            primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
            secondary = accent, onSecondary = onPrimary,
            secondaryContainer = primaryContainer, onSecondaryContainer = onPrimaryContainer,
            tertiary = accent, onTertiary = onPrimary,
            tertiaryContainer = primaryContainer, onTertiaryContainer = onPrimaryContainer,
            background = bg, onBackground = onSurface,
            surface = surface, onSurface = onSurface,
            surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
            surfaceTint = surface,
            surfaceContainerLowest = bg, surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surface, surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceVariant,
            outline = outline, outlineVariant = outlineVariant,
            error = error, onError = bestOnColor(error),
            errorContainer = errorContainer, onErrorContainer = bestOnColor(errorContainer),
            inverseSurface = Color(0xFF1D1B18), inverseOnSurface = Color(0xFFF2F0FA),
            inversePrimary = accent, scrim = Color.Black,
        )
    } else {
        darkColorScheme(
            primary = accent, onPrimary = onPrimary,
            primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
            secondary = accent, onSecondary = onPrimary,
            secondaryContainer = primaryContainer, onSecondaryContainer = onPrimaryContainer,
            tertiary = accent, onTertiary = onPrimary,
            tertiaryContainer = primaryContainer, onTertiaryContainer = onPrimaryContainer,
            background = bg, onBackground = onSurface,
            surface = surface, onSurface = onSurface,
            surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
            surfaceTint = surface,
            surfaceContainerLowest = bg, surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surface, surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceVariant,
            outline = outline, outlineVariant = outlineVariant,
            error = error, onError = bestOnColor(error),
            errorContainer = errorContainer, onErrorContainer = bestOnColor(errorContainer),
            inverseSurface = Color(0xFFFCF9F3), inverseOnSurface = Color(0xFF28263A),
            inversePrimary = accent, scrim = Color.Black,
        )
    }
}
