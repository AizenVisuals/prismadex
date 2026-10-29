package dev.bloody.prismadex.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Scalar move history only; historical effects, flags and targets are not certified.
 * Source: pinned PokeAPI BSD-3-Clause, with literal Showdown gen overrides (MIT).
 * Load once on IO, then apply the immutable parsed overrides to filtered learnsets.
 */
class HistoricalMoveData(context:Context) {
    private val assets=context.applicationContext.assets
    @Volatile private var games:JSONObject?=null
    suspend fun load():HistoricalMoveData=withContext(Dispatchers.IO){
        if(games==null)synchronized(this@HistoricalMoveData){
            if(games==null)games=JSONObject(assets.open("historical-move-overrides.json").bufferedReader().use{it.readText()}).getJSONObject("games")
        }
        this@HistoricalMoveData
    }
    fun applyHistoricalMove(move:Move,game:Int):Move {
        if(game==25)return move
        val root=checkNotNull(games){"Load historical move data on IO before applying it"}
        val row=root.optJSONObject(game.toString())?.getJSONObject("moves")?.optJSONObject(move.id.toString())?:return move
        return applyRow(move,row)
    }
    companion object {
        internal fun applyRow(move:Move,row:JSONObject):Move {
            fun scalar(key:String,original:Int?)=if(!row.has(key))original else if(row.isNull(key))null else row.getInt(key)
            return move.copy(power=scalar("power",move.power),accuracy=scalar("accuracy",move.accuracy),pp=scalar("pp",move.pp),priority=scalar("priority",move.priority)?:move.priority,type=row.optString("type",move.type),category=row.optString("category",move.category))
        }
    }
}
