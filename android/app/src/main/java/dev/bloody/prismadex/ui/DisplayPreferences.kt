package dev.bloody.prismadex.ui

import android.content.Context
import androidx.compose.runtime.*

/** Device preferences survive account changes; they never alter a saved battle set. */
class DisplayPreferences(context: Context, name: String = "prisma-display") {
    private val prefs = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)
    var language by mutableStateOf(prefs.getString("language", "system")!!.takeIf { it in languages } ?: "system")
        private set
    var textScale by mutableFloatStateOf(prefs.getFloat("textScale", 1f).takeIf { it in textSizes } ?: 1f)
        private set
    fun selectLanguage(value: String) {
        require(value in languages)
        language = value
        prefs.edit().putString("language", value).apply()
    }
    fun selectTextScale(value: Float) {
        val selected = textSizes.minBy { kotlin.math.abs(it - value) }
        textScale = selected
        prefs.edit().putFloat("textScale", selected).apply()
    }
    companion object {
        val languages = setOf("system", "pt", "en", "es")
        val textSizes = listOf(.85f, 1f, 1.15f, 1.3f, 1.5f)
    }
}

val LocalDisplayPreferences = staticCompositionLocalOf<DisplayPreferences> { error("Display preferences not provided") }
