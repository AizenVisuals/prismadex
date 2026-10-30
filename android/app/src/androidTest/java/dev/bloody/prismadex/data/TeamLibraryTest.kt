package dev.bloody.prismadex.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TeamLibraryTest{
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun legacyTeamMigratesWithoutLosingBuildsOrPreferences(){
        val old=JSONObject("""{"favorites":[6,150],"team":[6],"builds":{"6":{"itemId":247,"moves":[53],"ability":"blaze","nature":"Modest","sps":[0,0,0,32,0,0]}},"theme":"carmine","layout":"list","battleFormat":"singles","appearanceMode":"dark"}""")
        val state=Store.decode(old)
        assertEquals(1,state.teams.size);assertEquals(25,state.activeTeam.gameId)
        assertEquals(listOf(6),state.team);assertEquals(listOf(53),state.builds.getValue(6).moves)
        assertEquals("Modest",state.builds.getValue(6).nature);assertEquals("singles",state.activeTeam.format)
        assertEquals(setOf(6,150),state.favorites);assertEquals("carmine",state.theme)
        assertEquals(state,Store.decode(Store.encode(state)))
    }
    @Test fun teamViewPreferenceDefaultsToListAndSurvivesReopening()=runBlocking{
        val legacy=JSONObject("{\"theme\":\"aurora\",\"layout\":\"grid\"}")
        assertEquals("list",Store.decode(legacy).teamView)
        assertEquals("list",UserState().teamView)
        val name="team-view-${UUID.randomUUID()}";var store=Store(context,name)
        try{
            val state=UserState(teams=listOf(SavedTeam("g","Grade",23,listOf(25))),activeTeamId="g",teamView="grid")
            store.save(state);store.close();store=Store(context,name)
            assertEquals("grid",store.load().teamView)
            assertEquals(state,Store.decode(Store.encode(state)))
        }finally{store.close();context.deleteDatabase("$name-accounts.sqlite3");context.getSharedPreferences("$name-session",0).edit().clear().commit()}
    }
    @Test fun multipleTeamsHaveIndependentSetsAndSurviveReopening()=runBlocking{
        val name="team-library-${UUID.randomUUID()}";var store=Store(context,name)
        try{
            val c1=SavedTeam("c1","Sol",25,listOf(6),mapOf(6 to Build(247,listOf(53),"Modest")),"singles")
            val c2=SavedTeam("c2","Chuva",25,listOf(6),mapOf(6 to Build(211,listOf(7),"Adamant")),"doubles")
            val red=SavedTeam("red","Kanto",1,listOf(150),mapOf(150 to Build()))
            val state=UserState(teams=listOf(c1,c2,red),activeTeamId="c1")
            store.save(state);store.close();store=Store(context,name)
            assertEquals(state,store.load())
            val second=store.load().copy(activeTeamId="c2").updateTeam{it.copy(name="Chuva revisada")}
            store.save(second)
            assertEquals("Adamant",store.load().builds.getValue(6).nature)
            assertEquals("Modest",store.load().teams.first{it.id=="c1"}.builds.getValue(6).nature)
            val bad=second.copy(teams=second.teams+SavedTeam("bad","Incompatível",1,listOf(1000)))
            assertTrue(runCatching{store.save(bad)}.isFailure)
            assertEquals(second,store.load())
        }finally{store.close();context.deleteDatabase("$name-accounts.sqlite3");context.getSharedPreferences("$name-session",0).edit().clear().commit()}
    }
    @Test fun filtersUseExplicitVersionsAndGenerationRatherThanLearnsets()=runBlocking{
        val repo=PokemonRepository(context);val rows=repo.loadCatalog().associateBy{it.id}
        assertEquals(25,SupportedGames.size);assertEquals((1..25).toSet(),SupportedGames.map{it.id}.toSet())
        assertEquals(1,rows.getValue(150).generation);assertEquals(9,rows.getValue(1000).generation)
        assertTrue(rows.getValue(150).availableIn(1));assertFalse(rows.getValue(1000).availableIn(1))
        assertTrue(rows.getValue(10033).availableIn(25))
        assertTrue(repo.movesFor(rows.getValue(10033),25).isNotEmpty())
        assertEquals(1306+AdditionalFormsRepository(context).catalogEntries().size,rows.size);assertEquals(354,rows.values.count{it.champions})
        val featured=rows.values.filter{it.id==it.baseId}
        assertEquals(1025,featured.size);assertTrue(featured.any{it.id==150});assertTrue(featured.any{it.id==249});assertTrue(featured.any{it.id==384});assertTrue(featured.any{it.id==493})
    }
}
