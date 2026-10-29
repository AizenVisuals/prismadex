package dev.bloody.prismadex.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Extra national forms. Explicit empty availability never broadens Champions. */
class AdditionalFormsRepository(context: Context) {
    private val assets = context.applicationContext.assets
    private val entries: List<Pokemon> by lazy {
        val root = JSONObject(assets.open("additional-form-profiles.json").bufferedReader().use { it.readText() })
        require(root.getInt("schemaVersion") == 1)
        val records = root.getJSONArray("records")
        (0 until records.length()).mapNotNull { i ->
            val row = records.getJSONObject(i)
            val id = row.getInt("id")
            val sourceId = row.getInt("pokeapiId")
            val stats = row.getJSONArray("stats").ints()
            val types = row.getJSONArray("types").strings()
            if (id != 300000 + sourceId || stats.size != 6 || stats.any { it <= 0 } || types.size !in 1..2) return@mapNotNull null
            Pokemon(
                id = id, baseId = row.getInt("species"), name = row.getString("name"),
                types = types, stats = stats, abilities = row.getJSONArray("abilities").strings(),
                height = row.optDouble("height").takeIf { !row.isNull("height") && it.isFinite() },
                weight = row.optDouble("weight").takeIf { !row.isNull("weight") && it.isFinite() },
                champions = false, isMega = row.optBoolean("isMega"), generation = row.getInt("generation"),
                games = emptyList(), canonicalName = row.getString("canonical"),
                aliases = listOf(row.getString("canonical")), championsSource = "",
            )
        }.also { values -> require(values.map { it.id }.distinct().size == values.size) }
    }
    suspend fun catalogEntries(): List<Pokemon> = withContext(Dispatchers.IO) { entries }
}
