package dev.bloody.prismadex.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class NativeDataTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun bundledCatalogArtAndChampionsLearnsets() = runBlocking {
        val repository = PokemonRepository(context)
        val catalog = repository.loadCatalog()
        assertEquals(1306, catalog.size)
        assertEquals(1306, catalog.map { it.id }.toSet().size)
        // Every catalog record must have a decodable bundled art entry, not just a filename.
        assertEquals(1306, context.assets.list("art")!!.size)
        catalog.forEach { p ->
            val path = "art/${p.id}.webp"
            context.assets.open(path).use { input ->
                val bitmap = android.graphics.BitmapFactory.decodeStream(input)
                assertNotNull("Imagem ausente/inválida: ${p.id} ($path)", bitmap)
                bitmap?.recycle()
            }
        }
        assertEquals(73, repository.movesFor(catalog.first { it.id == 6 }).size)
        assertEquals(57, repository.movesFor(catalog.first { it.id == 10033 }).size)
        assertEquals(repository.movesFor(catalog.first { it.id == 6 }), repository.movesFor(catalog.first { it.id == 10034 }))
    }

    @Test fun heldItemsRespectSpeciesAndRejectUnconfirmedData() = runBlocking {
        val repository = PokemonRepository(context)
        val catalog = repository.loadCatalog().associateBy { it.id }
        val charizard = catalog.getValue(6)
        assertTrue(repository.heldItemsFor(charizard).any { it.id == 699 })
        assertFalse(repository.heldItemsFor(charizard).any { it.id == 213 })
        assertTrue(repository.heldItemsFor(catalog.getValue(25)).any { it.id == 213 })
        assertTrue(repository.validateBuild(charizard, Build(211, listOf(7))).isSuccess)
        assertTrue(repository.validateBuild(charizard, Build(1, listOf(7))).isFailure)
        assertTrue(repository.validateBuild(charizard, Build(null, listOf(5))).isFailure)
        assertTrue(repository.validateBuild(charizard, Build(null, listOf(7, 7))).isFailure)
    }

    @Test fun localAccountsAndVisitorStateRemainIsolated() = runBlocking {
        val name = "instrumentation-${UUID.randomUUID()}"
        var store = Store(context, name)
        try {
            val guest = UserState(favorites = setOf(25), theme = "pearl")
            store.save(guest)
            assertTrue(store.register("first@example.com", "password123").isSuccess)
            assertEquals("first@example.com", store.currentEmail)
            assertEquals(UserState(), store.load())
            val saved = UserState(favorites = setOf(6), teams = listOf(SavedTeam("default","Minha equipe",members=listOf(6),builds=mapOf(6 to Build(211,listOf(7))))), layout = "list")
            store.save(saved)
            store.close()
            store = Store(context, name)
            assertEquals(saved, store.load())
            assertTrue(runCatching { store.save(saved.updateTeam{it.copy(builds = mapOf(6 to Build(1, listOf(7))))}) }.isFailure)
            assertEquals(saved, store.load())
            store.logout()
            assertEquals(guest, store.load())
            assertTrue(store.register("second@example.com", "password123").isSuccess)
            assertEquals(UserState(), store.load())
            store.save(UserState(favorites = setOf(3)))
            store.logout()
            assertTrue(store.login("first@example.com", "incorrect").isFailure)
            assertNull(store.currentEmail)
            assertTrue(store.login("first@example.com", "password123").isSuccess)
            assertEquals(saved, store.load())
        } finally {
            store.close()
            // Only unique test-owned storage is removed. Never delete the user's database.
            context.deleteDatabase("$name-accounts.sqlite3")
            context.getSharedPreferences("$name-session", 0).edit().clear().commit()
        }
    }
}
