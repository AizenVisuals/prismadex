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
import androidx.compose.foundation.border
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Chrome owns space; scrolling content never shares hit areas with navigation. */
internal data class PrismaLayout(val expanded:Boolean=false,val bottom:Dp=92.dp)
internal val LocalPrismaLayout=staticCompositionLocalOf{PrismaLayout()}
@Composable internal fun Modifier.prismaViewport(top:Dp=72.dp,bottom:Dp=LocalPrismaLayout.current.bottom,maxWidth:Dp=840.dp):Modifier =
    fillMaxSize().padding(top=top,bottom=bottom).clipToBounds()
        .wrapContentWidth(Alignment.CenterHorizontally).widthIn(max=maxWidth)

@Composable internal fun PrismaNavigation(selected:String,onNavigate:(String)->Unit,modifier:Modifier=Modifier,vertical:Boolean=false){
    val destinations=listOf(Triple("dex","Pokédex",androidx.compose.material.icons.Icons.Rounded.CatchingPokemon),Triple("favorites","Favoritos",androidx.compose.material.icons.Icons.Rounded.FavoriteBorder),Triple("team","Equipe",androidx.compose.material.icons.Icons.Rounded.Hexagon),Triple("calc","Danos",androidx.compose.material.icons.Icons.Rounded.AutoAwesome),Triple("meta","Meta",androidx.compose.material.icons.Icons.Rounded.Public))
    val colors=androidx.compose.material3.MaterialTheme.colorScheme
    // Impeccable redo (11:32): one rhythm for both orientations — 8dp
    // outer padding, 4dp gaps, EQUAL cells, 56dp+ touch height, label
    // always present at default font scale. Active = filled pill +
    // primary tint + semibold label; inactive = plain + variant tint.
    // No animated width: equal weights never shift the bar when the
    // selection changes. At fontScale > 1.25 only the ACTIVE label shows
    // in the horizontal bar (icon-only elsewhere), so labels never clip.
    // Opaque bar (30/10/2026): solid theme surface, no translucent glass
    // tint mixing over the list. LiquidGlass stays in glass/ unused.
    Box(modifier.background(colors.surface,androidx.compose.foundation.shape.RoundedCornerShape(28.dp)).border(1.dp,colors.outlineVariant,androidx.compose.foundation.shape.RoundedCornerShape(28.dp))){
        if(vertical)Column(Modifier.fillMaxSize().padding(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
            destinations.forEach{(key,label,icon)->PrismaDestination(key==selected,label,icon,Modifier.weight(1f).fillMaxWidth(),vertical=true){onNavigate(key)}}
        }else Row(Modifier.fillMaxSize().padding(horizontal=8.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){
            destinations.forEach{(key,label,icon)->PrismaDestination(key==selected,label,icon,Modifier.weight(1f).fillMaxHeight()){onNavigate(key)}}
        }
    }
}
@Composable private fun PrismaDestination(active:Boolean,label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier,vertical:Boolean=false,onClick:()->Unit){
    val largeLabel=LocalDensity.current.fontScale>1.25f&&!vertical
    val showLabel=!largeLabel||active
    val name=LocalUiStrings.current.text(label)
    val colors=androidx.compose.material3.MaterialTheme.colorScheme
    val tint by androidx.compose.animation.animateColorAsState(if(active)colors.primary else colors.onSurfaceVariant,label="navigation state")
    val container by androidx.compose.animation.animateColorAsState(if(active)solidBlend(colors.primary,colors.surface,.20f) else androidx.compose.ui.graphics.Color.Transparent,label="navigation container")
    Column(modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
        .background(container)
        .selectable(selected=active,role=androidx.compose.ui.semantics.Role.Tab,onClick=onClick)
        .then(if(!showLabel)Modifier.semantics{contentDescription=name} else Modifier)
        .padding(horizontal=8.dp,vertical=8.dp)
        .heightIn(min=56.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
        LIcon(icon,null,Modifier.size(24.dp),tint=tint)
        AnimatedVisibility(showLabel,enter=fadeIn(tween(180)),exit=fadeOut(tween(120))){
            Column(horizontalAlignment=Alignment.CenterHorizontally){
                Spacer(Modifier.height(4.dp))
                LText(label,style=androidx.compose.material3.MaterialTheme.typography.labelMedium,fontWeight=if(active)FontWeight.SemiBold else FontWeight.Medium,textAlign=TextAlign.Center,color=tint,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
        }
    }
}
