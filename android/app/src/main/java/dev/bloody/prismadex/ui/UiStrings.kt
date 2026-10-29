package dev.bloody.prismadex.ui

import android.content.Context
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import org.json.JSONArray

/** Local UI catalog, with explicit numbered slots for dynamic counts and labels. */
class UiStrings(context: Context, val language: String) {
    private data class Template(val pattern: Regex, val target: String,val literalLength:Int)
    private val exact = mutableMapOf<String, String>()
    private val templates = mutableListOf<Template>()
    private val cache = object : LinkedHashMap<String,String>(256,.75f,true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String,String>?) = size > 1024
    }
    init {
        if (language != "pt") {
            val rows = JSONArray(context.assets.open("ui-languages.json").bufferedReader().use { it.readText() })
            for (i in 0 until rows.length()) {
                val row = rows.getJSONArray(i)
                val source = row.getString(0)
                val target = row.getString(if(language == "es") 2 else 1)
                if (Regex("\\{\\d+\\}").containsMatchIn(source)) {
                    val parts = source.split(Regex("\\{\\d+\\}"))
                    require(parts.any { it.length > 1 }) { "A translation template needs literal text" }
                    templates += Template(Regex(parts.joinToString("(.*?)") { Regex.escape(it) }, RegexOption.DOT_MATCHES_ALL),target,parts.sumOf{it.length})
                } else exact[source] = target
            }
            templates.sortByDescending{it.literalLength}
        }
    }
    @Synchronized fun text(value: String): String {
        if(language == "pt") return value
        cache[value]?.let { return it }
        val translated = exact[value] ?: templates.firstNotNullOfOrNull { template ->
            template.pattern.matchEntire(value)?.let { match ->
                Regex("\\{(\\d+)\\}").replace(template.target) { slot -> match.groupValues.getOrElse(slot.groupValues[1].toInt()+1) { slot.value } }
            }
        } ?: value
        cache[value] = translated
        return translated
    }
}
val LocalUiStrings = staticCompositionLocalOf<UiStrings> { error("UI language not provided") }

@Composable internal fun LText(
    text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified, fontWeight: FontWeight? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified, textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified, overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE, minLines: Int = 1, style: TextStyle = LocalTextStyle.current,
) = Text(LocalUiStrings.current.text(text),modifier,color,fontSize,fontWeight=fontWeight,
    letterSpacing=letterSpacing,textAlign=textAlign,lineHeight=lineHeight,overflow=overflow,
    maxLines=maxLines,minLines=minLines,style=style)

@Composable internal fun LIcon(imageVector: ImageVector, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    Icon(imageVector,contentDescription?.let { LocalUiStrings.current.text(it) },modifier,tint)
@Composable internal fun LIcon(painter: Painter, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    Icon(painter,contentDescription?.let { LocalUiStrings.current.text(it) },modifier,tint)

/** Translation resources contain text only; battle values and canonical names are untouched. */
object DescriptionCatalog {
    private var values by mutableStateOf<Map<String,Map<String,String>>>(emptyMap())
    suspend fun load(context:Context){
        if(values.isNotEmpty())return
        val loaded=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){
            val json=org.json.JSONObject(context.assets.open("localized-descriptions.json").bufferedReader().use{it.readText()})
            json.keys().asSequence().associate{source->val row=json.getJSONObject(source);source to row.keys().asSequence().associateWith{row.getString(it)}}
        }
        values=loaded
    }
    fun text(source:String,language:String)=if(language=="en")source else values[source]?.get(language)?:source
}
@Composable internal fun descriptionText(source:String):String {
    val ui=LocalUiStrings.current
    return DescriptionCatalog.text(source,ui.language).let{if(it==source)ui.text(source) else it}
}
