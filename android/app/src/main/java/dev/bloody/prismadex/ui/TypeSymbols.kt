package dev.bloody.prismadex.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val typeNames = mapOf("normal" to "Normal", "fire" to "Fogo", "water" to "Água", "grass" to "Planta", "electric" to "Elétrico", "ice" to "Gelo", "fighting" to "Lutador", "poison" to "Veneno", "ground" to "Terrestre", "flying" to "Voador", "psychic" to "Psíquico", "bug" to "Inseto", "rock" to "Pedra", "ghost" to "Fantasma", "dragon" to "Dragão", "dark" to "Sombrio", "steel" to "Aço", "fairy" to "Fada")
fun typeColor(t:String)=when(t){"fire"->Color(0xFFF5A17D);"water"->Color(0xFF82BBF5);"grass"->Color(0xFF99D8A5);"electric"->Color(0xFFF0D76F);"ghost","poison"->Color(0xFFC49EE9);"fairy","psychic"->Color(0xFFF1A8D8);"ice"->Color(0xFF91DFDC);"steel"->Color(0xFF9DBDCB);"ground","rock"->Color(0xFFD6BB8A);"fighting"->Color(0xFFE39D93);else->Color(0xFFBAADE7)}
/** MIT type badge vectors by James Watkins / partywhale, bundled locally. */
@Composable fun TypeSymbol(type:String, modifier:Modifier=Modifier) {
    val resource=when(type){
        "bug"->dev.bloody.prismadex.R.drawable.type_bug
        "dark"->dev.bloody.prismadex.R.drawable.type_dark
        "dragon"->dev.bloody.prismadex.R.drawable.type_dragon
        "electric"->dev.bloody.prismadex.R.drawable.type_electric
        "fairy"->dev.bloody.prismadex.R.drawable.type_fairy
        "fighting"->dev.bloody.prismadex.R.drawable.type_fighting
        "fire"->dev.bloody.prismadex.R.drawable.type_fire
        "flying"->dev.bloody.prismadex.R.drawable.type_flying
        "ghost"->dev.bloody.prismadex.R.drawable.type_ghost
        "grass"->dev.bloody.prismadex.R.drawable.type_grass
        "ground"->dev.bloody.prismadex.R.drawable.type_ground
        "ice"->dev.bloody.prismadex.R.drawable.type_ice
        "normal"->dev.bloody.prismadex.R.drawable.type_normal
        "poison"->dev.bloody.prismadex.R.drawable.type_poison
        "psychic"->dev.bloody.prismadex.R.drawable.type_psychic
        "rock"->dev.bloody.prismadex.R.drawable.type_rock
        "steel"->dev.bloody.prismadex.R.drawable.type_steel
        "water"->dev.bloody.prismadex.R.drawable.type_water
        else->dev.bloody.prismadex.R.drawable.type_normal
    }
    androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(resource),typeNames[type],modifier.size(18.dp),tint=Color.Unspecified)
}
@Composable fun TypePill(type:String, compact:Boolean=false){val c=typeColor(type);Row(Modifier.background(c.copy(alpha=.12f),RoundedCornerShape(12.dp)).padding(horizontal=if(compact)8.dp else 12.dp,vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){TypeSymbol(type,Modifier.size(if(compact)17.dp else 20.dp));Text(typeNames[type]?:type,color=androidx.compose.material3.MaterialTheme.colorScheme.onSurface,fontSize=if(compact)12.sp else 13.sp)}}
