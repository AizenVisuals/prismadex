package dev.bloody.prismadex.data

import android.graphics.BitmapFactory
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.bloody.prismadex.ui.DisplayPreferences
import dev.bloody.prismadex.ui.UiStrings
import dev.bloody.prismadex.ui.DescriptionCatalog
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DisplayAndMoveInfoTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun languageAndFontPreferencesSurviveReloadWithoutChangingAccountData() {
        val name="display-test-${UUID.randomUUID()}"
        try {
            val first=DisplayPreferences(context,name)
            assertEquals("system",first.language);assertEquals(1f,first.textScale,.001f)
            first.selectLanguage("es");first.selectTextScale(1.3f)
            val restored=DisplayPreferences(context,name)
            assertEquals("es",restored.language);assertEquals(1.3f,restored.textScale,.001f)
            restored.selectTextScale(20f);assertEquals(1.5f,restored.textScale,.001f)
            restored.selectTextScale(.1f);assertEquals(.85f,restored.textScale,.001f)
            assertTrue(runCatching{restored.selectLanguage("invalid")}.isFailure)
        } finally { context.getSharedPreferences(name,Context.MODE_PRIVATE).edit().clear().commit() }
    }

    @Test fun translatedControlsAndDynamicLabelsPreservePokemonAndUserNames() {
        val en=UiStrings(context,"en");val es=UiStrings(context,"es");val pt=UiStrings(context,"pt")
        assertEquals("Search Pokémon",en.text("Buscar Pokémon"))
        assertEquals("Tamaño del texto",es.text("Tamanho do texto"))
        assertEquals("354 Pokémon and forms",en.text("354 Pokémon e formas"))
        assertEquals("Información de Thunderbolt",es.text("Informações de Thunderbolt"))
        assertEquals("¿Eliminar 🌑 Eclipse Protocol?",es.text("Excluir 🌑 Eclipse Protocol?"))
        assertEquals("2 moves · Pokémon Champions",en.text("2 golpes · Pokémon Champions"))
        assertEquals("Indeedee-F",en.text("Indeedee-F"))
        assertEquals("Minha equipe",en.text("Minha equipe"))
        assertEquals("¿Eliminar el objetivo Venusaur?",es.text("Excluir alvo Venusaur?"))
        assertEquals("Remove Venusaur from team",en.text("Remover Venusaur da equipe"))
        assertEquals("Buscar Pokémon",pt.text("Buscar Pokémon"))
    }

    @Test fun championsDetailsKeepPriorityTargetsAndSpecificText()=runBlocking {
        val repo=PokemonRepository(context)
        val charizard=repo.loadCatalog().first{it.id==6}
        val moves=repo.movesFor(charizard)
        val protect=moves.first{it.id==182}
        assertEquals(4,protect.priority);assertEquals("self",protect.target)
        assertTrue(protect.description.isNotBlank());assertEquals(5,protect.pp)
        val earthquake=moves.first{it.id==89}
        assertEquals("allAdjacent",earthquake.target);assertFalse("contact" in earthquake.flags)
        val details=JSONObject(context.assets.open("move-details.json").bufferedReader().use{it.readText()})
        val belch=details.getJSONObject("562")
        assertTrue(belch.getBoolean("championsTextOverride"))
        assertNotEquals(belch.getString("description"),belch.getString("championsDescription"))
        val champions=JSONObject(context.assets.open("champions-move-data.json").bufferedReader().use{it.readText()})
        champions.keys().forEach{id->assertTrue("Missing effect: $id",details.getJSONObject(id).getString("championsDescription").isNotBlank())}
    }

    @Test fun descriptionsFollowLanguageAndKeepChampionsSpecificEffects()=runBlocking{
        DescriptionCatalog.load(context)
        val details=JSONObject(context.assets.open("move-details.json").bufferedReader().use{it.readText()})
        details.keys().forEach{id->val move=details.getJSONObject(id)
            listOf("description","summary","championsDescription","championsSummary").forEach{key->val source=move.getString(key)
                listOf("pt","es").forEach{language->val translated=DescriptionCatalog.text(source,language)
                    if(source.isNotBlank()){assertTrue("Missing $id/$key/$language",translated.isNotBlank());assertNotEquals("Untranslated $id/$key/$language",source,translated)}
                }
                assertEquals(source,DescriptionCatalog.text(source,"en"))
            }
        }
        val belch=details.getJSONObject("562").getString("championsDescription")
        assertTrue(DescriptionCatalog.text(belch,"pt").contains("Natural Gift"))
        assertTrue(DescriptionCatalog.text(belch,"es").contains("Bug Bite"))
        val makeItRain=details.getJSONObject("889").getString("championsDescription")
        assertEquals("Reduz o Ataque Especial do usuário em 2 estágios.",DescriptionCatalog.text(makeItRain,"pt"))
        assertEquals("Make It Rain",UiStrings(context,"pt").text("Make It Rain"))
        assertEquals("Black Glasses",UiStrings(context,"es").text("Black Glasses"))
    }

    @Test fun everyChampionsHeldItemHasItsOwnDecodableOfflineIcon() {
        val items=JSONObject(context.assets.open("items.json").bufferedReader().use{it.readText()})
        var count=0
        items.keys().forEach{id->val item=items.getJSONObject(id)
            if(item.optBoolean("champions")&&item.optBoolean("held")) {
                val options=BitmapFactory.Options().apply{inJustDecodeBounds=true}
                context.assets.open(item.getString("icon")).use{BitmapFactory.decodeStream(it,null,options)}
                assertTrue(item.getString("name"),options.outWidth>0&&options.outHeight>0);count++
            }
        }
        assertEquals(166,count)
    }
}
