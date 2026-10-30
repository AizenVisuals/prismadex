@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package dev.bloody.prismadex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.data.*
import java.util.UUID

@Composable internal fun TeamLibraryHeader(state:UserState,onState:(UserState)->Unit){
    var renaming by rememberSaveable{mutableStateOf(false)}
    var deleting by rememberSaveable{mutableStateOf(false)}
    val active=state.activeTeam
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        ChoiceField("EQUIPE ATIVA · ${state.teams.size}/100",active.name,state.teams.map{it.id to "${it.name} · ${gameName(it.gameId)} · ${it.members.size}/6"}){onState(state.copy(activeTeamId=it))}
        LText(gameName(active.gameId),fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary)
        LText("${active.members.size}/6 parceiros · sets independentes · salvo no aparelho",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow{TextButton(onClick={renaming=true}){LText("Renomear")};if(state.teams.size>1)TextButton(onClick={deleting=true}){LText("Excluir equipe")}}
        if(active.gameId!=25)LText("Planejamento pela lista explícita de versões. Golpes seguem o jogo escolhido. Itens e treinamento específicos deste jogo ainda não estão incluídos; não é um certificado de legalidade.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if(renaming){var name by rememberSaveable(active.id){mutableStateOf(active.name)};var game by rememberSaveable(active.id){mutableIntStateOf(active.gameId)};val canEditGame=active.members.isEmpty()&&active.builds.isEmpty();AlertDialog(onDismissRequest={renaming=false},title={LText("Nome e jogo da equipe")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){OutlinedTextField(name,{name=it.take(50)},singleLine=true,modifier=Modifier.fillMaxWidth());if(canEditGame)ChoiceField("JOGO",gameName(game),SupportedGames.map{it.id.toString() to it.name}){game=it.toInt()} else LText("Jogo: ${gameName(active.gameId)}. O jogo só pode ser alterado com a equipe vazia; crie outra equipe para outro jogo.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}},confirmButton={TextButton(enabled=name.trim().isNotEmpty(),onClick={onState(state.updateTeam{it.copy(name=name.trim(),gameId=game)});renaming=false}){LText("Salvar")}},dismissButton={TextButton(onClick={renaming=false}){LText("Cancelar")}})}
    if(deleting)AlertDialog(onDismissRequest={deleting=false},title={LText("Excluir ${active.name}?")},text={LText("Os Pokémon e sets desta equipe serão removidos. As outras equipes serão preservadas.")},confirmButton={TextButton(onClick={val remaining=state.teams.filterNot{it.id==active.id};onState(state.copy(teams=remaining,activeTeamId=remaining.first().id));deleting=false}){LText("Excluir")}},dismissButton={TextButton(onClick={deleting=false}){LText("Cancelar")}})
}

@Composable internal fun NewTeamDialog(state:UserState,onState:(UserState)->Unit,onDismiss:()->Unit){
        var name by rememberSaveable{mutableStateOf("")};var game by rememberSaveable{mutableStateOf(23)};var format by rememberSaveable{mutableStateOf(state.battleFormat)}
        AlertDialog(onDismissRequest={onDismiss()},title={LText("Nova equipe")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
            OutlinedTextField(name,{name=it.take(50)},label={LText("Nome da equipe")},singleLine=true,modifier=Modifier.fillMaxWidth())
            ChoiceField("JOGO · 25 grupos de versões",gameName(game),SupportedGames.map{it.id.toString() to it.name}){game=it.toInt()}
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(format=="singles",{format="singles"},label={LText("Singles · individual")});FilterChip(format=="doubles",{format="doubles"},label={LText("Doubles · duplas")})}
            LText("O jogo define a lista de Pokémon e golpes. Com parceiros na equipe, o jogo não pode mais ser alterado; crie outra equipe para outro jogo.",style=MaterialTheme.typography.bodySmall)
        }},confirmButton={TextButton(enabled=name.trim().isNotEmpty()&&state.teams.size<100,onClick={val team=SavedTeam(UUID.randomUUID().toString(),name.trim(),game,format=format);onState(state.copy(teams=state.teams+team,activeTeamId=team.id));onDismiss()}){LText("Criar equipe")}},dismissButton={TextButton(onClick={onDismiss()}){LText("Cancelar")}})

}
