package dev.bloody.prismadex.data
import org.junit.Assert.*
import org.junit.Test
class PokemonFiltersTest {
    private fun p(id:Int,types:List<String>,champions:Boolean=true,base:Int=id)=Pokemon(id,base,"Species $id",types,listOf(id,50,50,50,50,50),listOf("overgrow"),1.0,1.0,champions,false,generation=1,games=listOf(23),aliases=listOf("Alias$id"))
    private val rows=listOf(p(1,listOf("grass","poison")),p(2,listOf("grass")),p(3,listOf("water"),false),p(1001,listOf("grass","poison"),base=1))
    @Test fun combinesAvailabilityTypesAbilitiesFormsAndSearch(){
        val index=PokemonFilterIndex(rows)
        assertEquals(listOf(1),index.select(25,PokemonFilter(query="Alias1",types=setOf("grass","poison"),ability="overgrow",includeForms=false)).map{it.id})
        assertEquals(listOf(1,1001),index.select(25,PokemonFilter(query="#0001")).map{it.id})
        assertEquals(listOf(3),index.select(23,PokemonFilter(types=setOf("water"))).map{it.id})
        assertTrue(index.select(25,PokemonFilter(types=setOf("water"))).isEmpty())
        assertEquals(3,index.select(25,PokemonFilter(types=setOf("grass","water"),allTypes=false)).size)
        assertTrue(index.select(25,PokemonFilter(generation=2)).isEmpty())
        assertTrue(index.select(25,PokemonFilter(ability="torrent")).isEmpty())
    }
    @Test fun sortsStatDescendingWithoutChangingSource(){
        val index=PokemonFilterIndex(rows)
        assertEquals(1001,index.select(25,PokemonFilter(sort=PokemonSort.HP)).first().id)
        assertEquals(1,rows.first().id)
    }
}
