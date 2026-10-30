@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package dev.bloody.prismadex.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
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
    AppTheme("eevee","Eevee","Normal · caramelo e creme",dark(0xFFC9975A,0xFF121110,0xFF1D1B18),day(0xFF7A4A1F,0xFFF5F0E6,0xFFFCF9F3)),
    AppTheme("vaporeon","Vaporeon","Água · azul profundo",dark(0xFF4AA8D6,0xFF0E1216,0xFF171F27),day(0xFF0B5E8A,0xFFEFF2F3,0xFFFAFCFD)),
    AppTheme("jolteon","Jolteon","Elétrico · amarelo voltaico",dark(0xFFD1A50D,0xFF12120F,0xFF1D1C16),day(0xFF7A5C00,0xFFF5F1E3,0xFFFCFAF1)),
    AppTheme("flareon","Flareon","Fogo · chama laranja",dark(0xFFF08A5C,0xFF131110,0xFF1E1A16),day(0xFFB23A12,0xFFF5EEE8,0xFFFCF8F5)),
    AppTheme("espeon","Espeon","Psíquico · lilás",dark(0xFFB98AE0,0xFF121016,0xFF1C1824),day(0xFF6D3AA8,0xFFF3EFF5,0xFFFBF8FC)),
    AppTheme("umbreon","Umbreon","Sombrio · preto e dourado",dark(0xFFC2951F,0xFF0F0F10,0xFF1A1A1C),day(0xFF7A5A00,0xFFF2F0E8,0xFFFBF9F3)),
    AppTheme("leafeon","Leafeon","Planta · verde folha",dark(0xFF63C274,0xFF0F120F,0xFF181E18),day(0xFF1F6B33,0xFFEFF3EA,0xFFFAFBF6)),
    AppTheme("glaceon","Glaceon","Gelo · azul glacial",dark(0xFF63BBD8,0xFF0E1214,0xFF171F23),day(0xFF0E6B8A,0xFFEEF3F4,0xFFFAFCFD)),
    AppTheme("sylveon","Sylveon","Fada · rosa e laços",dark(0xFFF08BBB,0xFF131013,0xFF1F1A1E),day(0xFFA82A63,0xFFF6EFF1,0xFFFCF8FA)),
)
@Composable internal fun AppearancePicker(selected:String,onSelect:(String)->Unit){
    FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
        listOf("system" to "Sistema","light" to "Claro","dark" to "Escuro").forEach{(key,label)->
            FilterChip(selected==key,{onSelect(key)},label={LText(label)},modifier=Modifier.heightIn(min=48.dp))
        }
    }
}
@Composable internal fun PrismaMark(modifier:Modifier=Modifier,color:Color=MaterialTheme.colorScheme.primary){LIcon(painterResource(R.drawable.ic_prisma_mark),"Símbolo do PrismaDex",modifier,tint=color)}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun ThemeSettingRow(selected:String,light:Boolean=MaterialTheme.colorScheme.background.luminance()>.5f,onSelect:(String)->Unit){
    var showPicker by remember{mutableStateOf(false)}
    val current=AppThemes.firstOrNull{it.key==selected}?:AppThemes.first()
    val preview=current.colors(light)
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        LText("Tema",fontSize=17.sp,fontWeight=FontWeight.SemiBold)
        Surface(onClick={showPicker=true},shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){
            Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(preview.background,preview.surface))),contentAlignment=Alignment.Center){
                    PrismaMark(Modifier.size(36.dp),color=preview.accent)
                }
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)){
                    LText(current.name,fontSize=16.sp,fontWeight=FontWeight.SemiBold)
                    LText(current.detail,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LText("Mudar",fontSize=14.sp,color=MaterialTheme.colorScheme.primary)
            }
        }
        LText("Toque para escolher um tema Eeveelution. Cada tema tem versão clara e escura.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(showPicker){
            ModalBottomSheet(onDismissRequest={showPicker=false},containerColor=MaterialTheme.colorScheme.background){
                Column(Modifier.fillMaxWidth().padding(start=22.dp,end=22.dp,top=8.dp,bottom=28.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
                    LText("Escolher tema",fontSize=20.sp,fontWeight=FontWeight.SemiBold)
                    AppThemes.forEach{palette->
                        val theme=palette.colors(light)
                        val isSelected=palette.key==current.key
                        Surface(onClick={onSelect(palette.key);showPicker=false},shape=RoundedCornerShape(20.dp),border=BorderStroke(if(isSelected)2.dp else 1.dp,if(isSelected)MaterialTheme.colorScheme.primary else solidBlend(MaterialTheme.colorScheme.primary,MaterialTheme.colorScheme.surface,.18f)),color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){
                            Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                                Box(Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(theme.background,theme.surface))),contentAlignment=Alignment.Center){
                                    PrismaMark(Modifier.size(40.dp),color=theme.accent)
                                }
                                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)){
                                    LText(palette.name,fontSize=16.sp,fontWeight=FontWeight.SemiBold)
                                    LText(palette.detail,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if(isSelected)LIcon(Icons.Rounded.Check,"Tema selecionado",Modifier.size(20.dp),tint=MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    LText("Eevee é o tema padrão. Personagens e marcas pertencem aos titulares.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable internal fun AppDrawer(selected:String,onNavigate:(String)->Unit,onAccount:()->Unit){
    PrismaDrawer(selected,onNavigate,onAccount)
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
        item(key="themes"){Column(verticalArrangement=Arrangement.spacedBy(13.dp)){LText("Aparência",fontSize=20.sp,fontWeight=FontWeight.SemiBold);LText("Nove temas Eeveelution, cada um com versão clara e escura. O modo Sistema acompanha a aparência do Android.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);AppearancePicker(state.appearanceMode){onState(state.copy(appearanceMode=it))};ThemeSettingRow(state.theme,light=when(state.appearanceMode){"light"->true;"dark"->false;else->!androidx.compose.foundation.isSystemInDarkTheme()}){onState(state.copy(theme=it))}}}
        item(key="app-icon"){AppIconSettingRow(state.appIcon){onState(state.copy(appIcon=it))}}
        item(key="layout"){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){LText("Visualização da Pokédex",fontSize=17.sp,fontWeight=FontWeight.SemiBold);Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){FilterChip(state.layout=="grid",{onState(state.copy(layout="grid"))},label={LText("Grade")});FilterChip(state.layout=="list",{onState(state.copy(layout="list"))},label={LText("Lista")})}}}
        item(key="battle"){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){LText("Formato preferido",fontSize=17.sp,fontWeight=FontWeight.SemiBold);FlowRow(horizontalArrangement=Arrangement.spacedBy(9.dp)){FilterChip(state.battleFormat=="singles",{onState(state.copy(battleFormat="singles"))},label={LText("Singles · individual")});FilterChip(state.battleFormat=="doubles",{onState(state.copy(battleFormat="doubles"))},label={LText("Doubles · duplas")})};LText("Usado no TeamBuilder, no Meta e como padrão da calculadora. Os dois formatos têm regras próprias; conferir o regulamento continua necessário.",fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
        item(key="offline"){SettingsCard("Dados e modo offline","Os 1.306 registros, as artes, golpes, equipe, favoritos e alvos de teste ficam no aparelho. Apenas a consulta e atualização do Meta usam internet. O último relatório do Meta fica em cache.")}
        item(key="accessibility"){SettingsCard("Acessibilidade e movimento","O tamanho de texto escolhido aqui é combinado com a configuração do Android. O app acompanha a escala de animação do sistema. Os ícones de tipos também têm rótulos; a rolagem continua disponível por gesto.")}
        item(key="account"){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){LText("Conta e privacidade",fontSize=17.sp,fontWeight=FontWeight.SemiBold);LText(email?:"Você está usando como visitante.",fontSize=13.sp);LText("As contas desta versão são locais. Não há sincronização entre aparelhos, confirmação de e-mail ou recuperação de senha. Senhas são armazenadas como hash.",fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);OutlinedButton(onClick=onAccount,modifier=Modifier.fillMaxWidth()){LText(if(email==null)"Entrar ou criar conta local" else "Gerenciar minha conta")}}}
        item(key="about"){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){LText("Sobre o PrismaDex",fontSize=17.sp,fontWeight=FontWeight.SemiBold);LText("Prévia Android nativa · projeto de fã independente. Código GPL-3.0, com componentes MIT, Apache-2.0 e MPL-2.0. Personagens e marcas pertencem aos titulares.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);TextButton(onClick=onSources){LText("Champions, fontes e limites dos dados")}}}
    }
}
@Composable private fun SettingsCard(title:String,body:String){Surface(shape=RoundedCornerShape(23.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(11.dp)){LText(title,fontSize=17.sp,fontWeight=FontWeight.SemiBold);LText(body,fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
