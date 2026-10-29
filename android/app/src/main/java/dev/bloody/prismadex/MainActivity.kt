package dev.bloody.prismadex

import android.os.Bundle
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.bloody.prismadex.ui.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

class MainActivity : ComponentActivity() {
    private var languageAtCreation="system"
    override fun attachBaseContext(base:Context){
        languageAtCreation=base.getSharedPreferences("prisma-display",Context.MODE_PRIVATE).getString("language","system")?.takeIf{it in setOf("system","pt","en","es")}?:"system"
        val resolved=if(languageAtCreation=="system")Resources.getSystem().configuration.locales[0].language.takeIf{it in setOf("pt","es")}?:"en" else languageAtCreation
        val localized=base.createConfigurationContext(Configuration(base.resources.configuration).apply{setLocale(Locale(resolved))})
        super.attachBaseContext(localized)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val display = remember { DisplayPreferences(this) }
            LaunchedEffect(Unit){DescriptionCatalog.load(this@MainActivity)}
            LaunchedEffect(display.language){
                if(display.language!=languageAtCreation)recreate()
            }
            val systemConfiguration = LocalConfiguration.current
            val systemLanguage = Resources.getSystem().configuration.locales[0].language
            val language = if(display.language == "system") systemLanguage.takeIf { it in setOf("pt","es") } ?: "en" else display.language
            val strings = remember(language) { UiStrings(this,language) }
            val languageContext = remember(language,systemConfiguration) {
                createConfigurationContext(android.content.res.Configuration(systemConfiguration).apply {
                    setLocale(java.util.Locale(language))
                })
            }
            val systemDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDisplayPreferences provides display,
                LocalUiStrings provides strings,
                LocalContext provides languageContext,
                LocalConfiguration provides languageContext.resources.configuration,
                LocalDensity provides Density(systemDensity.density,systemDensity.fontScale * display.textScale),
            ) { PrismadexApp() }
        }
    }
}
