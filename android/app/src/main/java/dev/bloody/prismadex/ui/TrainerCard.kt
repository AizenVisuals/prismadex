@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package dev.bloody.prismadex.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.data.BadgeSetByGame
import dev.bloody.prismadex.data.BadgeSetSize
import dev.bloody.prismadex.data.Pokemon
import dev.bloody.prismadex.data.TrainerCardBackgroundKeys
import dev.bloody.prismadex.data.TrainerSpriteKeys
import dev.bloody.prismadex.data.trainerCardBackgroundColors
import dev.bloody.prismadex.data.trainerSpriteDisplayName
import dev.bloody.prismadex.data.trainerSpriteSearchText
import dev.bloody.prismadex.data.UserState
import dev.bloody.prismadex.data.gameName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Trainer card + badge images (00:02/00:07/00:11/00:17, registry pass 30/10/2026).
 *
 * Layout IDEA only from the user's trainer-card reference (name header,
 * 3x2 team grid, trainer sprite, badges row), rendered from the app's saved
 * data only in the app's own theme: trainer name + sprite chosen in the
 * Account tab, the CHOSEN team's members (bundled art, falling back to the
 * active team), that team's game as plain text, and the registered badge
 * images for that game's set. The reference card is landscape-wide; this
 * portrait card stacks header, grid and badges instead of squeezing the
 * sprite beside the grid. No graphics, fonts, or watermark copied from the
 * reference. Badge slots show ONLY the image, no visible badge name, in
 * the checklist and here (00:11 clarification). In the Account tab the
 * badge slots are tappable (48dp) and toggle the record for the card
 * team's game; games without a verified image set show an honest note
 * instead of invented badges.
 *
 * Trainer sprites: static PNGs from the Showdown trainers listing, imported
 * minimal-only under the user's accepted IP risk (see
 * research/SPRITE-SOURCES.md). Badge images: SteGriff CC BY 3.0
 * recreations (Kanto-Unova) + PokeAPI/sprites files 70-77 (Paldea).
 * No badge artwork is invented.
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
internal fun TrainerArt(key: String, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val assets = LocalContext.current.assets
    val path = "trainers/$key.png"
    val image by produceState<ImageBitmap?>(null, path) {
        value = if (key.isBlank() || key !in TrainerSpriteKeys) null else withContext(Dispatchers.IO) {
            runCatching { assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
        }
    }
    val ready = image
    if (ready != null) {
        Image(ready, contentDescription, modifier)
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
internal fun TrainerCard(
    state: UserState,
    catalog: List<Pokemon>,
    modifier: Modifier = Modifier,
    onBadgeToggle: ((Int) -> Unit)? = null,
    onKantoBadgeToggle: ((Int) -> Unit)? = null,
    styleKey: String = "classico",
    backgroundKey: String = "padrao",
) {
    val cardTeam = state.cardTeam
    val members = cardTeam.members.mapNotNull { id -> catalog.find { it.id == id } }
    val badgeSet = state.cardBadgeSet(cardTeam.gameId)
    val style = if (styleKey in setOf("classico", "faixa", "compacto")) styleKey else "classico"
    val background = if (backgroundKey in TrainerCardBackgroundKeys) backgroundKey else "padrao"
    val firstTypeArgb = members.firstOrNull()?.types?.firstOrNull()?.let { typeColor(it).value.toLong() }
    val (bgTopLong, bgBottomLong, bgInkLong) = trainerCardBackgroundColors(background, firstTypeArgb)
    val surface = MaterialTheme.colorScheme.surface
    val bgTop = if (background == "padrao") surface else Color(bgTopLong)
    val bgBottom = if (background == "padrao") surface else Color(bgBottomLong)
    val ink = if (background == "padrao") MaterialTheme.colorScheme.onSurface else Color(bgInkLong)
    val sub = if (background == "padrao") MaterialTheme.colorScheme.onSurfaceVariant else ink.copy(alpha = .8f)
    val nameText = if (state.trainerName.isNotBlank()) state.trainerName else "Cartão de treinador"

    @Composable fun TeamGrid() {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (0 until 2).forEach { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0 until 3).forEach { c ->
                        val p = members.getOrNull(r * 3 + c)
                        Box(
                            Modifier.weight(1f).aspectRatio(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (background == "padrao") solidBlend(MaterialTheme.colorScheme.surfaceVariant,MaterialTheme.colorScheme.surface,.45f) else Color.White.copy(alpha = .10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (p != null) { if (cardTeam.builds[p.id]?.shiny == true) PokemonArtTrimmedShiny(p.id, Modifier.size(64.dp), p.name) else PokemonArtTrimmed(p.id, Modifier.size(64.dp), p.name) }
                        }
                    }
                }
            }
        }
    }

    @Composable fun BadgeBlock() {
        if (badgeSet != null) {
            val earned = state.cardBadgeIndices(cardTeam.gameId)
            LText("Insígnias", fontSize = 13.sp, letterSpacing = 1.7.sp, color = if (background == "padrao") MaterialTheme.colorScheme.primary else ink)
            if (style == "faixa") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0 until 8).forEach { index ->
                        val won = index in earned
                        Surface(
                            onClick = { onBadgeToggle?.invoke(index) }, enabled = onBadgeToggle != null,
                            shape = RoundedCornerShape(14.dp),
                            color = if (background == "padrao") solidBlend(MaterialTheme.colorScheme.surfaceVariant,MaterialTheme.colorScheme.surface,.45f) else Color.White.copy(alpha = .10f),
                            modifier = Modifier.weight(1f).aspectRatio(1f).heightIn(min = 48.dp)
                                .semantics { contentDescription = "Insígnia ${index + 1} de 8, ${if (won) "conquistada" else "não conquistada"}" }
                        ) {
                            Box(Modifier.padding(4.dp), contentAlignment = Alignment.Center) { BadgeArt(badgeSet.prefix, index, won, Modifier.size(32.dp)) }
                        }
                    }
                }
            } else {
                BadgeRecordGrid(badgeSet.prefix, earned, onBadgeToggle)
            }
            if (cardTeam.gameId in setOf(3, 4, 10)) {
                LText("Kanto · pós-jogo", fontSize = 13.sp, color = if (background == "padrao") MaterialTheme.colorScheme.primary else ink)
                BadgeRecordGrid("kanto", state.kantoPostgameBadges[cardTeam.gameId].orEmpty(), onKantoBadgeToggle)
            }
        } else {
            LText(
                "Sem conjunto de insígnias com imagens para este jogo.",
                style = MaterialTheme.typography.bodySmall, color = sub
            )
        }
    }

    Surface(color = bgBottom, shape = RoundedCornerShape(28.dp), modifier = modifier.fillMaxWidth()) {
        Box(Modifier.background(Brush.verticalGradient(listOf(bgTop, bgBottom)))) {
            if (background != "padrao") {
                Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                    val radius = size.height * .62f
                    val center = Offset(size.width * .80f, size.height * .52f)
                    drawCircle(Color.White.copy(alpha = .13f), radius, center)
                    drawCircle(Color(bgTopLong), radius * .22f, center)
                }
            }
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (style) {
                    "faixa" -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TrainerArt(state.trainerSprite, Modifier.size(96.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                LText(nameText, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = ink)
                                LText(gameName(cardTeam.gameId), style = MaterialTheme.typography.bodySmall, color = sub)
                                LText(cardTeam.name, style = MaterialTheme.typography.bodySmall, color = sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        TeamGrid()
                        BadgeBlock()
                    }
                    "compacto" -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            TrainerArt(state.trainerSprite, Modifier.size(120.dp))
                            LText(nameText, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            LText("${gameName(cardTeam.gameId)} · ${cardTeam.name}", style = MaterialTheme.typography.bodySmall, color = sub, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                        TeamGrid()
                        BadgeBlock()
                    }
                    else -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                LText(nameText, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = ink)
                                LText(gameName(cardTeam.gameId), style = MaterialTheme.typography.bodySmall, color = sub)
                                LText(cardTeam.name, style = MaterialTheme.typography.bodySmall, color = sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            TrainerArt(state.trainerSprite, Modifier.size(80.dp))
                        }
                        TeamGrid()
                        BadgeBlock()
                    }
                }
            }
        }
    }
}

