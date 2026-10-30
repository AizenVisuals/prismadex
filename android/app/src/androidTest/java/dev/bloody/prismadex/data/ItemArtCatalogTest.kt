package dev.bloody.prismadex.data

import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ItemArtCatalogTest {
    @Test fun generalArtworkIsBoundToExactSourceIdentityAndDoesNotReplaceHeldIcons():Unit=runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val all=ItemCatalogRepository(context).all()
        val catalog=ItemArtCatalog(context)
        val master=all.first{it.name=="Master Ball"}
        assertNull(master.iconId)
        val art=catalog.forItem(master)!!
        assertTrue(art.sourceUrl.startsWith("https://archives.bulbagarden.net/wiki/"))
        assertEquals("general-item-art/${master.id}.webp",art.path)
        assertNull(catalog.forItem(master.copy(name="Not Master Ball")))
        assertNull(catalog.forItem(master.copy(slug="not-master-ball")))
        val bitmap=context.assets.open(art.path).use{BitmapFactory.decodeStream(it)}!!
        assertTrue(bitmap.width in 1..128&&bitmap.height in 1..128)
        val leftovers=all.first{it.name=="Leftovers"}
        assertNotNull(leftovers.iconId)
        assertNull(catalog.forItem(leftovers))
        val raw=JSONObject(context.assets.open("general-item-art-map.json").bufferedReader().use{it.readText()})
        val rows=raw.getJSONArray("items")
        assertEquals(843,rows.length())
        val byId=all.associateBy{it.id}
        val records=(0 until rows.length()).associate{rows.getJSONObject(it).let{r->r.getInt("id") to r}}
        listOf("ultra-ball","great-ball","poke-ball","heavy-ball").forEach{base->
            val ordinary=all.first{it.slug==base};val hisui=all.first{it.slug=="la$base"}
            val ordinaryArt=catalog.forItem(ordinary)!!;val hisuiArt=catalog.forItem(hisui)!!
            assertNotEquals(ordinaryArt.path,hisuiArt.path)
            assertEquals("SV",records.getValue(ordinary.id).getString("sourceGameTag"))
            assertEquals("LA",records.getValue(hisui.id).getString("sourceGameTag"))
        }
        val bridge=all.first{it.slug=="bridge-mail-v"}
        assertNotNull(catalog.forItem(bridge))
        repeat(rows.length()){index->val record=rows.getJSONObject(index);val item=byId.getValue(record.getInt("id"))
            assertNull(item.iconId)
            assertEquals(item.name,record.getString("name"));assertEquals(item.slug,record.getString("slug"))
            context.assets.open(record.getString("path")).use{assertTrue(it.read()>=0)}
        }
    }
}
