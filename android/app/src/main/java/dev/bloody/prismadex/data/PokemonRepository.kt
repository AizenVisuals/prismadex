package dev.bloody.prismadex.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal fun JSONArray.ints(): List<Int> = (0 until length()).map { getInt(it) }
internal fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
internal fun JSONObject.nullableInt(key: String): Int? = if (isNull(key)) null else optInt(key).takeIf { has(key) }

/** Bundled reference data. A Champions flag does not certify the current ranked regulation. */
class PokemonRepository(context: Context) {
    private val assets = context.applicationContext.assets
    private val rawCatalog: JSONArray by lazy { JSONArray(text("data.json")) }
    private val metadata:JSONObject by lazy { JSONObject(text("pokemon-metadata.json")) }
    private val historicalMoves=HistoricalMoveData(context)
    private val rawMoves: JSONObject by lazy { JSONObject(text("moves.json")) }
    private val identities:JSONObject by lazy { JSONObject(text("pokemon-identities.json")) }
    private val roster:JSONObject by lazy { JSONObject(text("champions-roster.json")).getJSONObject("forms") }
    private val championsLearnsets:JSONObject by lazy { JSONObject(text("champions-learnsets.json")) }
    private val championsMoveData:JSONObject by lazy { JSONObject(text("champions-move-data.json")) }
    private val rawItems: JSONObject by lazy { JSONObject(text("items.json")) }
    private val moveDetails: JSONObject by lazy { JSONObject(text("move-details.json")) }
    private fun text(path: String): String = assets.open(path).bufferedReader().use { it.readText() }

    private val catalog: List<Pokemon> by lazy {
        (0 until rawCatalog.length()).map { index ->
            val row = rawCatalog.getJSONObject(index)
            Pokemon(
                id = row.getInt("id"), baseId = row.optInt("baseId", row.getInt("id")),
                name = row.getString("name"), types = row.getJSONArray("types").strings(),
                stats = row.getJSONArray("stats").ints(), abilities = row.getJSONArray("abilities").strings(),
                height = if (row.isNull("height")) null else row.optDouble("height").takeIf { it.isFinite() },
                weight = if (row.isNull("weight")) null else row.optDouble("weight").takeIf { it.isFinite() },
                champions = row.optBoolean("champions"), isMega = row.optBoolean("isMega"),
                generation=metadata.optJSONObject(row.getInt("id").toString())?.optInt("generation",1)?:1,
                games=metadata.optJSONObject(row.getInt("id").toString())?.optJSONArray("games")?.ints()?:emptyList(),
                canonicalName=identities.optJSONObject(row.getInt("id").toString())?.optString("canonicalName"),
                aliases=identities.optJSONObject(row.getInt("id").toString())?.optJSONArray("aliases")?.strings()?:emptyList(),
                championsSource=roster.optJSONObject(row.getInt("id").toString())?.optString("source")?:"",
            )
        }
    }

    private val additionalForms=AdditionalFormsRepository(context)
    suspend fun loadCatalog(): List<Pokemon> = withContext(Dispatchers.IO) { catalog+additionalForms.catalogEntries() }

    private val speciesById: Map<Int, Int> by lazy {
        (0 until rawCatalog.length()).associate { rawCatalog.getJSONObject(it).let { row -> row.getInt("id") to row.optInt("species", row.optInt("baseId", row.getInt("id"))) } }
    }

    private fun learnset(id: Int): JSONArray = try { JSONArray(text("learnsets/$id.json")) } catch (_: Exception) { JSONArray() }
    private fun moveIds(rows: JSONArray, game: Int): List<Int> = (0 until rows.length()).mapNotNull { i ->
        val row = rows.getJSONObject(i)
        val versions = row.optJSONArray("versions") ?: JSONArray()
        row.getInt("id").takeIf { (0 until versions.length()).any { versions.getJSONObject(it).optInt("game") == game } }
    }

