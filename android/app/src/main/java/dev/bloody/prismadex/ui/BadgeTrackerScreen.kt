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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.data.*

/**
 * Per-game badge registration, stored locally only (same offline-first rule
 * as teams: clearing the app data erases the records).
 *
 * Audit 29/10/2026: the repo has NO badge data — no badge names, counts,
 * gym leaders, or badge art in any bundled asset or code; `games.json`
 * maps game ids to slugs only. Nothing is invented here: badge entries are
 * named by the user, one per earned badge, and progress is the count of
 * registered entries for the selected game. Verified badge names/counts
 * belong to the later data-fetch phase (see the gap list).
 */
@Composable internal fun BadgeTrackerScreen(state: UserState, onState: (UserState) -> Unit) {
    var selectedGame by rememberSaveable { mutableIntStateOf(state.activeTeam.gameId) }
    var newName by rememberSaveable(selectedGame) { mutableStateOf("") }
    var message by remember(selectedGame) { mutableStateOf<String?>(null) }
    val earned = state.badges[selectedGame].orEmpty()
    val totalEarned = state.badges.values.sumOf { it.size }
    val totalGames = state.badges.count { it.value.isNotEmpty() }
    fun update(next: List<String>) {
        val badges = if (next.isEmpty()) state.badges - selectedGame else state.badges + (selectedGame to next)
        onState(state.copy(badges = badges))
    }
    LazyColumn(Modifier.prismaViewport(), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LText("Insígnias por jogo", fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                LText("Marque as insígnias que você já conquistou em cada jogo. Os registros ficam só neste aparelho.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ChoiceField("JOGO", gameName(selectedGame), SupportedGames.map { it.id.toString() to it.name }) { selectedGame = it.toInt(); newName = "" }
                LText("${earned.size} insígnias registradas em ${gameName(selectedGame)}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (totalEarned > 0) LText("Total: $totalEarned insígnias em $totalGames ${if (totalGames == 1) "jogo" else "jogos"}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LText("Nomes e quantidades oficiais de insígnias ainda não estão na base local. Registre com o nome que você conhece; nada aqui é uma lista oficial.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
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
        item {
            LText("Limpar os dados do aplicativo apaga estes registros, como acontece com equipes e favoritos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
