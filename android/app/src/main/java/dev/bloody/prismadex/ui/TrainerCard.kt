@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package dev.bloody.prismadex.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CatchingPokemon
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.data.BadgeSetByGame
import dev.bloody.prismadex.data.BadgeSetSize
import dev.bloody.prismadex.data.Pokemon
import dev.bloody.prismadex.data.TrainerSpriteKeys
import dev.bloody.prismadex.data.UserState
import dev.bloody.prismadex.data.gameName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Trainer card + badge images (00:02/00:07/00:11/00:17).
 *
 * The card layout follows the user's own trainer-card reference (name header,
 * 3x2 team grid, trainer sprite, badges row) rendered from the app's saved
 * data only: trainer name + sprite chosen in the Account tab, the ACTIVE
 * team's members (bundled art), the active team's game as plain text, and
 * the registered badge images for that game's set. No graphics, fonts, or
 * watermark copied from the reference. Badge slots show ONLY the image, no
 * visible badge name, in the checklist and here (00:11 clarification).
 *
 * Trainer sprites: static PNGs from the Showdown trainers listing, imported
 * under the user's accepted IP risk (see research/SPRITE-SOURCES.md). Badge
 * images: SteGriff CC BY 3.0 recreations (Kanto-Unova) + PokeAPI/sprites
 * files 70-77 (Paldea). No badge artwork is invented.
 */

/** Badge image from `assets/badges/<prefix>-<n>.png`. Unearned: greyscale + dimmed, still recognisable. */
@Composable
internal fun BadgeArt(prefix: String, index: Int, earned: Boolean, modifier: Modifier = Modifier) {
    val assets = LocalContext.current.assets
    val path = "badges/$prefix-${index + 1}.png"
    val image by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            runCatching { assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
        }
    }
    val ready = image
    if (ready != null) {
        Image(
            ready,
            null,
            modifier.alpha(if (earned) 1f else .45f),
            colorFilter = if (earned) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        )
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            LIcon(
                Icons.Rounded.CatchingPokemon, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(.4f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/** Trainer sprite from `assets/trainers/<key>.png` (static, 80x80). Blank/unknown key: honest placeholder, never a fake sprite. */
@Composable
internal fun TrainerArt(key: String, modifier: Modifier = Modifier) {
    val assets = LocalContext.current.assets
    val path = "trainers/$key.png"
    val image by produceState<ImageBitmap?>(null, path) {
        value = if (key.isBlank() || key !in TrainerSpriteKeys) null else withContext(Dispatchers.IO) {
            runCatching { assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
        }
    }
    val ready = image
    if (ready != null) {
        Image(ready, null, modifier)
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            LIcon(
                Icons.Rounded.Person, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(.4f),
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

@Composable
internal fun TrainerCard(state: UserState, catalog: List<Pokemon>, modifier: Modifier = Modifier) {
    val members = state.team.mapNotNull { id -> catalog.find { it.id == id } }
    val badgeSet = BadgeSetByGame[state.activeTeam.gameId]
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(28.dp), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LText(
                        if (state.trainerName.isNotBlank()) state.trainerName else "Cartão de treinador",
                        fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    LText(gameName(state.activeTeam.gameId), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TrainerArt(state.trainerSprite, Modifier.size(80.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (0 until 3).forEach { r ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (0 until 2).forEach { c ->
                            val p = members.getOrNull(r * 2 + c)
                            Box(
                                Modifier.weight(1f).aspectRatio(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (p != null) PokemonArt(p.id, Modifier.size(72.dp), p.name)
                            }
                        }
                    }
                }
            }
            if (badgeSet != null) {
                val earned = state.badgeSets[state.activeTeam.gameId].orEmpty()
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0 until BadgeSetSize).forEach { i ->
                        BadgeArt(badgeSet.prefix, i, i in earned, Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun TrainerSpritePicker(selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            onClick = { onSelect("") },
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                if (selected.isBlank()) 2.dp else 1.dp,
                if (selected.isBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(.25f)
            ),
            modifier = Modifier.size(64.dp).heightIn(min = 48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                LIcon(Icons.Rounded.Person, "Sem sprite de treinador", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
            }
        }
        TrainerSpriteKeys.forEach { key ->
            Surface(
                onClick = { onSelect(key) },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    if (selected == key) 2.dp else 1.dp,
                    if (selected == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(.25f)
                ),
                modifier = Modifier.size(64.dp).heightIn(min = 48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    TrainerArt(key, Modifier.size(56.dp))
                }
            }
        }
    }
}
