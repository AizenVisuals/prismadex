package dev.bloody.prismadex.data
import org.junit.Assert.*
import org.junit.Test
class PokemonFiltersTest {
    private fun p(id:Int,types:List<String>,champions:Boolean=true,base:Int=id,mega:Boolean=false,name:String="Species $id",generation:Int=1,games:List<Int> = listOf(23))=Pokemon(id,base,name,types,listOf(id,50,50,50,50,50),listOf("overgrow"),1.0,1.0,champions,mega,generation=generation,games=games,aliases=listOf("Alias$id"))
    private val rows=listOf(p(1,listOf("grass","poison")),p(2,listOf("grass")),p(3,listOf("water"),false),p(1001,listOf("grass","poison"),base=1),p(1002,listOf("fire"),mega=true),p(200401,listOf("electric"),base=25,name="Gigantamax Species"))
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
    @Test fun formFiltersCombineAsUnion(){
        val index=PokemonFilterIndex(rows)
        assertEquals(listOf(1002),index.select(25,PokemonFilter(megaOnly=true)).map{it.id})
        assertEquals(listOf(200401),index.select(25,PokemonFilter(gigantamaxOnly=true)).map{it.id})
        assertEquals(listOf(1001),index.select(25,PokemonFilter(alternateFormsOnly=true)).map{it.id})
        assertEquals(setOf(1002,200401),index.select(25,PokemonFilter(megaOnly=true,gigantamaxOnly=true)).map{it.id}.toSet())
        assertEquals(setOf(1,2,1001,1002,200401),index.select(25,PokemonFilter()).map{it.id}.toSet())
    }
    @Test fun sortsStatDescendingWithoutChangingSource(){
        val index=PokemonFilterIndex(rows)
        assertEquals(200401,index.select(25,PokemonFilter(sort=PokemonSort.HP)).first().id) // highest HP in the fixture (Gigantamax row, HP = id)
        assertEquals(1,rows.first().id)
    }
    // Regional-first (11:56): game 23's regional dex is [3, 1] here — species 3
    // (a transfer-era species by national number) leads, species 1 follows in
    // its regional slot, its form 1001 stays with it, and the rest follow in
    // national order. Nothing is dropped; the national block keeps (baseId,id).
    @Test fun regionalDexComesFirstInEntryOrderThenNational(){
        val fixture=listOf(
            p(1,listOf("grass")),p(2,listOf("grass")),p(3,listOf("water"),false),
            p(1001,listOf("grass"),base=1),p(4,listOf("fire")),
        )
        val index=PokemonFilterIndex(fixture, mapOf(23 to listOf(3, 1)))
        assertEquals(listOf(3, 1, 1001, 2, 4), index.select(23,PokemonFilter()).map{it.id})
    }
    @Test fun regionalOrderDoesNotDropOrDuplicateAndKeepsExplicitSorts(){
        val fixture=listOf(
            p(1,listOf("grass")),p(2,listOf("grass")),p(3,listOf("water"),false),
            p(1001,listOf("grass"),base=1),p(4,listOf("fire")),
        )
        val index=PokemonFilterIndex(fixture, mapOf(23 to listOf(3, 1)))
        // Nacional mode (game 0) is unchanged: full national order.
        assertEquals(listOf(1, 1001, 2, 3, 4), index.select(0,PokemonFilter()).map{it.id})
        // A game without a regional dex keeps national order. Game 25
        // (Champions) filters by the champions flag, so id 3 — built
        // with champions=false, like the original fixture — is out.
        assertEquals(listOf(1, 1001, 2, 4), index.select(25,PokemonFilter()).map{it.id})
        // An explicit sort wins over regional order (NAME is lexicographic
        // on the fixture names: "Species 1" < "Species 1001" < "Species 2"...).
        assertEquals(listOf(1, 1001, 2, 3, 4), index.select(23,PokemonFilter(sort=PokemonSort.NAME)).map{it.id})
        // Every available record appears exactly once under regional order.
        val ordered=index.select(23,PokemonFilter())
        assertEquals(fixture.size, ordered.size)
        assertEquals(fixture.map{it.id}.toSet(), ordered.map{it.id}.toSet())
    }
}
