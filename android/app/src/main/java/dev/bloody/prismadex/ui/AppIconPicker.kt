package dev.bloody.prismadex.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.R
import dev.bloody.prismadex.data.AppIconKeys
import dev.bloody.prismadex.data.AppIconAliasSuffixes
import dev.bloody.prismadex.data.canonicalAppIcon

/** Launcher icon options (Settings). The PrismaDex mark is the default;
 *  Sylveon/Flareon and the 10 poster icons are user-supplied artwork
 *  (posters with the text removed, no zoom, no redraw; see the credit note in the
 *  picker and on the Sources screen). Characters are Nintendo / Game Freak /
 *  The Pokémon Company IP; Play Store acceptance is not guaranteed. */
internal data class AppIconOption(val key: String, val name: String, val detail: String, val preview: Int, val previewBackground: Color)

internal val AppIconOptions = listOf(
    AppIconOption("prisma", "PrismaDex", "A marca lilás sobre grafite", R.drawable.ic_prisma_foreground, Color(0xFF141322)),
    AppIconOption("sylveon", "Sylveon", "Arte enviada pelo responsável do projeto", R.drawable.ic_icon_sylveon, Color(0xFF444166)),
    AppIconOption("flareon", "Flareon", "Arte enviada pelo responsável do projeto", R.drawable.ic_icon_flareon, Color(0xFFFDB533)),
    AppIconOption("aggron", "Aggron", "Pôster enviado pelo responsável do projeto, sem o texto", R.drawable.ic_icon_aggron, Color(0xFF232120)),
    AppIconOption("pikachu", "Pikachu", "Pôster enviado pelo responsável do projeto, sem o texto", R.drawable.ic_icon_pikachu, Color(0xFFFFCE31)),
    AppIconOption("starmie", "Starmie", "Pôster enviado pelo responsável do projeto, sem o texto", R.drawable.ic_icon_starmie, Color(0xFF443576)),
    AppIconOption("lucario", "Lucario", "Pôster enviado pelo responsável do projeto, sem o texto", R.drawable.ic_icon_lucario, Color(0xFF0E5B86)),
    AppIconOption("cloyster", "Cloyster", "Pôster enviado pelo responsável do projeto, sem o texto", R.drawable.ic_icon_cloyster, Color(0xFF4F4B68)),
    AppIconOption("sigilyph", "Sigilyph", "Pôster enviado pelo responsável do projeto, sem o texto", R.drawable.ic_icon_sigilyph, Color(0xFFF8AD3E)),
    AppIconOption("haxorus", "Haxorus", "Pôster enviado pelo responsável do projeto, sem o texto", R.drawable.ic_icon_haxorus, Color(0xFF9A3432)),
    AppIconOption("vaporeon", "Vaporeon", "Pôster enviado pelo responsável do projeto, sem o texto", R.drawable.ic_icon_vaporeon, Color(0xFF2B4F73)),
    AppIconOption("infernape", "Infernape", "Pôster enviado pelo responsável do projeto, sem o texto", R.drawable.ic_icon_infernape, Color(0xFFED564D)),
    AppIconOption("umbreon", "Umbreon", "Pôster enviado pelo responsável do projeto, sem o texto", R.drawable.ic_icon_umbreon, Color(0xFF050501)),
    AppIconOption("ultraball", "Ultra Ball", "Arte enviada pelo responsável do projeto", R.drawable.ic_icon_ultraball, Color(0xFF433D3A)),
    AppIconOption("greatball", "Great Ball", "Arte enviada pelo responsável do projeto", R.drawable.ic_icon_greatball, Color(0xFF1743A2)),
    AppIconOption("pokedex", "Pokédex", "Arte enviada pelo responsável do projeto", R.drawable.ic_icon_pokedex, Color(0xFF6A1F2B)),
    AppIconOption("glaceon", "Glaceon", "Arte enviada pelo responsável do projeto", R.drawable.ic_icon_glaceon, Color(0xFF286099)),
    AppIconOption("masterball", "Master Ball", "Arte enviada pelo responsável do projeto", R.drawable.ic_icon_masterball, Color(0xFF410F89)),
    AppIconOption("pokeball", "Poké Ball", "Arte enviada pelo responsável do projeto", R.drawable.ic_icon_pokeball, Color(0xFF9F1F35)),
)

internal fun appIconOption(key: String): AppIconOption =
    AppIconOptions.firstOrNull { it.key == key } ?: AppIconOptions[0]

/** Picker grouping: balls/devices vs Pokémon faces. The PrismaDex mark
 *  stays first in the Pokémon group (it is the app mark, not a ball);
 *  the red Pokédex device belongs to Pokébolas. Every option appears in
 *  exactly one group; the two groups together are the full 19. */
internal enum class AppIconGroupKind { BALLS, POKEMON }

internal data class AppIconGroup(val kind: AppIconGroupKind, val title: String, val options: List<AppIconOption>)

private val BallIconKeys = setOf("ultraball", "greatball", "masterball", "pokeball", "pokedex")

internal val AppIconGroups: List<AppIconGroup> = listOf(
    AppIconGroup(AppIconGroupKind.BALLS, "Pokébolas", AppIconOptions.filter { it.key in BallIconKeys }),
    AppIconGroup(AppIconGroupKind.POKEMON, "Pokémon", AppIconOptions.filter { it.key !in BallIconKeys }),
)

