package dev.bloody.prismadex.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.data.Pokemon

/** No wrapping: the first and last Pokémon remain real list boundaries. */
internal fun adjacentPokemonId(order:List<Int>,current:Int,step:Int):Int? {
    if(step!=1&&step!= -1)return null
    val index=order.indexOf(current)
    return if(index<0)null else order.getOrNull(index+step)
}

@Composable internal fun Modifier.pokemonSwipe(id:Int,enabled:Boolean,previous:Int?,next:Int?,onBrowse:(Int)->Unit):Modifier {
    val browse=rememberUpdatedState(onBrowse)
    return if(!enabled)this else pointerInput(id,previous,next){
        var distance=0f
        val threshold=64.dp.toPx()
        detectHorizontalDragGestures(
            onDragStart={distance=0f},onDragCancel={distance=0f},
            onDragEnd={
                val target=if(distance<= -threshold)next else if(distance>=threshold)previous else null
                distance=0f;target?.let{browse.value(it)}
            },onHorizontalDrag={change,amount->change.consume();distance+=amount}
        )
    }
}

@Composable internal fun PokemonBrowseControls(previous:Pokemon?,next:Pokemon?,enabled:Boolean,onBrowse:(Int)->Unit){
    Column(Modifier.fillMaxWidth().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(2.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedButton(onClick={previous?.let{onBrowse(it.id)}},enabled=enabled&&previous!=null,modifier=Modifier.weight(1f).heightIn(min=56.dp),shape=RoundedCornerShape(18.dp),contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp)){
                LIcon(Icons.AutoMirrored.Rounded.ArrowBack,"Pokémon anterior",Modifier.size(20.dp))
                Text(previous?.name?:LocalUiStrings.current.text("Início da lista"),Modifier.weight(1f).padding(start=8.dp),fontSize=13.sp,textAlign=TextAlign.Start,maxLines=2,overflow=TextOverflow.Ellipsis)
            }
            OutlinedButton(onClick={next?.let{onBrowse(it.id)}},enabled=enabled&&next!=null,modifier=Modifier.weight(1f).heightIn(min=56.dp),shape=RoundedCornerShape(18.dp),contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp)){
                Text(next?.name?:LocalUiStrings.current.text("Fim da lista"),Modifier.weight(1f).padding(end=8.dp),fontSize=13.sp,textAlign=TextAlign.End,maxLines=2,overflow=TextOverflow.Ellipsis)
                LIcon(Icons.AutoMirrored.Rounded.ArrowForward,"Próximo Pokémon",Modifier.size(20.dp))
            }
        }
        LText(if(enabled)"Deslize para os lados para trocar de Pokémon." else "Salve ou descarte o rascunho antes de trocar de Pokémon.",Modifier.padding(horizontal=8.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,lineHeight=17.sp)
    }
}
