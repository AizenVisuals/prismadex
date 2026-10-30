package dev.bloody.prismadex.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CatchingPokemon
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import dev.bloody.prismadex.data.Pokemon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shiny art convention: a real shiny asset, when one exists, lives at
 * `art/shiny/<id>.webp` next to the normal `art/<id>.webp`. Coverage today
 * is ZERO (audited 29/10/2026: no `art/shiny/` folder, no shiny file in
 * `assets/art/`, no shiny reference in code) — so the Shiny option honestly
 * shows the unavailable state instead of a hue-shifted fake. Never fake a
 * shiny by recolouring normal art, never substitute another Pokémon's art.
 */
internal fun shinyArtPath(id: Int): String = "art/shiny/$id.webp"

/** True/false once probed, null while probing. A missing asset probes false. */
@Composable
internal fun rememberArtExists(path: String): Boolean? {
    val assets = LocalContext.current.assets
    var exists by remember(path) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(path) {
        exists = withContext(Dispatchers.IO) {
            runCatching { assets.open(path).use {} }.isSuccess
        }
    }
    return exists
}

/**
 * Normal art, or the real shiny asset when [shiny] is on. When shiny is on
 * and no asset exists, the same honest fallback marker as [PokemonArt]
 * (a Poké Ball icon, never another Pokémon's art, never a recolour).
 */
@Composable
internal fun ShinyArt(id: Int, shiny: Boolean, modifier: Modifier = Modifier, label: String? = null) {
    if (!shiny) {
        PokemonArt(id, modifier, label)
        return
    }
    val assets = LocalContext.current.assets
    val path = shinyArtPath(id)
    val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            runCatching { assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
        }
    }
    val ready = image
    if (ready != null) Image(ready, label, modifier)
    else Box(modifier, contentAlignment = Alignment.Center) {
        LIcon(
            Icons.Rounded.CatchingPokemon, label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(.4f),
            modifier = Modifier.size(44.dp)
        )
    }
}

/**
 * Full-screen art viewer in the app's own theme: title "Arte de <name>",
 * the Pokémon's own bundled art only (offline — art loads from assets, so
 * the reference's "requires an internet connection" line is false here and
 * is deliberately NOT shown), pinch to zoom + double-tap to zoom in/out,
 * pan when zoomed, back button / system back to dismiss. The Normal/Shiny
 * toggle is the same state as the detail page. Missing art (normal or
 * shiny) renders an honest empty state, never another Pokémon's art.
 * Zoom is gesture-driven only (no animated zoom), so reduce-motion needs
 * no special handling here. The shiny control is a single star icon only
 * (00:02): same Normal/Shiny state as the detail page, no label clutter.
 */
@Composable
internal fun ArtViewer(
    p: Pokemon,
    shiny: Boolean,
    onShiny: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    var scale by remember(p.id, shiny) { mutableStateOf(1f) }
    var offset by remember(p.id, shiny) { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = newScale
        offset = if (newScale <= 1f) {
            Offset.Zero
        } else {
            val maxX = (containerSize.width * (newScale - 1f)) / 2f
            val maxY = (containerSize.height * (newScale - 1f)) / 2f
            Offset(
                (offset.x + panChange.x).coerceIn(-maxX, maxX),
                (offset.y + panChange.y).coerceIn(-maxY, maxY)
            )
        }
    }
    val shinyExists = rememberArtExists(shinyArtPath(p.id))
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                    LIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar")
                }
                LText(
                    "Arte de ${p.name}",
                    Modifier.weight(1f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.size(48.dp))
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = { onShiny(!shiny) }, modifier = Modifier.size(48.dp)) {
                    LIcon(
                        Icons.Rounded.Star,
                        if (shiny) "Ver arte normal de ${p.name}" else "Ver arte shiny de ${p.name}",
                        tint = if (shiny) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(.55f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Box(
                Modifier.weight(1f).fillMaxWidth()
                    .onSizeChanged { containerSize = it }
                    .pointerInput(p.id, shiny) {
                        detectTapGestures(onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                            }
                        })
                    }
                    .transformable(transformableState),
                contentAlignment = Alignment.Center
            ) {
                if (shiny && shinyExists == false) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        LIcon(
                            Icons.Rounded.CatchingPokemon, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        LText(
                            "Arte shiny não disponível para este Pokémon.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Box(
                        Modifier.fillMaxSize().padding(24.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        ShinyArt(p.id, shiny, Modifier.fillMaxSize(), p.name)
                    }
                }
            }
            LText(
                "Toque duas vezes ou pinçe para ampliar e reduzir.",
                Modifier.fillMaxWidth().padding(bottom = 28.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
