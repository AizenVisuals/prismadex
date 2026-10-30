package dev.bloody.prismadex.data

/** Modern 18-type chart, matching bundled Smogon calc CHAMPIONS = SS (MIT).
 * Pure typing only: no abilities, held items, weather, historical charts or move exceptions.
 */
val AnalysisTypes = listOf("normal","grass","fire","water","electric","ice","flying","bug","poison","ground","rock","fighting","psychic","ghost","dragon","dark","steel","fairy")
private val typeChart = mapOf(
        "normal" to listOf(1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,0.5,1.0,1.0,0.0,1.0,1.0,0.5,1.0),
        "grass" to listOf(1.0,0.5,0.5,2.0,1.0,1.0,0.5,0.5,0.5,2.0,2.0,1.0,1.0,1.0,0.5,1.0,0.5,1.0),
        "fire" to listOf(1.0,2.0,0.5,0.5,1.0,2.0,1.0,2.0,1.0,1.0,0.5,1.0,1.0,1.0,0.5,1.0,2.0,1.0),
        "water" to listOf(1.0,0.5,2.0,0.5,1.0,1.0,1.0,1.0,1.0,2.0,2.0,1.0,1.0,1.0,0.5,1.0,1.0,1.0),
        "electric" to listOf(1.0,0.5,1.0,2.0,0.5,1.0,2.0,1.0,1.0,0.0,1.0,1.0,1.0,1.0,0.5,1.0,1.0,1.0),
        "ice" to listOf(1.0,2.0,0.5,0.5,1.0,0.5,2.0,1.0,1.0,2.0,1.0,1.0,1.0,1.0,2.0,1.0,0.5,1.0),
        "flying" to listOf(1.0,2.0,1.0,1.0,0.5,1.0,1.0,2.0,1.0,1.0,0.5,2.0,1.0,1.0,1.0,1.0,0.5,1.0),
        "bug" to listOf(1.0,2.0,0.5,1.0,1.0,1.0,0.5,1.0,0.5,1.0,1.0,0.5,2.0,0.5,1.0,2.0,0.5,0.5),
        "poison" to listOf(1.0,2.0,1.0,1.0,1.0,1.0,1.0,1.0,0.5,0.5,0.5,1.0,1.0,0.5,1.0,1.0,0.0,2.0),
        "ground" to listOf(1.0,0.5,2.0,1.0,2.0,1.0,0.0,0.5,2.0,1.0,2.0,1.0,1.0,1.0,1.0,1.0,2.0,1.0),
        "rock" to listOf(1.0,1.0,2.0,1.0,1.0,2.0,2.0,2.0,1.0,0.5,1.0,0.5,1.0,1.0,1.0,1.0,0.5,1.0),
        "fighting" to listOf(2.0,1.0,1.0,1.0,1.0,2.0,0.5,0.5,0.5,1.0,2.0,1.0,0.5,0.0,1.0,2.0,2.0,0.5),
        "psychic" to listOf(1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,2.0,1.0,1.0,2.0,0.5,1.0,1.0,0.0,0.5,1.0),
        "ghost" to listOf(0.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,2.0,2.0,1.0,0.5,1.0,1.0),
        "dragon" to listOf(1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,2.0,1.0,0.5,0.0),
        "dark" to listOf(1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,0.5,2.0,2.0,1.0,0.5,1.0,0.5),
        "steel" to listOf(1.0,1.0,0.5,0.5,0.5,2.0,1.0,1.0,1.0,1.0,2.0,1.0,1.0,1.0,1.0,1.0,0.5,2.0),
        "fairy" to listOf(1.0,1.0,0.5,1.0,1.0,1.0,1.0,1.0,0.5,1.0,1.0,2.0,1.0,1.0,2.0,2.0,0.5,1.0),
)

fun typeMultiplier(attack:String,defense:List<String>):Double {
    require(attack in AnalysisTypes) { "Unknown attack type: $attack" }
    require(defense.size in 1..2 && defense.distinct().size==defense.size && defense.all{it in AnalysisTypes}) { "Expected one or two distinct known defensive types" }
    return defense.fold(1.0){value,type->value*typeChart.getValue(attack)[AnalysisTypes.indexOf(type)]}
}

