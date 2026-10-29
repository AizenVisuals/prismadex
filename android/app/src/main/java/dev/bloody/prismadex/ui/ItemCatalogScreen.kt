package dev.bloody.prismadex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable internal fun ItemCatalogScreen(){
    val context=LocalContext.current
    val repo=remember{ItemCatalogRepository(context)}
    var items by remember{mutableStateOf<List<ReferenceItem>>(emptyList())}
    var error by remember{mutableStateOf(false)}
    var query by remember{mutableStateOf("")}
    var category by remember{mutableStateOf("")}
    LaunchedEffect(repo){try{items=repo.all()}catch(e:CancellationException){throw e}catch(_:Exception){error=true}}
    val language=LocalUiStrings.current.language
    val categories=remember(items,language){items.map{it.category}.distinct().sortedBy{referenceCategoryName(it,language)}}
    val indexed by produceState<List<Pair<ReferenceItem,String>>>(emptyList(),items){value=withContext(Dispatchers.Default){items.map{it to pokemonNameKey(it.name)}}}
    val matches by produceState<List<ReferenceItem>>(emptyList(),indexed,query,category){value=withContext(Dispatchers.Default){val key=pokemonNameKey(query);indexed.filter{(item,name)->(category.isEmpty()||item.category==category)&&(key.isBlank()||name.contains(key)||item.id.toString()==query.trim().removePrefix("#"))}.map{it.first}}}
    LazyColumn(Modifier.prismaViewport(),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
            LText("Catálogo de itens",fontSize=25.sp,fontWeight=FontWeight.Bold)
            LText("Referência geral offline. Esta lista não confirma itens seguráveis nem disponibilidade em Champions ou em outros jogos.",fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(query,{query=it},singleLine=true,label={LText("Buscar item")},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp))
            ChoiceField("CATEGORIA",if(category.isEmpty())LocalUiStrings.current.text("Todas as categorias") else referenceCategoryName(category,language),listOf("" to LocalUiStrings.current.text("Todas as categorias"))+categories.map{it to referenceCategoryName(it,language)}){category=it}
            LText("${matches.size} itens",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(error)LText("Não foi possível abrir o catálogo local de itens.",color=MaterialTheme.colorScheme.error,fontSize=13.sp)
            if(items.isEmpty()&&!error)LinearProgressIndicator(Modifier.fillMaxWidth())
            if(items.isNotEmpty()&&matches.isEmpty())LText("Nenhum item encontrado. Experimente outro nome ou categoria.",fontSize=14.sp)
        }}
        items(matches,key={it.id}){item->Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface){
            Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    if(item.iconId!=null)ItemArt(item.iconId,item.name,Modifier.size(38.dp)) else LIcon(Icons.Rounded.Inventory2,item.name,Modifier.size(38.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(Modifier.weight(1f)){Text(item.name,fontSize=16.sp,fontWeight=FontWeight.SemiBold);Text(referenceCategoryName(item.category,language),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                }
                item.description?.let{description->
                    Text(description.text(language),fontSize=14.sp,lineHeight=21.sp)
                }?:LText("Descrição de efeito não disponível nesta fonte.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                item.flingPower?.let{LText("Poder de Fling: $it",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            }
        }}
    }
}

private val referenceCategoryNames=mapOf(
        "stat-boosts" to ("Reforço de atributos" to "Mejoras de atributos"),"effort-drop" to ("Redução de treinamento" to "Reducción de entrenamiento"),"medicine" to ("Medicinais" to "Medicinales"),"other" to ("Outros" to "Otros"),"in-a-pinch" to ("Ativação em emergência" to "Activación en emergencia"),"picky-healing" to ("Cura com preferência" to "Curación con preferencia"),"type-protection" to ("Proteção por tipo" to "Protección por tipo"),"baking-only" to ("Culinária" to "Cocina"),"collectibles" to ("Colecionáveis" to "Coleccionables"),"evolution" to ("Evolução" to "Evolución"),"spelunking" to ("Exploração subterrânea" to "Exploración subterránea"),"held-items" to ("Itens segurados" to "Objetos equipados"),"choice" to ("Itens Choice" to "Objetos Choice"),"effort-training" to ("Treinamento de atributos" to "Entrenamiento de atributos"),"bad-held-items" to ("Itens segurados prejudiciais" to "Objetos equipados perjudiciales"),"training" to ("Treinamento" to "Entrenamiento"),"plates" to ("Plates" to "Plates"),"species-specific" to ("Específicos de espécie" to "Específicos de especie"),"type-enhancement" to ("Reforço de tipo" to "Mejora de tipo"),"event-items" to ("Eventos" to "Eventos"),"gameplay" to ("Utilitários" to "Utilitarios"),"plot-advancement" to ("História" to "Historia"),"unused" to ("Sem uso registrado" to "Sin uso registrado"),"loot" to ("Tesouros" to "Tesoros"),"all-mail" to ("Cartas" to "Cartas"),"vitamins" to ("Vitaminas" to "Vitaminas"),"healing" to ("Cura" to "Curación"),"pp-recovery" to ("Recuperação de PP" to "Recuperación de PP"),"revival" to ("Reanimação" to "Reanimación"),"status-cures" to ("Cura de condições" to "Curación de estados"),"mulch" to ("Fertilizantes" to "Fertilizantes"),"special-balls" to ("Poké Balls especiais" to "Poké Balls especiales"),"standard-balls" to ("Poké Balls padrão" to "Poké Balls estándar"),"dex-completion" to ("Conclusão da Pokédex" to "Completar la Pokédex"),"scarves" to ("Lenços" to "Pañuelos"),"all-machines" to ("TM/HM/TR" to "MT/MO/DT"),"flutes" to ("Flautas" to "Flautas"),"apricorn-balls" to ("Poké Balls de Apricorn" to "Poké Balls de Apricorn"),"apricorn-box" to ("Apricorns" to "Apricorns"),"data-cards" to ("Cartões de dados" to "Tarjetas de datos"),"jewels" to ("Gems" to "Gems"),"miracle-shooter" to ("Wonder Launcher" to "Wonder Launcher"),"mega-stones" to ("Mega Stones" to "Mega Stones"),"memories" to ("Memories" to "Memories"),"z-crystals" to ("Z-Crystals" to "Z-Crystals"),"species-candies" to ("Candies por espécie" to "Candies por especie"),"catching-bonus" to ("Bônus de captura" to "Bonificaciones de captura"),"dynamax-crystals" to ("Dynamax Crystals" to "Dynamax Crystals"),"nature-mints" to ("Nature Mints" to "Nature Mints"),"curry-ingredients" to ("Ingredientes de curry" to "Ingredientes de curry"),"tera-shard" to ("Tera Shards" to "Tera Shards"),"sandwich-ingredients" to ("Ingredientes de sanduíche" to "Ingredientes de bocadillo"),"tm-materials" to ("Materiais de TM" to "Materiales de MT"),"picnic" to ("Piquenique" to "Pícnic")
    )

private fun referenceCategoryName(key:String,language:String):String{
    return when(language){"pt"->referenceCategoryNames[key]?.first;"es"->referenceCategoryNames[key]?.second;else->null}?:key.replace('-',' ').replaceFirstChar{it.uppercase()}
}
