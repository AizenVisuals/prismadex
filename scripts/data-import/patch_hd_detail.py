#!/usr/bin/env python3
"""Patch DetailArt (PrismadexApp.kt) + ShinyArt (ArtViewer.kt) for HD art.

HD assets live in art-hd/ and art-hd/shiny/ and are used ONLY by the
detail header and the full-screen viewer. List/grid/team/spotlight keep
the 256px assets untouched. Behaviour: show the 256px trimmed bitmap
immediately, then upgrade to the HD trimmed bitmap when one exists;
when no HD asset exists the 256px bitmap stays (honest fallback).
"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
APP = ROOT / "android/app/src/main/java/dev/bloody/prismadex/ui/PrismadexApp.kt"
VIEW = ROOT / "android/app/src/main/java/dev/bloody/prismadex/ui/ArtViewer.kt"

# ---------- PrismadexApp.kt ----------
src = APP.read_text()
anchor = "private val shinyTrimmedBitmapCache=object:LruCache<Int,ImageBitmap>(18*1024*1024){override fun sizeOf(key:Int,value:ImageBitmap)=value.width*value.height*4}"
assert src.count(anchor) == 1, "shiny cache anchor not found exactly once"
helper = anchor + """

// HD detail art (30/10/2026): art-hd/<id>.webp (475px native PokeAPI,
// reframed for Mega/Gmax) and art-hd/shiny/<id>.webp (475px PokeAPI /
// 512px TonOfDirt726 originals). Same trim JSONs as the 256px assets:
// the crop scales by the bitmap's own width, so framing matches the
// 256px assets exactly. Returns null when no HD asset exists; callers
// keep the 256px bitmap (honest fallback, never an upscale).
private val hdTrimmedBitmapCache=object:LruCache<Int,ImageBitmap>(12*1024*1024){override fun sizeOf(key:Int,value:ImageBitmap)=value.width*value.height*4}
private val hdShinyTrimmedBitmapCache=object:LruCache<Int,ImageBitmap>(12*1024*1024){override fun sizeOf(key:Int,value:ImageBitmap)=value.width*value.height*4}
private fun hdTrimmedBitmap(context:android.content.Context,path:String,trim:IntArray?,cache:android.util.LruCache<Int,ImageBitmap>,key:Int):ImageBitmap?{
    cache.get(key)?.let{return it}
    val full=runCatching{context.assets.open(path).use{BitmapFactory.decodeStream(it)?.asImageBitmap()}}.getOrNull() ?: return null
    if(trim==null){cache.put(key,full);return full}
    val bmp=full.asAndroidBitmap(); val bw=bmp.width; val bh=bmp.height
    val tx=(trim[0]*bw/256f).toInt().coerceIn(0,bw-1); val ty=(trim[1]*bh/256f).toInt().coerceIn(0,bh-1)
    val tw=(trim[2]*bw/256f).toInt().coerceIn(1,bw-tx); val th=(trim[3]*bh/256f).toInt().coerceIn(1,bh-ty)
    return android.graphics.Bitmap.createBitmap(bmp,tx,ty,tw,th).asImageBitmap().also{cache.put(key,it)}
}"""
src = src.replace(anchor, helper)

start = src.index("@Composable fun DetailArt(")
end = src.index("@Composable fun PokemonArtTrimmedShiny(")
new_detail = '''@Composable fun DetailArt(id:Int,shiny:Boolean,modifier:Modifier=Modifier,label:String?=null){
    key(id,shiny){
    val context=LocalContext.current
    val image by produceState<ImageBitmap?>(null,id,shiny){
        // 256px first (immediate, same crop as the list), then the HD
        // asset when one exists; no HD asset keeps the 256px bitmap.
        val low=withContext(Dispatchers.IO){
            if(!shiny){
                trimmedBitmapCache.get(id) ?: runCatching{
                    var full=bitmapCache.get(id)
                    if(full==null){ full=context.assets.open("art/$id.webp").use{BitmapFactory.decodeStream(it)?.asImageBitmap()}; if(full!=null) bitmapCache.put(id,full) }
                    if(full==null) return@runCatching null
                    val t=ArtTrimData.get(context)[id] ?: return@runCatching full
                    val bmp=full.asAndroidBitmap(); val bw=bmp.width; val bh=bmp.height
                    val tx=(t[0]*bw/256f).toInt().coerceIn(0,bw-1); val ty=(t[1]*bh/256f).toInt().coerceIn(0,bh-1)
                    val tw=(t[2]*bw/256f).toInt().coerceIn(1,bw-tx); val th=(t[3]*bh/256f).toInt().coerceIn(1,bh-ty)
                    android.graphics.Bitmap.createBitmap(bmp,tx,ty,tw,th).asImageBitmap().also{trimmedBitmapCache.put(id,it)}
                }.getOrNull()
            } else {
                shinyTrimmedBitmapCache.get(id) ?: runCatching{
                    val full=context.assets.open("art/shiny/$id.webp").use{BitmapFactory.decodeStream(it)?.asImageBitmap()} ?: return@runCatching null
                    val tr=ShinyTrimData.get(context)[id] ?: return@runCatching full
                    val bmp=full.asAndroidBitmap(); val bw=bmp.width; val bh=bmp.height
                    val tx=(tr[0]*bw/256f).toInt().coerceIn(0,bw-1); val ty=(tr[1]*bh/256f).toInt().coerceIn(0,bh-1)
                    val tw=(tr[2]*bw/256f).toInt().coerceIn(1,bw-tx); val th=(tr[3]*bh/256f).toInt().coerceIn(1,bh-ty)
                    android.graphics.Bitmap.createBitmap(bmp,tx,ty,tw,th).asImageBitmap().also{shinyTrimmedBitmapCache.put(id,it)}
                }.getOrNull()
            }
        }
        value=low
        val hd=withContext(Dispatchers.IO){
            if(!shiny) hdTrimmedBitmap(context,"art-hd/$id.webp",ArtTrimData.get(context)[id],hdTrimmedBitmapCache,id)
            else hdTrimmedBitmap(context,"art-hd/shiny/$id.webp",ShinyTrimData.get(context)[id],hdShinyTrimmedBitmapCache,id)
        }
        if(hd!=null) value=hd
    }
    val ready=image
    if(ready!=null) Image(ready,label,modifier,contentScale=ContentScale.Fit,alignment=Alignment.BottomCenter) else Box(modifier,contentAlignment=Alignment.Center){LIcon(Icons.Rounded.CatchingPokemon,label,tint=MaterialTheme.colorScheme.onSurfaceVariant.copy(.4f),modifier=Modifier.size(44.dp))}
    }
}


'''
src = src[:start] + new_detail + src[end:]
APP.write_text(src)
print("PrismadexApp.kt patched")

# ---------- ArtViewer.kt ----------
vsrc = VIEW.read_text()
vstart = vsrc.index("@Composable\ninternal fun ShinyArt(")
vend = vsrc.index("/**\n * Full-screen art viewer")
new_shiny = '''@Composable
internal fun ShinyArt(id: Int, shiny: Boolean, modifier: Modifier = Modifier, label: String? = null) {
    // Full-screen viewer only: 256px bitmap first, then the HD asset
    // (art-hd/, 475px PokeAPI / 512px TonOfDirt726) when one exists.
    // No HD asset keeps the 256px bitmap; missing art keeps the honest
    // Poké Ball marker below (same as before).
    val assets = LocalContext.current.assets
    val path256 = if (shiny) shinyArtPath(id) else "art/$id.webp"
    val pathHd = if (shiny) "art-hd/shiny/$id.webp" else "art-hd/$id.webp"
    val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, id, shiny) {
        val low = withContext(Dispatchers.IO) {
            if (shiny) {
                shinyBitmapCache.get(id) ?: runCatching { assets.open(path256).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()?.also { shinyBitmapCache.put(id, it) }
            } else {
                runCatching { assets.open(path256).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
            }
        }
        value = low
        val hd = withContext(Dispatchers.IO) {
            runCatching { assets.open(pathHd).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
        }
        if (hd != null) value = hd
    }
    val ready = image
    if (ready != null) Image(ready, label, modifier, contentScale = androidx.compose.ui.layout.ContentScale.Fit, alignment = Alignment.Center)
    else Box(modifier, contentAlignment = Alignment.Center) {
        LIcon(
            Icons.Rounded.CatchingPokemon, label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(.4f),
            modifier = Modifier.size(44.dp)
        )
    }
}

'''
vsrc = vsrc[:vstart] + new_shiny + vsrc[vend:]
VIEW.write_text(vsrc)
print("ArtViewer.kt patched")
