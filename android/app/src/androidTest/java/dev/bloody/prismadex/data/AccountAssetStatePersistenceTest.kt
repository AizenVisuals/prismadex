package dev.bloody.prismadex.data
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class AccountAssetStatePersistenceTest {
 @Test fun genderAndBothGalarBadgeRecordsSurviveReopening(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val name="assets-regression-${UUID.randomUUID()}"
  var store=Store(context,name)
  try{
   val original=UserState(trainerName="Trainer",trainerSprite="red",kantoPostgameBadges=mapOf(10 to setOf(0,7)),galarVersion="shield",galarBadgeSets=mapOf("sword" to setOf(0,3),"shield" to setOf(1,7)))
   store.save(original);store.close();store=Store(context,name)
   assertEquals(original,store.load())
   val female=Build(gender="female")
   assertEquals(female,Store.decodeBuild(Store.encodeBuild(female)))
   assertEquals("galar-shield",original.cardBadgeSet(20)?.prefix)
   assertEquals(setOf(1,7),original.cardBadgeIndices(20))
   val toggled=original.toggleCardBadge(20,0)
   assertEquals(setOf(0,3),toggled.galarBadgeSets["sword"])
   assertEquals(setOf(0,1,7),toggled.galarBadgeSets["shield"])
  }finally{store.close();context.deleteDatabase("$name-accounts.sqlite3");context.getSharedPreferences("$name-session",0).edit().clear().commit()}
 }
}
