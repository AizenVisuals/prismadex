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

/** Gigantamax forms are the 33 catalog records whose display name starts with "Gigantamax " (verified in data.json, 30/10/2026); the bundled data has no isGigantamax flag and no "gmax" canonical name. */
fun Pokemon.isGigantamax(): Boolean = name.startsWith("Gigantamax ")
/** Alternate form = a non-base record that is neither a Mega nor a Gigantamax form. */
fun Pokemon.isAlternateForm(): Boolean = id != baseId && !isMega && !isGigantamax()

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
    /** Per-member gender chosen by the user ("male" / "female"); null = not set, renders empty on the share card. Never derived from species ratios. */
    val gender: String? = null,
    /** Per-member shiny choice (10:52 rule): true only when the shiny star was active on the detail at add time and a real shiny asset exists; false = standard. Old saves decode to false. */
    val shiny: Boolean = false,
)
data class TrainingDummy(val id: String, val label: String, val pokemonId: Int, val build: Build = Build())

data class SavedTeam(
    val id:String, val name:String, val gameId:Int=25,
    val members:List<Int> = emptyList(), val builds:Map<Int,Build> = emptyMap(),
    val format:String="doubles",
)

/** Launcher icon keys selectable in Settings. Unknown keys fall back to "prisma". */
internal val AppIconKeys: Set<String> = setOf("prisma", "sylveon", "flareon", "aggron", "pikachu", "starmie", "lucario", "cloyster", "sigilyph", "haxorus", "vaporeon", "infernape", "umbreon", "ultraball", "greatball", "pokedex", "glaceon", "masterball", "pokeball")
internal fun canonicalAppIcon(key: String): String = key.takeIf { it in AppIconKeys } ?: "prisma"
/** Activity-alias class suffix per icon key ("prisma" has no alias: it is
 *  MainActivity itself). Keys here must match AppIconKeys exactly; the
 *  picker and applyAppIcon both read this map, so an icon cannot exist in
 *  the manifest without a working switch, or vice versa. */
internal val AppIconAliasSuffixes: Map<String, String> = mapOf(
    "sylveon" to "IconSylveon",
    "flareon" to "IconFlareon",
    "aggron" to "IconAggron",
    "pikachu" to "IconPikachu",
    "starmie" to "IconStarmie",
    "lucario" to "IconLucario",
    "cloyster" to "IconCloyster",
    "sigilyph" to "IconSigilyph",
    "haxorus" to "IconHaxorus",
    "vaporeon" to "IconVaporeon",
    "infernape" to "IconInfernape",
    "umbreon" to "IconUmbreon",
    "ultraball" to "IconUltraBall",
    "greatball" to "IconGreatBall",
    "pokedex" to "IconPokedex",
    "glaceon" to "IconGlaceon",
    "masterball" to "IconMasterBall",
    "pokeball" to "IconPokeball",
)


