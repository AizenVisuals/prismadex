package dev.bloody.prismadex.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloody.prismadex.data.SupportedGames
import dev.bloody.prismadex.data.gameName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Games with a clean (transparent, English, correct-game) logo in
 *  assets/game-logos/<id>.webp. Games 1-4 keep their text label: no clean
 *  logo exists in the verified source (Japanese-only or opaque white
 *  background). Sources and rights: research/GAME-LOGO-SOURCES.md. */
internal val GameLogoGameIds = setOf(5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25)

/** Game logos, loaded once from bundled assets (IO thread). */
@Composable
internal fun rememberGameLogoBitmaps(): State<Map<Int, ImageBitmap>> {
    val context = LocalContext.current
    return produceState<Map<Int, ImageBitmap>>(emptyMap()) {
        value = withContext(Dispatchers.IO) {
            GameLogoGameIds.mapNotNull { id ->
                runCatching { context.assets.open("game-logos/$id.webp").use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()?.let { id to it }
            }.toMap()
        }
    }
}

/** Single-select game filter with logos (tap the selected game again for Nacional).
 *
 *  Uniform grid (Impeccable fix, 30/10): every cell is the same size
 *  (two columns, 64dp tall), every logo draws in the SAME 48dp box
 *  with ContentScale.Fit. Wide logos are width-limited, tall stacked
 *  pairs (Black/White, Sun/Moon) are height-limited, so no logo is
 *  tiny or oversized and paired versions stay ONE cell. Text-only
 *  games use identical cells with centered text. */
@Composable
internal fun GameLogoGrid(selectedGame: Int, onSelect: (Int) -> Unit, logos: Map<Int, ImageBitmap>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GameLogoChip(selectedGame == 0, "Nacional · todos os jogos", null, Modifier.fillMaxWidth().height(56.dp)) { onSelect(0) }
        SupportedGames.chunked(2).forEach { rowGames ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowGames.forEach { game ->
                    GameLogoChip(selectedGame == game.id, gameName(game.id), logos[game.id], Modifier.weight(1f).height(64.dp)) { onSelect(if (selectedGame == game.id) 0 else game.id) }
                }
                if (rowGames.size < 2) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GameLogoChip(selected: Boolean, label: String, logo: ImageBitmap?, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier.heightIn(min = 56.dp).semantics { contentDescription = label }
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            if (logo != null) Image(logo, label, Modifier.fillMaxWidth().height(48.dp), contentScale = ContentScale.Fit)
            else LText(label, fontSize = 13.sp, maxLines = 2)
        }
    }
}
