package dev.bloody.prismadex.data

import org.junit.Assert.*
import org.junit.Test

// Build.shiny only: the org.json encode/decode round-trip cannot run on the
// JVM unit-test classpath (stubbed org.json), so persistence is covered by
// the instrumented suite on the device; encode/decode mirror the existing
// gender field in the same functions.
class ShinyBuildTest {
    @Test fun shinyDefaultsToStandard() {
        assertFalse(Build().shiny)
        assertEquals(Build(), Build(shiny = false))
    }
    @Test fun shinyChoiceIsKeptOnCopy() {
        val build = Build(moves = listOf(1), shiny = true)
        assertTrue(build.copy(nature = "Timid").shiny)
        assertFalse(build.copy(shiny = false).shiny)
    }
    @Test fun shinyFlagLivesOnTheTeamMemberBuild() {
        val team = SavedTeam("t", "T", 25, listOf(25), mapOf(25 to Build(shiny = true)))
        assertTrue(team.builds[25]?.shiny == true)
        val state = UserState(teams = listOf(team), activeTeamId = "t")
        assertTrue(state.removeTeamMember("t", 25).teams.first().builds[25] == null)
    }
}
