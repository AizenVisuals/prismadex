package dev.bloody.prismadex.data
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
class ItemCatalogRepositoryTest {
 @Test fun generalReferenceIsOfflineDistinctAndKeepsChampionsIndependent():Unit=runBlocking{
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val repository=ItemCatalogRepository(context);val all=repository.all()
  assertEquals(2223,all.size);assertEquals(all.size,all.map{it.id}.distinct().size)
  assertTrue(all.any{it.name=="Master Ball"});assertTrue(all.any{it.name=="Leftovers"&&it.descriptionEnglish!=null})
  assertTrue(all.any{it.category=="mega-stones"});assertEquals(1154,all.count{it.descriptionEnglish!=null})
  assertTrue(all.filter{it.description!=null}.all{item->listOf("pt","en","es").all{item.description!!.text(it).isNotBlank()}})
  val leftovers=all.first{it.name=="Leftovers"}.description!!
  assertEquals("Ao final de cada turno, o portador recupera 1/16 de seu HP máximo.",leftovers.text("pt"))
  assertTrue(leftovers.text("es").contains("1/16"))
  assertTrue(all.all{it.name.isNotBlank()&&it.category.isNotBlank()})
  val pokemonRepository=PokemonRepository(context)
  val charizard=pokemonRepository.loadCatalog().first{it.id==6}
  assertTrue(pokemonRepository.heldItemsFor(charizard,23).isEmpty())
  val champion=pokemonRepository.heldItemsFor(charizard,25)
  assertTrue(champion.isNotEmpty());assertTrue(champion.none{it.name=="Master Ball"})
 }
}
