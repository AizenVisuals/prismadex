@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package dev.bloody.prismadex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.data.*
import dev.bloody.prismadex.damage.DamageEngine
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

private data class BattleConditions(
    val attackerBoosts:List<Int> = List(6){0}, val defenderBoosts:List<Int> = List(6){0},
    val attackerHP:Int=100,val defenderHP:Int=100,val attackerStatus:String="",val defenderStatus:String="",
    val weather:String="",val terrain:String="",val doubles:Boolean=true,val critical:Boolean=false,
    val reflect:Boolean=false,val lightScreen:Boolean=false,val helpingHand:Boolean=false,val friendGuard:Boolean=false,
    val attackerAbilityOn:Boolean=false,val defenderAbilityOn:Boolean=false,val hits:Int=0,
)

private val conditionsSaver=listSaver<BattleConditions,Any>(
    save={listOf(it.attackerBoosts.toIntArray(),it.defenderBoosts.toIntArray(),it.attackerHP,it.defenderHP,it.attackerStatus,it.defenderStatus,it.weather,it.terrain,it.doubles,it.critical,it.reflect,it.lightScreen,it.helpingHand,it.friendGuard,it.attackerAbilityOn,it.defenderAbilityOn,it.hits)},
    restore={BattleConditions((it[0] as IntArray).toList(),(it[1] as IntArray).toList(),it[2] as Int,it[3] as Int,it[4] as String,it[5] as String,it[6] as String,it[7] as String,it[8] as Boolean,it[9] as Boolean,it[10] as Boolean,it[11] as Boolean,it[12] as Boolean,it[13] as Boolean,it[14] as Boolean,it[15] as Boolean,it[16] as Int)}
)

