package dev.bloody.prismadex.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** One exact catalog identity: regional and sex-specific forms must not collapse. */
data class EvolutionNode(val pokemonId: Int, val speciesId: Int, val name: String, val stage: Int)

/** Each entry on an edge is an alternative method; clauses inside it are cumulative. */
data class EvolutionCondition(val pt: String, val en: String, val es: String) {
    fun text(language: String): String = when (language.substringBefore('-').substringBefore('_')) {
        "pt" -> pt
        "es" -> es
        else -> en
    }
}
data class EvolutionEdge(val fromId: Int, val toId: Int, val conditions: List<EvolutionCondition>)
data class EvolutionFamily(
    val nodes: List<EvolutionNode>,
    val edges: List<EvolutionEdge>,
    val isTransformation: Boolean = false,
    val sourceChainId: Int? = null,
) {
    val hasEvolution: Boolean get() = edges.isNotEmpty()
}

/**
 * Offline national families, not a statement that evolution exists inside Champions.
 * Methods vary across the main-series games. Missing exact form data returns null;
 * an explicitly recorded singleton returns a family with hasEvolution == false.
 * Mega forms show their original family, flagged as transformations, without an
 * invented permanent-evolution edge. All loading and graph lookup stay off the UI.
 */
class EvolutionRepository(context: Context) {
    private val assets = context.applicationContext.assets
    suspend fun family(pokemon: Pokemon, catalog: List<Pokemon>): EvolutionFamily? = withContext(Dispatchers.IO) {
        val families = load()
        val key = if (pokemon.isMega) pokemon.baseId else pokemon.id
        val recorded = families[key] ?: return@withContext null
        val identities = catalog.associateBy { it.id }
        // Fail closed when a future catalog no longer contains a recorded exact form.
        if (recorded.nodes.any { it.pokemonId !in identities }) return@withContext null
        recorded.copy(
            nodes = recorded.nodes.map { it.copy(name = identities.getValue(it.pokemonId).name) },
            isTransformation = pokemon.isMega,
        )
    }

    private suspend fun load(): Map<Int, EvolutionFamily> {
        cachedFamilies?.let { return it }
        return cacheMutex.withLock {
            cachedFamilies ?: decode(
                JSONObject(assets.open("evolution-families.json").bufferedReader().use { it.readText() })
            ).also { cachedFamilies = it }
        }
    }
    companion object {
        private val cacheMutex = Mutex()
        @Volatile private var cachedFamilies: Map<Int, EvolutionFamily>? = null
        internal fun decode(root: JSONObject): Map<Int, EvolutionFamily> {
            require(root.getInt("schemaVersion") == 1)
            val rows = root.getJSONArray("chains")
            val index = mutableMapOf<Int, EvolutionFamily>()
            repeat(rows.length()) { i ->
                val row = rows.getJSONObject(i)
                val rawNodes = row.getJSONArray("nodes")
                val nodes = (0 until rawNodes.length()).map { j ->
                    val n = rawNodes.getJSONObject(j)
                    EvolutionNode(n.getInt("pokemonId"), n.getInt("speciesId"), n.getString("name"), n.getInt("stage"))
                }
                val rawEdges = row.getJSONArray("edges")
                val edges = (0 until rawEdges.length()).map { j ->
                    val e = rawEdges.getJSONObject(j)
                    val alternatives = e.getJSONArray("conditions")
                    EvolutionEdge(e.getInt("fromId"), e.getInt("toId"), (0 until alternatives.length()).map { k ->
                        val c = alternatives.getJSONObject(k)
                        EvolutionCondition(c.getString("pt"), c.getString("en"), c.getString("es"))
                    })
                }
                require(nodes.map { it.pokemonId }.distinct().size == nodes.size) { "Duplicate evolution identity" }
                val ids = nodes.map { it.pokemonId }.toSet()
                require(edges.all { it.fromId in ids && it.toId in ids && it.fromId != it.toId })
                val family = EvolutionFamily(nodes, edges, sourceChainId = row.nullableInt("sourceChainId"))
                nodes.forEach { node ->
                    val previous = index[node.pokemonId]
                    // Some packaged records repeat a chain. Prefer the complete branch set.
                    val richerSameTopology = previous != null && family.edges.size == previous.edges.size &&
                        family.edges.map { it.fromId to it.toId }.toSet() == previous.edges.map { it.fromId to it.toId }.toSet() &&
                        family.edges.sumOf { edge -> edge.conditions.sumOf { it.en.length } } >
                        previous.edges.sumOf { edge -> edge.conditions.sumOf { it.en.length } }
                    if (previous == null || family.edges.size > previous.edges.size || richerSameTopology) index[node.pokemonId] = family
                }
            }
            return index.toMap()
        }
    }
}
