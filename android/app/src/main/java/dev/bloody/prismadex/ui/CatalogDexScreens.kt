@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.bloody.prismadex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.data.*

/**
 * Browseable catalogues in the same clean format as the Pokédex, in the app's
 * own theme. Every field shown comes from the bundled data; a missing
 * description/effect renders the honest empty text, never an invented one.
 * Ability PT/ES descriptions carry the bundled marker
 * "local-machine-translation-with-reviewed-overrides" — machine translation
 * with reviewed overrides, not fully human-reviewed (317/376 abilities).
 */

@Composable internal fun MovesDexScreen(catalog: List<Pokemon>, repo: PokemonRepository, onOpenPokemon: (Pokemon) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf<String?>(null) }
    var category by rememberSaveable { mutableStateOf("all") }
    var championsOnly by rememberSaveable { mutableStateOf(false) }
    var moves by remember { mutableStateOf<List<Move>>(emptyList()) }
    var learnersByMove by remember { mutableStateOf<Map<Int, List<Int>>>(emptyMap()) }
    var championsIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    LaunchedEffect(Unit) {
        moves = repo.allMoves()
        learnersByMove = repo.championsLearnersByMove()
        championsIds = repo.championsMoveIds()
    }
    val shown = remember(moves, query, type, category, championsOnly) {
        moves.filter { move ->
            (query.isBlank() || move.name.contains(query, true)) &&
                (type == null || move.type == type) &&
                (category == "all" || move.category == category) &&
                (!championsOnly || move.id in championsIds)
        }
    }
    val activeFilters = (if (type != null) 1 else 0) + (if (category != "all") 1 else 0) + (if (championsOnly) 1 else 0)
    LazyColumn(Modifier.prismaViewport(), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LText("Dex de golpes", fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                LText("${shown.size} de ${moves.size} golpes · catálogo geral", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = query, onValueChange = { query = it }, placeholder = { LText("Buscar golpe", style = MaterialTheme.typography.bodyLarge) }, leadingIcon = { LIcon(Icons.Rounded.Search, null) }, trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { LIcon(Icons.Rounded.Close, "Limpar busca") } }, singleLine = true, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth())
                LText("TIPO", fontSize = 13.sp, letterSpacing = 1.7.sp, color = MaterialTheme.colorScheme.primary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    item { FilterChip(type == null, { type = null }, label = { LText("Todos") }) }
                    items(typeNames.keys.toList()) { t -> FilterChip(type == t, { type = if (type == t) null else t }, label = { LText(typeNames.getValue(t)) }, leadingIcon = { TypeSymbol(t) }) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all" to "Todas as classes", "physical" to "Físico", "special" to "Especial", "status" to "Status").forEach { (key, label) ->
                        FilterChip(category == key, { category = key }, label = { LText(label) })
                    }
                    FilterChip(championsOnly, { championsOnly = !championsOnly }, label = { LText("Só Champions") })
                    if (query.isNotBlank() || activeFilters > 0) TextButton(onClick = { query = ""; type = null; category = "all"; championsOnly = false }) { LText("Limpar tudo") }
                }
            }
        }
        items(shown, key = { it.id }) { move ->
            val learners = remember(learnersByMove, move.id) { learnersByMove[move.id].orEmpty().mapNotNull { id -> catalog.find { it.id == id } } }
            MoveRow(move, learners = learners, onOpenLearner = onOpenPokemon)
        }
        if (shown.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                LText("Nenhum golpe encontrado.", fontSize = 16.sp, modifier = Modifier.padding(top = 18.dp))
                LText("Experimente outro nome ou filtro.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                if (query.isNotBlank() || activeFilters > 0) TextButton(onClick = { query = ""; type = null; category = "all"; championsOnly = false }) { LText("Limpar tudo") }
            }
        }
    }
}

