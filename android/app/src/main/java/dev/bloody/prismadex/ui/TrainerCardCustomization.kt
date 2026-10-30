@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package dev.bloody.prismadex.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.data.Pokemon
import dev.bloody.prismadex.data.TrainerCardBackgrounds
import dev.bloody.prismadex.data.TrainerCardStyles
import dev.bloody.prismadex.data.UserState
import dev.bloody.prismadex.data.trainerCardBackgroundColors
import kotlinx.coroutines.launch

/**
 * Trainer-card customization screen (10:28, item 1).
 *
 * Opens like a Pokémon detail (ficha): a small up-arrow exit at the top,
 * the card itself as the hero, then the choices that shape it — style,
 * background, name, trainer sprite, card team and badges. Badges are
 * toggled by tapping them on the card preview, exactly as in the Account
 * tab. Styles and backgrounds are the app's own (see
 * TrainerCardDesign.kt): no template graphics, fonts, watermark or
 * artwork are copied. Everything is local; clearing the app's data
 * erases the card.
 */
@Composable
internal fun TrainerCardCustomizationScreen(
    state: UserState,
    catalog: List<Pokemon>,
    onState: (UserState) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var sharing by remember { mutableStateOf(false) }
    var shareError by remember { mutableStateOf<String?>(null) }
    val firstTypeArgb = state.cardTeam.members.mapNotNull { id -> catalog.find { it.id == id } }
        .firstOrNull()?.types?.firstOrNull()?.let { typeColor(it).value.toLong() }

    Column(
        Modifier.prismaViewport(top = 0.dp, bottom = 0.dp).imePadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                LIcon(Icons.Rounded.KeyboardArrowUp, "Voltar", modifier = Modifier.size(28.dp))
            }
            LText("Cartão de treinador", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        TrainerCard(
            state, catalog,
            styleKey = state.trainerCardStyle, backgroundKey = state.trainerCardBackground,
            onBadgeToggle = { index -> onState(state.toggleCardBadge(state.cardTeam.gameId, index)) },
            onKantoBadgeToggle = { index -> onState(state.toggleKantoPostgameBadge(state.cardTeam.gameId, index)) },
        )
        ActionButton(
            if (sharing) "Gerando imagem…" else "Compartilhar como imagem",
            Icons.Rounded.Share, enabled = !sharing
        ) {
            sharing = true
            shareError = null
            scope.launch {
                shareError = runCatching { shareTrainerCardImage(context, state, catalog) }.exceptionOrNull()?.message
                sharing = false
            }
        }
        shareError?.let { LText("Não foi possível compartilhar: $it", fontSize = 13.sp, color = MaterialTheme.colorScheme.error) }
        LText(
            "Toque numa insígnia do cartão para marcar ou desmarcar. O cartão mostra a equipe escolhida e as insígnias do jogo dela.",
            fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LText("ESTILO", fontSize = 13.sp, letterSpacing = 1.7.sp, color = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TrainerCardStyles.forEach { style ->
                val selected = state.trainerCardStyle == style.key
                Surface(
                    onClick = { onState(state.copy(trainerCardStyle = style.key)) },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        if (selected) 2.dp else 1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(.25f)
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            LText(style.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            LText(style.detail, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (selected) LIcon(Icons.Rounded.Check, "Estilo selecionado", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        LText("FUNDO DO CARTÃO", fontSize = 13.sp, letterSpacing = 1.7.sp, color = MaterialTheme.colorScheme.primary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TrainerCardBackgrounds.forEach { background ->
                val selected = state.trainerCardBackground == background.key
                val (topLong, bottomLong, _) = trainerCardBackgroundColors(background.key, firstTypeArgb)
                val top = if (background.key == "padrao") MaterialTheme.colorScheme.surface else Color(topLong)
                val bottom = if (background.key == "padrao") MaterialTheme.colorScheme.surfaceVariant else Color(bottomLong)
                Surface(
                    onClick = { onState(state.copy(trainerCardBackground = background.key)) },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        if (selected) 2.dp else 1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(.25f)
                    ),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            Modifier.size(width = 88.dp, height = 48.dp)
                                .background(Brush.verticalGradient(listOf(top, bottom)), RoundedCornerShape(12.dp))
                        )
                        LText(background.name, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 1)
                    }
                }
            }
        }
        LText("Fundos nas cores do próprio aplicativo. “Cor do tipo” usa a cor do primeiro tipo da equipe do cartão.", fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        LText("NOME", fontSize = 13.sp, letterSpacing = 1.7.sp, color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(
            state.trainerName, { onState(state.copy(trainerName = it.take(40))) },
            label = { LText("Nome de treinador") }, singleLine = true,
            shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()
        )

        LText("PERSONAGEM", fontSize = 13.sp, letterSpacing = 1.7.sp, color = MaterialTheme.colorScheme.primary)
        TrainerSpritePicker(state.trainerSprite) { onState(state.copy(trainerSprite = it)) }
        TrainerCreditLabel(state.trainerSprite)

        ChoiceField("EQUIPE DO CARTÃO", state.cardTeam.name, state.teams.map { it.id to it.name }) { id ->
            onState(state.copy(trainerCardTeamId = id))
        }
        if (state.cardTeam.gameId == 20) {
            ChoiceField(
                "VERSÃO DAS INSÍGNIAS",
                if (state.galarVersion == "shield") "Shield" else "Sword",
                listOf("sword" to "Sword", "shield" to "Shield")
            ) { onState(state.copy(galarVersion = it)) }
        }

        LText(
            "Nome, personagem, equipe, estilo, fundo e insígnias ficam só neste aparelho — limpar os dados do aplicativo apaga tudo.",
            fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
