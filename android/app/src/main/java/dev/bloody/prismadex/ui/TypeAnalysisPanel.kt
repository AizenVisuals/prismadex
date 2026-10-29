@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package dev.bloody.prismadex.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.data.*

private fun multiplierLabel(value:Double)=when(value){0.0->"0×";.25->"¼×";.5->"½×";else->"${value.toInt()}×"}
@Composable internal fun TypeAnalysisPanel(members:List<Pokemon>,moves:List<Move> = emptyList(),team:Boolean=true){
 val rows=remember(members){TypeAnalysis.team(members)}
 var expanded by remember{mutableStateOf(false)}
 var offensiveType by remember{mutableStateOf<String?>(null)}
 var selected by remember{mutableStateOf<String?>(null)}
 Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
 LText(if(team)"Análise defensiva" else "Efetividade por tipo",fontSize=18.sp)
 LText("Tipagem moderna: não inclui habilidades, itens, clima ou regras históricas.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
 if(team)LText("↑ Fraquezas · ↓ Resistências · 0× Imunidades",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
 if(members.isEmpty())LText("Adicione Pokémon para analisar a equipe.",fontSize=14.sp)
 else {
 if(team)TextButton(onClick={expanded=!expanded}){LText(if(expanded)"Recolher análise" else "Ver os 18 tipos")}
 rows.filter{!team||expanded||it.weakCount>=3}.forEach{row->
 OutlinedButton(onClick={selected=if(selected==row.type)null else row.type},modifier=Modifier.fillMaxWidth(),contentPadding=PaddingValues(12.dp)){
 TypeSymbol(row.type);Spacer(Modifier.width(8.dp));LText(typeNames[row.type]?:row.type,Modifier.weight(1f),fontSize=13.sp)
 Text(if(team)"${row.weakCount} ↑ · ${row.resistantCount} ↓ · ${row.immuneCount} 0×" else multiplierLabel(row.members.single().multiplier),fontSize=13.sp)
 }
 if(row.weakCount>=3)LText("Fraqueza compartilhada: ${row.weakCount} integrantes",fontSize=13.sp,color=MaterialTheme.colorScheme.error)
 if(selected==row.type){row.members.forEach{match->LText("${members.first{it.id==match.pokemonId}.name} · ${multiplierLabel(match.multiplier)}",fontSize=14.sp)};if(!row.hasDefensiveAnswer)LText("Sem resistência ou imunidade por tipagem.",fontSize=13.sp)}
 }
 }
 if(team&&expanded){
 LText("Cobertura ofensiva dos golpes",fontSize=18.sp)
 if(moves.isEmpty())LText("Escolha golpes de dano nos sets para analisar a cobertura.",fontSize=13.sp)
 else FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){TypeAnalysis.offense(moves).forEach{row->AssistChip(onClick={offensiveType=if(offensiveType==row.defendingType)null else row.defendingType},label={Text("${typeNames[row.defendingType]} ${row.bestMultiplier?.let(::multiplierLabel)?:"—"}",fontSize=12.sp)},leadingIcon={TypeSymbol(row.defendingType)})}}
 offensiveType?.let{type->moves.filter{it.category!="status"}.distinctBy{it.id}.forEach{move->Text("${move.name} · ${multiplierLabel(typeMultiplier(move.type,listOf(type)))}",fontSize=13.sp)}}
 }
 }
}
