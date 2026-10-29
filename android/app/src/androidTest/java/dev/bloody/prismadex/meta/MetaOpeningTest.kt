package dev.bloody.prismadex.meta

import android.content.ContextWrapper
import android.os.SystemClock
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import dev.bloody.prismadex.data.PokemonIdentityIndex
import dev.bloody.prismadex.data.PokemonRepository
import dev.bloody.prismadex.data.resolvePokemonIdentity
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class MetaOpeningTest {
    @Test fun indexedReportPreservesEveryExactIdentity():Unit=runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val catalog=PokemonRepository(instrumentation.targetContext).loadCatalog()
        val report=instrumentation.context.assets.open("meta-doubles-fixture.txt").bufferedReader().use{it.readText()}
        val rows=MetaRepository.parseReport(report)
        val start=SystemClock.elapsedRealtimeNanos()
        val expected=rows.map{resolvePokemonIdentity(catalog,it.name)?.id}
        val middle=SystemClock.elapsedRealtimeNanos()
        val index=PokemonIdentityIndex(catalog)
        val actual=rows.map{index.resolve(it.name)?.id}
        val end=SystemClock.elapsedRealtimeNanos()
        assertEquals(expected,actual)
        assertNotEquals(index.resolve("Indeedee-F")?.id,index.resolve("Indeedee")?.id)
        assertNotNull(index.resolve("Indeedee-F"))
        Log.i("PrismaMetaTiming","rows=${rows.size} originalMs=${(middle-start)/1_000_000} indexedMs=${(end-middle)/1_000_000}")
        Unit
    }

    @Test fun expiredCacheIsFirstEmissionEvenWhenRefreshIsForced():Unit=runBlocking {
        val target=InstrumentationRegistry.getInstrumentation().targetContext
        val root=File(target.cacheDir,"meta-opening-${System.nanoTime()}").apply{mkdirs()}
        val context=object:ContextWrapper(target){override fun getFilesDir()=root; override fun getApplicationContext()=this}
        try {
            val directory=File(root,"meta-cache").apply{mkdirs()}
            File(directory,"singles.json").writeText("""{"month":"2026-01","formatCode":"gen9championsbssregma","sourceUrl":"https://www.smogon.com/stats/2026-01/report.txt","battles":100,"fetchedAt":1,"entries":[{"rank":1,"name":"Rillaboom","usage":42.5}]}""")
            // first() cancels before network refresh; this must work without a connection.
            val saved=withTimeout(2000){MetaRepository(context).observe("singles",true).first()}
            assertTrue(saved.cached)
            assertEquals(1L,saved.fetchedAt)
            assertEquals("Rillaboom",saved.entries.single().name)
            assertNull(MetaRepository(context).readCached("doubles"))
        }finally{root.deleteRecursively()}
    }
}
