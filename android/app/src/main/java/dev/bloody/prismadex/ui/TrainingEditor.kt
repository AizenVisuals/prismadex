package dev.bloody.prismadex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.data.*

internal val statLabels=listOf("HP","Ataque","Defesa","Atq. esp.","Def. esp.","Velocidade")

@Composable internal fun ChoiceField(label:String,value:String,choices:List<Pair<String,String>>,onSelect:(String)->Unit){
    val ui=LocalUiStrings.current
    var open by remember {mutableStateOf(false)}
    var query by remember {mutableStateOf("")}
    Box {
        Surface(Modifier.fillMaxWidth().clickable {open=true},shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.55f)) {
            Row(Modifier.padding(16.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Column(Modifier.weight(1f)){LText(label,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);LText(value,modifier=Modifier.padding(top=5.dp),fontSize=15.sp)};LIcon(androidx.compose.material.icons.Icons.Rounded.ExpandMore,null)}
        }
        if(choices.size<=7)DropdownMenu(expanded=open,onDismissRequest={open=false},modifier=Modifier.heightIn(max=320.dp)){choices.forEach{(key,text)->DropdownMenuItem(text={LText(text,fontSize=14.sp)},onClick={onSelect(key);open=false})}}
    }
    if(open&&choices.size>7)AlertDialog(onDismissRequest={open=false;query=""},title={LText(label)},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        OutlinedTextField(query,{query=it},label={LText("Buscar opção")},singleLine=true,modifier=Modifier.fillMaxWidth())
        androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max=360.dp)){
            val filtered=choices.filter{ui.text(it.second).contains(query,true)||it.second.contains(query,true)}
            if(filtered.isEmpty())item{LText("Nenhuma opção encontrada.",Modifier.padding(12.dp))}
            items(filtered.size){index->val (key,text)=filtered[index];TextButton(onClick={onSelect(key);open=false;query=""},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){LText(text,modifier=Modifier.fillMaxWidth(),fontSize=15.sp)}}
        }
    }},confirmButton={TextButton(onClick={open=false;query=""}){LText("Cancelar")}})
}

@Composable internal fun TrainingEditor(p:Pokemon,nature:String,ability:String?,sps:List<Int>,onNature:(String)->Unit,onAbility:(String?)->Unit,onSps:(List<Int>)->Unit){
    var importing by remember {mutableStateOf(false)}
    val assets=LocalContext.current.assets
    val statColumns=if(androidx.compose.ui.platform.LocalDensity.current.fontScale>1.25f)2 else 3
    val defaultAbility by produceState<String?>(null,p.id){value=withContext(Dispatchers.IO){DefaultAbilities.forPokemon(assets,p.id)}}
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        LText("TREINAMENTO · CHAMPIONS",fontSize=13.sp,letterSpacing=1.4.sp,color=MaterialTheme.colorScheme.primary)
        ChoiceField("Natureza / alinhamento",nature,NatureNames.map{it to it},onNature)
        ChoiceField("Habilidade do Pokémon",ability?.abilityLabel()?:defaultAbility?.let{"$it · padrão"}?:"Padrão da espécie",listOf("" to (defaultAbility?.let{"$it · padrão"}?:"Padrão da espécie"))+p.abilities.map{it to it.abilityLabel()}){onAbility(it.takeIf{v->v.isNotBlank()})}
        LText("${sps.sum()}/66 SP · nível 50",fontSize=14.sp,color=if(sps.sum()>66)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        for(row in 0 until 6/statColumns){Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){for(col in 0 until statColumns){val i=row*statColumns+col;OutlinedTextField(value=sps[i].toString(),onValueChange={raw->if(raw.isEmpty()||raw.all{it.isDigit()})onSps(sps.toMutableList().apply{set(i,(raw.toIntOrNull()?:0).coerceIn(0,32))})},label={LText(statLabels[i],fontSize=13.sp)},suffix={LText("SP",fontSize=13.sp)},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),shape=RoundedCornerShape(16.dp),modifier=Modifier.weight(1f))}}}
        LText("Champions usa pontos de atributo (SP): até 32 em cada atributo e 66 no total. IVs e nível não são editáveis neste modo.",fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick={importing=true}){LText("Converter EVs de outro jogo para SP")}
    }
    if(importing){
        var evs by remember {mutableStateOf(List(6){0})}
        val converted=evs.map{(it+7)/8}
        AlertDialog(onDismissRequest={importing=false},title={LText("EVs → SP")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
            LText("Conversão usada pelo modo Champions do Showdown. Os valores convertidos serão salvos em SP.",fontSize=13.sp)
            for(i in 0..5)OutlinedTextField(evs[i].toString(),{raw->if(raw.isBlank()||raw.all{it.isDigit()})evs=evs.toMutableList().apply{set(i,(raw.toIntOrNull()?:0).coerceIn(0,252))}},label={LText(statLabels[i])},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
            LText("${evs.sum()}/510 EVs → ${converted.sum()}/66 SP",fontSize=13.sp)
        }},confirmButton={TextButton(enabled=evs.sum()<=510&&converted.sum()<=66,onClick={onSps(converted);importing=false}){LText("Usar SP convertidos")}},dismissButton={TextButton(onClick={importing=false}){LText("Cancelar")}})
    }
}

private fun String.abilityLabel()=split('-').joinToString(" "){it.replaceFirstChar{c->c.uppercaseChar()}}

private object DefaultAbilities{
    private var table:Map<String,String>?=null
    @Synchronized fun forPokemon(assets:android.content.res.AssetManager,id:Int):String?{
        val values=table?:JSONObject(assets.open("calc-default-abilities.json").bufferedReader().use{it.readText()}).let{json->json.keys().asSequence().associateWith{json.getString(it)}}.also{table=it}
        return values[id.toString()]?.takeIf{it.isNotBlank()}
    }
}
