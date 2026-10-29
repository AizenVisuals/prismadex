package dev.bloody.prismadex.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EvolutionRepositoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private suspend fun catalog() = PokemonRepository(context).loadCatalog()

    @Test fun branchingFamiliesRetainAllAlternativeDestinationsAndMethods() = runBlocking {
        val catalog = catalog(); val repo = EvolutionRepository(context)
        val eevee = repo.family(catalog.first { it.id == 133 }, catalog)!!
        assertTrue(eevee.hasEvolution)
        assertEquals(setOf(134,135,136,196,197,470,471,700), eevee.edges.filter { it.fromId == 133 }.map { it.toId }.toSet())
        assertTrue(eevee.edges.first { it.toId == 134 }.conditions.first().en.contains("Water Stone"))
        assertTrue(eevee.edges.first { it.toId == 470 }.conditions.first().en.contains("Leaf Stone"))
        assertTrue(eevee.edges.first { it.toId == 471 }.conditions.first().en.contains("Ice Stone"))
        assertTrue(eevee.edges.first { it.toId == 470 }.conditions.drop(1).all { it.en.contains("earlier games") })
        val rockruff = repo.family(catalog.first { it.id == 744 }, catalog)!!
        assertTrue(rockruff.edges.first { it.toId == 20029 }.conditions.first().en.contains("Own Tempo"))
        assertTrue(rockruff.edges.first { it.toId == 20029 }.conditions.first().en.contains("dusk"))
        val snom = repo.family(catalog.first { it.id == 872 }, catalog)!!
        assertTrue(snom.edges.first().conditions.first().en.contains("friendship"))
        assertTrue(snom.edges.first().conditions.first().en.contains("night"))
        val tyrogue = repo.family(catalog.first { it.id == 236 }, catalog)!!
        assertEquals(setOf(106,107,237), tyrogue.edges.filter { it.fromId == 236 }.map { it.toId }.toSet())
        assertTrue(tyrogue.edges.first { it.toId == 237 }.conditions.first().en.contains("Attack = Defense"))
    }

    @Test fun regionalFormsDoNotResolveToTheirOrdinaryCounterparts() = runBlocking {
        val catalog = catalog(); val repo = EvolutionRepository(context)
        val meowth = repo.family(catalog.first { it.id == 20156 }, catalog)!!
        assertEquals(setOf(20156,20157), meowth.nodes.map { it.pokemonId }.toSet())
        assertFalse(meowth.nodes.any { it.pokemonId == 53 })
        val galar = repo.family(catalog.first { it.id == 200306 }, catalog)!!
        assertTrue(galar.edges.any { it.fromId == 200306 && it.toId == 863 })
        val raichu = repo.family(catalog.first { it.id == 20159 }, catalog)!!
        assertTrue(raichu.edges.any { it.fromId == 25 && it.toId == 20159 })
        assertTrue(raichu.edges.first { it.toId == 20159 }.conditions.first().en.contains("Alola"))
        val sliggoo = repo.family(catalog.first { it.id == 200513 }, catalog)!!
        assertTrue(sliggoo.edges.any { it.fromId == 704 && it.toId == 200513 })
        assertFalse(sliggoo.edges.any { it.fromId == 705 && it.toId == 200513 })
    }

    @Test fun megaIsATransformationAndUnknownFormsAreNotInvented() = runBlocking {
        val catalog = catalog(); val repo = EvolutionRepository(context)
        val mega = repo.family(catalog.first { it.id == 10034 }, catalog)!!
        assertTrue(mega.isTransformation)
        assertTrue(mega.edges.any { it.fromId == 5 && it.toId == 6 })
        assertFalse(mega.edges.any { it.toId == 10034 })
        val mewtwo = repo.family(catalog.first { it.id == 150 }, catalog)!!
        assertFalse(mewtwo.hasEvolution)
        assertFalse(mewtwo.isTransformation)
        val unknown = catalog.first { it.id == 10027 }
        assertNull(repo.family(unknown, catalog)) // No guessing Pumpkaboo-size inheritance.
        val phione = repo.family(catalog.first { it.id == 489 }, catalog)!!
        assertEquals(listOf(489), phione.nodes.map { it.pokemonId })
        assertFalse(phione.hasEvolution)
    }

    @Test fun specialEvolutionConditionsAreLocalizedWithoutChangingMoveNames() = runBlocking {
        val catalog = catalog(); val repo = EvolutionRepository(context)
        val shedinja = repo.family(catalog.first { it.id == 292 }, catalog)!!
        assertTrue(shedinja.edges.first { it.toId == 292 }.conditions.first().text("pt-BR").contains("Poké Ball"))
        val primeape = repo.family(catalog.first { it.id == 57 }, catalog)!!
        val condition = primeape.edges.first { it.toId == 979 }.conditions.first()
        assertTrue(condition.text("pt").contains("Rage Fist"))
        assertTrue(condition.text("es").contains("Usa Rage Fist"))
        assertTrue(condition.text("en").contains("Use Rage Fist"))
    }

    @Test fun bundledGraphHasExactCatalogIdentitiesAndThreeLanguagesForEveryRecordedMethod() = runBlocking {
        val catalog = catalog(); val ids = catalog.map { it.id }.toSet()
        val root = JSONObject(context.assets.open("evolution-families.json").bufferedReader().use { it.readText() })
        val index = EvolutionRepository.decode(root)
        assertTrue(index.size >= 1090)
        (1..1025).forEach { assertTrue("Missing national species $it", it in index) }
        index.values.distinct().forEach { family ->
            assertTrue(family.nodes.all { it.pokemonId in ids })
            family.edges.forEach { edge ->
                assertTrue(edge.conditions.isNotEmpty())
                edge.conditions.forEach { c -> listOf("pt","en","es").forEach { assertTrue(c.text(it).isNotBlank()) } }
            }
        }
    }
}
