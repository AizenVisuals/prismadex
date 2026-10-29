package dev.bloody.prismadex.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ProfileText(val pt: String, val en: String, val es: String) {
    fun text(language: String): String = when (language.substringBefore('-').substringBefore('_')) {
        "pt" -> pt
        "es" -> es
        else -> en
    }
}
data class AbilityProfile(
    val id: Int?, val name: String, val slug: String, val description: ProfileText?,
    val effectSource: String?, val championsSpecific: Boolean = false,
)
data class SpeciesAbility(
    val id: Int, val name: String, val slug: String, val slot: Int,
    val isHidden: Boolean?, val description: ProfileText?,
)
data class SpeciesProfile(
    val pokemonId: Int, val speciesId: Int, val pokeapiPokemonId: Int?, val formResolved: Boolean,
    val category: ProfileText?, val eggGroups: List<ProfileText>, val genderRateEighths: Int?,
    val growthRate: ProfileText?, val captureRate: Int?, val baseExperience: Int?,
    val hatchCounter: Int?, val baseHappiness: Int?, val isBaby: Boolean,
    val isLegendary: Boolean, val isMythical: Boolean, val hasGenderDifferences: Boolean,
    val abilities: List<SpeciesAbility>,
    val heightMeters: Double? = null, val weightKg: Double? = null, val pokeapiFormId: Int? = null, val formGender: String? = null,
)

/** National reference, separate from current game/Champions build legality and effects. */
class SpeciesProfileRepository(context: Context) {
    private val assets = context.applicationContext.assets
    private data class Snapshot(val profiles: Map<Int, SpeciesProfile>, val abilities: Map<String, AbilityProfile>)
    suspend fun profile(pokemon: Pokemon): SpeciesProfile? = withContext(Dispatchers.IO) {
        load().profiles[pokemon.id]
    }
    suspend fun ability(name: String): AbilityProfile? = withContext(Dispatchers.IO) {
        load().abilities[abilityKey(name)]
    }
    private suspend fun load(): Snapshot {
        cache?.let { return it }
        return mutex.withLock {
            cache ?: decode(
                JSONObject(assets.open("species-profiles.json").bufferedReader().use { it.readText() }),
                JSONObject(assets.open("ability-profiles.json").bufferedReader().use { it.readText() }),
            ).also { cache = it }
        }
    }
    companion object {
        private val mutex = Mutex()
        @Volatile private var cache: Snapshot? = null
        private fun abilityKey(name: String) = name.lowercase(java.util.Locale.ROOT).filter { it.isLetterOrDigit() }
        private fun JSONObject.localized(key: String): ProfileText? = optJSONObject(key)?.let {
            ProfileText(it.getString("pt"), it.getString("en"), it.getString("es"))
        }
        private fun decode(root: JSONObject, abilityRoot: JSONObject): Snapshot {
            require(root.getInt("schemaVersion") == 1 && abilityRoot.getInt("schemaVersion") == 1)
            val abilities = mutableMapOf<Int, AbilityProfile>()
            val lookup = mutableMapOf<String, AbilityProfile>()
            val rawAbilities = abilityRoot.getJSONObject("abilities")
            rawAbilities.keys().forEach { key ->
                val row = rawAbilities.getJSONObject(key)
                val ability = AbilityProfile(row.nullableInt("id"), row.getString("name"), row.getString("slug"), row.localized("description"), row.optString("effectSource").takeUnless { it == "null" }, row.optBoolean("championsSpecific"))
                ability.id?.let { abilities[it] = ability }
                lookup[abilityKey(ability.name)] = ability
                lookup[abilityKey(ability.slug)] = ability
            }
            val rawProfiles = root.getJSONObject("profiles")
            val profiles = rawProfiles.keys().asSequence().associate { key ->
                val r = rawProfiles.getJSONObject(key)
                val eggs = r.getJSONArray("eggGroups")
                val rawRelations = r.getJSONArray("abilities")
                val profile = SpeciesProfile(
                    r.getInt("pokemonId"), r.getInt("speciesId"), r.nullableInt("pokeapiPokemonId"), r.getBoolean("formResolved"),
                    r.localized("category"), (0 until eggs.length()).map { i -> eggs.getJSONObject(i).let { ProfileText(it.getString("pt"), it.getString("en"), it.getString("es")) } },
                    r.nullableInt("genderRateEighths"), r.localized("growthRate"), r.nullableInt("captureRate"),
                    r.nullableInt("baseExperience"), r.nullableInt("hatchCounter"), r.nullableInt("baseHappiness"),
                    r.getBoolean("isBaby"), r.getBoolean("isLegendary"), r.getBoolean("isMythical"), r.getBoolean("hasGenderDifferences"),
                    (0 until rawRelations.length()).map { i ->
                        val relation = rawRelations.getJSONObject(i)
                        val ability = requireNotNull(abilities[relation.getInt("id")]) { "Unresolved national ability" }
                        SpeciesAbility(relation.getInt("id"), ability.name, ability.slug, relation.getInt("slot"),
                            if (relation.isNull("isHidden")) null else relation.getBoolean("isHidden"), ability.description)
                    },
                    r.optDouble("heightMeters").takeIf { !r.isNull("heightMeters") && it.isFinite() },
                    r.optDouble("weightKg").takeIf { !r.isNull("weightKg") && it.isFinite() },
                    r.nullableInt("pokeapiFormId"),
                    r.optString("formGender").takeIf { it == "female" || it == "male" },
                )
                require(profile.pokemonId.toString() == key)
                require(profile.genderRateEighths == null || profile.genderRateEighths in -1..8)
                profile.pokemonId to profile
            }
            return Snapshot(profiles, lookup)
        }
    }
}
