package dev.bloody.prismadex.data
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
class LearnsetRepositoryTest {
 @Test fun numericMethodsLevelsAndVersionsRemainSeparated(){
  val rows=JSONArray("""[{"id":22,"versions":[{"game":1,"method":"level-up","level":13},{"game":23,"method":"1","level":3},{"game":23,"method":"4","level":0},{"game":23,"method":"4","level":0}]}]""")
  assertEquals(listOf(LearnMethod("level-up",13)),LearnsetRepository.parseHistorical(rows,1)[22])
  assertEquals(listOf(LearnMethod("level-up",3),LearnMethod("machine")),LearnsetRepository.parseHistorical(rows,23)[22])
  assertTrue(LearnsetRepository.parseHistorical(rows,25).isEmpty())
 }
 @Test fun championsDoesNotInventHistoricLearningMethods():Unit=runBlocking{
  val repository=LearnsetRepository(InstrumentationRegistry.getInstrumentation().targetContext)
  val champion=repository.methodsFor(6,25)
  assertTrue(champion.isNotEmpty());assertTrue(champion.values.flatten().all{it.method=="unknown"&&it.level==null})
  val historical=repository.methodsFor(1,1)
  assertTrue(historical.values.flatten().any{it.method=="level-up"&&it.level!=null})
  assertTrue(repository.methodsFor(9999999,1).isEmpty())
 }
}
