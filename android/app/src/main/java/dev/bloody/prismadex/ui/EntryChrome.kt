package dev.bloody.prismadex.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CatchingPokemon
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Hexagon
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Entry chrome (30/10/2026, user feedback on the 12:43 screenshots):
 * splash motion, top bar and side drawer.
 *
 * Splash: ONE authored entrance moment over the existing load — the mark
 * scales 0.92 -> 1 and fades in (520ms, FastOutSlowIn), the wordmark and
 * the status line fade in behind it. Nothing blocks content: the animation
 * only runs while the catalog is loading, and the screen is replaced the
 * moment loading finishes. With reduce-motion (animator scale 0) there is
 * no scale travel at all, only a 120ms fade. No loop, no timer.
 *
 * Top bar: a plain Surface row with an explicit statusBarsPadding() so the
 * buttons can never sit under the status bar or be clipped at the top edge
 * (the 12:43 screenshot showed the top icon cut). 48dp buttons, 24dp icons,
 * 28dp mark, title on one line. LiquidGlass is deliberately NOT used here:
 * its sampled backdrop can clip at the window edge; the header is chrome,
 * not floating content.
 *
 * Drawer: fixed 320dp sheet, header text wraps (subtitle maxLines=2, never
 * ellipsized — the 12:43 screenshot showed "Sua Pokedex c...mpleta" cut),
 * one icon style throughout (Rounded, 22dp, same tint rule), rows all
 * 52dp+ with the same 12dp side padding and 20dp radius, active state =
 * the same primary 0.20 fill the bottom navigation uses. All destinations
 * and labels are kept; only the "Insígnias" (Check -> EmojiEvents) and
 * "Catálogo de itens" (Info -> Inventory2) icons change, so no two rows
 * share a generic Info icon and every icon names its destination.
 */

@Composable
internal fun PrismaSplash(failure: String?, accent: Color, reduceMotion: Boolean) {
    // Animatable (not animateFloatAsState): the entrance must actually run
    // from 0 on first composition. One shot, no loop, no timer.
    val markProgress = remember { androidx.compose.animation.core.Animatable(if (reduceMotion) 1f else 0f) }
    val textProgress = remember { androidx.compose.animation.core.Animatable(if (reduceMotion) 1f else 0f) }
    androidx.compose.runtime.LaunchedEffect(reduceMotion) {
        if (!reduceMotion) {
            markProgress.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        }
    }
    androidx.compose.runtime.LaunchedEffect(reduceMotion) {
        if (!reduceMotion) {
            kotlinx.coroutines.delay(140)
            textProgress.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
        }
    }
    val markScale = if (reduceMotion) 1f else 0.92f + 0.08f * markProgress.value
    val markAlpha = if (reduceMotion) 1f else markProgress.value
    val textAlpha = textProgress.value
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(104.dp).scale(markScale).alpha(markAlpha)
                    .clip(CircleShape)
                    .background(solidBlend(accent,MaterialTheme.colorScheme.background,0.14f)),
                contentAlignment = Alignment.Center
            ) {
                PrismaMark(Modifier.size(60.dp), color = accent)
            }
            Spacer(Modifier.height(20.dp))
            Column(Modifier.alpha(textAlpha), horizontalAlignment = Alignment.CenterHorizontally) {
                LText("PrismaDex", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                LText(
                    failure ?: "Abrindo seu universo…",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (failure == null) {
                    Spacer(Modifier.height(20.dp))
                    LinearProgressIndicator(
                        Modifier.width(128.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = accent,
                        trackColor = solidBlend(accent,MaterialTheme.colorScheme.background,0.18f)
                    )
                }
            }
        }
    }
}

@Composable
internal fun PrismaTopBar(
    title: String,
    accent: Color,
    savedLabel: String,
    onOpenDrawer: () -> Unit,
    onOpenAccount: () -> Unit,
    background: Color
) {
    Surface(color = background) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 8.dp)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onOpenDrawer,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    LIcon(Icons.Rounded.Menu, "Abrir menu lateral", Modifier.size(24.dp), tint = accent)
                }
            }
            Spacer(Modifier.width(10.dp))
            PrismaMark(Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                LText(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!savedLabel.startsWith("Salvo")) {
                    LText(savedLabel, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            Spacer(Modifier.width(10.dp))
            Surface(
                onClick = onOpenAccount,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    LIcon(Icons.Rounded.PersonOutline, "Minha conta", Modifier.size(24.dp), tint = accent)
                }
            }
        }
    }
}

private data class DrawerDestination(val key: String, val title: String, val icon: ImageVector)

private val drawerDestinations = listOf(
    DrawerDestination("dex", "Pokédex", Icons.Rounded.CatchingPokemon),
    DrawerDestination("favorites", "Favoritos", Icons.Rounded.FavoriteBorder),
    DrawerDestination("team", "TeamBuilder", Icons.Rounded.Hexagon),
    DrawerDestination("calc", "Calculadora de dano", Icons.Rounded.AutoAwesome),
    DrawerDestination("meta", "Meta online", Icons.Rounded.Public),
    DrawerDestination("abilities", "Dex de habilidades", Icons.Rounded.Star),
    DrawerDestination("moves", "Dex de golpes", Icons.Rounded.OfflineBolt),
    DrawerDestination("badges", "Insígnias", Icons.Rounded.Check),
    DrawerDestination("items", "Catálogo de itens", Icons.Rounded.Inventory2),
    DrawerDestination("settings", "Configurações e temas", Icons.Rounded.Settings),
    DrawerDestination("sources", "Champions e fontes", Icons.Rounded.Info)
)

@Composable
internal fun PrismaDrawer(selected: String, onNavigate: (String) -> Unit, onAccount: () -> Unit) {
    ModalDrawerSheet(
        Modifier.width(320.dp).fillMaxHeight(),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            Column(
                Modifier.fillMaxWidth().statusBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp)
            ) {
                PrismaMark(Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
                LText("PrismaDex", fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                LText(
                    "Sua Pokédex completa",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            drawerDestinations.forEach { dest ->
                val active = selected == dest.key
                NavigationDrawerItem(
                    label = {
                        LText(
                            dest.title,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            maxLines = 2,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    selected = active,
                    onClick = { onNavigate(dest.key) },
                    icon = {
                        LIcon(
                            dest.icon,
                            null,
                            Modifier.size(22.dp),
                            tint = if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp).heightIn(min = 52.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = solidBlend(MaterialTheme.colorScheme.primary,MaterialTheme.colorScheme.surface,0.20f),
                        unselectedContainerColor = Color.Transparent
                    )
                )
            }
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(8.dp))
            NavigationDrawerItem(
                label = { LText("Minha conta", fontSize = 14.sp, lineHeight = 18.sp, maxLines = 1) },
                selected = false,
                onClick = onAccount,
                icon = {
                    LIcon(
                        Icons.Rounded.PersonOutline,
                        null,
                        Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp).heightIn(min = 52.dp),
                shape = RoundedCornerShape(20.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent
                )
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
