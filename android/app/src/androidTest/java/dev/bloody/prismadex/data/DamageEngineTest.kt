package dev.bloody.prismadex.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.bloody.prismadex.damage.DamageEngine
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DamageEngineTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext

    /** Golden ranges generated directly with the pinned upstream engine in Node, not this bridge. */
    @Test fun championsRangesMatchUpstreamAcrossWeatherScreensAndImmunity()=runBlocking {
        val repo=PokemonRepository(context);val catalog=repo.loadCatalog().associateBy{it.id};val engine=DamageEngine(context)
        val a=engine.combatant(catalog.getValue(6),Build(247,listOf(53),"Modest",null,listOf(0,0,0,32,0,0)),"Life Orb")
        val d=engine.combatant(catalog.getValue(3),Build(nature="Calm",sps=listOf(32,0,0,0,32,0)),null)
        for((field,expected) in listOf(JSONObject() to (140 to 166),JSONObject().put("weather","Rain") to (65 to 81),JSONObject().put("gameType","Doubles").put("defenderSide",JSONObject().put("isLightScreen",true)) to (94 to 111))){
            val r=engine.calculate(JSONObject().put("attacker",a).put("defender",d).put("move","Flamethrower").put("field",field))
            assertEquals("Charizard",r.getString("attackerName"));assertEquals("Venusaur",r.getString("defenderName"))
            assertEquals(if(field.optString("gameType")=="Doubles")"Doubles" else "Singles",r.getString("gameType"))
            assertEquals(expected.first,r.getInt("min"));assertEquals(expected.second,r.getInt("max"))
            assertEquals(177,r.getJSONObject("attackerStats").getInt("spa"));assertEquals(187,r.getJSONObject("defenderStats").getInt("hp"))
            assertEquals(16,r.getJSONArray("rolls").length())
        }
        val immune=engine.calculate(JSONObject().put("attacker",engine.combatant(catalog.getValue(445),Build(),null)).put("defender",engine.combatant(catalog.getValue(6),Build(),null)).put("move","Earthquake"))
        assertEquals(0,immune.getInt("max"));assertEquals("Charizard",immune.getString("defenderName"))
        assertEquals("Charizard-Mega-X",engine.canonicalName(catalog.getValue(10034)))
        assertNull(engine.canonicalName(catalog.getValue(670))) // Never silently replace an unsupported form.
    }

    @Test fun trainingAndDummiesPersistWithoutUsingTeamSlots()=runBlocking {
        val storage="damage-test-${UUID.randomUUID()}";val store=Store(context,storage)
        try {
            val build=Build(247,listOf(53),"Modest","blaze",listOf(0,0,0,32,2,32))
            val dummy=TrainingDummy("target-1","Venusaur defensivo",3,Build(nature="Calm",sps=listOf(32,0,2,0,32,0)))
            val state=UserState(teams=listOf(SavedTeam("default","Minha equipe",members=listOf(6),builds=mapOf(6 to build))),dummies=listOf(dummy),theme="eevee",appearanceMode="light")
            store.save(state);assertEquals(state,store.load());assertEquals(1,store.load().team.size)
            val repository=PokemonRepository(context);val charizard=repository.loadCatalog().first{it.id==6}
            assertTrue(repository.validateBuild(charizard,build.copy(sps=listOf(32,32,32,0,0,0))).isFailure)
            assertTrue(repository.validateBuild(charizard,build.copy(ability="wonder-guard")).isFailure)
            val old=JSONObject("{\"team\":[6],\"builds\":{\"6\":{\"itemId\":247,\"moves\":[53]}}}")
            assertEquals(Build(247,listOf(53)),Store.decode(old).builds.getValue(6))
        }finally{store.close();context.deleteDatabase("$storage-accounts.sqlite3");context.getSharedPreferences("$storage-session",0).edit().clear().commit()}
    }
}
