@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package dev.bloody.prismadex.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import dev.bloody.prismadex.R
import dev.bloody.prismadex.data.UserState

internal data class ThemeColors(val accent:Color,val background:Color,val surface:Color,val light:Boolean)
internal data class AppTheme(val key:String,val name:String,val detail:String,val dark:ThemeColors,val day:ThemeColors){
    fun colors(light:Boolean)=if(light)day else dark
}
private fun dark(accent:Long,bg:Long,surface:Long)=ThemeColors(Color(accent),Color(bg),Color(surface),false)
private fun day(accent:Long,bg:Long,surface:Long)=ThemeColors(Color(accent),Color(bg),Color(surface),true)
internal val AppThemes=listOf(
    AppTheme("aurora","Aurora","Grafite + lilás",dark(0xFFC4B5FD,0xFF10121B,0xFF1A1C29),day(0xFF6750A4,0xFFF4F0FF,0xFFFFFBFF)),
    AppTheme("carmine","Carmim","Grafite + carmim",dark(0xFFFF9EB5,0xFF1B1017,0xFF2B1C28),day(0xFFA32D50,0xFFFFF1F4,0xFFFFFAFB)),
    AppTheme("ocean","Oceano","Azul + ciano",dark(0xFF8CDAF4,0xFF0B1720,0xFF172B38),day(0xFF006785,0xFFEDF8FD,0xFFFAFDFF)),
    AppTheme("jade","Jade","Verde + menta",dark(0xFF9BE2BE,0xFF0C1915,0xFF1A2D25),day(0xFF1D6B48,0xFFEFF8F1,0xFFFAFFFB)),
    AppTheme("amber","Âmbar","Grafite + dourado",dark(0xFFF4D18C,0xFF1B160E,0xFF2D261A),day(0xFF805409,0xFFFFF7E8,0xFFFFFCF7)),
    AppTheme("pearl","Pérola","Prata + violeta",dark(0xFFCBCBDF,0xFF15151B,0xFF22222B),day(0xFF5C5D75,0xFFF3F3F5,0xFFFFFFFF)),
)
@Composable internal fun AppearancePicker(selected:String,onSelect:(String)->Unit){
    FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
        listOf("system" to "Sistema","light" to "Claro","dark" to "Escuro").forEach{(key,label)->
            FilterChip(selected==key,{onSelect(key)},label={LText(label)},modifier=Modifier.heightIn(min=48.dp))
        }
    }
}
@Composable internal fun PrismaMark(modifier:Modifier=Modifier,color:Color=MaterialTheme.colorScheme.primary){LIcon(painterResource(R.drawable.ic_prisma_mark),"Símbolo do PrismaDex",modifier,tint=color)}

@Composable internal fun ThemePicker(selected:String,light:Boolean=MaterialTheme.colorScheme.background.luminance()>.5f,onSelect:(String)->Unit){
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        for(row in AppThemes.chunked(2)){Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){row.forEach{palette->
            val theme=palette.colors(light)
            Surface(onClick={onSelect(palette.key)},shape=RoundedCornerShape(22.dp),border=BorderStroke(if(selected==palette.key)2.dp else 1.dp,if(selected==palette.key)theme.accent else theme.accent.copy(alpha=.18f)),modifier=Modifier.weight(1f)){
                Column(Modifier.background(Brush.linearGradient(listOf(theme.background,theme.surface))).padding(16.dp).heightIn(min=100.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){PrismaMark(Modifier.size(36.dp),color=theme.accent);Spacer(Modifier.weight(1f));if(selected==palette.key)LIcon(Icons.Rounded.Check,"Tema selecionado",Modifier.size(18.dp),tint=theme.accent)}
                    LText(palette.name,fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=if(theme.light)Color(0xFF28263A) else Color(0xFFF2F0FA))
                    LText(palette.detail,fontSize=13.sp,color=theme.accent)
                }
            }
        }}}
    }
}

