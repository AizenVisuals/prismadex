@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package dev.bloody.prismadex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable internal fun TeamPokemonPicker(catalog:List<Pokemon>,state:UserState,onAdd:(Pokemon)->Unit,onClose:()->Unit){
    var filter by remember{mutableStateOf(PokemonFilter())}
    var expanded by remember{mutableStateOf(false)}
    val index by produceState<PokemonFilterIndex?>(null,catalog){value=withContext(Dispatchers.Default){PokemonFilterIndex(catalog)}}
    val results by produceState<List<Pokemon>>(emptyList(),index,filter,state.activeTeam.gameId){value=withContext(Dispatchers.Default){index?.select(state.activeTeam.gameId,filter).orEmpty()}}
    val abilities=remember(catalog,state.activeTeam.gameId){catalog.filter{it.availableIn(state.activeTeam.gameId)}.flatMap{it.abilities}.distinct().sorted()}
    val team=state.team.mapNotNull{id->catalog.find{it.id==id}}
    LazyColumn(Modifier.prismaViewport(top=8.dp,bottom=16.dp),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){LText("Escolher parceiros",fontSize=24.sp,fontWeight=FontWeight.Bold);Text("${state.activeTeam.name} · ${state.team.size}/6",fontSize=14.sp);Text(gameName(state.activeTeam.gameId),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton(onClick=onClose){LIcon(Icons.Rounded.Close,"Fechar seleção")}}}
        item{OutlinedTextField(filter.query,{filter=filter.copy(query=it)},singleLine=true,label={LText("Nome ou número")},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp))}
        item{FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(expanded,{expanded=!expanded},label={LText("Filtros")});TextButton(onClick={filter=PokemonFilter()}){LText("Limpar filtros")};if(filter.generation!=0)LText("Geração ${filter.generation}");filter.types.forEach{TypePill(it)};filter.ability?.let{Text(it,fontSize=13.sp)}}}
        if(expanded)item{Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
            LText("Combinar tipos",fontSize=15.sp,fontWeight=FontWeight.SemiBold)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(filter.allTypes,{filter=filter.copy(allTypes=true)},label={LText("Todos os tipos escolhidos")});FilterChip(!filter.allTypes,{filter=filter.copy(allTypes=false)},label={LText("Qualquer tipo escolhido")})}
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){typeNames.forEach{(key,label)->FilterChip(key in filter.types,{filter=filter.copy(types=if(key in filter.types)filter.types-key else filter.types+key)},label={LText(label,fontSize=12.sp)},leadingIcon={TypeSymbol(key)})}}
            ChoiceField("GERAÇÃO DA ESPÉCIE",if(filter.generation==0)"Todas as gerações" else "Geração ${filter.generation}",listOf("0" to "Todas as gerações")+(1..9).map{it.toString() to "Geração $it"}){filter=filter.copy(generation=it.toInt())}
            ChoiceField("HABILIDADE",filter.ability?:"Todas as habilidades",listOf("" to "Todas as habilidades")+abilities.map{it to it}){filter=filter.copy(ability=it.takeIf{it.isNotBlank()})}
            val labels=listOf("Número","Nome","Geração","HP","Ataque","Defesa","Atq. especial","Def. especial","Velocidade")
            ChoiceField("ORDENAR",labels[filter.sort.ordinal],PokemonSort.entries.map{it.name to labels[it.ordinal]}){filter=filter.copy(sort=PokemonSort.valueOf(it))}
            Row(verticalAlignment=Alignment.CenterVertically){Switch(filter.includeForms,{filter=filter.copy(includeForms=it)});LText("Incluir formas alternativas",Modifier.padding(start=10.dp),fontSize=14.sp)}
        }}
        item{LText("${results.size} Pokémon e formas",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        if(results.isEmpty())item{LText("Nenhum Pokémon encontrado.",fontSize=16.sp);LText("Experimente outro nome ou filtro.",fontSize=13.sp)}
        items(results,key={it.id}){p->
            val present=team.any{it.baseId==p.baseId};val allowed=!present&&team.size<6
            Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
                PokemonArt(p.id,Modifier.size(60.dp),p.name)
                Column(Modifier.weight(1f)){Text(p.name,fontSize=15.sp,fontWeight=FontWeight.SemiBold);LText("#${p.baseId.toString().padStart(4,'0')}",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp)){p.types.forEach{TypePill(it)}};if(present)LText("Espécie já na equipe",fontSize=12.sp)}
                IconButton(enabled=allowed,onClick={onAdd(p)}){LIcon(Icons.Rounded.Add,"Adicionar ${p.name}")}
            }}
        }
    }
}
