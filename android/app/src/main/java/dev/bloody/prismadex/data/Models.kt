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
)

data class HeldItem(
    val id: Int, val name: String, val description: String,
    val compatibleSpecies: List<Int>? = null,
    val compatiblePokemonIds:List<Int>?=null,
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
    val layout: String = "grid",
    val dummies: List<TrainingDummy> = emptyList(),
    val battleFormat: String = "doubles",
    val appearanceMode: String = "system",
){
    val activeTeam:SavedTeam get()=teams.first{it.id==activeTeamId}
    val team:List<Int> get()=activeTeam.members
    val builds:Map<Int,Build> get()=activeTeam.builds
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
fun Pokemon.availableIn(game:Int)=if(game==25)champions else game in games