    suspend fun movesFor(p: Pokemon, game: Int = 25): List<Move> = withContext(Dispatchers.IO) {
        if (game == 25 && !p.champions) return@withContext emptyList()
        var ids = if(game==25)championsLearnsets.optJSONArray(p.id.toString())?.ints()?:emptyList() else moveIds(learnset(p.id), game)
        if (game!=25 && p.isMega && ids.isEmpty()) ids = moveIds(learnset(p.baseId), game)
        if(game!=25)historicalMoves.load()
        ids.distinct().mapNotNull { id ->
            val row = (if(game==25)championsMoveData else rawMoves).optJSONObject(id.toString()) ?: return@mapNotNull null
            val details = moveDetails.optJSONObject(id.toString())
            val flags = details?.optJSONObject("flags")
            Move(id, row.getString("name"), row.optString("type"), row.optString("category"),
                row.nullableInt("power"), row.nullableInt("accuracy"), row.nullableInt("pp"),
                details?.optString(if(game==25)"championsDescription" else "description") ?: "",
                details?.optString(if(game==25)"championsSummary" else "summary") ?: "",
                details?.optInt("priority") ?: 0, details?.optString("target") ?: "normal",
                flags?.keys()?.asSequence()?.filter { flags.optInt(it) > 0 }?.toSet() ?: emptySet(), game).let{if(game==25)it else historicalMoves.applyHistoricalMove(it,game)}
        }.sortedBy { it.name }
    }

    suspend fun heldItemsFor(p: Pokemon, game:Int=25): List<HeldItem> = withContext(Dispatchers.IO) {
        if (game!=25 || !p.champions) return@withContext emptyList()
        val species = speciesById[p.id] ?: p.baseId
        rawItems.keys().asSequence().map { rawItems.getJSONObject(it) }.filter { it.optBoolean("champions") && it.optBoolean("held") }.map { item ->
            HeldItem(item.getInt("id"), item.getString("name"), item.optString("description"), item.optJSONArray("compatibleSpecies")?.ints(),item.optJSONArray("compatiblePokemonIds")?.ints(),item.optString("icon").takeIf{it.isNotBlank()})
        }.filter { (it.compatibleSpecies == null || species in it.compatibleSpecies) && (it.compatiblePokemonIds==null || p.id in it.compatiblePokemonIds) && (!p.isMega || it.compatiblePokemonIds!=null) }.toList().sortedBy { it.name }
    }

    suspend fun validateBuild(p: Pokemon, build: Build,game:Int=25,allowLegacy:Boolean=false): Result<Unit> = runCatching {
        require(build.nature in NatureNames) { "Natureza inválida." }
        require(build.ability == null || (allowLegacy && build.ability.length in 1..64) || build.ability in p.abilities) { "Habilidade incompatível com a espécie." }
        require(build.sps.size == 6 && build.sps.all { it in 0..32 } && build.sps.sum() <= 66) { "Use até 32 SP por atributo e 66 SP no total." }
        require(build.moves.size <= 4 && build.moves.distinct().size == build.moves.size) { "Escolha até quatro golpes diferentes." }
        if(allowLegacy){
            require(build.moves.all{rawMoves.has(it.toString()) || championsMoveData.has(it.toString())}){"Golpe desconhecido."}
            require(build.itemId==null || rawItems.has(build.itemId.toString())){"Item desconhecido."}
            return@runCatching
        }
        require(p.availableIn(game)){"Pokémon não listado para este jogo na base local."}
        val moves = movesFor(p,game).map { it.id }.toSet()
        require(build.moves.all { it in moves }) { "Golpe não registrado para este Pokémon no jogo da equipe." }
        require(game!=25 || !p.isMega || build.itemId!=null){"Selecione a Mega Stone compatível com esta forma."}
        require(build.itemId == null || heldItemsFor(p,game).any { it.id == build.itemId }) { "Item incompatível ou sem confirmação na base Champions." }
    }
}