internal fun appIconGroupOf(key: String): AppIconGroupKind =
    if (AppIconGroups.first { it.kind == AppIconGroupKind.BALLS }.options.any { it.key == canonicalAppIcon(key) })
        AppIconGroupKind.BALLS else AppIconGroupKind.POKEMON

/** Switches the launcher icon via activity-aliases (the supported Android
 *  route for a user-chosen icon). Enables the target component FIRST, then
 *  disables the others, with DONT_KILL_APP so the running app is not killed.
 *  Returns true only when the target component reads back as enabled.
 *  Some launchers refresh the icon only after returning to the home screen
 *  or restarting the launcher; that delay is the launcher's, not a failure
 *  of the switch itself. */
internal fun applyAppIcon(context: Context, key: String): Boolean {
    val safe = canonicalAppIcon(key)
    val pkg = context.packageName
    val pm = context.packageManager
    val main = ComponentName(pkg, "dev.bloody.prismadex.MainActivity")
    val aliases = AppIconAliasSuffixes.mapValues { ComponentName(pkg, "$pkg.${it.value}") }
    val target = if (safe == "prisma") main else aliases.getValue(safe)
    return runCatching {
        pm.setComponentEnabledSetting(target, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        if (safe == "prisma") {
            aliases.values.forEach { pm.setComponentEnabledSetting(it, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP) }
        } else {
            aliases.filterKeys { it != safe }.values.forEach { pm.setComponentEnabledSetting(it, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP) }
            pm.setComponentEnabledSetting(main, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
        pm.getComponentEnabledSetting(target) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }.getOrDefault(false)
}


/** Full-bleed icon preview: the same artwork the launcher shows, at the
 *  artwork's own 15% corner radius. No background box and no tighter
 *  second clip, so the art is not inset or double-rounded. */
@Composable
private fun AppIconPreview(option: AppIconOption, size: Dp) {
    val radius = (size.value * 0.15f).dp
    if (option.key == "prisma") {
        Box(Modifier.size(size).clip(RoundedCornerShape(radius)).background(option.previewBackground), contentAlignment = Alignment.Center) {
            Image(painterResource(option.preview), null, Modifier.size(size), contentScale = ContentScale.Fit)
        }
    } else {
        Image(painterResource(option.preview), null, Modifier.size(size).clip(RoundedCornerShape(radius)), contentScale = ContentScale.FillBounds)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppIconSettingRow(selected: String, onSelect: (String) -> Unit) {
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }
    var applyFailed by remember { mutableStateOf(false) }
    // On open, only the group holding the current icon is expanded.
    var expandedGroups by remember(selected) { mutableStateOf(setOf(appIconGroupOf(selected))) }
    val current = appIconOption(selected)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LText("Ícone do app", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Surface(
            onClick = { showPicker = true },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppIconPreview(current, 56.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    LText(current.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    LText("Toque para escolher", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LText("Mudar", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
        LText(
            "O ícone escolhido aparece na tela inicial. Sylveon, Flareon, Glaceon e os ícones de pôster são personagens; Ultra Ball, Great Ball, Master Ball, Poké Ball e Pokédex são itens de Nintendo, Game Freak e The Pokémon Company; as artes foram enviadas pelo responsável do projeto.",
            fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (applyFailed) {
            LText(
                "Não foi possível trocar o ícone do inicializador agora; a escolha ficou salva e será tentada de novo ao abrir o app.",
                fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showPicker) {
            ModalBottomSheet(onDismissRequest = { showPicker = false }, containerColor = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f).padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 28.dp)) {
                    LText("Escolher ícone do app", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    LText("${AppIconOptions.size} ícones em ${AppIconGroups.size} grupos — toque para expandir", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)) {
                        AppIconGroups.forEach { group ->
                            val groupExpanded = group.kind in expandedGroups
                            item(key = "group-" + group.kind.name) {
                                Surface(
                                    onClick = {
                                        expandedGroups = if (groupExpanded) expandedGroups - group.kind else expandedGroups + group.kind
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                ) {
                                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            LText(group.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                            LText(
                                                group.options.size.toString() + " ícones" + if (group.options.any { it.key == canonicalAppIcon(selected) }) " · atual" else "",
                                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        LIcon(
                                            Icons.Rounded.ExpandMore,
                                            if (groupExpanded) "Recolher " + group.title else "Expandir " + group.title,
                                            Modifier.size(20.dp).rotate(if (groupExpanded) 180f else 0f),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                            if (groupExpanded) {
                                items(group.options, key = { it.key }) { option ->
                        val isSelected = option.key == canonicalAppIcon(selected)
                        Surface(
                            onClick = {
                                val ok = applyAppIcon(context, option.key)
                                applyFailed = !ok
                                onSelect(option.key)
                                showPicker = false
                            },
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else solidBlend(MaterialTheme.colorScheme.primary,MaterialTheme.colorScheme.surface,.18f)),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AppIconPreview(option, 64.dp)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    LText(option.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                    LText(option.detail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (isSelected) LIcon(Icons.Rounded.Check, "Ícone selecionado", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                                }
                            }
                        }
                    item {
                        LText(
                            "A troca vale para o ícone da tela inicial; o inicializador pode levar alguns segundos para atualizar.",
                            fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        }
    }
}
