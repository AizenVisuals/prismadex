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
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)){
        LText("LINHA EVOLUTIVA",fontSize=13.sp,letterSpacing=1.7.sp,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(top=8.dp))
        val current=family
        when {
            !loaded -> LText("Consultando evoluções no aparelho…",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            current==null -> LText("Linha evolutiva ainda não disponível nesta base.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
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
    }
}

@Composable private fun EvolutionIdentity(node:EvolutionNode,currentId:Int,modifier:Modifier,enabled:Boolean,onOpen:()->Unit){
    Column(modifier.clip(RoundedCornerShape(16.dp)).clickable(enabled=enabled&&node.pokemonId!=currentId,onClick=onOpen).padding(6.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)){
        PokemonArt(node.pokemonId,Modifier.size(64.dp),node.name)
        Text(node.name,fontSize=14.sp,lineHeight=19.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center)
        if(node.pokemonId==currentId)LText("Atual",fontSize=12.sp,color=MaterialTheme.colorScheme.primary)
    }
}
