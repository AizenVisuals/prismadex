package dev.bloody.prismadex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.data.*
import kotlinx.coroutines.CancellationException

@Composable internal fun EvolutionSection(pokemon:Pokemon,catalog:List<Pokemon>,enabled:Boolean,onOpen:(Pokemon)->Unit){
    val context=LocalContext.current
    val repository=remember{EvolutionRepository(context)}
    var family by remember(pokemon.id){mutableStateOf<EvolutionFamily?>(null)}
    var loaded by remember(pokemon.id){mutableStateOf(false)}
    LaunchedEffect(pokemon.id,catalog){
        try { family=repository.family(pokemon,catalog) }
        catch(e:CancellationException){throw e}
        catch(_:Exception){family=null}
        finally { loaded=true }
    }
    val identities=remember(catalog){catalog.associateBy{it.id}}
    val language=LocalUiStrings.current.language
    // Forms and transformations (11:58): every other catalog record sharing
    // this species (same baseId) — alternate forms, Mega Evolutions and
    // Gigantamax forms, all from verified bundled data, none invented.
    // Shown as options alongside the chain, never replacing it.
    val formOptions=remember(pokemon,catalog){catalog.filter{it.baseId==pokemon.baseId&&it.id!=pokemon.id}.sortedBy{it.id}}
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)){
        LText("LINHA EVOLUTIVA",fontSize=13.sp,letterSpacing=1.7.sp,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(top=8.dp))
        val current=family
        when {
            !loaded -> LText("Consultando evoluções no aparelho…",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            current==null -> {
                if(formOptions.isEmpty())LText("Linha evolutiva ainda não disponível nesta base.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> {
                if(current.isTransformation)LText("Transformações como Mega Evolução não são etapas permanentes da linha evolutiva.",fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(!current.hasEvolution)LText("Sem evolução registrada para esta espécie.",fontSize=14.sp,lineHeight=20.sp)
                val nodes=current.nodes.associateBy{it.pokemonId}
                current.edges.forEach{edge->
                    val from=nodes.getValue(edge.fromId)
                    val to=nodes.getValue(edge.toId)
                    Surface(shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.surface){
                        Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                EvolutionIdentity(from,pokemon.id,Modifier.weight(1f),enabled){identities[from.pokemonId]?.let(onOpen)}
                                LIcon(Icons.AutoMirrored.Rounded.ArrowForward,"Evolui para ${to.name}",Modifier.size(20.dp),tint=MaterialTheme.colorScheme.primary)
                                EvolutionIdentity(to,pokemon.id,Modifier.weight(1f),enabled){identities[to.pokemonId]?.let(onOpen)}
                            }
                            edge.conditions.forEachIndexed{index,condition->
                                if(index>0)LText("ou",fontSize=12.sp,color=MaterialTheme.colorScheme.primary)
                                Text(condition.text(language),fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                LText("Alguns métodos recentes e detalhes específicos de cada jogo ainda não estão completos.",fontSize=12.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                LText("A evolução depende do jogo. Esta linha não confirma disponibilidade ou evolução dentro de Champions.",fontSize=12.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if(pokemon.id==802){
            // Zenith Marshadow: battle-only cosmetic transformation. No separate
            // PokeAPI record/stats (pokemon-form/marshadow-zenith = 404; single
            // stats table 90/125/80/90/90/125 shared with the species). Text only:
            // no new dex record, no duplicated stats, no art import.
            // Sources: Bulbapedia "Marshadow (Pokémon)" § Zenith Marshadow;
            // Victory Road Wiki "Marshadow" (Zenith artwork caption, single stats
            // table, Soul-Stealing 7-Star Strike Z-Move note).
            LText("ZENITH MARSHADOW",fontSize=13.sp,letterSpacing=1.7.sp,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(top=8.dp))
            LText("Quando fica irritado ou ataca — inclusive durante o Z-Move Soul-Stealing 7-Star Strike —, o espírito de luta de Marshadow começa a arder e ele assume a forma Zenith: capacete, saliências dos pulsos e gola se inflamam como chamas verdes e amarelas, e os olhos também se inflamam, sem mudar de cor.",fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            LText("É só uma transformação visual em batalha: não há stats, tipos ou habilidade próprios do Zenith — valem os dados desta mesma ficha. Na versão shiny, as chamas do Zenith ficam roxas.",fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if(loaded&&formOptions.isNotEmpty()){
            LText("FORMAS E TRANSFORMAÇÕES",fontSize=13.sp,letterSpacing=1.7.sp,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(top=8.dp))
            LText("Toque para abrir a ficha de cada forma. Só aparecem formas e Mega Evoluções registradas nesta base.",fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            formOptions.forEach{option->
                val kind=when{
                    option.isMega->"Mega Evolução"
                    option.isGigantamax()->"Gigantamax"
                    else->"Forma"
                }
                Surface(onClick={identities[option.id]?.let(onOpen)},enabled=enabled,shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth()){
                    Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
                        PokemonArt(option.id,Modifier.size(48.dp),option.name)
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)){
                            Text(option.name,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
                            LText(kind,fontSize=12.sp,color=MaterialTheme.colorScheme.primary)
                        }
                        LIcon(Icons.AutoMirrored.Rounded.ArrowForward,"Abrir ficha de ${option.name}",Modifier.size(20.dp),tint=MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable private fun EvolutionIdentity(node:EvolutionNode,currentId:Int,modifier:Modifier,enabled:Boolean,onOpen:()->Unit){
    Column(modifier.clip(RoundedCornerShape(16.dp)).clickable(enabled=enabled&&node.pokemonId!=currentId,onClick=onOpen).padding(6.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)){
        PokemonArt(node.pokemonId,Modifier.size(64.dp),node.name)
        Text(node.name,fontSize=14.sp,lineHeight=19.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center)
        if(node.pokemonId==currentId)LText("Atual",fontSize=12.sp,color=MaterialTheme.colorScheme.primary)
    }
}