@Composable internal fun AbilitiesDexScreen(catalog: List<Pokemon>, onOpenPokemon: (Pokemon) -> Unit) {
    val context = LocalContext.current
    val repo = remember { SpeciesProfileRepository(context) }
    val language = LocalUiStrings.current.language
    var query by rememberSaveable { mutableStateOf("") }
    var withDescriptionOnly by rememberSaveable { mutableStateOf(false) }
    var abilities by remember { mutableStateOf<List<AbilityProfile>>(emptyList()) }
    var selected by remember { mutableStateOf<AbilityProfile?>(null) }
    LaunchedEffect(Unit) { abilities = repo.allAbilities() }
    val shown = remember(abilities, query, withDescriptionOnly, language) {
        abilities.filter { ability ->
            val effect = ability.description?.text(language).orEmpty()
            (query.isBlank() || ability.name.contains(query, true) || effect.contains(query, true)) &&
                (!withDescriptionOnly || effect.isNotBlank())
        }
    }
    selected?.let { ability ->
        AbilityDetailSheet(ability, catalog, onDismiss = { selected = null }, onOpenPokemon = { p -> selected = null; onOpenPokemon(p) })
    }
    LazyColumn(Modifier.prismaViewport(), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LText("Dex de habilidades", fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                LText("${shown.size} de ${abilities.size} habilidades · referência nacional", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = query, onValueChange = { query = it }, placeholder = { LText("Buscar por nome ou efeito", style = MaterialTheme.typography.bodyLarge) }, leadingIcon = { LIcon(Icons.Rounded.Search, null) }, trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { LIcon(Icons.Rounded.Close, "Limpar busca") } }, singleLine = true, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(withDescriptionOnly, { withDescriptionOnly = !withDescriptionOnly }, label = { LText("Com descrição") })
                    if (query.isNotBlank() || withDescriptionOnly) TextButton(onClick = { query = ""; withDescriptionOnly = false }) { LText("Limpar tudo") }
                }
                LText("Descrições em português e espanhol são traduções automáticas com revisões pontuais; o texto original é o inglês.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(shown, key = { it.slug }) { ability ->
            val effect = ability.description?.text(language).orEmpty()
            Surface(onClick = { selected = ability }, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LText(ability.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    if (effect.isNotBlank()) Text(effect, fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    else LText("Sem descrição registrada para esta habilidade.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LText(if (ability.championsSpecific) "Champions" else "Referência geral da habilidade", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (shown.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                LText("Nenhuma habilidade encontrada.", fontSize = 16.sp, modifier = Modifier.padding(top = 18.dp))
                LText("Experimente outro nome ou filtro.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                if (query.isNotBlank() || withDescriptionOnly) TextButton(onClick = { query = ""; withDescriptionOnly = false }) { LText("Limpar tudo") }
            }
        }
    }
}

@Composable internal fun AbilityDetailSheet(ability: AbilityProfile, catalog: List<Pokemon>, onDismiss: () -> Unit, onOpenPokemon: ((Pokemon) -> Unit)? = null) {
    val context = LocalContext.current
    val repo = remember { SpeciesProfileRepository(context) }
    val language = LocalUiStrings.current.language
    var learners by remember(ability) { mutableStateOf<List<Pair<Pokemon, Boolean?>>>(emptyList()) }
    var learnersLoaded by remember(ability) { mutableStateOf(false) }
    LaunchedEffect(ability) {
        val id = ability.id
        learners = if (id == null) emptyList() else repo.abilityLearners(id).mapNotNull { (pokemonId, hidden) -> catalog.find { it.id == pokemonId }?.let { it to hidden } }
        learnersLoaded = true
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().heightIn(max = 620.dp).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LText(ability.name, Modifier.weight(1f), fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { LIcon(Icons.Rounded.Close, "Fechar informações da habilidade") }
            }
            LText(if (ability.championsSpecific) "Champions" else "Referência geral da habilidade", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            val effect = ability.description?.text(language).orEmpty()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LText("Efeito", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                if (effect.isNotBlank()) Text(effect, fontSize = 15.sp, lineHeight = 23.sp)
                else LText("Sem descrição registrada para esta habilidade.", fontSize = 15.sp, lineHeight = 23.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LText("Pokémon com esta habilidade", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                if (ability.id == null) {
                    LText("Lista de Pokémon não disponível para esta habilidade na base local.", fontSize = 13.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (!learnersLoaded) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                } else if (learners.isEmpty()) {
                    LText("Nenhum Pokémon com esta habilidade na base local.", fontSize = 13.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LText("${learners.size} Pokémon na referência nacional.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    learners.take(60).forEach { (p, hidden) ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp)).clickable(enabled = onOpenPokemon != null) { onOpenPokemon?.invoke(p) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PokemonArt(p.id, Modifier.size(40.dp), p.name)
                            LText(p.name, Modifier.weight(1f), fontSize = 15.sp)
                            if (hidden == true) LText("Habilidade oculta", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (learners.size > 60) LText("Mostrando 60 de ${learners.size}.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            LText("Estas informações gerais não substituem as habilidades ou regras de Champions no seu set.", fontSize = 13.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A tappable ability name that opens the ability's detail sheet. Used by the detail page. */
@Composable internal fun AbilityNameButton(name: String, catalog: List<Pokemon>, onOpenPokemon: ((Pokemon) -> Unit)? = null) {
    val context = LocalContext.current
    val repo = remember { SpeciesProfileRepository(context) }
    var profile by remember(name) { mutableStateOf<AbilityProfile?>(null) }
    var showSheet by remember(name) { mutableStateOf(false) }
    LaunchedEffect(name) { profile = repo.ability(name) }
    val resolved = profile
    if (showSheet && resolved != null) {
        AbilityDetailSheet(resolved, catalog, onDismiss = { showSheet = false }, onOpenPokemon = onOpenPokemon)
    }
    TextButton(onClick = { if (resolved != null) showSheet = true }, modifier = Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 0.dp)) {
        LText(name.replace('-', ' '), fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
    }
}
