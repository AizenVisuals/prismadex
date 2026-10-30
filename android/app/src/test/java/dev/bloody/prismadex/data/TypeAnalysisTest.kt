package dev.bloody.prismadex.data

import org.junit.Assert.*
import org.junit.Test

class TypeAnalysisTest {
    private fun pokemon(id:Int,vararg types:String)=Pokemon(id,id,"P$id",types.toList(),List(6){50},emptyList(),null,null,true,false)
    private fun move(id:Int,type:String,category:String="physical",power:Int?=80)=Move(id,"Move$id",type,category,power,100,10)
    @Test fun dualTypingMultipliesAndImmunityWins(){
        assertEquals(4.0,typeMultiplier("rock",listOf("fire","flying")),0.0)
        assertEquals(.25,typeMultiplier("grass",listOf("fire","flying")),0.0)
        assertEquals(0.0,typeMultiplier("electric",listOf("water","ground")),0.0)
        assertEquals(1.0,typeMultiplier("ice",listOf("water","flying")),0.0)
        assertEquals(0.0,typeMultiplier("dragon",listOf("fairy")),0.0)
        assertEquals(1.0,typeMultiplier("ghost",listOf("steel")),0.0)
    }
    @Test fun teamShowsIndividualMultipliersInsteadOfAddingThem(){
        val team=listOf(pokemon(1,"fire","flying"),pokemon(2,"fire"),pokemon(3,"ice"),pokemon(4,"bug"),pokemon(5,"ground"),pokemon(6,"steel"))
        val rock=TypeAnalysis.team(team).single{it.type=="rock"}
        assertEquals(4,rock.weakCount);assertEquals(2,rock.resistantCount);assertEquals(0,rock.immuneCount)
        assertEquals(4.0,rock.members.single{it.pokemonId==1}.multiplier,0.0)
        assertTrue(rock.hasDefensiveAnswer)
        val electric=TypeAnalysis.team(listOf(pokemon(1,"water"),pokemon(2,"water"))).single{it.type=="electric"}
        assertFalse(electric.hasDefensiveAnswer)
        assertEquals(2,electric.weakCount)
    }
    @Test fun offensiveCoverageExcludesStatusAndKeepsUnknownCoverageDistinct(){
        val moves=listOf(move(1,"fire"),move(2,"electric","status",null))
        assertTrue(TypeAnalysis.offense(moves).single{it.defendingType=="grass"}.superEffectiveMoveIds.contains(1))
        assertTrue(TypeAnalysis.offense(moves).single{it.defendingType=="water"}.superEffectiveMoveIds.isEmpty())
        assertNull(TypeAnalysis.offense(emptyList()).first().bestMultiplier)
        val matchup=TypeAnalysis.offenseAgainst(listOf(move(3,"ice")),listOf(pokemon(4,"dragon","flying"))).single()
        assertEquals(4.0,matchup.bestMultiplier!!,0.0)
        assertEquals(listOf(3),matchup.superEffectiveMoveIds)
    }
    @Test fun allModernTypePairsAndEmptyTeamsAreWellFormed(){
        assertEquals(18,AnalysisTypes.size)
        AnalysisTypes.forEach{a->AnalysisTypes.forEach{d->assertTrue(typeMultiplier(a,listOf(d)) in setOf(0.0,.5,1.0,2.0))}}
        assertTrue(TypeAnalysis.team(emptyList()).all{it.members.isEmpty()&&!it.hasDefensiveAnswer})
    }
    @Test(expected=IllegalArgumentException::class) fun unknownTypeDoesNotSilentlyBecomeNeutral(){typeMultiplier("mystery",listOf("fire"))}
    @Test fun singleGroupsOrderWorstFirstAndCoverAllTypesOnce(){
        val groups=TypeAnalysis.singleGroups(pokemon(722,"grass","flying"))
        assertEquals(listOf(4.0,2.0,0.5,0.25,0.0,1.0),groups.map{it.multiplier})
        assertEquals(listOf("ice"),groups.single{it.multiplier==4.0}.types)
        assertEquals(listOf("fire","flying","poison","rock"),groups.single{it.multiplier==2.0}.types)
        assertEquals(listOf("water","fighting"),groups.single{it.multiplier==0.5}.types)
        assertEquals(listOf("grass","steel"),groups.single{it.multiplier==0.25}.types)
        assertEquals(listOf("ground"),groups.single{it.multiplier==0.0}.types)
        assertEquals(18,groups.sumOf{it.types.size})
        assertEquals(18,groups.flatMap{it.types}.distinct().size)
        groups.forEach{g->g.types.forEach{t->assertEquals(g.multiplier,typeMultiplier(t,listOf("grass","flying")),0.0)}}
    }
    @Test fun teamSectionsPartitionRowsBySharedWeakness(){
        val team=listOf(pokemon(1,"fire","flying"),pokemon(2,"fire"),pokemon(3,"ice"),pokemon(4,"bug"),pokemon(5,"ground"),pokemon(6,"steel"))
        val sections=TypeAnalysis.teamSections(TypeAnalysis.team(team))
        assertEquals(listOf(TeamSectionKind.SHARED_WEAKNESS,TeamSectionKind.WEAKNESS,TeamSectionKind.NO_WEAKNESS),sections.map{it.kind})
        assertTrue(sections.single{it.kind==TeamSectionKind.SHARED_WEAKNESS}.rows.any{it.type=="rock"&&it.weakCount==4})
        assertEquals(18,sections.sumOf{it.rows.size})
        assertEquals(18,sections.flatMap{it.rows}.map{it.type}.distinct().size)
        assertTrue(sections.single{it.kind==TeamSectionKind.SHARED_WEAKNESS}.rows.all{it.weakCount>=3})
        assertTrue(sections.single{it.kind==TeamSectionKind.WEAKNESS}.rows.all{it.weakCount in 1..2})
        assertTrue(sections.single{it.kind==TeamSectionKind.NO_WEAKNESS}.rows.all{it.weakCount==0})
    }
}