@Composable internal fun AppDrawer(selected:String,onNavigate:(String)->Unit,onAccount:()->Unit){
    ModalDrawerSheet(Modifier.widthIn(max=320.dp).fillMaxHeight(),drawerContainerColor=MaterialTheme.colorScheme.surface){
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
        Column(Modifier.padding(24.dp)){
            PrismaMark(Modifier.size(66.dp));LText("PrismaDex",fontSize=27.sp,fontWeight=FontWeight.Bold);LText("Seu laboratório Champions",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=8.dp,bottom=10.dp))
        }
        listOf(Triple("dex","Pokédex",Icons.Rounded.CatchingPokemon),Triple("favorites","Favoritos",Icons.Rounded.FavoriteBorder),Triple("team","TeamBuilder",Icons.Rounded.Hexagon),Triple("calc","Calculadora de dano",Icons.Rounded.AutoAwesome),Triple("meta","Meta online",Icons.Rounded.Public),Triple("items","Catálogo de itens",Icons.Rounded.Info),Triple("settings","Configurações e temas",Icons.Rounded.Settings),Triple("sources","Champions e fontes",Icons.Rounded.Info)).forEach{(key,title,icon)->NavigationDrawerItem(label={LText(title,fontSize=14.sp,lineHeight=18.sp,maxLines=2)},selected=selected==key,onClick={onNavigate(key)},icon={LIcon(icon,null)},modifier=Modifier.padding(horizontal=12.dp,vertical=2.dp))}
        Spacer(Modifier.height(24.dp));HorizontalDivider(Modifier.padding(horizontal=24.dp));NavigationDrawerItem(label={LText("Minha conta")},selected=false,onClick=onAccount,icon={LIcon(Icons.Rounded.PersonOutline,null)},modifier=Modifier.padding(12.dp))
        }
    }
}

