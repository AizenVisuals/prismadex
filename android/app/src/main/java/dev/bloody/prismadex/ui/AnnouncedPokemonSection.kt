package dev.bloody.prismadex.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Announcement facts have no catalog ID and cannot enter saved teams or the calculator. */
@Composable internal fun AnnouncedPokemonSection(query:String="") {
    val assets=LocalContext.current.applicationContext.assets
    val uri=LocalUriHandler.current
    val language=LocalUiStrings.current.language
    var expanded by rememberSaveable{mutableStateOf(false)}
    val logoImage by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null){
        value=withContext(Dispatchers.IO){runCatching{assets.open("winds-waves-logo.webp").use{BitmapFactory.decodeStream(it)?.asImageBitmap()}}.getOrNull()}
    }
    val source by produceState<Pair<JSONObject,JSONObject>?>(null){
        value=withContext(Dispatchers.IO){
            runCatching {
                JSONObject(assets.open("announced-pokemon.json").bufferedReader().use{it.readText()}) to
                    JSONObject(assets.open("announced-pokemon-ui.json").bufferedReader().use{it.readText()})
            }.getOrNull()
        }
    }
    val loaded=source?:return
    val langIndex=when(language){"pt"->0;"es"->2;else->1}
    fun copy(key:String)=loaded.second.getJSONArray(key).getString(langIndex)
    val records=loaded.first.getJSONArray("pokemon")
    val matches=(0 until records.length()).map{records.getJSONObject(it)}.filter{it.getString("name").contains(query.trim(),true)}
    if(matches.isEmpty())return
    // Default: hidden behind the Winds/Waves logo (30/10). Tap expands;
    // collapsed logo is 52dp tall, the SAME rendered size as the expanded
    // header logo below (asset aspect 3.30 -> ~172dp wide), centered by
    // this Column (CenterHorizontally) + ContentScale.Fit (11:22).
    // a text search always reveals the matching records.
    val showContent=expanded||query.isNotBlank()
    if(!showContent){
        Surface(onClick={expanded=true},shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth()){
            Column(Modifier.fillMaxWidth().padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)){
                logoImage?.let{Image(it,"Pokémon Winds e Pokémon Waves",Modifier.fillMaxWidth().height(52.dp),contentScale=ContentScale.Fit)}
                LText("Pokémon anunciados · toque para ver",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Surface(onClick={expanded=false},shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth()){
            // Same fixed logo as the collapsed state (52dp tall, centered,
            // 11:25): only the content below expands/collapses, the logo
            // itself never resizes. "Ocultar" overlays the right end so the
            // logo stays truly centered.
            Box(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),contentAlignment=Alignment.Center){
                logoImage?.let{Image(it,"Pokémon Winds e Pokémon Waves",Modifier.fillMaxWidth().height(52.dp),contentScale=ContentScale.Fit)}
                Row(Modifier.align(Alignment.CenterEnd),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(2.dp)){
                    LText("Ocultar",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    LIcon(Icons.Rounded.KeyboardArrowUp,null,modifier=Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text(copy("title"),fontSize=22.sp,fontWeight=FontWeight.SemiBold)
        Text(copy("scope"),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        matches.forEach { pokemon ->
            Surface(shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.surface){
                Row(Modifier.fillMaxWidth().padding(18.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)){
                        Text(pokemon.getString("name"),fontSize=20.sp,fontWeight=FontWeight.SemiBold)
                        TypePill(pokemon.getString("type"))
                        Text(when(pokemon.getString("name")){"Browt"->when(language){"pt"->"Pokémon Pintinho Feijão";"es"->"Pokémon Pollito Haba";else->pokemon.getString("category")}; "Pombon"->when(language){"pt"->"Pokémon Filhote";"es"->"Pokémon Cachorro";else->pokemon.getString("category")}; "Gecqua"->when(language){"pt"->"Pokémon Lagartixa Aquática";"es"->"Pokémon Geco Acuático";else->pokemon.getString("category")};else->pokemon.getString("category")},fontSize=14.sp)
                        Text("${pokemon.getDouble("height")} m · ${pokemon.getDouble("weight")} kg",fontSize=14.sp)
                        Text("${copy("ability")}: ${pokemon.getString("ability")}",fontSize=14.sp)
                        Text(copy("unknown"),fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AnnouncedArt(pokemon.getString("key"),pokemon.getString("name"),Modifier.size(120.dp))
                }
            }
        }
        Text(copy("isolated"),fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick={uri.openUri(loaded.first.getString("source"))}){Text(copy("source"))}
    }
}

/** Starter artwork only (30/10/2026): cropped to its own content box,
 *  uniform resize to fit 256px, drawn with ContentScale.Fit — no stretch,
 *  no clipping. Artwork is Nintendo/Game Freak/TPC IP reproduced from the
 *  Universo Nintendo fan site; see announced-art-sources.json and the
 *  Sources screen. No data/text changes were made for these arts. */
@Composable private fun AnnouncedArt(key:String,label:String,modifier:Modifier=Modifier){
    val context=LocalContext.current
    val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null,key){
        value=withContext(Dispatchers.IO){
            runCatching{context.assets.open("announced-art/$key.webp").use{BitmapFactory.decodeStream(it)?.asImageBitmap()}}.getOrNull()
        }
    }
    val ready=image?:return
    Image(ready,label,modifier,contentScale=ContentScale.Fit,alignment=Alignment.Center)
}
