package dev.bloody.prismadex.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Level is supplied only for level-up methods; unknown is not a machine/tutor guess. */
data class LearnMethod(val method:String,val level:Int?=null,val source:String="bundled-historical-learnset")

class LearnsetRepository(context:Context){
    private val assets=context.applicationContext.assets
    private val champions by lazy { JSONObject(assets.open("champions-learnsets.json").bufferedReader().use{it.readText()}) }
    private val cached=mutableMapOf<Pair<Int,Int>,Map<Int,List<LearnMethod>>>()
    suspend fun methodsFor(pokemonId:Int,gameId:Int):Map<Int,List<LearnMethod>> = withContext(Dispatchers.IO){
        val key=pokemonId to gameId
        synchronized(cached){cached[key]}?.let{return@withContext it}
        val result=if(gameId==25){
            val rows=champions.optJSONArray(pokemonId.toString())?:JSONArray()
            (0 until rows.length()).associate{rows.getInt(it) to listOf(LearnMethod("unknown",source="champions-versioned-learnset"))}
        }else{
            val rows=runCatching{JSONArray(assets.open("learnsets/$pokemonId.json").bufferedReader().use{it.readText()})}.getOrElse{JSONArray()}
            parseHistorical(rows,gameId)
        }
        synchronized(cached){if(cached.size>=64)cached.clear();cached[key]=result}
        result
    }
    companion object{
        /** PokeAPI pokemon_move_methods.csv, pinned 2bf639e5572812a67d2b94903fdcb9d095f5aeb2. */
        private val numeric=mapOf("1" to "level-up","2" to "egg","3" to "tutor","4" to "machine","5" to "stadium-surfing-pikachu","6" to "light-ball-egg","7" to "colosseum-purification","8" to "xd-shadow","9" to "xd-purification","10" to "form-change","11" to "zygarde-cube","12" to "train")
        internal fun parseHistorical(rows:JSONArray,gameId:Int):Map<Int,List<LearnMethod>>{
            val result=linkedMapOf<Int,List<LearnMethod>>()
            repeat(rows.length()){i->val row=rows.getJSONObject(i);val versions=row.optJSONArray("versions")?:JSONArray()
                val methods=(0 until versions.length()).map{versions.getJSONObject(it)}.filter{it.optInt("game")==gameId}.map{
                    val raw=it.optString("method","unknown");val method=numeric[raw]?:raw
                    LearnMethod(method,if(method=="level-up")it.optInt("level").takeIf{level->level>0}else null)
                }.distinct()
                if(methods.isNotEmpty())result[row.getInt("id")]=methods
            }
            return result
        }
    }
}
