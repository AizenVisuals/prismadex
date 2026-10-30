package dev.bloody.prismadex.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Listed fan artist, without guessing authorship for uncredited game sprites. */
data class TrainerCredit(val key: String, val artist: String?, val sourceUrl: String, val sourceIndexUrl: String)
class TrainerCreditsRepository(context: Context) {
    private val assets = context.applicationContext.assets
    private val credits: Map<String, TrainerCredit> by lazy {
        val root = JSONObject(assets.open("trainers/credit-manifest.json").bufferedReader().use { it.readText() })
        require(root.getInt("schemaVersion") == 1)
        val source = root.getString("sourceUrl")
        val rows = root.getJSONArray("records")
        (0 until rows.length()).mapNotNull { index ->
            val row = rows.getJSONObject(index)
            if (!row.getBoolean("available")) return@mapNotNull null
            val key = row.getString("key")
            key to TrainerCredit(key, row.optString("artist").takeUnless { it.isBlank() || it == "null" }, row.getString("url"), source)
        }.toMap()
    }
    suspend fun credit(key: String): TrainerCredit? = withContext(Dispatchers.IO) { credits[key] }
}