data class UserState(
    val favorites: Set<Int> = emptySet(),
    val teams:List<SavedTeam> = listOf(SavedTeam("default","Minha equipe")),
    val activeTeamId:String="default",
    val theme: String = "eevee",
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
    /** Trainer card (Account tab): id of the saved team shown on the card.
     *  Blank or unknown = the active team. Stored locally only. */
    val trainerCardTeamId: String = "",
    /** Trainer card customization (dedicated screen): arrangement style
     *  and background keys from TrainerCardDesign. Unknown keys fall back
     *  to "classico"/"padrao" on decode and are rejected on save. */
    val trainerCardStyle: String = "classico",
    val trainerCardBackground: String = "padrao",
    /** Badge image sets: gameId -> earned badge indices (0..7) for games with
     *  a verified 8-image set. Kept alongside [badges] (user-named entries),
     *  which remains the fallback for games without an image set. */
    val badgeSets: Map<Int, Set<Int>> = emptyMap(),
    /** Galar has version-exclusive gyms; keep both earned sets separately. */
    val galarVersion: String = "sword",
    val galarBadgeSets: Map<String, Set<Int>> = emptyMap(),
    /** The eight additional Kanto badges in Gold/Silver, Crystal and HG/SS. */
    val kantoPostgameBadges: Map<Int, Set<Int>> = emptyMap(),
    /** Launcher icon choice (Settings): "prisma" (default), "sylveon" or
     *  "flareon" (user-supplied artwork). Unknown keys fall back to "prisma". */
    val appIcon: String = "prisma",
){
    val activeTeam:SavedTeam get()=teams.first{it.id==activeTeamId}
    /** Team shown on the trainer card: the chosen team, or the active team
     *  when none is chosen or the chosen id no longer exists. */
    val cardTeam:SavedTeam get()=teams.firstOrNull{it.id==trainerCardTeamId}?:activeTeam
    /** Toggle one badge image for a game with a verified image set.
     *  Games without a set and out-of-range indices leave the state
     *  unchanged; nothing is invented for them. */
    fun cardBadgeSet(gameId:Int):BadgeSet? = if(gameId==20) {
        BadgeSet(if(galarVersion=="shield") "galar-shield" else "galar-sword",setOf(20))
    } else BadgeSetByGame[gameId]
    fun cardBadgeIndices(gameId:Int):Set<Int> = if(gameId==20) {
        galarBadgeSets[if(galarVersion=="shield") "shield" else "sword"].orEmpty()
    } else badgeSets[gameId].orEmpty()
    fun toggleCardBadge(gameId:Int,index:Int):UserState{
        if(cardBadgeSet(gameId)==null||index !in 0 until BadgeSetSize)return this
        if(gameId==20){
            val variant=if(galarVersion=="shield") "shield" else "sword"
            val current=galarBadgeSets[variant].orEmpty()
            val next=if(index in current)current-index else current+index
            val updated=if(next.isEmpty())galarBadgeSets-variant else galarBadgeSets+(variant to next)
            return copy(galarBadgeSets=updated)
        }
        val current=badgeSets[gameId].orEmpty()
        val next=if(index in current)current-index else current+index
        val badgeSets=if(next.isEmpty())badgeSets-gameId else badgeSets+(gameId to next)
        return copy(badgeSets=badgeSets)
    }
    fun toggleKantoPostgameBadge(gameId:Int,index:Int):UserState{
        if(gameId !in setOf(3,4,10)||index !in 0 until BadgeSetSize)return this
        val current=kantoPostgameBadges[gameId].orEmpty()
        val next=if(index in current)current-index else current+index
        val updated=if(next.isEmpty())kantoPostgameBadges-gameId else kantoPostgameBadges+(gameId to next)
        return copy(kantoPostgameBadges=updated)
    }
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

/** Verified gym-image sets. SteGriff remains preferred for the first five
 *  regions; pinned PokeAPI badge images fill Kalos, Galar and BW2 changes.
 *  Galar's selected version is resolved through UserState.cardBadgeSet(). */
data class BadgeSet(val prefix: String, val gameIds: Set<Int>)
val BadgeSets = listOf(
    BadgeSet("kanto", setOf(1, 2, 7, 19)),
    BadgeSet("johto", setOf(3, 4, 10)),
    BadgeSet("hoenn", setOf(5, 6, 16)),
    BadgeSet("sinnoh", setOf(9)),
    BadgeSet("sinnoh-dp", setOf(8, 21)),
    BadgeSet("unova", setOf(11)),
    BadgeSet("unova2", setOf(14)),
    BadgeSet("kalos", setOf(15)),
    BadgeSet("galar-sword", setOf(20)),
    BadgeSet("paldea", setOf(23)),
)
val BadgeSetByGame: Map<Int, BadgeSet> = BadgeSets.flatMap { s -> s.gameIds.map { it to s } }.toMap()
const val BadgeSetSize = 8

/** Full 1,500-entry public trainer catalog, imported under the user's accepted
 *  IP risk. Raw PNGs are never edited; per-file artist/source credits live in
 *  assets/trainers/credit-manifest.json. Original 37 choices remain first. */
val TrainerSpriteKeys = listOf(
    "red", "blue", "green", "ethan", "lyra", "kris", "silver", "brendan",
    "may", "wally", "lucas", "dawn", "barry", "hilbert", "hilda", "nate",
    "rosa", "calem", "serena", "shauna", "elio", "selene", "hau", "chase",
    "elaine", "victor", "gloria", "hop", "rei", "akari", "penny", "kieran",
    "florian-s", "juliana-s", "nemona-s", "arven-s", "leaf-gen3", "aaron", "aarune", "acerola-masters",
    "acerola-masters2", "acerola-masters3", "acerola", "acetrainer-gen1", "acetrainer-gen1rb", "acetrainer-gen2", "acetrainer-gen3", "acetrainer-gen3jp",
    "acetrainer-gen3rs", "acetrainer-gen4", "acetrainer-gen4dp", "acetrainer-gen6", "acetrainer-gen6xy", "acetrainer-gen7", "acetrainer", "acetrainercouple-gen3",
    "acetrainercouple", "acetrainerf-gen1", "acetrainerf-gen1rb", "acetrainerf-gen2", "acetrainerf-gen3", "acetrainerf-gen3rs", "acetrainerf-gen4", "acetrainerf-gen4dp",
    "acetrainerf-gen6", "acetrainerf-gen6xy", "acetrainerf-gen7", "acetrainerf", "acetrainersnow", "acetrainersnowf", "adaman-masters", "adaman-masters2",
    "adaman", "aetheremployee", "aetheremployeef", "aetherfoundation", "aetherfoundation2", "aetherfoundationf", "agatha-gen1", "agatha-gen1rb",
    "agatha-gen3", "agatha-lgpe", "akari-isekai", "alain", "alder", "alec-anime", "allister-masters", "allister-unmasked",
    "allister", "amarys", "amelia-shuffle", "anabel-gen3", "anabel-gen7", "anabel", "ansha-cook", "ansha",
    "anthe", "anthea", "anvin", "aquagrunt-rse", "aquagrunt", "aquagruntf-rse", "aquagruntf", "aquasuit",
    "archer", "archie-gen3", "archie-gen6", "archie-usum", "arezu", "argenta", "ariana", "arlo",
    "aromalady-gen3", "aromalady-gen3rs", "aromalady-gen6", "aromalady", "artist-gen4", "artist-gen6", "artist-gen8", "artist-gen9",
    "artist", "artistf-gen6", "arven-masters", "arven-v", "ash-alola", "ash-capbackward", "ash-hoenn", "ash-johto",
    "ash-kalos", "ash-sinnoh", "ash-unova", "ash", "atticus", "avery", "az-lza", "az",
    "backers", "backersf", "backpacker-gen6", "backpacker-gen8", "backpacker-gen9", "backpacker", "backpackerf", "baker",
    "ballguy-masters", "ballguy", "baoba", "barry-masters", "battlegirl-gen3", "battlegirl-gen4", "battlegirl-gen6", "battlegirl-gen6xy",
    "battlegirl", "bea-masters", "bea", "beauty-gen1", "beauty-gen1rb", "beauty-gen2", "beauty-gen2jp", "beauty-gen3",
    "beauty-gen3rs", "beauty-gen4dp", "beauty-gen5bw2", "beauty-gen6", "beauty-gen6xy", "beauty-gen7", "beauty-gen8", "beauty-gen9",
    "beauty-masters", "beauty", "becca", "bede-leader", "bede-masters", "bede-masters2", "bede", "bellelba",
    "bellepa", "bellhop", "bellis", "benga", "beni-ninja", "beni", "bertha", "bianca-masters",
    "bianca-pwt", "bianca", "biker-gen1", "biker-gen1rb", "biker-gen2", "biker-gen3", "biker-gen4", "biker",
    "bill-gen3", "bill", "birch-gen3", "birch", "birdkeeper-gen1", "birdkeeper-gen1rb", "birdkeeper-gen2", "birdkeeper-gen3",
    "birdkeeper-gen3rs", "birdkeeper-gen4dp", "birdkeeper-gen6", "birdkeeper", "blackbelt-gen1", "blackbelt-gen1rb", "blackbelt-gen2", "blackbelt-gen3",
    "blackbelt-gen3rs", "blackbelt-gen4", "blackbelt-gen4dp", "blackbelt-gen6", "blackbelt-gen7", "blackbelt-gen8", "blackbelt-gen9", "blackbelt",
    "blaine-gen1", "blaine-gen1rb", "blaine-gen2", "blaine-gen3", "blaine-lgpe", "blaine", "blanche-casual", "blanche",
    "blue-gen1", "blue-gen1champion", "blue-gen1rb", "blue-gen1rbchampion", "blue-gen1rbtwo", "blue-gen1two", "blue-gen2", "blue-gen3",
    "blue-gen3champion", "blue-gen3two", "blue-gen7", "blue-lgpe", "blue-masters", "blue-masters2", "boarder-gen2", "boarder",
    "bodybuilder-gen9", "bodybuilderf-gen9", "brandon-gen3", "brandon", "brassius", "brawly-gen3", "brawly-gen6", "brawly",
    "brendan-contest", "brendan-e", "brendan-gen3", "brendan-gen3rs", "brendan-masters", "brendan-masters2", "brendan-masters3", "brendan-rs",
    "briar", "brigette", "brock-gen1", "brock-gen1rb", "brock-gen2", "brock-gen3", "brock-lgpe", "brock-masters",
    "brock", "bruno-gen1", "bruno-gen1rb", "bruno-gen2", "bruno-gen3", "bruno", "brycen", "brycenman",
    "bryony", "buck", "bugcatcher-gen1", "bugcatcher-gen1rb", "bugcatcher-gen2", "bugcatcher-gen3", "bugcatcher-gen3rs", "bugcatcher-gen4dp",
    "bugcatcher-gen6", "bugcatcher", "bugmaniac-gen3", "bugmaniac-gen6", "bugsy-gen2", "bugsy-masters", "bugsy", "burgh-masters",
    "burgh", "burglar-gen1", "burglar-gen1rb", "burglar-gen2", "burglar-gen3", "burglar-lgpe", "burglar", "burnet-radar",
    "burnet", "butler", "byron", "cabbie-gen9", "cabbie", "cafemaster", "caitlin-gen4", "caitlin-masters",
    "caitlin", "calaba", "calem-masters", "cameraman-gen6", "cameraman-gen8", "cameraman", "camper-gen2", "camper-gen3",
    "camper-gen3rs", "camper-gen6", "camper", "canari", "candela-casual", "candela", "candice-masters", "candice",
    "caraliss", "caretaker", "carmine-festival", "carmine-masters", "carmine", "cedricjuniper", "celio", "channeler-gen1",
    "channeler-gen1rb", "channeler-gen3", "channeler-lgpe", "charm", "charon", "chef", "cheren-gen5bw2", "cheren-masters",
    "cheren", "cheryl", "chili", "choy", "christoph", "chuck-gen2", "chuck", "cilan",
    "clair-gen2", "clair-masters", "clair", "clavell-s", "clay", "clemont", "clerk-boss", "clerk-gen8",
    "clerk-unite", "clerk", "clerkf-gen8", "clerkf", "cliff", "clive-v", "clover", "clown",
    "cogita", "coin", "collector-gen3", "collector-gen6", "collector-gen7", "collector-masters", "collector", "colress-gen7",
    "colress", "colza", "concordia", "cook-gen7", "cook-gen9", "cook", "corbeau", "courier",
    "courtney-gen3", "courtney", "cowgirl", "crasherwake", "cress", "crispin", "crushgirl-gen3", "crushkin-gen3",
    "cueball-gen1", "cueball-gen1rb", "cueball-gen3", "curtis", "cyclist-gen4", "cyclist", "cyclistf-gen4", "cyclistf",
    "cyllene", "cynthia-anime", "cynthia-anime2", "cynthia-gen4", "cynthia-gen7", "cynthia-masters", "cynthia-masters2", "cynthia-masters3",
    "cynthia-masters4", "cynthia-masters5", "cynthia", "cyrano", "cyrus-masters", "cyrus", "dagero", "dahlia",
    "daisy-gen3", "daisy", "dana", "dancer-gen7", "dancer-gen8", "dancer", "darach-caitlin", "darach",
    "dawn-contest", "dawn-gen4pt", "dawn-masters", "dawn-masters2", "dawn-masters3", "delinquent-gen9", "delinquent", "delinquentf-gen9",
    "delinquentf2-gen9", "dendra", "depotagent", "dexio-gen6", "dexio", "diamondclanmember", "diantha-masters", "diantha-masters2",
    "diantha", "doctor-gen8", "doctor", "doctorf-gen8", "doubleteam", "dragontamer-gen3", "dragontamer-gen6", "dragontamer-gen9",
    "dragontamer", "drake-gen3", "drasna", "drayden", "drayton", "dulse", "elesa-gen5bw2", "elesa-masters",
    "elesa-masters2", "elesa-masters3", "elesa", "elio-masters", "elio-usum", "elm", "emma-lza", "emma",
    "emmet-masters", "emmet", "engineer-gen1", "engineer-gen1rb", "engineer-gen3", "erbie-unite", "eri", "erika-gen1",
    "erika-gen1rb", "erika-gen2", "erika-gen3", "erika-lgpe", "erika-masters", "erika-masters2", "erika-masters3", "erika",
    "essentia", "ethan-gen2", "ethan-gen2c", "ethan-masters", "ethan-pokeathlon", "eusine-gen2", "eusine", "evelyn",
    "expert-gen3", "expert-gen6", "expertf-gen3", "expertf-gen6", "faba", "fairytalegirl", "falkner-gen2", "falkner",
    "fantina", "fennel", "firebreather-gen2", "firebreather", "firefighter", "fisher-gen8", "fisherman-gen1", "fisherman-gen1rb",
    "fisherman-gen2jp", "fisherman-gen3", "fisherman-gen3rs", "fisherman-gen4", "fisherman-gen6", "fisherman-gen6xy", "fisherman-gen7", "fisherman",
    "flannery-gen3", "flannery-gen6", "flannery", "flaregrunt", "flaregruntf", "flint", "florian-bb", "florian-festival",
    "florian-masters", "freediver", "furisodegirl-black", "furisodegirl-blue", "furisodegirl-pink", "furisodegirl-white", "gaeric", "galacticgrunt",
    "galacticgruntf", "gambler-gen1", "gambler-gen1rb", "gambler", "gamer-gen3", "garcon", "gardener", "gardenia-masters",
    "gardenia", "geeta", "gentleman-gen1", "gentleman-gen1rb", "gentleman-gen2", "gentleman-gen3", "gentleman-gen3rs", "gentleman-gen4",
    "gentleman-gen4dp", "gentleman-gen6", "gentleman-gen6xy", "gentleman-gen7", "gentleman-gen8", "gentleman-lgpe", "gentleman", "ghetsis-gen5bw",
    "ghetsis", "giacomo", "ginchiyo-conquest", "ginter", "giovanni-gen1", "giovanni-gen1rb", "giovanni-gen3", "giovanni-lgpe",
    "giovanni-masters", "giovanni-masters2", "giovanni", "glacia-gen3", "glacia", "gladion-masters", "gladion-masters2", "gladion-stance",
    "gladion", "gloria-dojo", "gloria-league", "gloria-masters", "gloria-masters2", "gloria-tundra", "golfer", "gordie",
    "grace", "grant", "greta-gen3", "greta", "grimsley-gen7", "grimsley-masters", "grimsley", "grisham",
    "grusha", "guitarist-gen2", "guitarist-gen3", "guitarist-gen4", "guitarist-gen6", "guitarist", "gurkinn", "guzma-masters",
    "guzma", "gwynn", "hala", "hanbei-conquest", "hapu", "harlequin", "harmony", "hassel",
    "hau-masters", "hau-stance", "hayley", "heath", "hero-conquest", "hero2-conquest", "heroine-conquest", "heroine2-conquest",
    "hexmaniac-gen3", "hexmaniac-gen3jp", "hexmaniac-gen6", "hiker-gen1", "hiker-gen1rb", "hiker-gen2", "hiker-gen3", "hiker-gen3rs",
    "hiker-gen4", "hiker-gen6", "hiker-gen7", "hiker-gen8", "hiker-gen9", "hiker", "hilbert-masters", "hilbert-masters2",
    "hilbert-masters3", "hilbert-wonderlauncher", "hilda-masters", "hilda-masters2", "hilda-masters3", "hilda-masters4", "hilda-wonderlauncher", "hooligans",
    "hoopster", "hop-masters", "hugh-masters", "hugh", "hyde", "idol", "ilima", "infielder",
    "ingo-hisui", "ingo-masters", "ingo", "interviewers-gen3", "interviewers-gen6", "interviewers", "iono-masters", "iono-masters2",
    "iono", "irida-masters", "irida-masters2", "irida", "iris-gen5bw2", "iris-masters", "iris-masters2", "iris",
    "iscan", "ivor", "jacinthe", "jacq", "jamie", "janine-gen2", "janine", "janitor-gen7",
    "janitor-gen9", "janitor", "jasmine-contest", "jasmine-gen2", "jasmine-masters", "jasmine-masters2", "jasmine-masters3", "jasmine",
    "jessiejames-gen1", "jogger", "johanna-contest", "johanna", "jrtrainer-gen1", "jrtrainer-gen1rb", "jrtrainerf-gen1", "jrtrainerf-gen1rb",
    "juan-gen3", "juan", "juggler-gen1", "juggler-gen1rb", "juggler-gen2", "juggler-gen3", "juggler", "juliana-bb",
    "juliana-festival", "juliana-masters", "juniper", "jupiter", "kabu-masters", "kabu", "kahili", "kamado-armor",
    "kamado", "karen-gen2", "karen", "katy", "kiawe", "kieran-champion", "kieran-festival", "kieran-masters",
    "kimonogirl-gen2", "kimonogirl", "kindler-gen3", "kindler-gen6", "klara", "kofu", "koga-gen1", "koga-gen1rb",
    "koga-gen2", "koga-gen3", "koga-lgpe", "koga", "korrina-masters", "korrina", "kris-gen2", "kris-masters",
    "kris-masters2", "kukui-stand", "kukui", "kunoichi-conquest", "kunoichi2-conquest", "kurt", "lacey-masters", "lacey",
    "lady-gen3", "lady-gen3rs", "lady-gen4", "lady-gen6", "lady-gen6oras", "lady", "lana-masters", "lana-masters2",
    "lana", "lance-gen1", "lance-gen1rb", "lance-gen2", "lance-gen3", "lance-lgpe", "lance-masters", "lance-masters2",
    "lance", "lanette", "larry-masters", "larry-masters2", "larry", "lass-gen1", "lass-gen1rb", "lass-gen2",
    "lass-gen3", "lass-gen3rs", "lass-gen4", "lass-gen4dp", "lass-gen6", "lass-gen6oras", "lass-gen7", "lass-gen8",
    "lass", "laventon", "laventon2", "leaf-masters", "leaf-masters2", "leaguestaff", "leaguestafff", "lebanne",
    "lenora", "leon-masters", "leon-masters2", "leon-tower", "leon", "li", "lian", "lida",
    "liko", "lillie-masters", "lillie-masters2", "lillie-masters3", "lillie-masters4", "lillie-masters5", "lillie-z", "lillie",
    "linebacker", "lisia-masters", "lisia", "liza-gen6", "liza-masters", "liza", "lorelei-gen1", "lorelei-gen1rb",
    "lorelei-gen3", "lorelei-lgpe", "ltsurge-gen1", "ltsurge-gen1rb", "ltsurge-gen2", "ltsurge-gen3", "ltsurge", "lucas-contest",
    "lucas-gen4pt", "lucian", "lucy-gen3", "lucy", "lusamine-masters", "lusamine-nihilego", "lusamine", "lyra-masters",
    "lyra-masters2", "lyra-pokeathlon", "lysandre-masters", "lysandre", "mable", "madame-gen4", "madame-gen4dp", "madame-gen6",
    "madame-gen7", "madame-gen8", "madame", "magmagrunt-rse", "magmagrunt", "magmagruntf-rse", "magmagruntf", "magmasuit",
    "magnolia", "magnus", "mai", "maid-gen4", "maid-gen6", "maid", "mallow-masters", "mallow",
    "malva", "marley-masters", "marley", "marlon", "marnie-league", "marnie-masters", "marnie-masters2", "marnie-masters3",
    "marnie-masters4", "marnie", "mars", "marshal", "masamune-conquest", "mateo", "matt-gen3", "matt",
    "maxie-gen3", "maxie-gen6", "may-contest", "may-e", "may-gen3", "may-gen3rs", "may-masters", "may-masters2",
    "may-masters3", "may-masters4", "may-rs", "maylene", "medium-gen2jp", "medium", "mela", "melli",
    "melony", "miku-fairy", "miku-fire", "miku-flying", "miku-ghost", "miku-grass", "miku-ground", "miku-ice",
    "miku-psychic", "miku-water", "milo", "mina-lgpe", "mina-masters", "mina", "mira", "miriam",
    "mirror", "misty-gen1", "misty-gen1rb", "misty-gen2", "misty-gen3", "misty-lgpe", "misty-masters", "misty",
    "model-gen8", "mohn-anime", "mohn", "molayne", "mom-alola", "mom-hoenn", "mom-johto", "mom-paldea",
    "mom-unova", "mom-unova2", "morgan", "morty-gen2", "morty-masters", "morty-masters2", "morty-masters3", "morty",
    "mrbriney", "mrfuji-gen3", "mrstone", "musician-gen8", "musician-gen9", "musician", "mustard-champion", "mustard-master",
    "mustard", "n-masters", "n-masters2", "n-masters3", "n", "nancy", "nanu", "nate-masters",
    "nate-pokestar", "nate-pokestar3", "nate-wonderlauncher", "naveen", "nemona-masters", "nemona-v", "neroli", "nessa-masters",
    "nessa", "ninjaboy-gen3", "ninjaboy-gen6", "ninjaboy", "nita", "nobunaga-conquest", "noland-gen3", "noland",
    "norman-gen3", "norman-gen6", "norman", "nurse", "nurseryaide", "oak-gen1", "oak-gen1rb", "oak-gen2",
    "oak-gen3", "oak", "officer-gen2", "officeworker-gen9", "officeworker", "officeworkerf-gen9", "officeworkerf", "ogreclan",
    "oichi-conquest", "oldcouple-gen3", "oleana", "olivia", "olympia", "opal", "ortega", "owner",
    "painter-gen3", "palina", "palmer", "parasollady-gen3", "parasollady-gen4", "parasollady-gen6", "parasollady", "paulo-masters",
    "paxton", "pearlclanmember", "peonia", "peony-league", "peony", "perrin-masters", "perrin", "pesselle",
    "petrel", "phil", "phillipe", "phoebe-gen3", "phoebe-gen6", "phoebe-masters", "phorus-unite", "phyco",
    "picnicker-gen2", "picnicker-gen3", "picnicker-gen3rs", "picnicker-gen6", "picnicker", "piers-league", "piers-masters", "piers",
    "pilot", "plasmagrunt-gen5bw", "plasmagrunt", "plasmagruntf-gen5bw", "plasmagruntf", "player-go", "playerf-go", "plumeria-league",
    "plumeria", "pokefan-gen2", "pokefan-gen3", "pokefan-gen4", "pokefan-gen6", "pokefan-gen6xy", "pokefan", "pokefanf-gen2",
    "pokefanf-gen3", "pokefanf-gen4", "pokefanf-gen6", "pokefanf-gen6xy", "pokefanf", "pokekid-gen8", "pokekid", "pokekidf-gen8",
    "pokemaniac-gen1", "pokemaniac-gen1rb", "pokemaniac-gen2", "pokemaniac-gen3", "pokemaniac-gen3rs", "pokemaniac-gen6", "pokemaniac-gen9", "pokemaniac",
    "pokemonbreeder-gen3", "pokemonbreeder-gen4", "pokemonbreeder-gen6", "pokemonbreeder-gen6xy", "pokemonbreeder-gen7", "pokemonbreeder-gen8", "pokemonbreeder", "pokemonbreederf-gen3",
    "pokemonbreederf-gen3frlg", "pokemonbreederf-gen4", "pokemonbreederf-gen6", "pokemonbreederf-gen6xy", "pokemonbreederf-gen7", "pokemonbreederf-gen8", "pokemonbreederf", "pokemoncenterlady",
    "pokemonranger-gen3", "pokemonranger-gen3rs", "pokemonranger-gen4", "pokemonranger-gen6", "pokemonranger-gen6xy", "pokemonranger", "pokemonrangerf-gen3", "pokemonrangerf-gen3rs",
    "pokemonrangerf-gen4", "pokemonrangerf-gen6", "pokemonrangerf-gen6xy", "pokemonrangerf", "policeman-gen4", "policeman-gen7", "policeman-gen8", "policeman",
    "poppy-masters", "poppy", "postman", "preschooler-gen6", "preschooler-gen7", "preschooler", "preschoolerf-gen6", "preschoolerf-gen7",
    "preschoolerf", "preschoolers", "proton", "pryce-gen2", "pryce", "psychic-gen1", "psychic-gen1rb", "psychic-gen2",
    "psychic-gen3", "psychic-gen3rs", "psychic-gen4", "psychic-gen6", "psychic-lgpe", "psychic", "psychicf-gen3", "psychicf-gen3rs",
    "psychicf-gen4", "psychicf", "psychicfjp-gen3", "punkgirl-gen7", "punkgirl-masters", "punkgirl", "punkguy-gen7", "punkguy",
    "raifort", "raihan-masters", "raihan", "railstaff", "rainbowrocketgrunt", "rainbowrocketgruntf", "ramos", "rancher",
    "ranmaru-conquest", "red-gen1", "red-gen1main", "red-gen1rb", "red-gen1title", "red-gen2", "red-gen3", "red-gen7",
    "red-lgpe", "red-masters", "red-masters2", "red-masters3", "red-masters4", "rei-isekai", "rei-masters", "reporter-gen6",
    "reporter-gen8", "reporter", "rhi", "richboy-gen3", "richboy-gen4", "richboy-gen6", "richboy-gen6xy", "richboy",
    "rika-masters", "rika", "riley", "risingstar-gen6", "risingstar", "risingstarf-gen6", "risingstarf", "rita",
    "river", "roark", "rocker-gen1", "rocker-gen1rb", "rocker-gen3", "rocket-gen1", "rocket-gen1rb", "rocketexecutive-gen2",
    "rocketexecutivef-gen2", "rocketgrunt-gen2", "rocketgrunt", "rocketgruntf-gen2", "rocketgruntf", "rollerskater", "rollerskaterf", "rood",
    "rosa-masters", "rosa-masters2", "rosa-masters3", "rosa-masters4", "rosa-pokestar", "rosa-pokestar2", "rosa-pokestar3", "rosa-wonderlauncher",
    "rose-zerosuit", "rose", "roughneck-gen4", "roughneck", "rowan", "roxanne-gen3", "roxanne-gen6", "roxanne-masters",
    "roxanne", "roxie-masters", "roxie", "roy", "ruffian", "ruinmaniac-gen3", "ruinmaniac-gen3rs", "ruinmaniac-gen6",
    "ruinmaniac", "rye", "ryme", "ryuki", "sabi", "sabrina-frlg", "sabrina-gen1", "sabrina-gen1rb",
    "sabrina-gen2", "sabrina-gen3", "sabrina-lgpe", "sabrina-masters", "sabrina", "sada-ai", "sada", "sage-gen2",
    "sage-gen2jp", "sage", "saguaro", "sailor-gen1", "sailor-gen1rb", "sailor-gen2", "sailor-gen3", "sailor-gen3jp",
    "sailor-gen3rs", "sailor-gen6", "sailor", "salvatore", "samsonoak", "sanqua", "saturn", "sbcmember",
    "schoolboy-gen2", "schoolboy", "schoolgirl", "schoolkid-gen3", "schoolkid-gen4", "schoolkid-gen4dp", "schoolkid-gen6", "schoolkid-gen8",
    "schoolkid", "schoolkidf-gen3", "schoolkidf-gen4", "schoolkidf-gen6", "schoolkidf-gen8", "schoolkidf", "scientist-gen1", "scientist-gen1rb",
    "scientist-gen2", "scientist-gen3", "scientist-gen4", "scientist-gen4dp", "scientist-gen6", "scientist-gen7", "scientist-gen9", "scientist",
    "scientistf-gen6", "scientistf", "scott", "scottie-masters", "scubadiver", "securitycorps", "securitycorpsf", "selene-masters",
    "selene-masters2", "selene-usum", "serena-anime", "serena-masters", "serena-masters2", "serena-masters3", "shadowtriad", "shauna-masters",
    "shauntal-masters", "shauntal", "shelly-gen3", "shelly", "shielbert", "sidney-gen3", "sidney", "siebold-masters",
    "siebold", "sierra", "sightseer", "sightseerf", "silver-gen2", "silver-gen2kanto", "silver-masters", "silver-masters2",
    "sina-gen6", "sina", "sisandbro-gen3", "sisandbro-gen3rs", "sisandbro", "skier-gen2", "skier", "skierf-gen4dp",
    "skierf", "skullgrunt", "skullgruntf", "skyla-masters", "skyla-masters2", "skyla-masters3", "skyla", "skytrainer",
    "skytrainerf", "smasher", "soliera", "sonia-masters", "sonia-masters2", "sonia-professor", "sonia", "sophocles",
    "sordward-shielbert", "sordward", "spark-casual", "spark", "spenser-gen3", "spenser", "srandjr-gen3", "stargrunt-s",
    "stargrunt-v", "stargruntf-s", "stargruntf-v", "steven-gen3", "steven-gen6", "steven-masters", "steven-masters2", "steven-masters3",
    "steven-masters4", "steven-masters5", "steven", "streetthug-masters", "streetthug", "striker", "supernerd-gen1", "supernerd-gen1rb",
    "supernerd-gen2", "supernerd-gen3", "supernerd", "surfer", "swimmer-gen1", "swimmer-gen1rb", "swimmer-gen4", "swimmer-gen4dp",
    "swimmer-gen4jp", "swimmer-gen6", "swimmer-gen7", "swimmer-gen8", "swimmer-masters", "swimmer", "swimmerf-gen2", "swimmerf-gen3",
    "swimmerf-gen3rs", "swimmerf-gen4", "swimmerf-gen4dp", "swimmerf-gen6", "swimmerf-gen7", "swimmerf-gen8", "swimmerf", "swimmerf2-gen6",
    "swimmerf2-gen7", "swimmerfjp-gen2", "swimmerm-gen2", "swimmerm-gen3", "swimmerm-gen3rs", "sycamore-masters", "sycamore", "tabitha-gen3",
    "tabitha", "tamer-gen1", "tamer-gen1rb", "tamer-gen3", "taohua", "tarragon", "tate-gen6", "tate-masters",
    "tate", "tateandliza-gen3", "tateandliza-gen6", "taunie", "teacher-gen2", "teacher-gen7", "teacher", "teamaquabeta-gen3",
    "teamaquagruntf-gen3", "teamaquagruntm-gen3", "teammagmagruntf-gen3", "teammagmagruntm-gen3", "teammates", "teamrocket", "teamrocketgruntf-gen3", "teamrocketgruntm-gen3",
    "theroyal", "thorton", "tierno", "tina-masters", "toddsnap", "toddsnap2", "tourist", "touristf",
    "touristf2", "trace", "trevor", "trialguide", "trialguidef", "triathletebiker-gen6", "triathletebikerf-gen3", "triathletebikerm-gen3",
    "triathleterunner-gen6", "triathleterunnerf-gen3", "triathleterunnerm-gen3", "triathleteswimmer-gen6", "triathleteswimmerf-gen3", "triathleteswimmerm-gen3", "tricia-masters", "trinnia-masters",
    "trista-masters", "tuber-gen3", "tuber-gen6", "tuber", "tuberf-gen3", "tuberf-gen3rs", "tuberf-gen6", "tuberf",
    "tucker-gen3", "tucker", "tuli", "tulip", "turo-ai", "turo", "twins-gen2", "twins-gen3",
    "twins-gen3rs", "twins-gen4", "twins-gen4dp", "twins-gen6", "twins", "tyme", "ultraforestkartenvoy", "unknown-flipped",
    "unknown", "unknownf-flipped", "unknownf", "urbain", "valerie", "vessa", "veteran-gen4", "veteran-gen6",
    "veteran-gen7", "veteran", "veteranf-gen6", "veteranf-gen7", "veteranf", "victor-dojo", "victor-league", "victor-masters",
    "victor-tundra", "vince", "viola-masters", "viola", "volkner-masters", "volkner", "volo-ginkgo", "volo",
    "waiter-gen4", "waiter-gen4dp", "waiter-gen9", "waiter", "waitress-gen4", "waitress-gen6", "waitress-gen9", "waitress",
    "wallace-gen3", "wallace-gen3rs", "wallace-gen6", "wallace-masters", "wallace", "wally-gen3", "wally-masters", "wally-rse",
    "wattson-gen3", "wattson", "whitney-gen2", "whitney-masters", "whitney", "wicke", "wikstrom", "will-gen2",
    "will", "willem", "willow-casual", "willow", "winona-gen3", "winona-gen6", "winona", "worker-gen4",
    "worker-gen6", "worker-gen7", "worker-gen8", "worker-gen9", "worker-lgpe", "worker", "worker2-gen6", "workerf-gen8",
    "workerice", "wulfric", "xerosic", "yancy", "yellgrunt", "yellgruntf", "yellow", "youngathlete",
    "youngathletef", "youngcouple-gen3", "youngcouple-gen3rs", "youngcouple-gen4dp", "youngcouple-gen6", "youngcouple", "youngn", "youngster-gen1",
    "youngster-gen1rb", "youngster-gen2", "youngster-gen3", "youngster-gen3rs", "youngster-gen4", "youngster-gen4dp", "youngster-gen6", "youngster-gen6xy",
    "youngster-gen7", "youngster-gen8", "youngster-gen9", "youngster-masters", "youngster", "yukito-hideko", "zinnia-masters", "zinnia",
    "zinzolin", "zirco-unite", "zisu", "zossie",
)
fun Pokemon.availableIn(game:Int)=if(game==25)champions else game in games
