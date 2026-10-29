package dev.bloody.prismadex.data

import android.content.Context
import org.json.JSONObject
import java.security.MessageDigest

/** Drafts are private, local and isolated by account, team and Pokémon. Saved sets stay authoritative. */
class BuildDraftStore(context:Context,namespace:String="build-drafts") {
    private val prefs=context.getSharedPreferences(namespace,Context.MODE_PRIVATE)
    fun key(account:String?,teamId:String,pokemonId:Int):String = MessageDigest.getInstance("SHA-256")
        .digest("${account?:"guest"}\u0000$teamId\u0000$pokemonId".toByteArray()).joinToString(""){"%02x".format(it)}
    fun load(key:String,base:Build):Build? = runCatching {
        val row=JSONObject(prefs.getString(key,null)?:return null)
        if(Store.decodeBuild(row.getJSONObject("base"))!=base){clear(key);return null}
        Store.decodeBuild(row.getJSONObject("draft"))
    }.getOrNull()
    fun save(key:String,base:Build,draft:Build){
        if(base==draft){clear(key);return}
        prefs.edit().putString(key,JSONObject().put("base",Store.encodeBuild(base)).put("draft",Store.encodeBuild(draft)).toString()).apply()
    }
    fun clear(key:String){prefs.edit().remove(key).apply()}
}

data class BattleSetChoice(val key:String,val pokemonId:Int,val build:Build,val label:String,val teamId:String?=null)
fun battleTeamChoices(state:UserState,catalog:List<Pokemon>):List<BattleSetChoice> {
    val byId=catalog.associateBy{it.id}
    return state.teams.filter{it.gameId==25}.flatMap{team->team.members.mapNotNull{id->byId[id]?.let{p->
        BattleSetChoice("team/${team.id}/$id",id,team.builds[id]?:Build(),"${p.name} · ${team.name}",team.id)
    }}}
}
fun battleDefenderChoices(state:UserState,catalog:List<Pokemon>)=battleTeamChoices(state,catalog)+state.dummies.map{
    BattleSetChoice("dummy/${it.id}",it.pokemonId,it.build,"${it.label} · alvo independente")
}
