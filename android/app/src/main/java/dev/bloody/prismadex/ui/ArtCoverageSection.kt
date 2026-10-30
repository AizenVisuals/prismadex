package dev.bloody.prismadex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.data.Pokemon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class ArtCoverage(val total: Int, val withArt: Int, val missingIds: List<Int>, val itemIconFiles: Int)

/**
 * Datadex-equivalent transparency, built only from the app's own bundled assets:
 * datadex organises art into art / art-full / art-thumb / sprites / v4 folders.
 * The equivalent here is a live count of what is actually bundled — full art used
 * at every size, plus the fallback marker for the records that have none. No
 * external source is consulted and nothing is copied.
 */
@Composable internal fun ArtCoverageSection(catalog: List<Pokemon>) {
    val context = LocalContext.current
    val coverage by produceState<ArtCoverage?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val artIds = context.assets.list("art")
                    ?.mapNotNull { it.removeSuffix(".webp").toIntOrNull() }
                    ?.toSet() ?: emptySet()
                val missing = catalog.map { it.id }.filter { it !in artIds }
                ArtCoverage(
                    total = catalog.size,
                    withArt = catalog.count { it.id in artIds },
                    missingIds = missing,
                    itemIconFiles = context.assets.list("item-art")?.size ?: 0
                )
            }.getOrNull()
        }
    }
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LText("Cobertura de arte", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            val c = coverage
            if (c == null) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                LText(
                    "Arte dos Pokémon: ${c.withArt}/${c.total} registros com arte local.",
                    fontSize = 13.sp, lineHeight = 18.sp
                )
                if (c.missingIds.isNotEmpty()) {
                    LText(
                        "Sem arte local (${c.missingIds.size}): ${c.missingIds.joinToString(", ")} — exibidos com o marcador neutro, sem copiar arte de terceiros.",
                        fontSize = 13.sp, lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LText(
                    "Ícones de itens: ${c.itemIconFiles} arquivos locais. Itens sem ícone usam o marcador genérico; nenhum ícone foi importado sem licença verificada.",
                    fontSize = 13.sp, lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
