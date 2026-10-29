package dev.bloody.prismadex.data

import java.text.Normalizer
import java.util.Locale

/** Explicit retired IDs; distinct cosmetic, sex and battle forms are never merged by artwork. */
val RetiredPokemonIds=mapOf(20101 to 200401,20102 to 200402,20103 to 200403,20104 to 20010801,20105 to 200301,20106 to 200302,20107 to 200303,20108 to 200304,20109 to 200413,20110 to 200414,20111 to 200415,20112 to 200412,20113 to 200411)
fun canonicalPokemonId(id:Int)=RetiredPokemonIds[id]?:id
fun pokemonNameKey(value:String)=Normalizer.normalize(value,Normalizer.Form.NFD).replace(Regex("\\p{M}"),"").lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"),"")
fun Pokemon.matchesQuery(query:String):Boolean{
    if(query.isBlank())return true
    if(baseId.toString()==query.trim().trimStart('0'))return true
    val key=pokemonNameKey(query)
    return key.isNotEmpty()&&(listOf(name)+aliases+listOfNotNull(canonicalName)).any{pokemonNameKey(it).contains(key)}
}
/** Exact identity matching for sources. A female form must never fall back to the male by substring. */
fun resolvePokemonIdentity(catalog:List<Pokemon>,name:String):Pokemon?{
    val key=pokemonNameKey(name)
    return catalog.firstOrNull{p->p.canonicalName?.let(::pokemonNameKey)==key}
        ?:catalog.firstOrNull{p->p.aliases.any{pokemonNameKey(it)==key}}
        ?:catalog.firstOrNull{pokemonNameKey(it.name)==key}
}

/** Same exact-match precedence as resolvePokemonIdentity, built once for bulk source lookups. */
class PokemonIdentityIndex(catalog: List<Pokemon>) {
    private val byName = buildMap<String, Pokemon> {
        // putIfAbsent retains catalog ordering, including ambiguous aliases.
        catalog.forEach { putIfAbsent(pokemonNameKey(it.name), it) }
    }.toMutableMap().apply {
        val aliases = mutableMapOf<String, Pokemon>()
        catalog.forEach { p -> p.aliases.forEach { aliases.putIfAbsent(pokemonNameKey(it), p) } }
        putAll(aliases)
        val canonical = mutableMapOf<String, Pokemon>()
        catalog.forEach { p -> p.canonicalName?.let { canonical.putIfAbsent(pokemonNameKey(it), p) } }
        putAll(canonical)
    }
    fun resolve(name: String): Pokemon? = byName[pokemonNameKey(name)]
}
