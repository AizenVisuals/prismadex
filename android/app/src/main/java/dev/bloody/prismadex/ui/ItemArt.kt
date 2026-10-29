package dev.bloody.prismadex.ui

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val itemImages = LruCache<Int,ImageBitmap>(180)

@Composable internal fun ItemArt(id: Int?, name: String?, modifier: Modifier = Modifier) {
    val assets = LocalContext.current.assets
    val label = name?.let { LocalUiStrings.current.text("Item segurado: $it") }
    val bitmap by produceState<ImageBitmap?>(id?.let { itemImages.get(it) },id) {
        value = id?.let { itemId -> withContext(Dispatchers.IO) {
            itemImages.get(itemId) ?: runCatching {
                assets.open("item-art/$itemId.png").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            }.getOrNull()?.also { itemImages.put(itemId,it) }
        } }
    }
    val image = bitmap
    if(image != null) Image(image,label,modifier)
    else LIcon(Icons.Rounded.Inventory2,label,modifier,tint=MaterialTheme.colorScheme.onSurfaceVariant)
}
