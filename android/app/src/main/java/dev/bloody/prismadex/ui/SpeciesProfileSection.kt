package dev.bloody.prismadex.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.data.*

/**
 * Reading used for the 23:27 direction ("Biologia" and "Referência Nacional"
 * become buttons, abilities too): the two wordings in this file are the
 * section header "Biologia e referência nacional" and the subheader
 * "Habilidades da referência nacional". Both are now tappable buttons that
 * expand/collapse the information below them (CLOSED by default per the
 * 00:37 direction: a fresh detail open always shows the closed headers),
 * and every ability name is a button
 * that opens that ability's detail sheet. All fields shown before are
 * still shown, from the same bundled data.
 */
@Composable internal fun SpeciesProfileSection(p:Pokemon,catalog:List<Pokemon> = emptyList(),onOpenPokemon: ((Pokemon) -> Unit)? = null){
 val context=LocalContext.current;val repo=remember{SpeciesProfileRepository(context)}
 val profile by produceState<SpeciesProfile?>(null,p.id){value=repo.profile(p)}
 val language=LocalUiStrings.current.language
 var biologyExpanded by remember(p.id){mutableStateOf(false)}
 var abilitiesExpanded by remember(p.id){mutableStateOf(false)}
 profile?.let{s->Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
 Surface(onClick={biologyExpanded=!biologyExpanded},shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth()){
  Row(Modifier.fillMaxWidth().heightIn(min=48.dp).padding(horizontal=14.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){
   LText("Biologia e referência nacional",Modifier.weight(1f),fontSize=18.sp,fontWeight=FontWeight.SemiBold)
   LIcon(Icons.Rounded.ExpandMore,if(biologyExpanded)"Recolher biologia e referência nacional" else "Expandir biologia e referência nacional",Modifier.rotate(if(biologyExpanded)180f else 0f),tint=MaterialTheme.colorScheme.primary)
  }
 }
 if(biologyExpanded)Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
 s.category?.let{Text(it.text(language),fontSize=15.sp)}
 LText("Grupos de ovos",fontSize=13.sp);Text(s.eggGroups.joinToString(" · "){it.text(language)},fontSize=14.sp)
 s.formGender?.let{LText(if(it=="female")"Forma feminina" else "Forma masculina",fontSize=14.sp)}
 LText("Proporção de gênero da espécie",fontSize=13.sp)
 Text(when(val rate=s.genderRateEighths){null->LocalUiStrings.current.text("Não informado");-1->LocalUiStrings.current.text("Sem gênero");else->"${(8-rate)*12.5}% ♂ · ${rate*12.5}% ♀"},fontSize=14.sp)
 s.growthRate?.let{LText("Crescimento",fontSize=13.sp);Text(it.text(language),fontSize=14.sp)}
 Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){s.captureRate?.let{LText("Taxa de captura: $it",fontSize=13.sp)};s.baseExperience?.let{LText("Experiência base: $it",fontSize=13.sp)}}
 s.hatchCounter?.let{LText("Ciclos para chocar: $it",fontSize=13.sp)}
 s.baseHappiness?.let{LText("Amizade base: $it",fontSize=13.sp)}
 if(s.isLegendary)LText("Lendário",fontSize=13.sp)
 if(s.isMythical)LText("Mítico",fontSize=13.sp)
 if(s.isBaby)LText("Pokémon bebê",fontSize=13.sp)
 }
 Surface(onClick={abilitiesExpanded=!abilitiesExpanded},shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth()){
  Row(Modifier.fillMaxWidth().heightIn(min=48.dp).padding(horizontal=14.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){
   LText("Habilidades da referência nacional",Modifier.weight(1f),fontSize=17.sp,fontWeight=FontWeight.SemiBold)
   LIcon(Icons.Rounded.ExpandMore,if(abilitiesExpanded)"Recolher habilidades da referência nacional" else "Expandir habilidades da referência nacional",Modifier.rotate(if(abilitiesExpanded)180f else 0f),tint=MaterialTheme.colorScheme.primary)
  }
 }
 if(abilitiesExpanded)Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
  s.abilities.forEach{a->Column{
   Row(verticalAlignment=Alignment.CenterVertically){
    AbilityNameButton(a.name,catalog,onOpenPokemon)
    if(a.isHidden==true){Spacer(Modifier.width(8.dp));LText("Habilidade oculta",fontSize=12.sp,color=MaterialTheme.colorScheme.primary)}
   }
   a.description?.let{Text(it.text(language),fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
  }}
  LText("Estas informações gerais não substituem as habilidades ou regras de Champions no seu set.",fontSize=12.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
 }
 }}
}

@Composable internal fun AbilityEffect(name:String?){
 if(name==null)return
 val context=LocalContext.current;val repo=remember{SpeciesProfileRepository(context)}
 val effect by produceState<AbilityProfile?>(null,name){value=repo.ability(name)}
 effect?.description?.let{Column{LText(if(effect?.championsSpecific==true)"Champions" else "Referência geral da habilidade",fontSize=12.sp,color=MaterialTheme.colorScheme.primary);Text(it.text(LocalUiStrings.current.language),fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
}
