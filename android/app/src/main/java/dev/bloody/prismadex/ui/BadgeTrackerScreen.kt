package dev.bloody.prismadex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.data.*

/**
 * Per-game badge registration, stored locally only (same offline-first rule
 * as teams: clearing the app data erases the records).
 *
 * Audit 29/10/2026: the repo had NO badge data — no badge names, counts,
 * gym leaders, or badge art in any bundled asset or code; `games.json`
 * maps game ids to slugs only. Badge IMAGES (30/10/2026, 00:11/00:17/00:19):
 * six regions now have a verified 8-image set (SteGriff CC BY 3.0
 * recreations for Kanto/Johto/Hoenn/Sinnoh/Unova; PokeAPI/sprites files
 * 70-77 for Paldea). For those games the checklist is IMAGE-ONLY: tap a
 * badge image to mark/unmark it, no badge name is written anywhere visible
 * (00:11 clarification). Games without an image set keep the user-named
 * entries as the honest fallback; nothing is invented for them, and the
 * missing Kalos/Galar images are a recorded gap (see research/BADGE-SOURCES.md).
 */
@Composable internal fun BadgeTrackerScreen(state: UserState, onState: (UserState) -> Unit) {
    // Games without a verified badge image set are NOT listed (11:38):
    // Colosseum, XD, Sun/Moon, Ultra Sun/Moon, Legends: Arceus,
    // Legends: Z-A and Champions are removed from the selector. Only
    // the 18 games in BadgeSetByGame appear, each with 8 images.
    // Stored named entries for removed games are preserved untouched
    // in Store; they are simply not reachable until a set exists.
    val badgeGames = SupportedGames.filter { BadgeSetByGame.containsKey(it.id) }
    var selectedGame by rememberSaveable { mutableIntStateOf(state.activeTeam.gameId.takeIf { BadgeSetByGame.containsKey(it) } ?: 1) }
    var newName by rememberSaveable(selectedGame) { mutableStateOf("") }
    var message by remember(selectedGame) { mutableStateOf<String?>(null) }
    val earned = state.badges[selectedGame].orEmpty()
    // Galar (game 20) resolves through the shared version state, exactly
    // as the trainer card does: Sword shows Fighting/Rock, Shield shows
    // Ghost/Ice, and progress lives in galarBadgeSets per version. Using
    // BadgeSetByGame here would always show Sword and store progress in
    // badgeSets[20], where the card never looks. Any badgeSets[20] data
    // saved by the old tracker is left untouched (not migrated: indices
    // mean different badges per version, so migration would be wrong).
    val badgeSet = state.cardBadgeSet(selectedGame)
    val earnedSet = state.cardBadgeIndices(selectedGame)
    val totalEarned = state.badges.values.sumOf { it.size } + state.badgeSets.values.sumOf { it.size } + state.galarBadgeSets.values.sumOf { it.size }
    val totalGames = (state.badges.filter { it.value.isNotEmpty() }.keys + state.badgeSets.filter { it.value.isNotEmpty() }.keys + (if (state.galarBadgeSets.values.any { it.isNotEmpty() }) setOf(20) else emptySet())).distinct().size
    fun update(next: List<String>) {
        val badges = if (next.isEmpty()) state.badges - selectedGame else state.badges + (selectedGame to next)
        onState(state.copy(badges = badges))
    }
    fun updateSet(next: Set<Int>) {
        if (selectedGame == 20) {
            val variant = if (state.galarVersion == "shield") "shield" else "sword"
            val updated = if (next.isEmpty()) state.galarBadgeSets - variant else state.galarBadgeSets + (variant to next)
            onState(state.copy(galarBadgeSets = updated))
        } else {
            val badgeSets = if (next.isEmpty()) state.badgeSets - selectedGame else state.badgeSets + (selectedGame to next)
            onState(state.copy(badgeSets = badgeSets))
        }
    }
    LazyColumn(Modifier.prismaViewport(), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LText("Insígnias por jogo", fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                LText("Marque as insígnias que você já conquistou em cada jogo. Os registros ficam só neste aparelho.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ChoiceField("JOGO", gameName(selectedGame), badgeGames.map { it.id.toString() to it.name }) { selectedGame = it.toInt(); newName = "" }
                if (selectedGame == 20) ChoiceField("VERSÃO DE GALAR", if (state.galarVersion == "shield") "Shield" else "Sword", listOf("sword" to "Sword", "shield" to "Shield")) { onState(state.copy(galarVersion = it)) }
                if (badgeSet != null) {
                    LText("${earnedSet.size} de $BadgeSetSize insígnias em ${gameName(selectedGame)}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    LText("Toque numa insígnia para marcar ou desmarcar. Só a imagem aparece — sem nome escrito.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LText("${earned.size} ${if (earned.size == 1) "insígnia registrada" else "insígnias registradas"} em ${gameName(selectedGame)}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    LText("Nomes e quantidades oficiais de insígnias ainda não estão na base local. Registre com o nome que você conhece; nada aqui é uma lista oficial.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (totalEarned > 0) LText("Total: $totalEarned ${if (totalEarned == 1) "insígnia" else "insígnias"} em $totalGames ${if (totalGames == 1) "jogo" else "jogos"}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (badgeSet != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0 until 2).forEach { r ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (0 until 4).forEach { c ->
                                val idx = r * 4 + c
                                val isEarned = idx in earnedSet
                                Surface(
                                    onClick = { updateSet(if (isEarned) earnedSet - idx else earnedSet + idx) },
                                    shape = RoundedCornerShape(18.dp),
                                    // Identity, same selection language as
                                    // the sprite picker: 2dp primary ring
                                    // when earned, 1dp faint ring when not.
                                    // Art stays greyscale when unearned.
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isEarned) 2.dp else 1.dp,
                                        if (isEarned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(.25f)
                                    ),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.weight(1f).aspectRatio(1f).heightIn(min = 48.dp)
                                        .semantics { contentDescription = "Insígnia ${idx + 1} de $BadgeSetSize, ${if (isEarned) "conquistada" else "não conquistada"}" }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                                        BadgeArt(badgeSet.prefix, idx, isEarned, Modifier.size(52.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                TextButton(onClick = { updateSet(emptySet()) }, modifier = Modifier.heightIn(min = 48.dp)) { LText("Limpar insígnias deste jogo") }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it.take(50) }, placeholder = { LText("Nome da insígnia") }, singleLine = true, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        val name = newName.trim()
                        when {
                            name.isEmpty() -> message = "Escreva o nome da insígnia."
                            earned.any { it.equals(name, ignoreCase = true) } -> message = "Essa insígnia já está registrada neste jogo."
                            earned.size >= 100 -> message = "Limite de 100 insígnias por jogo."
                            else -> { update(earned + name); newName = ""; message = null }
                        }
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(18.dp)) { LText("Adicionar insígnia") }
                    message?.let { LText(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                }
            }
            if (earned.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        LText("Nenhuma insígnia registrada neste jogo.", fontSize = 16.sp)
                        LText("Adicione a primeira insígnia acima.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            } else {
                items(earned, key = { it }) { name ->
                    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = true, onCheckedChange = { update(earned - name) }, modifier = Modifier.size(48.dp))
                            LText(name, Modifier.weight(1f), fontSize = 16.sp)
                            IconButton(onClick = { update(earned - name) }, modifier = Modifier.size(48.dp)) { LIcon(Icons.Rounded.Close, "Remover $name") }
                        }
                    }
                }
                item {
                    TextButton(onClick = { update(emptyList()); message = null }, modifier = Modifier.heightIn(min = 48.dp)) { LText("Limpar insígnias deste jogo") }
                }
            }
        }
        item {
            LText("Limpar os dados do aplicativo apaga estes registros, como acontece com equipes e favoritos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
