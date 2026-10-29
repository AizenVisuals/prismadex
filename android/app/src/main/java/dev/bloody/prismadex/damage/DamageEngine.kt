package dev.bloody.prismadex.damage

import android.content.Context
import dev.bloody.prismadex.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.Context as JsContext

/** Local Showdown Champions engine. No WebView, network, Java bridge, or UI-thread evaluation. */
class DamageEngine(context: Context) {
    private val assets=context.applicationContext.assets
    private val gate=Mutex()
    private val species by lazy { JSONObject(assets.open("calc-species-map.json").bufferedReader().use { it.readText() }) }
    private var scope:Scriptable?=null
    private var deadline=0L
    private val factory=object:ContextFactory(){
        override fun makeContext():JsContext=super.makeContext().apply {
            optimizationLevel=-1
            languageVersion=JsContext.VERSION_ES6
            instructionObserverThreshold=20_000
            setClassShutter { false }
        }
        override fun observeInstructionCount(cx:JsContext,instructionCount:Int){
            check(System.nanoTime()<deadline){"O cálculo demorou mais que o limite. Tente um cenário mais simples."}
        }
    }
    suspend fun canonicalName(p:Pokemon):String?=withContext(Dispatchers.IO){species.optString(p.id.toString()).takeIf{it.isNotBlank()}}
    suspend fun calculate(request:JSONObject):JSONObject=withContext(Dispatchers.Default){gate.withLock {
        val cx=factory.enterContext()
        try {
            deadline=System.nanoTime()+30_000_000_000L
            val local=scope?:cx.initSafeStandardObjects().also { target ->
                for(file in listOf("damage-engine.js","damage-bridge.js"))cx.evaluateString(target,assets.open(file).bufferedReader().use{it.readText()},file,1,null)
                scope=target
            }
            deadline=System.nanoTime()+5_000_000_000L
            val function=local.get("prismaDamage",local) as Function
            JSONObject(JsContext.toString(function.call(cx,local,local,arrayOf(request.toString()))))
        } finally {JsContext.exit()}
    }}
    suspend fun combatant(p:Pokemon,build:Build,itemName:String?,boosts:List<Int> = List(6){0},hpPercent:Int=100,status:String="",abilityOn:Boolean=false):JSONObject {
        val name=canonicalName(p)?:error("Esta forma ainda não tem correspondência verificada no motor Champions.")
        require(boosts.size==6 && boosts.all{it in -6..6})
        require(hpPercent in 1..100)
        return JSONObject().apply {
            put("name",name);put("nature",build.nature);put("sps",JSONArray(build.sps));put("item",itemName?:JSONObject.NULL)
            put("ability",build.ability?:JSONObject.NULL);put("hpPercent",hpPercent);put("status",status);put("abilityOn",abilityOn)
            put("boosts",JSONObject().apply {StatKeys.forEachIndexed{i,k->put(k,boosts[i])}})
        }
    }
}
