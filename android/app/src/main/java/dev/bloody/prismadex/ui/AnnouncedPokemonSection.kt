package dev.bloody.prismadex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text(copy("title"),fontSize=22.sp,fontWeight=FontWeight.SemiBold)
        Text(copy("scope"),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        matches.forEach { pokemon ->
            Surface(shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.surface){
                Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    Text(pokemon.getString("name"),fontSize=20.sp,fontWeight=FontWeight.SemiBold)
                    TypePill(pokemon.getString("type"))
                    Text(when(pokemon.getString("name")){"Browt"->when(language){"pt"->"Pokémon Pintinho Feijão";"es"->"Pokémon Pollito Haba";else->pokemon.getString("category")};"Pombon"->when(language){"pt"->"Pokémon Filhote";"es"->"Pokémon Cachorro";else->pokemon.getString("category")};"Gecqua"->when(language){"pt"->"Pokémon Lagartixa Aquática";"es"->"Pokémon Geco Acuático";else->pokemon.getString("category")};else->pokemon.getString("category")},fontSize=14.sp)
                    Text("${pokemon.getDouble("height")} m · ${pokemon.getDouble("weight")} kg",fontSize=14.sp)
                    Text("${copy("ability")}: ${pokemon.getString("ability")}",fontSize=14.sp)
                    Text(copy("unknown"),fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text(copy("isolated"),fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick={uri.openUri(loaded.first.getString("source"))}){Text(copy("source"))}
    }
}
