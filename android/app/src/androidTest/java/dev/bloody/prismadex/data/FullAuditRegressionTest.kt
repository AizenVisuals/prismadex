package dev.bloody.prismadex.data

import android.os.StrictMode
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

class FullAuditRegressionTest {
 @Test fun expandedReferenceReadersWorkWithNetworkForbidden():Unit=runBlocking {
  withContext(Dispatchers.IO){
   val previous=StrictMode.getThreadPolicy()
   StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectNetwork().penaltyDeath().build())
   try {
    val context=InstrumentationRegistry.getInstrumentation().targetContext
    val repository=PokemonRepository(context);val catalog=repository.loadCatalog()
    assertEquals(1357,catalog.size)
    assertEquals(354,catalog.count{it.champions})
    val index=PokemonFilterIndex(catalog)
    assertEquals(catalog.size,index.select(0,PokemonFilter()).size)
    assertEquals(354,index.select(25,PokemonFilter()).size)
    assertEquals(100,catalog.first{it.id==51}.stats[1])
    assertEquals(110,catalog.first{it.id==488}.stats[2])
    assertEquals(120,catalog.first{it.id==488}.stats[4])
    assertEquals(80,repository.movesFor(catalog.first{it.id==842},20).first{it.name=="Apple Acid"}.power)
    assertEquals(90,repository.movesFor(catalog.first{it.id==842},25).first{it.name=="Apple Acid"}.power)
    assertEquals(2223,ItemCatalogRepository(context).all().size)
    assertNotNull(SpeciesProfileRepository(context).profile(catalog.first{it.id==310151}))
    assertNotNull(EvolutionRepository(context).family(catalog.first{it.id==133},catalog))
   }finally{StrictMode.setThreadPolicy(previous)}
  }
 }
}
