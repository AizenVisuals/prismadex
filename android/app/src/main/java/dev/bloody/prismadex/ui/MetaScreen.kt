package dev.bloody.prismadex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.R
import dev.bloody.prismadex.data.Pokemon
import dev.bloody.prismadex.data.PokemonIdentityIndex
import dev.bloody.prismadex.meta.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import java.util.Locale

@Composable internal fun MetaScreen(catalog:List<Pokemon>,format:String,onFormat:(String)->Unit,onOpen:(Pokemon)->Unit){
    val context=LocalContext.current;val uri=LocalUriHandler.current;val repository=remember{MetaRepository(context)}
    var content by remember(format){mutableStateOf<Pair<MetaSnapshot,Map<String,Pokemon?>>?>(null)}
    var error by remember(format){mutableStateOf<String?>(null)}
    var refresh by remember(format){mutableIntStateOf(0)}
    var loading by remember(format){mutableStateOf(false)}
    LaunchedEffect(format,refresh,catalog){
        loading=true;error=null
        try {
            val identities=withContext(Dispatchers.Default){PokemonIdentityIndex(catalog)}
            repository.observe(format,refresh>0).collect { snapshot ->
                val resolved=withContext(Dispatchers.Default){
                    snapshot.entries.associate { it.name to identities.resolve(it.name) }
                }
                content=snapshot to resolved
            }
        } catch(e:CancellationException){throw e}
          catch(e:Exception){error=e.message?:"Sem conexão com a fonte."}
        finally { loading=false }
    }
    val data=content?.first
    val resolved=content?.second.orEmpty()
    LazyColumn(Modifier.prismaViewport(),contentPadding=PaddingValues(start=22.dp,end=22.dp,top=16.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item(key="meta-heading"){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
            LText("Entenda o meta.\nPrepare a resposta.",fontSize=29.sp,lineHeight=34.sp,fontWeight=FontWeight.Bold)
            LText("Fontes online, com período e formato identificados. A última consulta fica disponível offline.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){FilterChip(format=="singles",{onFormat("singles")},label={LText("Singles · individual")});FilterChip(format=="doubles",{onFormat("doubles")},label={LText("Doubles · duplas")});IconButton(onClick={refresh++},enabled=!loading){LIcon(Icons.Rounded.Refresh,"Atualizar Meta")}}
            if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let{LText(it,fontSize=13.sp,color=MaterialTheme.colorScheme.error)}
        }}
        data?.let{s->
            item(key="meta-source"){Surface(color=MaterialTheme.colorScheme.primary.copy(alpha=.1f),shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
                LText("SMOGON · POKÉMON SHOWDOWN",fontSize=13.sp,fontWeight=FontWeight.SemiBold)
                LText("${s.month} · ${s.formatCode.substringAfter("reg").uppercase(Locale.ROOT).let{"Reg. M-${it.last()}"}} · faixa 1630+",fontSize=13.sp)
                LText("${s.battles} batalhas no relatório · ${LocalUiStrings.current.text(if(s.cached)"consulta salva" else "atualizado pela internet")}",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                LText("Uso ponderado no simulador. Estes números não representam o ranking oficial dentro do jogo.",fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                s.notice?.let{LText(it,fontSize=13.sp,color=MaterialTheme.colorScheme.primary)}
                TextButton(onClick={uri.openUri(s.sourceUrl)}){LText("Abrir relatório original")}
            }}}
            items(s.entries,key={it.name}){entry->val p=resolved[entry.name];Surface(onClick={p?.let(onOpen)},enabled=p!=null,shape=RoundedCornerShape(21.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                LText(entry.rank.toString().padStart(2,'0'),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.width(25.dp))
                if(p!=null)PokemonArt(p.id,Modifier.size(56.dp),entry.name) else LIcon(painterResource(R.drawable.ic_prisma_foreground),null,Modifier.size(56.dp),tint=androidx.compose.ui.graphics.Color.Unspecified)
                Column(Modifier.weight(1f)){LText(p?.name?:entry.name,fontSize=14.sp,fontWeight=FontWeight.SemiBold);if(p!=null&&p.name!=entry.name)LText(entry.name,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);p?.let{Row(horizontalArrangement=Arrangement.spacedBy(4.dp),modifier=Modifier.padding(top=6.dp)){it.types.forEach{t->TypeSymbol(t)}}};LText(String.format(Locale.ROOT,"%.2f%%",entry.usage),fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(top=6.dp))}
            }}}
        }
        item(key="meta-more"){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
            LText("OUTRAS FONTES",fontSize=13.sp,letterSpacing=1.4.sp,color=MaterialTheme.colorScheme.primary)
            LText("PokéChamp DB organiza snapshots do jogo. Consulte a temporada e o formato na própria fonte; os números não são misturados ao relatório do simulador.",fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick={uri.openUri("https://pokechamdb.com/en")},modifier=Modifier.fillMaxWidth()){LText("Consultar ranking do jogo")}
            TextButton(onClick={uri.openUri("https://champions.pokemon.com/en-us/gameplay/")}){LText("Formatos e regras oficiais")}
        }}
    }
}
