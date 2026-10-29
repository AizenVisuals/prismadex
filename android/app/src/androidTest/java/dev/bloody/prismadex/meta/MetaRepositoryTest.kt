package dev.bloody.prismadex.meta

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MetaRepositoryTest {
    @Test fun usageParserKeepsSourcePercentages(){
        val report=InstrumentationRegistry.getInstrumentation().context.assets.open("meta-doubles-fixture.txt").bufferedReader().use{it.readText()}
        val rows=MetaRepository.parseReport(report)
        assertEquals("Rillaboom",rows.first().name);assertEquals(45.95769,rows.first().usage,.000001);assertTrue(rows.size>100)
    }
    @Test fun publicLiveReportsAreSeparatedAndCached()=runBlocking {
        val repository=MetaRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        val singles=repository.load("singles",true);val doubles=repository.load("doubles",true)
        assertTrue(singles.formatCode.contains("championsbss"));assertFalse(singles.formatCode.contains("ou"));assertTrue(doubles.formatCode.contains("championsvgc"))
        assertTrue(singles.battles>0&&doubles.battles>0);assertTrue(singles.entries.isNotEmpty()&&doubles.entries.isNotEmpty())
        assertTrue(repository.load("singles").cached);assertTrue(singles.sourceUrl.startsWith("https://www.smogon.com/stats/"))
    }
}
