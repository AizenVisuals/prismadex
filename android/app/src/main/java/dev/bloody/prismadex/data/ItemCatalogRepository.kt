package dev.bloody.prismadex.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ReferenceItem(val id:Int,val name:String,val category:String,val description:ProfileText?,val flingPower:Int?,val flingEffectId:Int?,val iconId:Int?,val slug:String){
    val descriptionEnglish:String? get()=description?.en
}

/** Read-only general reference. These IDs do not grant held-item availability in any game. */
class ItemCatalogRepository(context:Context){
    private val assets=context.applicationContext.assets
    suspend fun all():List<ReferenceItem> = withContext(Dispatchers.IO){
        saved?.let{return@withContext it}
        mutex.withLock{saved?:decode(JSONObject(assets.open("general-items.json").bufferedReader().use{it.readText()})).also{saved=it}}
    }
    companion object{
        private val mutex=Mutex()
        @Volatile private var saved:List<ReferenceItem>?=null
        internal fun decode(root:JSONObject):List<ReferenceItem>{
            require(root.getInt("schemaVersion")==1)
            val rows=root.getJSONArray("items")
            val result=(0 until rows.length()).map{index->val r=rows.getJSONObject(index)
                ReferenceItem(r.getInt("id"),r.getString("name"),r.getString("category"),r.optJSONObject("description")?.let{text->ProfileText(text.getString("pt"),text.getString("en"),text.getString("es"))},r.nullableInt("flingPower"),r.nullableInt("flingEffectId"),r.nullableInt("iconId"),r.getString("slug"))
            }
            require(result.map{it.id}.distinct().size==result.size)
            return result.sortedWith(compareBy({it.name},{it.id}))
        }
    }
}
