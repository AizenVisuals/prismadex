package dev.bloody.prismadex.data

import org.junit.Assert.*
import org.junit.Test

class TeamRemovalTest{
    @Test fun removalOnlyChangesTheRequestedTeamAndClearsItsSet(){
        val first=SavedTeam("a","A",25,listOf(6,9),mapOf(6 to Build(moves=listOf(53))))
        val second=SavedTeam("b","B",25,listOf(6),mapOf(6 to Build(moves=listOf(7))))
        val original=UserState(favorites=setOf(6),teams=listOf(first,second),activeTeamId="b")
        val removed=original.removeTeamMember("a",6)
        assertEquals(listOf(9),removed.teams[0].members)
        assertFalse(removed.teams[0].builds.containsKey(6))
        assertEquals(second,removed.activeTeam)
        assertEquals(original.favorites,removed.favorites)
        assertEquals(original.dummies,removed.dummies)
        assertEquals(original,original.removeTeamMember("missing",6))
        assertEquals(removed,removed.removeTeamMember("a",6))
    }
}
