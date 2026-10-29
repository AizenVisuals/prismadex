package dev.bloody.prismadex.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpeciesProfileRepositoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun everyNationalSpeciesHasOfflineBiologyAndLocalizedCategory() = runBlocking {
        val catalog = PokemonRepository(context).loadCatalog()
        val profiles = SpeciesProfileRepository(context)
        (1..1025).forEach { id ->
            val p = profiles.profile(catalog.first { it.id == id })!!
            assertEquals(id, p.speciesId)
            assertNotNull(p.category); assertNotNull(p.growthRate)
            assertTrue(p.eggGroups.isNotEmpty()); assertNotNull(p.captureRate)
            assertNotNull(p.genderRateEighths); assertNotNull(p.hatchCounter)
            listOf("pt","en","es").forEach { language -> assertTrue(p.category!!.text(language).isNotBlank()) }
        }
        val bulbasaur = profiles.profile(catalog.first { it.id == 1 })!!
        assertEquals("Pokémon Semente", bulbasaur.category!!.text("pt-BR"))
        assertEquals(45, bulbasaur.captureRate)
        assertTrue(bulbasaur.abilities.any { it.slug == "chlorophyll" && it.isHidden == true })
        assertTrue(bulbasaur.abilities.any { it.slug == "overgrow" && it.isHidden == false })
    }

    @Test fun exactFemaleAndRegionalFormsDoNotInheritWrongAbilityOrPhysicalIdentity() = runBlocking {
        val catalog = PokemonRepository(context).loadCatalog(); val repo = SpeciesProfileRepository(context)
        val female = repo.profile(catalog.first { it.id == 20010803 })!!
        val male = repo.profile(catalog.first { it.id == 876 })!!
        assertEquals(10186, female.pokeapiPokemonId)
        assertEquals("female", female.formGender)
        assertTrue(female.abilities.any { it.slug == "own-tempo" })
        assertFalse(female.abilities.any { it.slug == "inner-focus" })
        assertTrue(male.abilities.any { it.slug == "inner-focus" })
        val alola = repo.profile(catalog.first { it.id == 20159 })!!
        assertNotEquals(26, alola.pokeapiPokemonId)
        assertTrue(alola.abilities.any { it.slug == "surge-surfer" })
        assertEquals(21.0, alola.weightKg!!, 0.001)
        val unknown = repo.profile(catalog.first { it.id == 20201 })!!
        assertFalse(unknown.formResolved); assertNull(unknown.heightMeters); assertNull(unknown.weightKg)
        assertNull(unknown.baseExperience); assertTrue(unknown.abilities.isEmpty())
    }

    @Test fun exactCosmeticsRetainSeparateIdsAndKnownSharedSourceFormStats() = runBlocking {
        val catalog = PokemonRepository(context).loadCatalog(); val repo = SpeciesProfileRepository(context)
        val ordinary = repo.profile(catalog.first { it.id == 1012 })!!
        val artisan = repo.profile(catalog.first { it.id == 20011011 })!!
        assertNotEquals(ordinary.pokemonId, artisan.pokemonId)
        assertEquals(10447, artisan.pokeapiFormId)
        assertEquals(1012, artisan.pokeapiPokemonId)
        assertTrue(artisan.formResolved)
    }

    @Test fun additionalSourceFormsHaveStableNonCollidingIdsAndDoNotBroadenChampions() = runBlocking {
        val extra = AdditionalFormsRepository(context).catalogEntries()
        val raw = org.json.JSONArray(context.assets.open("data.json").bufferedReader().use { it.readText() })
        val baseIds = (0 until raw.length()).map { raw.getJSONObject(it).getInt("id") }.toSet()
        assertEquals(51, extra.size)
        assertEquals(extra.size, extra.map { it.id }.distinct().size)
        assertTrue(extra.none { it.id in baseIds })
        assertTrue(extra.all { !it.champions && it.games.isEmpty() && !it.availableIn(25) })
        assertTrue(extra.all { it.stats.size == 6 && it.stats.all { stat -> stat > 0 } && it.types.size in 1..2 })
        val ownTempo = extra.first { it.id == 310151 }
        assertEquals(744, ownTempo.baseId)
        assertEquals(listOf("own-tempo"), ownTempo.abilities)
        assertTrue(extra.any { it.id == 310318 && it.isMega }) // Original-color Mega Magearna.
        val profiles = SpeciesProfileRepository(context)
        extra.forEach { assertTrue(profiles.profile(it)!!.formResolved) }
    }

    @Test fun abilityEffectsFollowLanguageAndAreExplicitlyGeneralReference() = runBlocking {
        val repo = SpeciesProfileRepository(context)
        val intimidate = repo.ability("Intimidate")!!
        assertEquals(intimidate, repo.ability("intimidate"))
        assertTrue(intimidate.description!!.text("pt").contains("reduz o Ataque"))
        assertTrue(intimidate.description.text("es").contains("reduce el Ataque"))
        assertFalse(intimidate.championsSpecific)
        val asOne = repo.ability("as-one-shadow-rider")!!
        assertTrue(asOne.description!!.text("pt").contains("Grim Neigh"))
        assertEquals("As One (Spectrier)", asOne.name)
        assertNull(repo.ability("not-a-real-ability"))
    }
}
