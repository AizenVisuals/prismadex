package dev.bloody.prismadex.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
        listOf("system" to "Sistema","light" to "Claro","dark" to "Escuro").forEach{(key,label)->
            FilterChip(selected==key,{onSelect(key)},label={Text(label)},modifier=Modifier.weight(1f))
        }
    }
}
@Composable internal fun PrismaMark(modifier:Modifier=Modifier,color:Color=MaterialTheme.colorScheme.primary){Icon(painterResource(R.drawable.ic_prisma_mark),"Símbolo do PrismaDex",modifier,tint=color)}

@Composable internal fun ThemePicker(selected:String,light:Boolean=MaterialTheme.colorScheme.background.luminance()>.5f,onSelect:(String)->Unit){
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        for(row in AppThemes.chunked(2)){Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){row.forEach{palette->
            val theme=palette.colors(light)
            Surface(onClick={onSelect(palette.key)},shape=RoundedCornerShape(22.dp),border=BorderStroke(if(selected==palette.key)2.dp else 1.dp,if(selected==palette.key)theme.accent else theme.accent.copy(alpha=.18f)),modifier=Modifier.weight(1f)){
                Column(Modifier.background(Brush.linearGradient(listOf(theme.background,theme.surface))).padding(16.dp).heightIn(min=100.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){PrismaMark(Modifier.size(36.dp),color=theme.accent);Spacer(Modifier.weight(1f));if(selected==palette.key)Icon(Icons.Rounded.Check,"Tema selecionado",Modifier.size(18.dp),tint=theme.accent)}
                    Text(palette.name,fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=if(theme.light)Color(0xFF28263A) else Color(0xFFF2F0FA))
                    Text(palette.detail,fontSize=13.sp,color=theme.accent)
                }
            }
        }}}
    }
}

@Composable internal fun AppDrawer(selected:String,onNavigate:(String)->Unit,onAccount:()->Unit){
    ModalDrawerSheet(Modifier.widthIn(max=320.dp).fillMaxHeight(),drawerContainerColor=MaterialTheme.colorScheme.surface){
        Column(Modifier.padding(24.dp)){
            PrismaMark(Modifier.size(66.dp));Text("PrismaDex",fontSize=27.sp,fontWeight=FontWeight.Bold);Text("Seu laboratório Champions",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=8.dp,bottom=10.dp))
        }
        listOf(Triple("dex","Pokédex",Icons.Rounded.CatchingPokemon),Triple("favorites","Favoritos",Icons.Rounded.FavoriteBorder),Triple("team","TeamBuilder",Icons.Rounded.Hexagon),Triple("calc","Calculadora de dano",Icons.Rounded.AutoAwesome),Triple("meta","Meta online",Icons.Rounded.Public),Triple("settings","Configurações e temas",Icons.Rounded.Settings),Triple("sources","Champions e fontes",Icons.Rounded.Info)).forEach{(key,title,icon)->NavigationDrawerItem(label={Text(title,fontSize=14.sp)},selected=selected==key,onClick={onNavigate(key)},icon={Icon(icon,null)},modifier=Modifier.padding(horizontal=12.dp,vertical=2.dp))}
        Spacer(Modifier.weight(1f));HorizontalDivider(Modifier.padding(horizontal=24.dp));NavigationDrawerItem(label={Text("Minha conta")},selected=false,onClick=onAccount,icon={Icon(Icons.Rounded.PersonOutline,null)},modifier=Modifier.padding(12.dp))
    }
}

@Composable internal fun SettingsScreen(state:UserState,email:String?,onState:(UserState)->Unit,onAccount:()->Unit,onSources:()->Unit){
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(start=22.dp,end=22.dp,top=88.dp,bottom=120.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){
        item(key="heading"){Column{Text("CONFIGURAÇÕES",fontSize=13.sp,letterSpacing=1.5.sp,color=MaterialTheme.colorScheme.primary);Text("O seu PrismaDex.",fontSize=29.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=10.dp))}}
        item(key="themes"){Column(verticalArrangement=Arrangement.spacedBy(13.dp)){Text("Aparência",fontSize=20.sp,fontWeight=FontWeight.SemiBold);Text("Seis paletas, cada uma com versão clara e escura. O modo Sistema acompanha a aparência do Android.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);AppearancePicker(state.appearanceMode){onState(state.copy(appearanceMode=it))};ThemePicker(state.theme,light=when(state.appearanceMode){"light"->true;"dark"->false;else->!androidx.compose.foundation.isSystemInDarkTheme()}){onState(state.copy(theme=it))}}}
        item(key="layout"){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Visualização da Pokédex",fontSize=17.sp,fontWeight=FontWeight.SemiBold);Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){FilterChip(state.layout=="grid",{onState(state.copy(layout="grid"))},label={Text("Grade")});FilterChip(state.layout=="list",{onState(state.copy(layout="list"))},label={Text("Lista")})}}}
        item(key="battle"){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Formato preferido",fontSize=17.sp,fontWeight=FontWeight.SemiBold);Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){FilterChip(state.battleFormat=="singles",{onState(state.copy(battleFormat="singles"))},label={Text("Singles · individual")});FilterChip(state.battleFormat=="doubles",{onState(state.copy(battleFormat="doubles"))},label={Text("Doubles · duplas")})};Text("Usado no TeamBuilder, no Meta e como padrão da calculadora. Os dois formatos têm regras próprias; conferir o regulamento continua necessário.",fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
        item(key="offline"){SettingsCard("Dados e modo offline","Os 1.306 registros, as artes, golpes, equipe, favoritos e alvos de teste ficam no aparelho. Apenas a consulta e atualização do Meta usam internet. O último relatório do Meta fica em cache.")}
        item(key="accessibility"){SettingsCard("Acessibilidade e movimento","O app acompanha o tamanho de texto e a escala de animação configurados no Android. Os ícones de tipos também têm rótulos; a rolagem continua disponível por gesto.")}
        item(key="account"){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Conta e privacidade",fontSize=17.sp,fontWeight=FontWeight.SemiBold);Text(email?:"Você está usando como visitante.",fontSize=13.sp);Text("As contas desta versão são locais. Não há sincronização entre aparelhos, confirmação de e-mail ou recuperação de senha. Senhas são armazenadas como hash.",fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);OutlinedButton(onClick=onAccount,modifier=Modifier.fillMaxWidth()){Text(if(email==null)"Entrar ou criar conta local" else "Gerenciar minha conta")}}}
        item(key="about"){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Sobre o PrismaDex",fontSize=17.sp,fontWeight=FontWeight.SemiBold);Text("Prévia Android nativa · projeto de fã independente. Código GPL-3.0, com componentes MIT, Apache-2.0 e MPL-2.0. Personagens e marcas pertencem aos titulares.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);TextButton(onClick=onSources){Text("Champions, fontes e limites dos dados")}}}
    }
}
@Composable private fun SettingsCard(title:String,body:String){Surface(shape=RoundedCornerShape(23.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(11.dp)){Text(title,fontSize=17.sp,fontWeight=FontWeight.SemiBold);Text(body,fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
