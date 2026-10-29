package dev.bloody.prismadex.glass

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.bloody.prismadex.glass.backdrop.backdrops.LayerBackdrop
import dev.bloody.prismadex.glass.backdrop.backdrops.layerBackdrop
import dev.bloody.prismadex.glass.backdrop.backdrops.rememberLayerBackdrop
import dev.bloody.prismadex.glass.backdrop.drawBackdrop
import dev.bloody.prismadex.glass.backdrop.effects.blur
import dev.bloody.prismadex.glass.backdrop.effects.lens
import dev.bloody.prismadex.glass.backdrop.highlight.Highlight
import dev.bloody.prismadex.glass.backdrop.shadow.Shadow

/** Provide one backdrop around the page and its sibling floating controls. */
val LocalPrismaBackdrop = compositionLocalOf<LayerBackdrop?> { null }

@Composable
fun rememberPrismaBackdrop(): LayerBackdrop = rememberLayerBackdrop()

/** Record page content only. Keep sampling overlays outside this modifier's subtree. */
fun Modifier.prismaBackdrop(backdrop: LayerBackdrop): Modifier = layerBackdrop(backdrop)

/**
 * Native recorded-background glass. Android 13+ uses AGSL refraction with dispersion,
 * Android 12 uses RenderEffect blur, older releases retain the frosted surface.
 * A spring runs only during touch feedback; no animation loop or per-frame timer.
 */
@Composable
fun LiquidGlass(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    tint: Color = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = .32f),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val backdrop = LocalPrismaBackdrop.current
    val scheme=androidx.compose.material3.MaterialTheme.colorScheme
    val light=scheme.background.luminance()>.5f
    val shape = remember(radius) { RoundedCornerShape(radius) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) .965f else 1f,
        animationSpec = spring(dampingRatio = .78f, stiffness = 550f),
        label = "glass press"
    )
    val optical = if (backdrop != null && Build.VERSION.SDK_INT >= 31) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                val resolution = .25f
                blur(8.dp.toPx() * resolution)
                if (Build.VERSION.SDK_INT >= 33) {
                    lens(
                        refractionHeight = 20.dp.toPx() * resolution,
                        refractionAmount = 26.dp.toPx() * resolution,
                        depthEffect = true,
                        chromaticAberration = true
                    )
                }
            },
            highlight = { Highlight(width = .6f.dp, alpha = if(light).42f else .48f) },
            shadow = { Shadow(radius = 16.dp, color = Color.Black.copy(alpha = if(light).14f else .3f)) },
            backdropScale = .25f,
            onDrawSurface = { drawRect(tint) }
        )
    } else Modifier.shadow(10.dp, shape).background(tint.copy(alpha = .88f), shape)
    val click = if (onClick != null) Modifier.clickable(
        interactionSource = interaction,
        indication = null,
        onClick = onClick
    ) else Modifier
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(optical)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = if(light).28f else .085f), scheme.primary.copy(alpha=.018f), Color.White.copy(alpha = .035f))))
            .border(.6f.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = if(light).6f else .28f), scheme.primary.copy(alpha=.07f), Color.White.copy(alpha = .12f))), shape)
            .then(click),
        content = content
    )
}
