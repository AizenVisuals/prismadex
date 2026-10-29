package dev.bloody.prismadex.data

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class HistoricalMoveDataTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun move(id:Int,power:Int?,pp:Int,type:String="normal",category:String="physical")=Move(id,"Test",type,category,power,100,pp)
    @Test fun recoverPpAndTacklePowerFollowSelectedGeneration():Unit=runBlocking {
        val data=HistoricalMoveData(context).load()
        val recover=move(105,null,10,category="status")
        assertEquals(20,data.applyHistoricalMove(recover,1).pp)
        assertEquals(10,data.applyHistoricalMove(recover,15).pp)
        assertEquals(10,data.applyHistoricalMove(recover,20).pp)
        assertEquals(5,data.applyHistoricalMove(recover,23).pp)
        val tackle=move(33,50,35)
        assertEquals(35,data.applyHistoricalMove(tackle,1).power)
        assertEquals(50,data.applyHistoricalMove(tackle,11).power)
        assertEquals(40,data.applyHistoricalMove(tackle,17).power)
    }
    @Test fun historicalTypesAndPreSplitCategoriesArePreserved():Unit=runBlocking {
        val data=HistoricalMoveData(context).load()
        val bite=move(44,60,25,"dark")
        assertEquals("normal",data.applyHistoricalMove(bite,1).type)
        assertEquals("special",data.applyHistoricalMove(bite,5).category)
        assertEquals("physical",data.applyHistoricalMove(bite,8).category)
        val gust=move(16,40,35,"flying","special")
        assertEquals("normal",data.applyHistoricalMove(gust,1).type)
    }
    @Test fun newerMoveIdentitiesDoNotAssumePokeApiNumericIds():Unit=runBlocking {
        val data=HistoricalMoveData(context).load()
        // Internal750 is Apple Acid, while source PokeAPI787 is Apple Acid.
        val appleAcid=move(750,80,10,"grass","special")
        assertEquals(80,data.applyHistoricalMove(appleAcid,20).power)
        // Internal787 is G-Max Replenish: never substitute source787 Apple Acid.
        val replenish=move(787,1,10,"normal","physical")
        val restored=data.applyHistoricalMove(replenish,20)
        assertEquals(1,restored.power);assertEquals("normal",restored.type)
        assertEquals(100,data.applyHistoricalMove(move(725,100,5,"dragon","special"),20).power)
        assertEquals(140,data.applyHistoricalMove(move(759,140,5,"steel","special"),20).power)
    }
    @Test fun championsAndUnknownGamesAreNeverOverwritten():Unit=runBlocking {
        val data=HistoricalMoveData(context).load();val champions=move(105,null,5,category="status")
        assertSame(champions,data.applyHistoricalMove(champions,25))
        assertSame(champions,data.applyHistoricalMove(champions,999))
    }
}
