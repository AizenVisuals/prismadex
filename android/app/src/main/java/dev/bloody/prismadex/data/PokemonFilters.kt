package dev.bloody.prismadex.data

import android.content.Context
import org.json.JSONObject

enum class PokemonSort { NUMBER, NAME, GENERATION, HP, ATTACK, DEFENSE, SPECIAL_ATTACK, SPECIAL_DEFENSE, SPEED }
data class PokemonFilter(val query:String="",val types:Set<String> = emptySet(),val allTypes:Boolean=true,val generation:Int=0,val ability:String?=null,val includeForms:Boolean=true,val sort:PokemonSort=PokemonSort.NUMBER,val megaOnly:Boolean=false,val gigantamaxOnly:Boolean=false,val alternateFormsOnly:Boolean=false)

/**
 * Regional Pokédex order, loaded once from the bundled `regional-dex.json`
 * (PokeAPI pokedex entry numbers, pinned CSV commit — see
 * research/REGIONAL-DEX-SOURCES-2026-10-30.md). Map: gameId -> species ids
 * in regional entry order. Games without a regional dex (Nacional mode,
 * Colosseum/XD, Champions) are absent and keep national order. Any load
 * failure falls back to an empty map (national order everywhere) rather
 * than crashing the list.
 */
internal object RegionalDexData {
    @Volatile private var cached: Map<Int, List<Int>>? = null
    fun get(context: Context): Map<Int, List<Int>> {
        cached?.let { return it }
        val loaded = runCatching {
            val root = JSONObject(context.assets.open("regional-dex.json").bufferedReader().use { it.readText() })
            val games = root.getJSONObject("games")
            val out = mutableMapOf<Int, List<Int>>()
            val keys = games.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val arr = games.getJSONArray(k)
                out[k.toInt()] = (0 until arr.length()).map { arr.getInt(it) }
            }
            out.toMap()
        }.getOrDefault(emptyMap())
        cached = loaded
        return loaded
    }
}

/** Index normalized names once, rather than repeating Unicode/regex work on every keystroke. */
class PokemonFilterIndex(catalog:List<Pokemon>, regionalDex: Map<Int, List<Int>> = emptyMap()) {
    private data class Entry(val pokemon:Pokemon,val names:List<String>)
    private val entries=catalog.map{Entry(it,(listOf(it.name)+it.aliases+listOfNotNull(it.canonicalName)).map(::pokemonNameKey))}
    /** Regional entry position per game: speciesId -> 0-based entry order. */
    private val regionalRank: Map<Int, Map<Int, Int>> = regionalDex.mapValues { (_, ids) ->
        ids.withIndex().associate { (index, speciesId) -> speciesId to index }
    }
    fun select(game:Int,filter:PokemonFilter):List<Pokemon>{
        val query=pokemonNameKey(filter.query)
        val number=filter.query.trim().removePrefix("#").toIntOrNull()
        val candidates=entries.asSequence().filter{e->val p=e.pokemon
            (game==0||p.availableIn(game))&&(filter.includeForms||p.id==p.baseId)&&
            (filter.generation==0||p.generation==filter.generation)&&(!filter.megaOnly&&!filter.gigantamaxOnly&&!filter.alternateFormsOnly||(filter.megaOnly&&p.isMega)||(filter.gigantamaxOnly&&p.isGigantamax())||(filter.alternateFormsOnly&&p.isAlternateForm()))&&
            (filter.ability==null||filter.ability in p.abilities)&&
            (filter.types.isEmpty()||if(filter.allTypes)p.types.containsAll(filter.types) else p.types.any{it in filter.types})&&
            (query.isBlank()||p.baseId==number||e.names.any{it.contains(query)})
        }.map{it.pokemon}.toList()
        val tie=compareBy<Pokemon>({it.baseId},{it.id})
        // Regional-first (11:56): with a game selected and the default NUMBER
        // sort, the game's regional Pokédex comes first in regional entry
        // order (forms follow their species' entry), then every other
        // available Pokémon in national order. Transfer/old-game Pokémon
        // are NOT excluded — they form the national block. An explicit sort
        // (name, stats, generation) always wins over regional order, and
        // games without a regional dex keep plain national order.
        val ranks = if (filter.sort == PokemonSort.NUMBER) regionalRank[game] else null
        val comparator=when(filter.sort){
            PokemonSort.NUMBER->if (ranks != null) compareBy<Pokemon>({ ranks[it.baseId] ?: Int.MAX_VALUE },{it.baseId},{it.id}) else tie
            PokemonSort.NAME->compareBy<Pokemon>{it.name}.then(tie)
            PokemonSort.GENERATION->compareBy<Pokemon>{it.generation}.then(tie)
            else->{val stat=filter.sort.ordinal-PokemonSort.HP.ordinal;compareByDescending<Pokemon>{it.stats.getOrElse(stat){0}}.then(tie)}
        }
        return candidates.sortedWith(comparator)
    }
}
