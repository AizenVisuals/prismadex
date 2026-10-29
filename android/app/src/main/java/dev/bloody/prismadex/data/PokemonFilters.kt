package dev.bloody.prismadex.data

enum class PokemonSort { NUMBER, NAME, GENERATION, HP, ATTACK, DEFENSE, SPECIAL_ATTACK, SPECIAL_DEFENSE, SPEED }
data class PokemonFilter(val query:String="",val types:Set<String> = emptySet(),val allTypes:Boolean=true,val generation:Int=0,val ability:String?=null,val includeForms:Boolean=true,val sort:PokemonSort=PokemonSort.NUMBER)

/** Index normalized names once, rather than repeating Unicode/regex work on every keystroke. */
class PokemonFilterIndex(catalog:List<Pokemon>) {
    private data class Entry(val pokemon:Pokemon,val names:List<String>)
    private val entries=catalog.map{Entry(it,(listOf(it.name)+it.aliases+listOfNotNull(it.canonicalName)).map(::pokemonNameKey))}
    fun select(game:Int,filter:PokemonFilter):List<Pokemon>{
        val query=pokemonNameKey(filter.query)
        val number=filter.query.trim().removePrefix("#").toIntOrNull()
        val candidates=entries.asSequence().filter{e->val p=e.pokemon
            (game==0||p.availableIn(game))&&(filter.includeForms||p.id==p.baseId)&&
            (filter.generation==0||p.generation==filter.generation)&&
            (filter.ability==null||filter.ability in p.abilities)&&
            (filter.types.isEmpty()||if(filter.allTypes)p.types.containsAll(filter.types) else p.types.any{it in filter.types})&&
            (query.isBlank()||p.baseId==number||e.names.any{it.contains(query)})
        }.map{it.pokemon}.toList()
        val tie=compareBy<Pokemon>({it.baseId},{it.id})
        val comparator=when(filter.sort){
            PokemonSort.NUMBER->tie
            PokemonSort.NAME->compareBy<Pokemon>{it.name}.then(tie)
            PokemonSort.GENERATION->compareBy<Pokemon>{it.generation}.then(tie)
            else->{val stat=filter.sort.ordinal-PokemonSort.HP.ordinal;compareByDescending<Pokemon>{it.stats.getOrElse(stat){0}}.then(tie)}
        }
        return candidates.sortedWith(comparator)
    }
}
