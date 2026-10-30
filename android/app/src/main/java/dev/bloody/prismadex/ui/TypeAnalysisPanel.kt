@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package dev.bloody.prismadex.ui
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.data.*

private fun multiplierLabel(value:Double)=when(value){0.0->"0×";.25->"¼×";.5->"½×";else->"${value.toInt()}×"}

/** Grouped effectiveness: worst multipliers first, neutral last, every type reachable in one tap.
 *  The multipliers themselves come from [TypeAnalysis] unchanged; this file only groups and orders them. */
@Composable internal fun TypeAnalysisPanel(members:List<Pokemon>,moves:List<Move> = emptyList(),team:Boolean=true){
 val rows=remember(members){TypeAnalysis.team(members)}
 val memberIds=remember(members){members.map{it.id}}
 // Expansion is keyed on the members shown, so swiping to another Pokémon resets to the
 // defaults for that Pokémon instead of carrying the previous detail's open groups over.
 var singleExpanded by remember(memberIds){mutableStateOf(setOf(4.0,2.0,0.0))}
 var teamExpandedSections by remember(memberIds){mutableStateOf(setOf(TeamSectionKind.SHARED_WEAKNESS))}
 var offenseExpanded by remember(memberIds){mutableStateOf(false)}
 var offensiveType by remember(memberIds){mutableStateOf<String?>(null)}
 var selected by remember(memberIds){mutableStateOf<String?>(null)}
 Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
 LText(if(team)"Análise defensiva" else "Efetividade por tipo",fontSize=18.sp,fontWeight=FontWeight.SemiBold)
 LText("Tipagem moderna: não inclui habilidades, itens, clima ou regras históricas.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
 if(team)LText("↑ Fraquezas · ↓ Resistências · 0× Imunidades",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
 if(members.isEmpty())LText("Adicione Pokémon para analisar a equipe.",fontSize=14.sp)
 else if(!team){
  val groups=remember(members){TypeAnalysis.singleGroups(members.single())}
  groups.forEach{group->
   val expanded=group.multiplier in singleExpanded
   EffectivenessGroupHeader(
    title=singleGroupTitle(group.multiplier),
    badge=multiplierLabel(group.multiplier),
    badgeColor=when{group.multiplier>1.0->MaterialTheme.colorScheme.error;group.multiplier==1.0->MaterialTheme.colorScheme.onSurfaceVariant;else->MaterialTheme.colorScheme.primary},
    countLabel="${group.types.size} ${if(group.types.size==1)"tipo" else "tipos"}",
    expanded=expanded,
    onToggle={singleExpanded=if(expanded)singleExpanded-group.multiplier else singleExpanded+group.multiplier}
   )
   AnimatedVisibility(expanded,enter=expandEnter(LocalReduceMotion.current),exit=shrinkExit(LocalReduceMotion.current)){
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(top=2.dp,bottom=6.dp)){
     group.types.forEach{type->TypePill(type,compact=true)}
    }
   }
  }
 } else {
  val sections=remember(rows){TypeAnalysis.teamSections(rows)}
  sections.forEach{section->
   val expanded=section.kind in teamExpandedSections
   EffectivenessGroupHeader(
    title=teamSectionTitle(section.kind),
    badge="${section.rows.size}",
    badgeColor=if(section.kind==TeamSectionKind.SHARED_WEAKNESS)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    countLabel="${section.rows.size} ${if(section.rows.size==1)"tipo" else "tipos"}",
    expanded=expanded,
    onToggle={teamExpandedSections=if(expanded)teamExpandedSections-section.kind else teamExpandedSections+section.kind}
   )
   AnimatedVisibility(expanded,enter=expandEnter(LocalReduceMotion.current),exit=shrinkExit(LocalReduceMotion.current)){
    Column(verticalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(bottom=6.dp)){
     section.rows.forEach{row->
      OutlinedButton(onClick={selected=if(selected==row.type)null else row.type},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),contentPadding=PaddingValues(horizontal=12.dp,vertical=8.dp)){
       TypeSymbol(row.type);Spacer(Modifier.width(8.dp));LText(typeNames[row.type]?:row.type,Modifier.weight(1f),fontSize=14.sp)
       Text("${row.weakCount} ↑ · ${row.resistantCount} ↓ · ${row.immuneCount} 0×",fontSize=13.sp)
      }
      if(row.weakCount>=3)LText("Fraqueza compartilhada: ${row.weakCount} integrantes",fontSize=13.sp,color=MaterialTheme.colorScheme.error)
      if(selected==row.type){row.members.forEach{match->LText("${members.first{it.id==match.pokemonId}.name} · ${multiplierLabel(match.multiplier)}",fontSize=14.sp)};if(!row.hasDefensiveAnswer)LText("Sem resistência ou imunidade por tipagem.",fontSize=13.sp)}
     }
    }
   }
  }
  if(moves.isNotEmpty()){
   EffectivenessGroupHeader(
    title="Cobertura ofensiva dos golpes",
    badge="${TypeAnalysis.offense(moves).count{it.bestMultiplier!=null&&it.bestMultiplier>1.0}}",
    badgeColor=MaterialTheme.colorScheme.primary,
    countLabel="tipos super-efetivos",
    expanded=offenseExpanded,
    onToggle={offenseExpanded=!offenseExpanded}
   )
   AnimatedVisibility(offenseExpanded,enter=expandEnter(LocalReduceMotion.current),exit=shrinkExit(LocalReduceMotion.current)){
    Column(verticalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.padding(bottom=6.dp)){
     FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){TypeAnalysis.offense(moves).forEach{row->AssistChip(onClick={offensiveType=if(offensiveType==row.defendingType)null else row.defendingType},label={Text("${typeNames[row.defendingType]} ${row.bestMultiplier?.let(::multiplierLabel)?:"—"}",fontSize=12.sp)},leadingIcon={TypeSymbol(row.defendingType)})}}
     offensiveType?.let{type->moves.filter{it.category!="status"}.distinctBy{it.id}.forEach{move->Text("${move.name} · ${multiplierLabel(typeMultiplier(move.type,listOf(type)))}",fontSize=13.sp)}}
    }
   }
  } else {
   LText("Escolha golpes de dano nos sets para analisar a cobertura.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
 }
}

private fun singleGroupTitle(multiplier:Double)=when(multiplier){
 4.0->"Fraquezas graves";2.0->"Fraquezas";0.5->"Resistências";0.25->"Resistências fortes";0.0->"Imunidades";else->"Neutro"
}

private fun teamSectionTitle(kind:TeamSectionKind)=when(kind){
 TeamSectionKind.SHARED_WEAKNESS->"Fraqueza compartilhada";TeamSectionKind.WEAKNESS->"Outras fraquezas";TeamSectionKind.NO_WEAKNESS->"Sem fraqueza por tipagem"
}

@Composable private fun EffectivenessGroupHeader(title:String,badge:String,badgeColor:androidx.compose.ui.graphics.Color,countLabel:String,expanded:Boolean,onToggle:()->Unit){
 Surface(onClick=onToggle,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surface){
  Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){
   LText(badge,fontSize=15.sp,fontWeight=FontWeight.Bold,color=badgeColor,modifier=Modifier.padding(end=10.dp))
   Column(Modifier.weight(1f)){LText(title,fontSize=15.sp,fontWeight=FontWeight.SemiBold);LText(countLabel,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
   LIcon(if(expanded)Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,if(expanded)"Recolher $title" else "Expandir $title")
  }
 }
}
