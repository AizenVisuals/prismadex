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
import dev.bloody.prismadex.data.resolvePokemonIdentity
import dev.bloody.prismadex.meta.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

@Composable internal fun MetaScreen(catalog:List<Pokemon>,format:String,onFormat:(String)->Unit,onOpen:(Pokemon)->Unit){
    val context=LocalContext.current;val uri=LocalUriHandler.current;val repository=remember{MetaRepository(context)}
    var data by remember{mutableStateOf<MetaSnapshot?>(null)};var error by remember{mutableStateOf<String?>(null)};var refresh by remember{mutableIntStateOf(0)};var loading by remember{mutableStateOf(false)}
    LaunchedEffect(format,refresh){loading=true;data=null;error=null;runCatching{repository.load(format,refresh>0)}.onSuccess{data=it}.onFailure{error=it.message?:"Sem conexão com a fonte."};loading=false}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(start=22.dp,end=22.dp,top=88.dp,bottom=120.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item(key="meta-heading"){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("OBSERVATÓRIO CHAMPIONS",fontSize=13.sp,letterSpacing=1.5.sp,color=MaterialTheme.colorScheme.primary)
            Text("Entenda o meta.\nPrepare a resposta.",fontSize=29.sp,lineHeight=34.sp,fontWeight=FontWeight.Bold)
            Text("Fontes online, com período e formato identificados. A última consulta fica disponível offline.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){FilterChip(format=="singles",{onFormat("singles")},label={Text("Singles")});FilterChip(format=="doubles",{onFormat("doubles")},label={Text("Doubles")});IconButton(onClick={refresh++},enabled=!loading){Icon(Icons.Rounded.Refresh,"Atualizar Meta")}}
            if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let{Text(it,fontSize=13.sp,color=MaterialTheme.colorScheme.error)}
        }}
        data?.let{s->
            item(key="meta-source"){Surface(color=MaterialTheme.colorScheme.primary.copy(alpha=.1f),shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
                Text("SMOGON · POKÉMON SHOWDOWN",fontSize=13.sp,fontWeight=FontWeight.SemiBold)
                Text("${s.month} · ${s.formatCode.substringAfter("reg").uppercase(Locale.ROOT).let{"Reg. M-${it.last()}"}} · faixa 1630+",fontSize=13.sp)
                Text("${s.battles} batalhas no relatório · ${if(s.cached)"consulta salva" else "atualizado pela internet"}",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Uso ponderado no simulador. Estes números não representam o ranking oficial dentro do jogo.",fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                s.notice?.let{Text(it,fontSize=13.sp,color=MaterialTheme.colorScheme.primary)}
                TextButton(onClick={uri.openUri(s.sourceUrl)}){Text("Abrir relatório original")}
            }}}
            items(s.entries,key={it.name}){entry->val p=resolvePokemonIdentity(catalog,entry.name);Surface(onClick={p?.let(onOpen)},enabled=p!=null,shape=RoundedCornerShape(21.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                Text(entry.rank.toString().padStart(2,'0'),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.width(25.dp))
                if(p!=null)PokemonArt(p.id,Modifier.size(56.dp),entry.name) else Icon(painterResource(R.drawable.ic_prisma_foreground),null,Modifier.size(56.dp),tint=androidx.compose.ui.graphics.Color.Unspecified)
                Column(Modifier.weight(1f)){Text(p?.name?:entry.name,fontSize=14.sp,fontWeight=FontWeight.SemiBold);if(p!=null&&p.name!=entry.name)Text(entry.name,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);p?.let{Row(horizontalArrangement=Arrangement.spacedBy(4.dp),modifier=Modifier.padding(top=6.dp)){it.types.forEach{t->TypeSymbol(t)}}}}
                Text(String.format(Locale.ROOT,"%.2f%%",entry.usage),fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary)
            }}}
        }
        item(key="meta-more"){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("OUTRAS FONTES",fontSize=13.sp,letterSpacing=1.4.sp,color=MaterialTheme.colorScheme.primary)
            Text("PokéChamp DB organiza snapshots do jogo. Consulte a temporada e o formato na própria fonte; os números não são misturados ao relatório do simulador.",fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick={uri.openUri("https://pokechamdb.com/en")},modifier=Modifier.fillMaxWidth()){Text("Consultar ranking do jogo")}
            TextButton(onClick={uri.openUri("https://champions.pokemon.com/en-us/gameplay/")}){Text("Formatos e regras oficiais")}
        }}
    }
}
private fun metaId(value:String)=value.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"),"")
