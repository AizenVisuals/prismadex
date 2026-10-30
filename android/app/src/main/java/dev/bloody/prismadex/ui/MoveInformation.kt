@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.bloody.prismadex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.data.Move

@Composable internal fun MoveFacts(move: Move) {
    FlowRow(horizontalArrangement=Arrangement.spacedBy(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        val category=when(move.category){"physical"->"Físico";"special"->"Especial";else->"Status"}
        listOf("PODER" to (move.power?.takeIf{it>0}?.toString()?:if(move.category=="status")"—" else "Variável"),"PRECISÃO" to (move.accuracy?.let{"$it%"}?:"—"),"PP" to (move.pp?.toString()?:"—"),"CLASSE" to category).forEach{(label,value)->
            Column(verticalArrangement=Arrangement.spacedBy(4.dp)){LText(label,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);LText(value,fontSize=14.sp,fontWeight=FontWeight.Medium)}
        }
    }
}

private val moveTargets=mapOf(
    "normal" to "Um Pokémon adjacente", "any" to "Qualquer outro Pokémon", "self" to "Usuário",
    "allAdjacent" to "Todos os Pokémon adjacentes", "allAdjacentFoes" to "Adversários adjacentes",
    "adjacentAlly" to "Aliado adjacente", "adjacentAllyOrSelf" to "Usuário ou aliado adjacente",
    "adjacentFoe" to "Adversário adjacente", "allySide" to "Lado dos aliados", "foeSide" to "Lado dos adversários",
    "allyTeam" to "Equipe do usuário", "all" to "Todos os Pokémon", "randomNormal" to "Adversário aleatório",
    "scripted" to "Definido pelo efeito do golpe",
)
private val moveFlags=mapOf("contact" to "Contato físico", "sound" to "Sonoro", "bite" to "Mordida", "punch" to "Punho", "bullet" to "Projétil", "powder" to "Pó", "pulse" to "Pulso", "wind" to "Vento", "slicing" to "Corte", "protect" to "Bloqueável por Protect", "authentic" to "Ignora Substitute", "defrost" to "Descongela o usuário", "dance" to "Dança")

@Composable internal fun MoveInfoSheet(move: Move, onDismiss: () -> Unit, learners: List<dev.bloody.prismadex.data.Pokemon> = emptyList(), onOpenLearner: ((dev.bloody.prismadex.data.Pokemon) -> Unit)? = null) {
    var original by androidx.compose.runtime.remember(move.id){androidx.compose.runtime.mutableStateOf(false)}
    ModalBottomSheet(onDismissRequest=onDismiss,containerColor=MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().heightIn(max=620.dp).verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                TypeSymbol(move.type,Modifier.size(32.dp));LText(move.name,Modifier.weight(1f),fontSize=25.sp,lineHeight=29.sp,fontWeight=FontWeight.Bold)
                IconButton(onClick=onDismiss){LIcon(Icons.Rounded.Close,"Fechar informações do golpe")}
            }
            TypePill(move.type)
            MoveFacts(move)
            Row(horizontalArrangement=Arrangement.spacedBy(24.dp)){
                Column{LText("PRIORIDADE",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);LText(if(move.priority>0)"+${move.priority}" else move.priority.toString(),fontSize=17.sp)}
                Column(Modifier.weight(1f)){LText("ALVO",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);LText(moveTargets[move.target]?:move.target,fontSize=15.sp,lineHeight=21.sp)}
            }
            if(move.summary.isNotBlank())Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                LText("Resumo",fontSize=14.sp,fontWeight=FontWeight.SemiBold)
                Text(if(original)move.summary else descriptionText(move.summary),fontSize=15.sp,lineHeight=22.sp)
            }
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                LText("Efeito completo",fontSize=17.sp,fontWeight=FontWeight.SemiBold)
                Text((if(original)move.description else descriptionText(move.description)).ifBlank{LocalUiStrings.current.text("Sem descrição registrada para este golpe.")},fontSize=15.sp,lineHeight=23.sp)
            }
            if(LocalUiStrings.current.language!="en")TextButton(onClick={original=!original}){LText(if(original)"Ver tradução" else "Ver texto original") }
            val flags=moveFlags.filterKeys{it in move.flags}.values
            if(flags.isNotEmpty())Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                LText("Propriedades",fontSize=17.sp,fontWeight=FontWeight.SemiBold)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){flags.forEach{Surface(shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceVariant){LText(it,Modifier.padding(horizontal=12.dp,vertical=8.dp),fontSize=13.sp)}}}
            }
            if(learners.isNotEmpty())Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                LText("Pokémon que aprendem · Champions",fontSize=17.sp,fontWeight=FontWeight.SemiBold)
                LText("${learners.size} Pokémon na lista Champions.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                learners.take(24).forEach{p->Row(Modifier.fillMaxWidth().heightIn(min=48.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).clickable(enabled=onOpenLearner!=null){onOpenLearner?.invoke(p);onDismiss()}.padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){PokemonArt(p.id,Modifier.size(40.dp),p.name);LText(p.name,Modifier.weight(1f),fontSize=15.sp)}}
                if(learners.size>24)LText("Mostrando 24 de ${learners.size}.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LText(if(move.referenceGame==25)"Referência Champions do Showdown · base 1242c1e2. O texto específico de Champions tem prioridade sobre a descrição geral." else "Descrição da referência geral atual. Efeitos, alvo e prioridade podem variar nos jogos antigos.",fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
