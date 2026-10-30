package dev.bloody.prismadex

import dev.bloody.prismadex.data.Build
import dev.bloody.prismadex.ui.cardAbilityLabel
import dev.bloody.prismadex.ui.cardLevelLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareTeamCardTest {
    @Test fun championsLevelIsFixed50() {
        assertEquals("Nv. 50", cardLevelLabel(25))
    }

    @Test fun otherGamesHaveNoStoredLevel() {
        assertNull(cardLevelLabel(23))
        assertNull(cardLevelLabel(0))
    }

    @Test fun abilityUsesOnlyTheChosenBuildAbility() {
        assertEquals("Thick Fat", cardAbilityLabel(Build(ability = "thick-fat")))
        assertEquals("Leaf Guard", cardAbilityLabel(Build(ability = "leaf-guard")))
    }

    @Test fun missingAbilityRendersEmptyNotAGuess() {
        assertNull(cardAbilityLabel(Build()))
        assertNull(cardAbilityLabel(Build(ability = "")))
        assertNull(cardAbilityLabel(null))
    }
}
