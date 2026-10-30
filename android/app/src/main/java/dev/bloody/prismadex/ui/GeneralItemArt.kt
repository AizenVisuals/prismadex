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
import dev.bloody.prismadex.data.ItemArtCatalog
import dev.bloody.prismadex.data.ReferenceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val referenceArtImages=object:LruCache<String,ImageBitmap>(6*1024*1024){override fun sizeOf(key:String,value:ImageBitmap)=value.width*value.height*4}

/** Existing held-item icons retain their original IDs and bytes. New art uses a separate map. */
@Composable internal fun GeneralItemArt(item:ReferenceItem,modifier:Modifier=Modifier){
    if(item.iconId!=null){ItemArt(item.iconId,item.name,modifier);return}
    val context=LocalContext.current
    val catalog=remember{ItemArtCatalog(context)}
    val image by produceState<ImageBitmap?>(null,item.id,item.name,item.slug){
        value=withContext(Dispatchers.IO){
            catalog.forItem(item)?.let{record->referenceArtImages.get(record.path)?:runCatching{
                context.assets.open(record.path).use{BitmapFactory.decodeStream(it)?.asImageBitmap()}
            }.getOrNull()?.also{referenceArtImages.put(record.path,it)}}
        }
    }
    val ready=image
    if(ready!=null)Image(ready,item.name,modifier)
    else LIcon(Icons.Rounded.Inventory2,item.name,modifier,tint=MaterialTheme.colorScheme.onSurfaceVariant)
}
