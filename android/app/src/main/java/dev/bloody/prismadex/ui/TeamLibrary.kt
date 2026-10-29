package dev.bloody.prismadex.ui

import androidx.compose.foundation.layout.*
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
        Text(gameName(active.gameId),fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary)
        Text("${active.members.size}/6 parceiros · sets independentes · salvo no aparelho",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row{TextButton(onClick={renaming=true}){Text("Renomear")};if(state.teams.size>1)TextButton(onClick={deleting=true}){Text("Excluir equipe")}}
        if(active.gameId!=25)Text("Planejamento pela lista explícita de versões. Golpes seguem o jogo escolhido. Itens e treinamento específicos deste jogo ainda não estão incluídos; não é um certificado de legalidade.",fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if(renaming){var name by rememberSaveable(active.id){mutableStateOf(active.name)};AlertDialog(onDismissRequest={renaming=false},title={Text("Nome da equipe")},text={OutlinedTextField(name,{name=it.take(50)},singleLine=true)},confirmButton={TextButton(enabled=name.trim().isNotEmpty(),onClick={onState(state.updateTeam{it.copy(name=name.trim())});renaming=false}){Text("Salvar")}},dismissButton={TextButton(onClick={renaming=false}){Text("Cancelar")}})}
    if(deleting)AlertDialog(onDismissRequest={deleting=false},title={Text("Excluir ${active.name}?")},text={Text("Os Pokémon e sets desta equipe serão removidos. As outras equipes serão preservadas.")},confirmButton={TextButton(onClick={val remaining=state.teams.filterNot{it.id==active.id};onState(state.copy(teams=remaining,activeTeamId=remaining.first().id));deleting=false}){Text("Excluir")}},dismissButton={TextButton(onClick={deleting=false}){Text("Cancelar")}})
}

@Composable internal fun NewTeamDialog(state:UserState,onState:(UserState)->Unit,onDismiss:()->Unit){
        var name by rememberSaveable{mutableStateOf("")};var game by rememberSaveable{mutableStateOf(25)};var format by rememberSaveable{mutableStateOf(state.battleFormat)}
        AlertDialog(onDismissRequest={onDismiss()},title={Text("Nova equipe")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
            OutlinedTextField(name,{name=it.take(50)},label={Text("Nome da equipe")},singleLine=true,modifier=Modifier.fillMaxWidth())
            ChoiceField("JOGO · 25 grupos de versões",gameName(game),SupportedGames.map{it.id.toString() to it.name}){game=it.toInt()}
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(format=="singles",{format="singles"},label={Text("Singles")});FilterChip(format=="doubles",{format="doubles"},label={Text("Doubles")})}
            Text("O jogo define a lista de Pokémon e golpes. Não pode ser alterado depois; crie outra equipe para outro jogo.",fontSize=13.sp,lineHeight=17.sp)
        }},confirmButton={TextButton(enabled=name.trim().isNotEmpty()&&state.teams.size<100,onClick={val team=SavedTeam(UUID.randomUUID().toString(),name.trim(),game,format=format);onState(state.copy(teams=state.teams+team,activeTeamId=team.id));onDismiss()}){Text("Criar equipe")}},dismissButton={TextButton(onClick={onDismiss()}){Text("Cancelar")}})

}