data class MemberTypeMatchup(val pokemonId:Int,val multiplier:Double)
data class TeamTypeMatchup(val type:String,val members:List<MemberTypeMatchup>){
    val weakCount get()=members.count{it.multiplier>1.0}
    val resistantCount get()=members.count{it.multiplier>0.0&&it.multiplier<1.0}
    val immuneCount get()=members.count{it.multiplier==0.0}
    val neutralCount get()=members.count{it.multiplier==1.0}
    val hasDefensiveAnswer get()=resistantCount+immuneCount>0
}
data class OffensiveTypeCoverage(val defendingType:String,val bestMultiplier:Double?,val superEffectiveMoveIds:List<Int>)
data class OffensivePokemonCoverage(val pokemonId:Int,val bestMultiplier:Double?,val superEffectiveMoveIds:List<Int>)
object TypeAnalysis {
    fun team(members:List<Pokemon>):List<TeamTypeMatchup> = AnalysisTypes.map { type ->
        TeamTypeMatchup(type,members.map{MemberTypeMatchup(it.id,typeMultiplier(type,it.types))})
    }
    private fun damaging(moves:List<Move>)=moves.distinctBy{it.id}.filter{it.category in listOf("physical","special")&&it.type in AnalysisTypes}
    /** This reports type matchups, not damage. Status moves do not provide damaging coverage. */
    fun offense(moves:List<Move>):List<OffensiveTypeCoverage>{
        val attacks=damaging(moves)
        return AnalysisTypes.map{type->OffensiveTypeCoverage(type,attacks.maxOfOrNull{typeMultiplier(it.type,listOf(type))},attacks.filter{typeMultiplier(it.type,listOf(type))>1.0}.map{it.id})}
    }
    fun offenseAgainst(moves:List<Move>,targets:List<Pokemon>):List<OffensivePokemonCoverage>{
        val attacks=damaging(moves)
        return targets.map{target->OffensivePokemonCoverage(target.id,attacks.maxOfOrNull{typeMultiplier(it.type,target.types)},attacks.filter{typeMultiplier(it.type,target.types)>1.0}.map{it.id})}
    }
}

/** Presentation grouping only: the multipliers come from [TypeAnalysis.team] unchanged.
 *  Single-Pokémon groups are ordered worst-first (4x, 2x, 1/2x, 1/4x, 0x) with neutral last;
 *  types inside a group keep [AnalysisTypes] order. Empty groups are omitted. */
data class SingleEffectivenessGroup(val multiplier:Double,val types:List<String>)
enum class TeamSectionKind { SHARED_WEAKNESS, WEAKNESS, NO_WEAKNESS }
data class TeamEffectivenessSection(val kind:TeamSectionKind,val rows:List<TeamTypeMatchup>)
object EffectivenessGrouping {
    private val singleOrder=listOf(4.0,2.0,0.5,0.25,0.0,1.0)
    fun singleGroups(pokemon:Pokemon):List<SingleEffectivenessGroup>{
        val rows=TypeAnalysis.team(listOf(pokemon))
        return singleOrder.mapNotNull{multiplier->
            val types=rows.filter{it.members.single().multiplier==multiplier}.map{it.type}
            if(types.isEmpty())null else SingleEffectivenessGroup(multiplier,types)
        }
    }
    fun teamSections(rows:List<TeamTypeMatchup>):List<TeamEffectivenessSection>{
        val sections=listOf(
            TeamEffectivenessSection(TeamSectionKind.SHARED_WEAKNESS,rows.filter{it.weakCount>=3}),
            TeamEffectivenessSection(TeamSectionKind.WEAKNESS,rows.filter{it.weakCount in 1..2}),
            TeamEffectivenessSection(TeamSectionKind.NO_WEAKNESS,rows.filter{it.weakCount==0})
        )
        return sections.filter{it.rows.isNotEmpty()}
    }
}
// Backwards-compatible accessors on the existing object; grouping lives in EffectivenessGrouping.
fun TypeAnalysis.singleGroups(pokemon:Pokemon)=EffectivenessGrouping.singleGroups(pokemon)
fun TypeAnalysis.teamSections(rows:List<TeamTypeMatchup>)=EffectivenessGrouping.teamSections(rows)
