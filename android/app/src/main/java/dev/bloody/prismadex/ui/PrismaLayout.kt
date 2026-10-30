package dev.bloody.prismadex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Chrome owns space; scrolling content never shares hit areas with navigation. */
internal data class PrismaLayout(val expanded:Boolean=false,val bottom:Dp=92.dp)
internal val LocalPrismaLayout=staticCompositionLocalOf{PrismaLayout()}
@Composable internal fun Modifier.prismaViewport(top:Dp=72.dp,bottom:Dp=LocalPrismaLayout.current.bottom,maxWidth:Dp=840.dp):Modifier =
    fillMaxSize().padding(top=top,bottom=bottom).clipToBounds()
        .wrapContentWidth(Alignment.CenterHorizontally).widthIn(max=maxWidth)

@Composable internal fun PrismaNavigation(selected:String,onNavigate:(String)->Unit,modifier:Modifier=Modifier,vertical:Boolean=false){
    val destinations=listOf(Triple("dex","Pokédex",androidx.compose.material.icons.Icons.Rounded.CatchingPokemon),Triple("favorites","Favoritos",androidx.compose.material.icons.Icons.Rounded.FavoriteBorder),Triple("team","Equipe",androidx.compose.material.icons.Icons.Rounded.Hexagon),Triple("calc","Danos",androidx.compose.material.icons.Icons.Rounded.AutoAwesome),Triple("meta","Meta",androidx.compose.material.icons.Icons.Rounded.Public))
    val compactLabels=LocalDensity.current.fontScale>1.25f
    val colors=androidx.compose.material3.MaterialTheme.colorScheme
    dev.bloody.prismadex.glass.LiquidGlass(modifier,radius=30.dp,tint=colors.surface.copy(alpha=if(colors.background.luminance()>.5f).6f else .35f)){
        if(vertical)Column(Modifier.fillMaxSize().padding(6.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            destinations.forEach{(key,label,icon)->PrismaDestination(key==selected,label,icon,Modifier.weight(1f).fillMaxWidth(),vertical=true){onNavigate(key)}}
        }else Row(Modifier.fillMaxSize().padding(6.dp)){
            destinations.forEach{(key,label,icon)->
                val weight by animateFloatAsState(if(compactLabels&&key==selected)1.75f else 1f,if(LocalReduceMotion.current)snap() else tween(220),label="navigation width")
                PrismaDestination(key==selected,label,icon,Modifier.weight(weight).fillMaxHeight()){onNavigate(key)}
            }
        }
    }
}
@Composable private fun PrismaDestination(active:Boolean,label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier,vertical:Boolean=false,onClick:()->Unit){
    val largeLabel=LocalDensity.current.fontScale>1.25f&&!vertical
    val showLabel=!largeLabel||active
    val name=LocalUiStrings.current.text(label)
    val colors=androidx.compose.material3.MaterialTheme.colorScheme
    val tint by androidx.compose.animation.animateColorAsState(if(active)colors.primary else colors.onSurfaceVariant,label="navigation state")
    Column(modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
        .background(if(active)colors.primary.copy(.17f) else androidx.compose.ui.graphics.Color.Transparent)
        .selectable(selected=active,role=androidx.compose.ui.semantics.Role.Tab,onClick=onClick)
        .then(if(!showLabel)Modifier.semantics{contentDescription=name} else Modifier)
        .padding(horizontal=2.dp,vertical=4.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
        LIcon(icon,null,Modifier.size(23.dp),tint=tint)
        AnimatedVisibility(showLabel,enter=expandEnter(LocalReduceMotion.current),exit=shrinkExit(LocalReduceMotion.current)){
            Column{Spacer(Modifier.height(3.dp));LText(label,Modifier.fillMaxWidth(),fontSize=12.sp,lineHeight=16.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center,color=tint,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)}
        }
    }
}
