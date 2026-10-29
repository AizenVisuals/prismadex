package dev.bloody.prismadex.ui

import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.data.LearnMethod

@Composable internal fun LearnMethodLabel(methods:List<LearnMethod>){
    val language=LocalUiStrings.current.language
    val labels=methods.distinct().map{m->
        val code=when(m.method){"level-up"->if(m.level!=null)0 else 1;"egg"->2;"machine"->3;"tutor"->4;"light-ball-egg"->5;"form-change"->6;"stadium-surfing-pikachu"->7;"colosseum-purification","xd-purification"->8;"xd-shadow"->9;"zygarde-cube"->10;"train"->11;else->12}
        val pt=listOf("Nível ${m.level}","Ao subir de nível","Ovo","TM/HM/TR · máquina","Tutor","Ovo · Light Ball","Mudança de forma","Pikachu surfista · Stadium","Purificação","Shadow · XD","Zygarde Cube","Treinamento","Método não informado nesta fonte")
        val en=listOf("Level ${m.level}","Level up","Egg","TM/HM/TR · machine","Tutor","Egg · Light Ball","Form change","Surfing Pikachu · Stadium","Purification","Shadow · XD","Zygarde Cube","Training","Method not supplied by this source")
        val es=listOf("Nivel ${m.level}","Al subir de nivel","Huevo","MT/MO/DT · máquina","Tutor","Huevo · Light Ball","Cambio de forma","Pikachu surfista · Stadium","Purificación","Shadow · XD","Zygarde Cube","Entrenamiento","Método no indicado en esta fuente")
        when(language){"pt"->pt;"es"->es;else->en}[code]
    }
    androidx.compose.material3.Text(labels.joinToString(" · "),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
}
