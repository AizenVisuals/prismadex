package dev.bloody.prismadex.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import dev.bloody.prismadex.damage.DamageEngine
import org.json.JSONObject

@RunWith(AndroidJUnit4::class)
class BuildWorkflowTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun sameSpeciesInDifferentTeamsKeepsItsOwnSetAndStableIdentity()=runBlocking {
        val catalog=PokemonRepository(context).loadCatalog()
        val a=Build(moves=listOf(182),nature="Adamant",sps=listOf(0,32,0,0,0,32))
        val b=Build(moves=listOf(182),nature="Bold",sps=listOf(32,0,32,0,0,0))
        val state=UserState(teams=listOf(SavedTeam("a","Ataque",members=listOf(812),builds=mapOf(812 to a)),SavedTeam("b","Defesa",members=listOf(812),builds=mapOf(812 to b)),SavedTeam("old","Histórica",23,listOf(812))),activeTeamId="a",dummies=listOf(TrainingDummy("target","Alvo",812,b)))
        val attackers=battleTeamChoices(state,catalog);val defenders=battleDefenderChoices(state,catalog)
        assertEquals(2,attackers.size);assertEquals(3,defenders.size)
        assertNotEquals(attackers[0].key,attackers[1].key)
        assertEquals(a,attackers.first{it.teamId=="a"}.build)
        assertEquals(b,defenders.first{it.teamId=="b"}.build)
        assertEquals("dummy/target",defenders.last().key)
        assertEquals(attackers.map{it.key},battleTeamChoices(state.copy(activeTeamId="b"),catalog).map{it.key})
        val engine=DamageEngine(context);val p=catalog.first{it.id==812}
        for(format in listOf("Singles","Doubles")){
            suspend fun result(target:BattleSetChoice)=engine.calculate(JSONObject().put("attacker",engine.combatant(p,attackers[0].build,null)).put("defender",engine.combatant(p,target.build,null)).put("move","Wood Hammer").put("field",JSONObject().put("gameType",format)))
            val offensive=result(defenders.first{it.teamId=="a"});val defensive=result(defenders.first{it.teamId=="b"})
            assertEquals(format,defensive.getString("gameType"))
            assertTrue(defensive.getJSONObject("defenderStats").getInt("hp")>offensive.getJSONObject("defenderStats").getInt("hp"))
            assertTrue(defensive.getInt("max")<offensive.getInt("max"))
        }
    }
    @Test fun draftsSurviveAStoreRecreationAndStayIsolatedFromSavedSets(){
        val namespace="draft-test-${UUID.randomUUID()}";val drafts=BuildDraftStore(context,namespace)
        val base=Build();val draft=base.copy(nature="Modest",moves=listOf(182),sps=listOf(0,0,0,32,0,32))
        try{
            val key=drafts.key(null,"one",812);drafts.save(key,base,draft)
            assertEquals(draft,BuildDraftStore(context,namespace).load(key,base))
            assertNull(drafts.load(drafts.key(null,"two",812),base))
            assertNull(drafts.load(drafts.key("local-account","one",812),base))
            assertEquals("Serious",base.nature)
            drafts.clear(key);assertNull(drafts.load(key,base))
        }finally{context.getSharedPreferences(namespace,0).edit().clear().commit()}
    }
    @Test fun anExternallyUpdatedSavedSetCannotBeOverwrittenByAnOlderDraft(){
        val namespace="draft-stale-${UUID.randomUUID()}";val drafts=BuildDraftStore(context,namespace)
        try{
            val key=drafts.key(null,"team",812);val base=Build();drafts.save(key,base,base.copy(nature="Modest"))
            assertNull(drafts.load(key,base.copy(nature="Adamant")))
            assertNull(drafts.load(key,base))
            drafts.save(key,base,base);assertNull(drafts.load(key,base))
        }finally{context.getSharedPreferences(namespace,0).edit().clear().commit()}
    }
}