@Composable internal fun DamageScreen(catalog:List<Pokemon>,state:UserState,repo:PokemonRepository,engine:DamageEngine,onSelectTeam:(String)->Unit,onEdit:(Pokemon,String?)->Unit,onDummy:(String?)->Unit,onDeleteDummy:(String)->Unit,requestedDefender:String?=null,onDefenderConsumed:()->Unit={},onChoosePokemon:()->Unit={}){
    var inspectedMove by remember{mutableStateOf<Move?>(null)}
    inspectedMove?.let{MoveInfoSheet(it){inspectedMove=null}}
    var deletingDummy by rememberSaveable{mutableStateOf<String?>(null)}
    state.dummies.find{it.id==deletingDummy}?.let{d->AlertDialog(onDismissRequest={deletingDummy=null},title={LText("Excluir alvo ${d.label}?")},text={LText("Este alvo e sua configuração serão removidos do aparelho.")},confirmButton={TextButton(onClick={onDeleteDummy(d.id);deletingDummy=null}){LText("Excluir",color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton(onClick={deletingDummy=null}){LText("Cancelar")}})}
    val scope=rememberCoroutineScope()
    val attackers=remember(state.teams,catalog){battleTeamChoices(state,catalog)}
    val defenders=remember(state.teams,state.dummies,catalog){battleDefenderChoices(state,catalog)}
    var attackerKey by rememberSaveable{mutableStateOf(attackers.firstOrNull{it.teamId==state.activeTeamId}?.key?:attackers.firstOrNull()?.key?:"")}
    var defenderKey by rememberSaveable{mutableStateOf(defenders.firstOrNull{it.key!=attackerKey}?.key?:defenders.firstOrNull()?.key?:"")}
    LaunchedEffect(requestedDefender){requestedDefender?.let{defenderKey=it;onDefenderConsumed()}}
    LaunchedEffect(attackers,defenders){
        if(attackerKey.isBlank())attackerKey=attackers.firstOrNull()?.key?:""
        if(defenderKey.isBlank())defenderKey=defenders.firstOrNull{it.key!=attackerKey}?.key?:defenders.firstOrNull()?.key?:""
    }
    val attackerChoice=attackers.firstOrNull{it.key==attackerKey}
    val defenderChoice=defenders.firstOrNull{it.key==defenderKey}
    val attackerId=attackerChoice?.pokemonId
    val attacker=catalog.find{it.id==attackerId}
    val attackerBuild=attackerChoice?.build?:Build()
    val dummy=state.dummies.find{"dummy/${it.id}"==defenderKey}
    val defender=catalog.find{it.id==defenderChoice?.pokemonId}
    val defenderBuild=defenderChoice?.build?:Build()
    var moves by remember{mutableStateOf<List<Move>>(emptyList())}
    var moveId by rememberSaveable{mutableStateOf<Int?>(null)}
    LaunchedEffect(attacker,attackerBuild){moves=attacker?.let{p->repo.movesFor(p).filter{it.id in attackerBuild.moves}}?:emptyList();if(moves.none{it.id==moveId})moveId=moves.firstOrNull()?.id}
    var format by rememberSaveable{mutableStateOf(state.activeTeam.format)}
    var singles by rememberSaveable(state.activeTeamId,stateSaver=conditionsSaver){mutableStateOf(BattleConditions(doubles=false))}
    var doubles by rememberSaveable(state.activeTeamId,stateSaver=conditionsSaver){mutableStateOf(BattleConditions(doubles=true))}
    val conditions=if(format=="doubles")doubles else singles
    fun updateConditions(value:BattleConditions){if(format=="doubles")doubles=value.copy(doubles=true) else singles=value.copy(doubles=false,helpingHand=false,friendGuard=false)}
    var result by remember{mutableStateOf<JSONObject?>(null)}
    var error by remember{mutableStateOf<String?>(null)}
    var busy by remember{mutableStateOf(false)}
    var advanced by rememberSaveable{mutableStateOf(false)}
    val signature=listOf(attackerId,attackerBuild,defenderKey,defenderBuild,moveId,conditions)
    val latestSignature by rememberUpdatedState(signature)
    LaunchedEffect(signature){result=null;error=null}
    fun calculate(){
        val a=attacker?:return;val d=defender?:return;val move=moves.find{it.id==moveId}?:return
        val signatureAtStart=signature;val c=conditions;busy=true;error=null
        scope.launch{
            runCatching {
                repo.validateBuild(a,attackerBuild).getOrThrow();repo.validateBuild(d,defenderBuild).getOrThrow()
                val ai=repo.heldItemsFor(a).find{it.id==attackerBuild.itemId}?.name
                val di=repo.heldItemsFor(d).find{it.id==defenderBuild.itemId}?.name
                val request=JSONObject().apply{
                    put("attacker",engine.combatant(a,attackerBuild,ai,c.attackerBoosts,c.attackerHP,c.attackerStatus,c.attackerAbilityOn))
                    put("defender",engine.combatant(d,defenderBuild,di,c.defenderBoosts,c.defenderHP,c.defenderStatus,c.defenderAbilityOn))
                    put("move",move.name);put("critical",c.critical);put("hits",c.hits)
                    put("field",JSONObject().apply{
                        put("gameType",if(c.doubles)"Doubles" else "Singles")
                        if(c.weather.isNotEmpty())put("weather",c.weather)
                        if(c.terrain.isNotEmpty())put("terrain",c.terrain)
                        put("attackerSide",JSONObject().put("isHelpingHand",c.helpingHand))
                        put("defenderSide",JSONObject().put("isReflect",c.reflect).put("isLightScreen",c.lightScreen).put("isFriendGuard",c.friendGuard))
                    })
                }
                engine.calculate(request)
            }.onSuccess{if(signatureAtStart==latestSignature)result=it}.onFailure{if(signatureAtStart==latestSignature)error=it.message?.take(250)?:"Não foi possível calcular este cenário."}
            busy=false
        }
    }
    LazyColumn(Modifier.prismaViewport(),contentPadding=PaddingValues(start=22.dp,end=22.dp,top=16.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        item(key="intro"){LText("Compare seus sets",fontSize=22.sp,fontWeight=FontWeight.SemiBold)}
        item(key="format"){Column(verticalArrangement=Arrangement.spacedBy(9.dp)){
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                FilterChip(format=="singles",{format="singles"},label={LText("Singles · individual")})
                FilterChip(format=="doubles",{format="doubles"},label={LText("Doubles · duplas")})
            }
            LText(if(format=="doubles")"Cálculo Doubles · redução de golpes em área e efeitos de aliados." else "Cálculo Singles · confronto individual, sem efeitos de aliados.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }}
        if(attackers.isEmpty())item{Column(verticalArrangement=Arrangement.spacedBy(14.dp)){
            LText("Monte um set para começar. O atacante e o defensor podem vir de equipes diferentes.",fontSize=15.sp,lineHeight=22.sp)
            ActionButton("Montar equipe",Icons.Rounded.Add,true,onChoosePokemon)
            OutlinedButton(onClick={onDummy(null)},modifier=Modifier.fillMaxWidth()){LText("Criar alvo de teste")}
        }}
        else{
            item(key="attacker"){Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
                ChoiceField("Atacante · equipe e Pokémon",attackerChoice?.label?:"Escolha um atacante",attackers.map{it.key to it.label}){attackerKey=it}
                attacker?.let{p->CombatantSummary(p,attackerBuild,attackerChoice?.label?:"Set salvo");TextButton(onClick={onEdit(p,attackerChoice?.teamId)}){LText("Editar set do atacante")}}
            }}
            item(key="defender"){Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
                ChoiceField("Defensor · equipe ou alvo",defenderChoice?.label?:"Escolha um alvo",defenders.map{it.key to it.label}){defenderKey=it}
                defender?.let{p->CombatantSummary(p,defenderBuild,if(dummy!=null)"Alvo independente · não ocupa vaga" else defenderChoice?.label?:"Set salvo");TextButton(onClick={if(dummy!=null)onDummy(dummy.id) else onEdit(p,defenderChoice?.teamId)}){LText("Editar configuração do defensor")}}
                OutlinedButton(onClick={onDummy(null)},modifier=Modifier.fillMaxWidth()){LIcon(Icons.Rounded.Add,null);LText("Criar alvo de teste",Modifier.padding(8.dp))}
            }}
            item(key="moves"){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
                LText("GOLPE DO SET",fontSize=13.sp,letterSpacing=1.5.sp,color=MaterialTheme.colorScheme.primary)
                if(moves.isEmpty())LText("Salve pelo menos um golpe no set do atacante.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){moves.forEach{m->FilterChip(moveId==m.id,{moveId=m.id},label={LText(m.name,fontSize=13.sp)},leadingIcon={TypeSymbol(m.type)})}}
                moves.find{it.id==moveId}?.let{move->TextButton(onClick={inspectedMove=move}){LIcon(Icons.Rounded.Info,null);LText("Ver efeito e propriedades",Modifier.padding(start=8.dp))}}
                FilterChip(conditions.critical,{updateConditions(conditions.copy(critical=!conditions.critical))},label={LText("Acerto crítico")})
                OutlinedButton(onClick={advanced=!advanced},modifier=Modifier.fillMaxWidth()){LText(if(advanced)"Recolher condições da batalha" else "Condições da batalha e modificadores")}
            }}
            item(key="conditions"){AnimatedVisibility(advanced,enter=expandEnter(LocalReduceMotion.current),exit=shrinkExit(LocalReduceMotion.current)){ConditionsEditor(conditions){updateConditions(it)}}}
            item(key="calculate"){Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
                ActionButton(if(busy)"Calculando no aparelho…" else "Calcular dano",Icons.Rounded.AutoAwesome,!busy&&moves.isNotEmpty()&&defender!=null){calculate()}
                if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let{LText(it,color=MaterialTheme.colorScheme.error,fontSize=13.sp,lineHeight=18.sp)}
                LText("Motor Showdown · modo Champions. Nível 50, SP, naturezas, habilidades, itens e variação de dano. Acerto do golpe e decisões do adversário não são uma previsão.",fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }}
            result?.let{r->item(key="result"){DamageResult(r,motionPlacement(LocalReduceMotion.current))}}
        }
        if(state.dummies.isNotEmpty()){
            item(key="dummy-heading"){LText("ALVOS SALVOS · ${state.dummies.size}/50",fontSize=13.sp,letterSpacing=1.5.sp,color=MaterialTheme.colorScheme.primary)}
            items(state.dummies,key={"dummy/${it.id}"}){d->Surface(shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){PokemonArt(d.pokemonId,Modifier.size(56.dp),d.label);Column(Modifier.weight(1f).padding(start=12.dp)){Text(d.label,fontSize=14.sp,fontWeight=FontWeight.SemiBold);LText("${d.build.nature} · ${d.build.sps.sum()}/66 SP",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
                Row(Modifier.align(Alignment.End),horizontalArrangement=Arrangement.spacedBy(8.dp)){IconButton(onClick={onDummy(d.id)}){LIcon(Icons.Rounded.Edit,"Editar alvo ${d.label}")};IconButton(onClick={deletingDummy=d.id}){LIcon(Icons.Rounded.Close,"Excluir alvo ${d.label}")}}
            }}}

        }
        item{LText("A base e o motor estão versionados; a elegibilidade do regulamento atual ainda precisa ser conferida. Cenários especiais não expostos nos controles usam os padrões do motor.",fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
}

@Composable private fun CombatantSummary(p:Pokemon,build:Build,subtitle:String){
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){Box(Modifier.size(76.dp)){PokemonArt(p.id,Modifier.fillMaxSize(),p.name);if(build.itemId!=null)ItemArt(build.itemId,null,Modifier.align(Alignment.BottomEnd).size(28.dp))};Column(Modifier.weight(1f)){LText("${build.nature} · ${build.sps.sum()}/66 SP",fontSize=14.sp,fontWeight=FontWeight.SemiBold);LText(build.ability?.replace('-',' ')?:"Habilidade padrão da espécie",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);LText(subtitle,fontSize=13.sp,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(top=6.dp))}}
}

@Composable private fun ConditionsEditor(c:BattleConditions,onChange:(BattleConditions)->Unit){
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        ChoiceField("Clima",mapOf("" to "Sem clima","Sun" to "Sol","Rain" to "Chuva","Sand" to "Tempestade de areia","Snow" to "Neve").getValue(c.weather),listOf("" to "Sem clima","Sun" to "Sol","Rain" to "Chuva","Sand" to "Tempestade de areia","Snow" to "Neve")){onChange(c.copy(weather=it))}
        ChoiceField("Terreno",mapOf("" to "Sem terreno","Electric" to "Elétrico","Grassy" to "Grama","Misty" to "Névoa","Psychic" to "Psíquico").getValue(c.terrain),listOf("" to "Sem terreno","Electric" to "Elétrico","Grassy" to "Grama","Misty" to "Névoa","Psychic" to "Psíquico")){onChange(c.copy(terrain=it))}
        for(side in 0..1){
            val hp=if(side==0)c.attackerHP else c.defenderHP;val status=if(side==0)c.attackerStatus else c.defenderStatus
            LText(if(side==0)"Atacante" else "Defensor",fontWeight=FontWeight.SemiBold)
            OutlinedTextField(hp.toString(),{raw->if(raw.isBlank()||raw.all{it.isDigit()}){val value=(raw.toIntOrNull()?:1).coerceIn(1,100);onChange(if(side==0)c.copy(attackerHP=value) else c.copy(defenderHP=value))}},label={LText("HP atual (%)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
            val statuses=listOf("" to "Sem condição","brn" to "Queimadura","par" to "Paralisia","psn" to "Veneno","tox" to "Veneno tóxico","slp" to "Sono","frz" to "Congelamento")
            ChoiceField("Condição",statuses.first{it.first==status}.second,statuses){onChange(if(side==0)c.copy(attackerStatus=it) else c.copy(defenderStatus=it))}
            val boosts=if(side==0)c.attackerBoosts else c.defenderBoosts
            LText("Estágios em batalha (−6 a +6)",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            val columns=if(androidx.compose.ui.platform.LocalDensity.current.fontScale>1.25f)2 else 3
            for(indices in (1..5).toList().chunked(columns))Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                indices.forEach{i->ChoiceFieldSmall(statLabels[i],boosts[i],Modifier.weight(1f)){value->val next=boosts.toMutableList().apply{set(i,value)};onChange(if(side==0)c.copy(attackerBoosts=next) else c.copy(defenderBoosts=next))}}
                repeat(columns-indices.size){Spacer(Modifier.weight(1f))}
            }
            val on=if(side==0)c.attackerAbilityOn else c.defenderAbilityOn
            ToggleLine("Habilidade condicional ativada",on){onChange(if(side==0)c.copy(attackerAbilityOn=it) else c.copy(defenderAbilityOn=it))}
        }
        ToggleLine("Reflect no defensor",c.reflect){onChange(c.copy(reflect=it))}
        ToggleLine("Light Screen no defensor",c.lightScreen){onChange(c.copy(lightScreen=it))}
        if(c.doubles){
            ToggleLine("Helping Hand no atacante",c.helpingHand){onChange(c.copy(helpingHand=it))}
            ToggleLine("Friend Guard no defensor",c.friendGuard){onChange(c.copy(friendGuard=it))}
        }
        ChoiceField("Número de acertos · golpes múltiplos",if(c.hits==0)"Padrão do motor" else c.hits.toString(),listOf("0" to "Padrão do motor")+(1..10).map{it.toString() to it.toString()}){onChange(c.copy(hits=it.toInt()))}
    }
}
@Composable private fun ChoiceFieldSmall(label:String,value:Int,modifier:Modifier,onChange:(Int)->Unit){Column(modifier){ChoiceField(label,if(value>0)"+$value" else value.toString(),(-6..6).map{it.toString() to if(it>0)"+$it" else it.toString()}){onChange(it.toInt())}}}
@Composable private fun ToggleLine(label:String,checked:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){LText(label,Modifier.weight(1f),fontSize=13.sp);Switch(checked,onChange)}}

@Composable private fun DamageResult(r:JSONObject,modifier:Modifier=Modifier){
    val ui=LocalUiStrings.current
    Surface(modifier=modifier,shape=RoundedCornerShape(26.dp),color=MaterialTheme.colorScheme.primary.copy(alpha=.12f)){
        Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(13.dp)){
            LText("${r.optString("attackerName")} → ${r.optString("defenderName")} · ${r.optString("gameType")}",fontSize=13.sp,fontWeight=FontWeight.SemiBold)
            LText("DANO ESTIMADO",fontSize=13.sp,letterSpacing=1.5.sp,color=MaterialTheme.colorScheme.primary)
            LText("${r.getInt("min")}–${r.getInt("max")} HP",fontSize=30.sp,fontWeight=FontWeight.Bold)
            LText("${r.getDouble("percentMin")}%–${r.getDouble("percentMax")}% do HP máximo",fontSize=15.sp)
            LinearProgressIndicator(progress={(r.getDouble("percentMax")/100).coerceIn(0.0,1.0).toFloat()},modifier=Modifier.fillMaxWidth().height(8.dp))
            val ko=r.optString("ko");LText(if(ko.startsWith("guaranteed "))ko.removePrefix("guaranteed ")+" garantido" else ko,fontSize=14.sp,fontWeight=FontWeight.SemiBold)
            val hp=r.getInt("currentHP");LText("HP restante: ${(hp-r.getInt("max")).coerceAtLeast(0)}–${(hp-r.getInt("min")).coerceAtLeast(0)} / ${r.getInt("hp")}",fontSize=13.sp)
            for((key,title) in listOf("attackerStats" to "Atributos do atacante","defenderStats" to "Atributos do defensor")){
                LText(title,fontSize=13.sp,fontWeight=FontWeight.SemiBold)
                val stats=r.getJSONObject(key)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(13.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){StatKeys.forEachIndexed{i,k->LText("${ui.text(statLabels[i])} ${stats.getInt(k)}",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
            }
            LText("Habilidades: ${r.optString("attackerAbility")} × ${r.optString("defenderAbility")}",fontSize=13.sp)
            val rolls=r.optJSONArray("rolls")
            if(rolls!=null&&rolls.length()>0)LText("Variações de dano: "+(0 until rolls.length()).joinToString(" · "){rolls.getInt(it).toString()},fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            LText(r.optString("description"),fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable internal fun DummyEditorScreen(catalog:List<Pokemon>,repo:PokemonRepository,existing:TrainingDummy?,onBack:()->Unit,onSave:(TrainingDummy)->Unit){
    var pokemonId by rememberSaveable(existing?.id){mutableStateOf(existing?.pokemonId)}
    var label by rememberSaveable(existing?.id){mutableStateOf(existing?.label?:"")}
    var query by rememberSaveable{mutableStateOf("")}
    val p=catalog.find{it.id==pokemonId}
    var moves by remember(p?.id){mutableStateOf<List<Move>>(emptyList())}
    var items by remember(p?.id){mutableStateOf<List<HeldItem>>(emptyList())}
    LaunchedEffect(p){if(p!=null){moves=repo.movesFor(p);items=repo.heldItemsFor(p)}}
    LazyColumn(Modifier.prismaViewport(top=0.dp,bottom=0.dp).imePadding(),contentPadding=PaddingValues(top=8.dp,bottom=48.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        item{Column(Modifier.padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(13.dp)){
            IconButton(onClick=onBack){LIcon(Icons.AutoMirrored.Rounded.ArrowBack,"Voltar")}
            LText(if(existing==null)"Novo alvo\nde teste." else "Editar alvo\nde teste.",fontSize=30.sp,lineHeight=34.sp,fontWeight=FontWeight.Bold)
            LText("Configure um defensor sem ocupar vaga na equipe. Ele fica salvo no aparelho e pode ser reutilizado nas simulações.",fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(label,{label=it.take(50)},label={LText("Nome do alvo")},placeholder={LText("Ex.: Venusaur defensivo")},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp))
            if(p==null){OutlinedTextField(query,{query=it},placeholder={LText("Buscar Pokémon")},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp))}
            else{Row(verticalAlignment=Alignment.CenterVertically){PokemonArt(p.id,Modifier.size(80.dp),p.name);LText(p.name,Modifier.weight(1f),fontSize=18.sp,fontWeight=FontWeight.SemiBold);TextButton(onClick={pokemonId=null}){LText("Trocar")}}}
        }}
        if(p==null){items(catalog.filter{it.champions&&it.matchesQuery(query)}.sortedBy{it.baseId},key={it.id}){pokemon->Surface(onClick={pokemonId=pokemon.id;if(label.isBlank())label="${pokemon.name} · alvo"},shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface,modifier=Modifier.padding(horizontal=24.dp)){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){PokemonArt(pokemon.id,Modifier.size(56.dp),pokemon.name);LText(pokemon.name,Modifier.padding(start=14.dp),fontSize=15.sp)}}}}
        else item(key="dummy-build/${p.id}"){BuildEditor(p,existing?.takeIf{it.pokemonId==p.id}?.build?:Build(),true,moves,items,{},onSave={build->onSave(TrainingDummy(existing?.id?:UUID.randomUUID().toString(),label.trim().ifBlank{"${p.name} · alvo"},p.id,build))})}
    }
}