@Composable
internal fun TrainerSpritePicker(selected: String, onSelect: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()
    val keys = remember(q) {
        if (q.isBlank()) TrainerSpriteKeys
        else TrainerSpriteKeys.filter { trainerSpriteSearchText(it).contains(q) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      androidx.compose.material3.OutlinedTextField(query, { query = it }, label = { LText("Buscar personagem") }, singleLine = true, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth())
      LText("${keys.size} personagens", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      if (keys.isEmpty()) LText("Nenhum personagem encontrado.", fontSize = 13.sp)
      LazyVerticalGrid(columns = GridCells.Adaptive(96.dp), modifier = Modifier.fillMaxWidth().height(360.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "no-trainer") {
        Surface(
            onClick = { onSelect("") },
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                if (selected.isBlank()) 2.dp else 1.dp,
                if (selected.isBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(.25f)
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LIcon(Icons.Rounded.Person, "Sem sprite de treinador", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                LText("Sem sprite", fontSize = 12.sp, lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        }
        items(keys, key = { it }) { key ->
            val name = trainerSpriteDisplayName(key)
            Surface(
                onClick = { onSelect(key) },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    if (selected == key) 2.dp else 1.dp,
                    if (selected == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(.25f)
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TrainerArt(key, Modifier.size(56.dp), contentDescription = name)
                    LText(name, fontSize = 12.sp, lineHeight = 15.sp, fontWeight = if (selected == key) FontWeight.SemiBold else FontWeight.Normal, color = if (selected == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
      LText("Nomes exibidos a partir da chave do arquivo; a fonte verificada não traz nomes oficiais de personagens.", fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
}

@Composable private fun BadgeRecordGrid(prefix:String,earned:Set<Int>,onToggle:((Int)->Unit)?){
 val ui=LocalUiStrings.current
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
  (0 until 2).forEach{r->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   (0 until 4).forEach{c->val index=r*4+c;val won=index in earned
    Surface(onClick={onToggle?.invoke(index)},enabled=onToggle!=null,shape=RoundedCornerShape(18.dp),color=solidBlend(MaterialTheme.colorScheme.surfaceVariant,MaterialTheme.colorScheme.surface,.45f),modifier=Modifier.weight(1f).aspectRatio(1f).heightIn(min=48.dp).semantics{contentDescription=ui.text("Insígnia ${index+1} de 8, ${if(won)"conquistada" else "não conquistada"}")}){
     Box(Modifier.padding(8.dp),contentAlignment=Alignment.Center){BadgeArt(prefix,index,won,Modifier.size(32.dp))}
    }
   }
  }}
 }
}
