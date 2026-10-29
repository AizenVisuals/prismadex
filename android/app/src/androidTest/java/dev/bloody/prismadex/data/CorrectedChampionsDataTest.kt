package dev.bloody.prismadex.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.bloody.prismadex.damage.DamageEngine
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CorrectedChampionsDataTest{
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun everyOfficialEntryAndSupplementalFormHasItsOwnVerifiedData()=runBlocking{
        val repo=PokemonRepository(context);val catalog=repo.loadCatalog()
        assertEquals(1306,catalog.size);assertEquals(catalog.size,catalog.map{it.name.lowercase()}.distinct().size)
        assertTrue(catalog.none{it.id in RetiredPokemonIds});assertTrue(catalog.all{it.stats.size==6&&it.stats.sum()>0})
        assertEquals(262,catalog.count{it.championsSource=="official-regulation"})
        assertEquals(354,catalog.count{it.champions})
        for(p in catalog.filter{it.champions})assertTrue("Learnset ausente: ${p.id} ${p.name}",repo.movesFor(p).isNotEmpty())
        val rilla=catalog.first{it.id==812}
        assertTrue(rilla.champions);assertEquals("official-regulation",rilla.championsSource)
        assertEquals(67,repo.movesFor(rilla).size);assertTrue(repo.movesFor(rilla).any{it.name=="Grassy Glide"})
        assertTrue(repo.validateBuild(rilla,Build(moves=repo.movesFor(rilla).take(4).map{it.id},ability="grassy-surge")).isSuccess)
    }
    @Test fun indeedeeSexResolvesIdenticallyAcrossMetaSearchMovesAndCalculator()=runBlocking{
        val repo=PokemonRepository(context);val catalog=repo.loadCatalog();val engine=DamageEngine(context)
        val female=resolvePokemonIdentity(catalog,"Indeedee-F")!!;val male=resolvePokemonIdentity(catalog,"Indeedee")!!
        assertEquals(20010803,female.id);assertEquals(876,male.id);assertNotEquals(female.id,male.id)
        assertTrue(female.matchesQuery("Indeedee-F"));assertFalse(male.matchesQuery("Indeedee-F"))
        assertEquals(listOf(70,55,65,95,105,85),female.stats);assertEquals(listOf(60,65,55,105,95,95),male.stats)
        assertEquals("Indeedee-F",engine.canonicalName(female));assertEquals("Indeedee",engine.canonicalName(male))
        val fm=repo.movesFor(female);val mm=repo.movesFor(male)
        assertEquals(45,fm.size);assertEquals(46,mm.size)
        assertTrue(fm.any{it.name=="Follow Me"});assertFalse(mm.any{it.name=="Follow Me"})
        assertFalse(fm.any{it.name=="Expanding Force"});assertTrue(mm.any{it.name=="Expanding Force"})
        assertEquals(10326,resolvePokemonIdentity(catalog,"Meowstic-F-Mega")!!.id)
        assertEquals(200732,resolvePokemonIdentity(catalog,"Meowstic-M-Mega")!!.id)
        assertNotNull(engine.canonicalName(catalog.first{it.id==10326}))
    }
    @Test fun championsOverridesDoNotRewriteOtherGamesAndItemsRespectTheExactForm()=runBlocking{
        val repo=PokemonRepository(context);val catalog=repo.loadCatalog().associateBy{it.id}
        val appletun=catalog.getValue(842)
        assertEquals(90,repo.movesFor(appletun).first{it.name=="Apple Acid"}.power)
        assertEquals(80,repo.movesFor(appletun,20).first{it.name=="Apple Acid"}.power)
        for(p in catalog.values.filter{it.champions})assertTrue(repo.movesFor(p).all{it.pp==null||it.pp<=20})
        assertTrue(repo.heldItemsFor(catalog.getValue(812)).any{it.name=="Terrain Extender"})
        assertTrue(repo.heldItemsFor(catalog.getValue(10061)).any{it.id==1778})
        assertTrue(repo.heldItemsFor(catalog.getValue(670)).isEmpty())
        val alolan=catalog.values.first{it.canonicalName=="Raichu-Alola"}
        assertFalse(repo.heldItemsFor(alolan).any{it.name.startsWith("Raichunite")})
        assertTrue(repo.heldItemsFor(catalog.getValue(26)).any{it.name=="Raichunite X"})
        val mega=catalog.getValue(10034)
        assertTrue(repo.heldItemsFor(mega).all{it.compatiblePokemonIds?.contains(mega.id)==true})
        assertTrue(repo.validateBuild(mega,Build()).isFailure)
        val stone=repo.heldItemsFor(mega).first()
        assertTrue(repo.validateBuild(mega,Build(itemId=stone.id)).isSuccess)
    }
    @Test fun duplicateIdsMigrateAndAnUnchangedLegacySetDoesNotPreventSaving()=runBlocking{
        val name="v3-migration-${UUID.randomUUID()}";val prefs=context.getSharedPreferences("$name-session",0)
        val raw="""{"favorites":[20101,200401],"teams":[{"id":"old","name":"Preservada","gameId":25,"members":[670],"builds":{"670":{"moves":[]}},"format":"singles"}],"activeTeamId":"old"}"""
        prefs.edit().putString("guest-state",raw).commit();val store=Store(context,name)
        try{
            val state=store.load();assertEquals(setOf(200401),state.favorites);assertEquals(listOf(670),state.team)
            assertTrue(prefs.contains("state-before-v3--1"))
            store.save(state.copy(favorites=state.favorites+812))
            assertEquals(state.builds,store.load().builds);assertEquals(listOf(670),store.load().team)
            val migrated=Store.decode(JSONObject("""{"team":[20101,200401],"builds":{"20101":{"moves":[]},"200401":{"moves":[182]}}}"""))
            assertEquals(listOf(200401),migrated.team);assertEquals(listOf(182),migrated.builds.getValue(200401).moves)
        }finally{store.close();context.deleteDatabase("$name-accounts.sqlite3");prefs.edit().clear().commit()}
    }
    @Test fun correctedArtworkMatchesTheAuditedSourceIdentities(){
        fun hash(id:Int)=context.assets.open("art/$id.webp").use{MessageDigest.getInstance("SHA-256").digest(it.readBytes()).toList()}
        assertNotEquals(hash(200733),hash(200732))
        // The two primary-source Mega Meowstic files differ in metadata but currently share pixels.
        assertEquals(hash(10326),hash(200732))
    }
}
