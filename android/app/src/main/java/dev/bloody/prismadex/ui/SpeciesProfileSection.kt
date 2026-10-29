package dev.bloody.prismadex.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.data.*

@Composable internal fun SpeciesProfileSection(p:Pokemon){
 val context=LocalContext.current;val repo=remember{SpeciesProfileRepository(context)}
 val profile by produceState<SpeciesProfile?>(null,p.id){value=repo.profile(p)}
 val language=LocalUiStrings.current.language
 profile?.let{s->Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
 LText("Biologia e referência nacional",fontSize=18.sp)
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
 LText("Habilidades da referência nacional",fontSize=17.sp)
 s.abilities.forEach{a->Text(a.name,fontSize=15.sp);if(a.isHidden==true)LText("Habilidade oculta",fontSize=12.sp,color=MaterialTheme.colorScheme.primary);a.description?.let{Text(it.text(language),fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
 LText("Estas informações gerais não substituem as habilidades ou regras de Champions no seu set.",fontSize=12.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
 }}
}

@Composable internal fun AbilityEffect(name:String?){
 if(name==null)return
 val context=LocalContext.current;val repo=remember{SpeciesProfileRepository(context)}
 val effect by produceState<AbilityProfile?>(null,name){value=repo.ability(name)}
 effect?.description?.let{Column{LText(if(effect?.championsSpecific==true)"Champions" else "Referência geral da habilidade",fontSize=12.sp,color=MaterialTheme.colorScheme.primary);Text(it.text(LocalUiStrings.current.language),fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
}
