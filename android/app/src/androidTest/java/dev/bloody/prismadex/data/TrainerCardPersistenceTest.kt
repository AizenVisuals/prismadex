package dev.bloody.prismadex.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TrainerCardPersistenceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val teamA = SavedTeam("a", "Equipe A", 1, listOf(25, 6))
    private val teamB = SavedTeam("b", "Equipe B", 23, listOf(1))

    @Test
    fun trainerCardTeamIdSurvivesEncodeDecodeRoundTrip() {
        val state = UserState(teams = listOf(teamA, teamB), activeTeamId = "a", trainerCardTeamId = "b")
        val decoded = Store.decode(Store.encode(state))
        assertEquals("b", decoded.trainerCardTeamId)
        assertEquals(teamB, decoded.cardTeam)
    }

    @Test
    fun decodeFallsBackWhenCardTeamIdIsUnknownOrMissing() {
        val state = UserState(teams = listOf(teamA, teamB), activeTeamId = "a", trainerCardTeamId = "missing")
        val decoded = Store.decode(Store.encode(state))
        assertEquals("", decoded.trainerCardTeamId)
        assertEquals(teamA, decoded.cardTeam)
        val legacy = Store.decode(JSONObject(Store.encode(state).toString()).apply { remove("trainerCardTeamId") })
        assertEquals("", legacy.trainerCardTeamId)
        assertEquals(teamA, legacy.cardTeam)
    }

    @Test
    fun trainerCardFieldsSurviveSaveAndLoad() {
        val name = "trainer-card-${UUID.randomUUID()}"
        val store = Store(context, name)
        try {
            val state = UserState(
                teams = listOf(SavedTeam("g", "Grade", 23, listOf(25))),
                activeTeamId = "g",
                trainerName = "Teste",
                trainerSprite = "red",
                trainerCardTeamId = "g",
                badgeSets = mapOf(23 to setOf(0, 3)),
            )
            store.save(state)
            val second = Store(context, name)
            val loaded = second.load()
            second.close()
            assertEquals("Teste", loaded.trainerName)
            assertEquals("red", loaded.trainerSprite)
            assertEquals("g", loaded.trainerCardTeamId)
            assertEquals(setOf(0, 3), loaded.badgeSets[23])
            assertEquals("Grade", loaded.cardTeam.name)
        } finally {
            store.close()
        }
    }
}
