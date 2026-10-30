package dev.bloody.prismadex.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** On-device accounts only: no cloud sync, verification email, or remote recovery. */
class Store(context: Context, private val storageName: String = "prisma") {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("$storageName-session", Context.MODE_PRIVATE)
    private val db = Accounts(app, "$storageName-accounts.sqlite3")
    private val repository = PokemonRepository(app)
    private val gate = Any()
    private val themes = ValidThemes
    private var attempts = mutableListOf<Long>()

    val currentEmail: String?
        get() = synchronized(gate) {
            val id = prefs.getLong("account", -1)
            if (id == -1L) null else db.readableDatabase.rawQuery("SELECT email FROM accounts WHERE id=?", arrayOf(id.toString())).use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }

    fun load(): UserState = synchronized(gate) {
        val id = prefs.getLong("account", -1)
        val raw = if (id == -1L) prefs.getString("guest-state", null) else db.readableDatabase.rawQuery("SELECT state FROM accounts WHERE id=?", arrayOf(id.toString())).use {
            if (it.moveToFirst()) it.getString(0) else null
        }
        if(raw==null)UserState() else {
            val json=JSONObject(raw)
            if(json.optInt("schemaVersion",0)<3&&!prefs.contains("state-before-v3-$id"))check(prefs.edit().putString("state-before-v3-$id",raw).commit()){"Falha ao preservar o estado anterior à atualização."}
            decode(json)
        }
    }

    /** Call from a worker dispatcher; validates IDs and each persisted build against bundled Champions data. */
    fun save(state: UserState) = synchronized(gate) {
        require(state.appearanceMode in setOf("system", "light", "dark")) { "Modo de aparência inválido." }
        require(state.battleFormat in setOf("singles", "doubles")) { "Formato inválido." }
        require(state.theme in themes) { "Tema inválido." }
        require(state.layout in setOf("grid", "list")) { "Layout inválido." }
        require(state.teamView in setOf("list", "grid")) { "Visualização da equipe inválida." }
        require(state.badges.all { (gameId, names) -> SupportedGames.any { it.id == gameId } && names.size <= 100 && names.all { it.isNotBlank() && it.length <= 50 } }) { "Registro de insígnia inválido." }
        require(state.trainerName.length <= 40) { "Nome de treinador inválido." }
        require(state.trainerCardTeamId.isBlank() || state.teams.any { it.id == state.trainerCardTeamId }) { "Equipe do cartão inválida." }
        require(state.trainerSprite.isBlank() || state.trainerSprite in TrainerSpriteKeys) { "Sprite de treinador inválido." }
        require(state.trainerCardStyle in TrainerCardStyleKeys) { "Estilo do cartão inválido." }
        require(state.trainerCardBackground in TrainerCardBackgroundKeys) { "Fundo do cartão inválido." }
        require(state.kantoPostgameBadges.all{(game,indices)->game in setOf(3,4,10)&&indices.all{it in 0 until BadgeSetSize}}) { "Insígnias de Kanto inválidas." }
        require(state.galarVersion in setOf("sword","shield")) { "Versão de Galar inválida." }
        require(state.appIcon in AppIconKeys) { "Ícone do app inválido." }
        require(state.galarBadgeSets.all { (version,indices)->version in setOf("sword","shield")&&indices.all{it in 0 until BadgeSetSize} }) { "Insígnias de Galar inválidas." }
        require(state.badgeSets.all { (gameId, indices) -> BadgeSetByGame[gameId] != null && indices.size <= BadgeSetSize && indices.all { it in 0 until BadgeSetSize } }) { "Registro de insígnia inválido." }
        require(state.teams.size in 1..100 && state.teams.map{it.id}.distinct().size==state.teams.size){"Use de 1 a 100 equipes."}
        require(state.teams.any{it.id==state.activeTeamId}){"Equipe ativa não encontrada."}
        require(state.dummies.size<=50 && state.dummies.map{it.id}.distinct().size==state.dummies.size){"Use até 50 alvos de teste."}
        require(state.dummies.all{it.id.length in 1..64 && it.label.length in 1..50}){"Nome do alvo inválido."}
        require(state.favorites.size<=2000){"Lista de favoritos muito grande."}
        val previous=load()
        runBlocking(Dispatchers.IO){
            val catalog=repository.loadCatalog().associateBy{it.id}
            require(state.favorites.all{it in catalog}){"Pokémon não encontrado."}
            state.teams.forEach{team->
                require(team.id.length in 1..64 && team.name.trim().length in 1..50){"Nome da equipe inválido."}
                require(SupportedGames.any{it.id==team.gameId}){"Jogo inválido."}
                require(team.format in setOf("singles","doubles")){"Formato inválido."}
                require(team.members.size<=6 && team.members.distinct().size==team.members.size){"Escolha até seis Pokémon diferentes."}
                val oldTeam=previous.teams.find{it.id==team.id && it.gameId==team.gameId}
                require(team.members.all{catalog[it]!=null && (catalog.getValue(it).availableIn(team.gameId) || it in (oldTeam?.members?:emptyList()))}){"Pokémon fora da seleção do jogo."}
                require(team.members.map{catalog.getValue(it).baseId}.distinct().size==team.members.size){"Escolha uma forma por espécie."}
                require(team.builds.keys.all{it in team.members}){"Set fora da equipe."}
                team.builds.forEach{(id,build)->repository.validateBuild(catalog.getValue(id),build,team.gameId,allowLegacy=oldTeam?.builds?.get(id)==build).getOrThrow()}
            }
            state.dummies.forEach{dummy->
                val p=catalog[dummy.pokemonId]?:error("Pokémon do alvo não encontrado.")
                val oldDummy=previous.dummies.find{it.id==dummy.id}
                require(p.champions || oldDummy?.pokemonId==dummy.pokemonId){"Alvo fora de Champions."}
                repository.validateBuild(p,dummy.build,allowLegacy=oldDummy==dummy).getOrThrow()
            }
        }
        val raw = encode(state).toString()
        val id = prefs.getLong("account", -1)
        if (id == -1L) {
            check(prefs.edit().putString("guest-state", raw).commit()) { "Não foi possível salvar os dados." }
        } else {
            val values = ContentValues().apply { put("state", raw) }
            check(db.writableDatabase.update("accounts", values, "id=?", arrayOf(id.toString())) == 1) { "Conta não encontrada. Entre novamente." }
        }
    }