@Composable internal fun SettingsScreen(state:UserState,email:String?,onState:(UserState)->Unit,onAccount:()->Unit,onSources:()->Unit){
    val display=LocalDisplayPreferences.current
    val textSizeLabel=LocalUiStrings.current.text("Tamanho do texto")
    var pendingTextSize by remember(display.textScale){mutableFloatStateOf(DisplayPreferences.textSizes.indexOf(display.textScale).toFloat())}
    val previewScale=DisplayPreferences.textSizes[pendingTextSize.roundToInt().coerceIn(0,4)]
    val density=LocalDensity.current
    LazyColumn(Modifier.prismaViewport(),contentPadding=PaddingValues(start=22.dp,end=22.dp,top=16.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){
        item(key="heading"){LText("O seu PrismaDex.",fontSize=29.sp,lineHeight=34.sp,fontWeight=FontWeight.Bold)}
        item(key="language"){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
            LText("Idioma",fontSize=20.sp,fontWeight=FontWeight.SemiBold)
            val languages=listOf("system" to "Seguir o Android","pt" to "Português (Brasil)","en" to "English","es" to "Español")
            ChoiceField("IDIOMA DO APLICATIVO",languages.first{it.first==display.language}.second,languages){display.selectLanguage(it)}
            LText("A interface funciona offline nos três idiomas. Nomes de golpes e itens permanecem originais; descrições acompanham o idioma escolhido.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }}
        item(key="text-size"){Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){LText("Tamanho do texto",Modifier.weight(1f),fontSize=20.sp,fontWeight=FontWeight.SemiBold);LText("${(previewScale*100).roundToInt()}%",color=MaterialTheme.colorScheme.primary)}
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                LText("A",fontSize=13.sp)
                Slider(value=pendingTextSize,onValueChange={pendingTextSize=it},onValueChangeFinished={display.selectTextScale(previewScale)},valueRange=0f..4f,steps=3,modifier=Modifier.weight(1f).semantics{contentDescription=textSizeLabel;stateDescription="${(previewScale*100).roundToInt()}%"})
                LText("A",fontSize=24.sp)
            }
            CompositionLocalProvider(LocalDensity provides Density(density.density,density.fontScale/display.textScale*previewScale)){
            Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                LText("Sua próxima equipe começa aqui.",fontSize=18.sp,fontWeight=FontWeight.SemiBold)
                LText("Explore Pokémon, compare golpes e prepare cada detalhe do seu time.",fontSize=14.sp,lineHeight=21.sp)
            }}
            }
            LText("Ajuste de 85% a 150%, combinado com o tamanho de texto do Android. A prévia muda na hora.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick={display.selectTextScale(1f)}){LText("Restaurar tamanho padrão")}
        }}
        item(key="themes"){Column(verticalArrangement=Arrangement.spacedBy(13.dp)){LText("Aparência",fontSize=20.sp,fontWeight=FontWeight.SemiBold);LText("Seis paletas, cada uma com versão clara e escura. O modo Sistema acompanha a aparência do Android.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);AppearancePicker(state.appearanceMode){onState(state.copy(appearanceMode=it))};ThemePicker(state.theme,light=when(state.appearanceMode){"light"->true;"dark"->false;else->!androidx.compose.foundation.isSystemInDarkTheme()}){onState(state.copy(theme=it))}}}
        item(key="layout"){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){LText("Visualização da Pokédex",fontSize=17.sp,fontWeight=FontWeight.SemiBold);Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){FilterChip(state.layout=="grid",{onState(state.copy(layout="grid"))},label={LText("Grade")});FilterChip(state.layout=="list",{onState(state.copy(layout="list"))},label={LText("Lista")})}}}
        item(key="battle"){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){LText("Formato preferido",fontSize=17.sp,fontWeight=FontWeight.SemiBold);FlowRow(horizontalArrangement=Arrangement.spacedBy(9.dp)){FilterChip(state.battleFormat=="singles",{onState(state.copy(battleFormat="singles"))},label={LText("Singles · individual")});FilterChip(state.battleFormat=="doubles",{onState(state.copy(battleFormat="doubles"))},label={LText("Doubles · duplas")})};LText("Usado no TeamBuilder, no Meta e como padrão da calculadora. Os dois formatos têm regras próprias; conferir o regulamento continua necessário.",fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
        item(key="offline"){SettingsCard("Dados e modo offline","Os 1.306 registros, as artes, golpes, equipe, favoritos e alvos de teste ficam no aparelho. Apenas a consulta e atualização do Meta usam internet. O último relatório do Meta fica em cache.")}
        item(key="accessibility"){SettingsCard("Acessibilidade e movimento","O tamanho de texto escolhido aqui é combinado com a configuração do Android. O app acompanha a escala de animação do sistema. Os ícones de tipos também têm rótulos; a rolagem continua disponível por gesto.")}
        item(key="account"){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){LText("Conta e privacidade",fontSize=17.sp,fontWeight=FontWeight.SemiBold);LText(email?:"Você está usando como visitante.",fontSize=13.sp);LText("As contas desta versão são locais. Não há sincronização entre aparelhos, confirmação de e-mail ou recuperação de senha. Senhas são armazenadas como hash.",fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);OutlinedButton(onClick=onAccount,modifier=Modifier.fillMaxWidth()){LText(if(email==null)"Entrar ou criar conta local" else "Gerenciar minha conta")}}}
        item(key="about"){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){LText("Sobre o PrismaDex",fontSize=17.sp,fontWeight=FontWeight.SemiBold);LText("Prévia Android nativa · projeto de fã independente. Código GPL-3.0, com componentes MIT, Apache-2.0 e MPL-2.0. Personagens e marcas pertencem aos titulares.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);TextButton(onClick=onSources){LText("Champions, fontes e limites dos dados")}}}
    }
}
@Composable private fun SettingsCard(title:String,body:String){Surface(shape=RoundedCornerShape(23.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(11.dp)){LText(title,fontSize=17.sp,fontWeight=FontWeight.SemiBold);LText(body,fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
