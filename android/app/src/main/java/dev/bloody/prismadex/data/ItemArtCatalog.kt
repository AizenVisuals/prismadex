package dev.bloody.prismadex.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Namespace-bound artwork identity: reference-item ID plus exact source name and slug. */
data class ReferenceItemArt(val id:Int,val name:String,val slug:String,val path:String,val sourceUrl:String)
class ItemArtCatalog(context:Context){
    private val assets=context.applicationContext.assets
    suspend fun forItem(item:ReferenceItem):ReferenceItemArt?=withContext(Dispatchers.IO){
        load()[item.id]?.takeIf{it.name==item.name&&it.slug==item.slug}
    }
    private suspend fun load():Map<Int,ReferenceItemArt>{
        cache?.let{return it}
        return mutex.withLock{cache?:runCatching{
            val root=JSONObject(assets.open("general-item-art-map.json").bufferedReader().use{it.readText()})
            require(root.getInt("schemaVersion")==1)
            val rows=root.getJSONArray("items")
            (0 until rows.length()).map{index->val row=rows.getJSONObject(index)
                val id=row.getInt("id");val path=row.getString("path")
                require(path=="general-item-art/$id.webp")
                ReferenceItemArt(id,row.getString("name"),row.getString("slug"),path,row.getString("descriptionUrl"))
            }.also{require(it.map{r->r.id}.distinct().size==it.size)}.associateBy{it.id}
        }.getOrElse{emptyMap()}.also{cache=it}}
    }
    companion object{
        private val mutex=Mutex()
        @Volatile private var cache:Map<Int,ReferenceItemArt>?=null
    }
}