    suspend fun register(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { synchronized(gate) {
            rateLimit()
            val normalized = email.trim().lowercase(java.util.Locale.ROOT)
            require(normalized.length <= 254 && Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(normalized)) { "Informe um email válido." }
            require(password.length in 8..128) { "Use senha de 8 a 128 caracteres." }
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val hash = derive(password, salt)
            val values = ContentValues().apply {
                put("email", normalized); put("salt", salt); put("hash", hash)
                put("state", encode(UserState()).toString())
            }
            val id = db.writableDatabase.insertWithOnConflict("accounts", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            require(id != -1L) { "Não foi possível criar uma conta com este email." }
            check(prefs.edit().putLong("account", id).commit()) { "Conta criada. Entre novamente para continuar." }
            Unit
        } }
    }

    suspend fun login(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { synchronized(gate) {
            rateLimit()
            require(password.length <= 128 && email.length <= 254) { "Email ou senha incorretos." }
            val normalized = email.trim().lowercase(java.util.Locale.ROOT)
            var id = -1L
            var salt = ByteArray(16)
            var expected = ByteArray(32)
            db.readableDatabase.rawQuery("SELECT id,salt,hash FROM accounts WHERE email=?", arrayOf(normalized)).use {
                if (it.moveToFirst()) { id = it.getLong(0); salt = it.getBlob(1); expected = it.getBlob(2) }
            }
            val matches = MessageDigest.isEqual(derive(password, salt), expected)
            require(matches && id != -1L) { "Email ou senha incorretos." }
            check(prefs.edit().putLong("account", id).commit()) { "Não foi possível abrir a sessão." }
            Unit
        } }
    }

    fun logout(): Unit = synchronized(gate) {
        check(prefs.edit().remove("account").commit()) { "Não foi possível encerrar a sessão." }
    }

    fun close() = synchronized(gate) { db.close() }

    private fun rateLimit() {
        val now = android.os.SystemClock.elapsedRealtime()
        attempts = attempts.filter { now - it < 60_000 }.toMutableList()
        require(attempts.size < 10) { "Aguarde um minuto para tentar novamente." }
        attempts.add(now)
    }

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, 600_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() }
    }

    private class Accounts(context: Context, name: String) : SQLiteOpenHelper(context, name, null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE accounts(id INTEGER PRIMARY KEY,email TEXT UNIQUE NOT NULL,salt BLOB NOT NULL,hash BLOB NOT NULL,state TEXT NOT NULL)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }

    companion object {
        private val ValidThemes = setOf("eevee", "vaporeon", "jolteon", "flareon", "espeon", "umbreon", "leafeon", "glaceon", "sylveon")
        internal fun encode(state: UserState): JSONObject = JSONObject().apply {
            put("schemaVersion",3)
            put("favorites", JSONArray(state.favorites.toList())); put("team", JSONArray(state.team))
            put("activeTeamId",state.activeTeamId)
            put("teams",JSONArray().apply{state.teams.forEach{team->put(JSONObject().apply{
                put("id",team.id);put("name",team.name);put("gameId",team.gameId);put("format",team.format)
                put("members",JSONArray(team.members));put("builds",JSONObject().apply{team.builds.forEach{(id,b)->put(id.toString(),encodeBuild(b))}})
            })}})
            put("theme", state.theme); put("layout", state.layout); put("battleFormat", state.battleFormat); put("appearanceMode", state.appearanceMode); put("teamView", state.teamView)
            put("builds", JSONObject().apply { state.builds.forEach { (id, build) ->
                put(id.toString(), encodeBuild(build))
            } })
            put("dummies", JSONArray().apply { state.dummies.forEach { d -> put(JSONObject().apply { put("id", d.id); put("label", d.label); put("pokemonId", d.pokemonId); put("build", encodeBuild(d.build)) }) } })
            put("badges", JSONObject().apply { state.badges.forEach { (gameId, names) -> put(gameId.toString(), JSONArray(names)) } })
            put("trainerName", state.trainerName); put("trainerSprite", state.trainerSprite); put("trainerCardTeamId", state.trainerCardTeamId)
            put("trainerCardStyle", state.trainerCardStyle); put("trainerCardBackground", state.trainerCardBackground)
            put("kantoPostgameBadges",JSONObject().apply{state.kantoPostgameBadges.forEach{(game,indices)->put(game.toString(),JSONArray(indices.sorted()))}})
            put("galarVersion",state.galarVersion)
            put("appIcon",state.appIcon)
            put("galarBadgeSets",JSONObject().apply{state.galarBadgeSets.forEach{(version,indices)->put(version,JSONArray(indices.sorted()))}})
            put("badgeSets", JSONObject().apply { state.badgeSets.forEach { (gameId, indices) -> put(gameId.toString(), JSONArray(indices.sorted().toList())) } })
        }
        internal fun encodeBuild(build: Build) = JSONObject().apply {
            put("itemId", build.itemId ?: JSONObject.NULL); put("moves", JSONArray(build.moves))
            put("nature", build.nature); put("ability", build.ability ?: JSONObject.NULL); put("sps", JSONArray(build.sps)); put("gender", build.gender?.takeIf { it == "male" || it == "female" } ?: JSONObject.NULL); put("shiny", build.shiny)
        }
        internal fun decodeBuild(build: JSONObject) = Build(build.nullableInt("itemId"), build.optJSONArray("moves")?.ints() ?: emptyList(), build.optString("nature", "Serious"), if(build.isNull("ability") || !build.has("ability")) null else build.getString("ability"), build.optJSONArray("sps")?.ints() ?: List(6) { 0 }, build.optString("gender").takeIf { it == "male" || it == "female" }, build.optBoolean("shiny", false))
        internal fun decode(json: JSONObject): UserState {
            val team = json.optJSONArray("team")?.ints()?.map(::canonicalPokemonId)?.distinct() ?: emptyList()
            val rawBuilds = json.optJSONObject("builds") ?: JSONObject()
            val builds = rawBuilds.keys().asSequence().sortedBy{if(it.toIntOrNull() in RetiredPokemonIds)0 else 1}.mapNotNull { key ->
                val id = key.toIntOrNull()?.let(::canonicalPokemonId) ?: return@mapNotNull null
                if (id !in team) return@mapNotNull null
                val build = rawBuilds.optJSONObject(key) ?: return@mapNotNull null
                id to decodeBuild(build)
            }.toMap()
            val rawTeams=json.optJSONArray("teams")
            val teams=if(rawTeams!=null&&rawTeams.length()>0)(0 until rawTeams.length()).map{i->
                val t=rawTeams.getJSONObject(i);val members=t.getJSONArray("members").ints().map(::canonicalPokemonId).distinct();val b=t.optJSONObject("builds")?:JSONObject()
                SavedTeam(t.getString("id"),t.getString("name"),t.getInt("gameId"),members,b.keys().asSequence().sortedBy{if(it.toIntOrNull() in RetiredPokemonIds)0 else 1}.mapNotNull{k->k.toIntOrNull()?.let(::canonicalPokemonId)?.takeIf{it in members}?.let{it to decodeBuild(b.getJSONObject(k))}}.toMap(),t.optString("format","doubles"))
            } else listOf(SavedTeam("default","Minha equipe",25,team,builds,json.optString("battleFormat","doubles")))
            val active=json.optString("activeTeamId","default").takeIf{id->teams.any{it.id==id}}?:teams.first().id
            return UserState(favorites=json.optJSONArray("favorites")?.ints()?.map(::canonicalPokemonId)?.toSet()?:emptySet(),teams=teams,activeTeamId=active,
                theme=json.optString("theme","eevee").takeIf { it in ValidThemes } ?: "eevee",layout=json.optString("layout","list"),teamView=json.optString("teamView","list"),
                badges=(json.optJSONObject("badges")?.keys()?.asSequence()?.mapNotNull { key -> key.toIntOrNull()?.takeIf { id -> SupportedGames.any { it.id == id } }?.let { id -> id to (json.optJSONObject("badges")?.optJSONArray(key)?.strings().orEmpty().map { it.trim() }.filter { it.isNotEmpty() && it.length <= 50 }.distinct().take(100)) } }?.toMap() ?: emptyMap()),
                trainerName=json.optString("trainerName","").trim().take(40),trainerSprite=json.optString("trainerSprite","").takeIf{it in TrainerSpriteKeys} ?: "",
                trainerCardTeamId=json.optString("trainerCardTeamId","").takeIf { id -> id.isNotBlank() && teams.any { it.id == id } } ?: "",
                trainerCardStyle=json.optString("trainerCardStyle","classico").takeIf { it in TrainerCardStyleKeys } ?: "classico",
                trainerCardBackground=json.optString("trainerCardBackground","padrao").takeIf { it in TrainerCardBackgroundKeys } ?: "padrao",
                kantoPostgameBadges=json.optJSONObject("kantoPostgameBadges")?.let{rows->listOf(3,4,10).mapNotNull{game->rows.optJSONArray(game.toString())?.ints()?.filter{it in 0 until BadgeSetSize}?.toSet()?.let{game to it}}.toMap()}.orEmpty(),
                galarVersion=json.optString("galarVersion","sword").takeIf{it in setOf("sword","shield")}?:"sword",
                appIcon=canonicalAppIcon(json.optString("appIcon","prisma")),
                galarBadgeSets=json.optJSONObject("galarBadgeSets")?.let{rows->listOf("sword","shield").mapNotNull{version->rows.optJSONArray(version)?.ints()?.filter{it in 0 until BadgeSetSize}?.toSet()?.let{version to it}}.toMap()}.orEmpty(),
                badgeSets=(json.optJSONObject("badgeSets")?.keys()?.asSequence()?.mapNotNull { key -> key.toIntOrNull()?.takeIf { id -> BadgeSetByGame.containsKey(id) }?.let { id -> id to (json.optJSONObject("badgeSets")?.optJSONArray(key)?.ints().orEmpty().filter { it in 0 until BadgeSetSize }.distinct().take(BadgeSetSize).toSet()) } }?.toMap() ?: emptyMap()),
                dummies=json.optJSONArray("dummies")?.let { rows -> (0 until rows.length()).map { i -> rows.getJSONObject(i).let { d -> TrainingDummy(d.getString("id"), d.getString("label"), canonicalPokemonId(d.getInt("pokemonId")), decodeBuild(d.getJSONObject("build"))) } } } ?: emptyList(),
                battleFormat=json.optString("battleFormat","doubles"),appearanceMode=json.optString("appearanceMode","system"))
        }
    }
}
