package dev.bloody.prismadex.data

data class Pokemon(
    val id: Int,
    val baseId: Int,
    val name: String,
    val types: List<String>,
    val stats: List<Int>,
    val abilities: List<String>,
    val height: Double?,
    val weight: Double?,
    val champions: Boolean,
    val isMega: Boolean,
    val generation: Int = 1,
    val games: List<Int> = emptyList(),
    val canonicalName:String?=null,
    val aliases:List<String> = emptyList(),
    val championsSource:String="",
)

data class Move(
    val id: Int, val name: String, val type: String, val category: String,
    val power: Int?, val accuracy: Int?, val pp: Int?,
    val description: String = "", val summary: String = "",
    val priority: Int = 0, val target: String = "normal",
    val flags: Set<String> = emptySet(), val referenceGame: Int = 25,
)

data class HeldItem(
    val id: Int, val name: String, val description: String,
    val compatibleSpecies: List<Int>? = null,
    val compatiblePokemonIds:List<Int>?=null,
    val icon: String? = null,
)

val StatKeys = listOf("hp", "atk", "def", "spa", "spd", "spe")
val NatureNames = listOf("Hardy", "Lonely", "Brave", "Adamant", "Naughty", "Bold", "Docile", "Relaxed", "Impish", "Lax", "Timid", "Hasty", "Serious", "Jolly", "Naive", "Modest", "Mild", "Quiet", "Bashful", "Rash", "Calm", "Gentle", "Sassy", "Careful", "Quirky")
data class Build(
    val itemId: Int? = null, val moves: List<Int> = emptyList(),
    val nature: String = "Serious", val ability: String? = null,
    val sps: List<Int> = List(6) { 0 },
)
data class TrainingDummy(val id: String, val label: String, val pokemonId: Int, val build: Build = Build())

data class SavedTeam(
    val id:String, val name:String, val gameId:Int=25,
    val members:List<Int> = emptyList(), val builds:Map<Int,Build> = emptyMap(),
    val format:String="doubles",
)

data class UserState(
    val favorites: Set<Int> = emptySet(),
    val teams:List<SavedTeam> = listOf(SavedTeam("default","Minha equipe")),
    val activeTeamId:String="default",
    val theme: String = "aurora",
    val layout: String = "list",
    val dummies: List<TrainingDummy> = emptyList(),
    val battleFormat: String = "doubles",
    val appearanceMode: String = "system",
    val teamView: String = "list",
    /** Badge tracker: gameId -> user-named earned badges, stored locally only.
     *  No official badge names/counts exist in the bundled data, so entries
     *  are named by the user until verified badge data is imported. */
    val badges: Map<Int, List<String>> = emptyMap(),
    /** Trainer card (Account tab): display name + chosen trainer sprite key
     *  (empty = no sprite, honest placeholder). Stored locally only. */
    val trainerName: String = "",
    val trainerSprite: String = "",
    /** Badge image sets: gameId -> earned badge indices (0..7) for games with
     *  a verified 8-image set. Kept alongside [badges] (user-named entries),
     *  which remains the fallback for games without an image set. */
    val badgeSets: Map<Int, Set<Int>> = emptyMap(),
){
    val activeTeam:SavedTeam get()=teams.first{it.id==activeTeamId}
    val team:List<Int> get()=activeTeam.members
    val builds:Map<Int,Build> get()=activeTeam.builds
    fun removeTeamMember(teamId:String,pokemonId:Int):UserState=copy(teams=teams.map{
        if(it.id==teamId)it.copy(members=it.members-pokemonId,builds=it.builds-pokemonId) else it
    })
    fun updateTeam(change:(SavedTeam)->SavedTeam)=copy(teams=teams.map{if(it.id==activeTeamId)change(it) else it})
}

data class GameOption(val id:Int,val name:String,val generation:Int)
val SupportedGames=listOf(
    GameOption(25,"Pokémon Champions",9),
    GameOption(1,"Red / Blue",1),GameOption(2,"Yellow",1),
    GameOption(3,"Gold / Silver",2),GameOption(4,"Crystal",2),
    GameOption(5,"Ruby / Sapphire",3),GameOption(6,"Emerald",3),GameOption(7,"FireRed / LeafGreen",3),
    GameOption(12,"Colosseum",3),GameOption(13,"XD: Gale of Darkness",3),
    GameOption(8,"Diamond / Pearl",4),GameOption(9,"Platinum",4),GameOption(10,"HeartGold / SoulSilver",4),
    GameOption(11,"Black / White",5),GameOption(14,"Black 2 / White 2",5),
    GameOption(15,"X / Y",6),GameOption(16,"Omega Ruby / Alpha Sapphire",6),
    GameOption(17,"Sun / Moon",7),GameOption(18,"Ultra Sun / Ultra Moon",7),GameOption(19,"Let's Go Pikachu / Eevee",7),
    GameOption(20,"Sword / Shield",8),GameOption(21,"Brilliant Diamond / Shining Pearl",8),GameOption(22,"Legends: Arceus",8),
    GameOption(23,"Scarlet / Violet",9),GameOption(24,"Legends: Z–A",9),
)
fun gameName(id:Int)=SupportedGames.firstOrNull{it.id==id}?.name?:"Jogo desconhecido"

/** Verified 8-badge image sets (00:11/00:17/00:19). Prefix = asset folder
 *  file stem in `assets/badges/<prefix>-<1..8>.png`. SteGriff CC BY 3.0
 *  recreations for Kanto-Unova; PokeAPI/sprites files 70-77 for Paldea.
 *  Kalos/Galar have NO verified set yet (recorded gap, do not substitute). */
data class BadgeSet(val prefix: String, val gameIds: Set<Int>)
val BadgeSets = listOf(
    BadgeSet("kanto", setOf(1, 2, 7, 19)),
    BadgeSet("johto", setOf(3, 4, 10)),
    BadgeSet("hoenn", setOf(5, 6, 16)),
    BadgeSet("sinnoh", setOf(8, 9, 21)),
    BadgeSet("unova", setOf(11, 14)),
    BadgeSet("paldea", setOf(23)),
)
val BadgeSetByGame: Map<Int, BadgeSet> = BadgeSets.flatMap { s -> s.gameIds.map { it to s } }.toMap()
const val BadgeSetSize = 8

/** Trainer sprite keys: static PNGs in `assets/trainers/<key>.png` from the
 *  Showdown trainers listing, imported minimal-only under the user's
 *  accepted IP risk (00:07/00:08). Exact filenames that do not exist
 *  (leaf, florian, juliana, nemona, arven) are gaps, not aliases. */
val TrainerSpriteKeys = listOf(
    "red", "blue", "green", "ethan", "lyra", "kris", "silver",
    "brendan", "may", "wally", "lucas", "dawn", "barry",
    "hilbert", "hilda", "nate", "rosa", "calem", "serena", "shauna",
    "elio", "selene", "hau", "chase", "elaine",
    "victor", "gloria", "hop", "rei", "akari", "penny", "kieran",
)
fun Pokemon.availableIn(game:Int)=if(game==25)champions else game in games
