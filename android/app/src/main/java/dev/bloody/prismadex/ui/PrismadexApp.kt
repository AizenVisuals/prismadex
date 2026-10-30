@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.bloody.prismadex.ui

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.bloody.prismadex.data.*
import dev.bloody.prismadex.damage.DamageEngine
import dev.bloody.prismadex.glass.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.IntOffset

private val Lilac=Color(0xFFC4B5FD)
private val Ink=Color(0xFF10121B)
private val Muted=Color(0xFFABAABE)
private val bitmapCache=object:LruCache<Int,ImageBitmap>(18*1024*1024){override fun sizeOf(key:Int,value:ImageBitmap)=value.width*value.height*4}

// Trim rects (08:34, art-trim.json: [x,y,w,h] in 256px canvas coords,
// measured with ImageMagick -trim for all 1,353 arts). Loaded once, lazily,
// on first list render; a missing id falls back to the full canvas.
private object ArtTrimData{ @Volatile var map:Map<Int,IntArray>?=null
 fun get(context:android.content.Context):Map<Int,IntArray>{ map?.let{return it}; synchronized(this){ map?.let{return it}; val m=mutableMapOf<Int,IntArray>(); runCatching{ val root=org.json.JSONObject(context.assets.open("art-trim.json").bufferedReader().use{it.readText()}); val keys=root.keys(); while(keys.hasNext()){ val k=keys.next(); val a=root.getJSONArray(k); m[k.toInt()]=intArrayOf(a.getInt(0),a.getInt(1),a.getInt(2),a.getInt(3)) } }.getOrNull(); val done=m.toMap(); map=done; return done } } }

@Composable fun PokemonArt(id:Int,modifier:Modifier=Modifier,label:String?=null){
    key(id){
    val assets=LocalContext.current.assets
    val image by produceState<ImageBitmap?>(bitmapCache.get(id),id){value=withContext(Dispatchers.IO){bitmapCache.get(id)?:runCatching{assets.open("art/$id.webp").use{BitmapFactory.decodeStream(it)?.asImageBitmap()}}.getOrNull()?.also{bitmapCache.put(id,it)}}}
    val ready=image
    if(ready!=null) Image(ready,label,modifier,contentScale=ContentScale.Fit,alignment=Alignment.Center) else Box(modifier,contentAlignment=Alignment.Center){LIcon(Icons.Rounded.CatchingPokemon,label,tint=solidBlend(MaterialTheme.colorScheme.onSurfaceVariant,MaterialTheme.colorScheme.surface,.4f),modifier=Modifier.size(44.dp))}
    }
}

// Shared trimmed-art loader (13:17 scroll fix): decode + crop once, in
// ONE place. PokemonArtTrimmed, PokemonArtTrimmedShiny and the scroll
// prefetch all go through here, so the three copies of the decode/crop
// code cannot drift apart. Callers run on Dispatchers.IO already.
private fun cropToTrim(full:ImageBitmap,trim:IntArray):ImageBitmap{
    val bmp=full.asAndroidBitmap(); val bw=bmp.width; val bh=bmp.height
    val tx=(trim[0]*bw/256f).toInt().coerceIn(0,bw-1); val ty=(trim[1]*bh/256f).toInt().coerceIn(0,bh-1)
    val tw=(trim[2]*bw/256f).toInt().coerceIn(1,bw-tx); val th=(trim[3]*bh/256f).toInt().coerceIn(1,bh-ty)
    return android.graphics.Bitmap.createBitmap(bmp,tx,ty,tw,th).asImageBitmap()
}
private fun loadTrimmedBitmap(context:android.content.Context,id:Int,shiny:Boolean):ImageBitmap?{
    if(!shiny){
        trimmedBitmapCache.get(id)?.let{return it}
        var full=bitmapCache.get(id)
        if(full==null){ full=runCatching{context.assets.open("art/$id.webp").use{BitmapFactory.decodeStream(it)?.asImageBitmap()}}.getOrNull() }
        if(full==null)return null
        val trim=ArtTrimData.get(context)[id] ?: return full.also{bitmapCache.put(id,it)}
        return cropToTrim(full,trim).also{trimmedBitmapCache.put(id,it)}
    }
    shinyTrimmedBitmapCache.get(id)?.let{return it}
    val full=runCatching{context.assets.open("art/shiny/$id.webp").use{BitmapFactory.decodeStream(it)?.asImageBitmap()}}.getOrNull() ?: return null
    val trim=ShinyTrimData.get(context)[id] ?: return full
    return cropToTrim(full,trim).also{shinyTrimmedBitmapCache.put(id,it)}
}


// List-only trimmed art (08:45 fix, supersedes the 08:34 size+offset
// version): the bitmap is CROPPED to its own trimmed content box once
// (art-trim.json) and drawn with ContentScale.Fit in the card box.
// The 08:34 version sized the full 256px bitmap larger than the
// 148x128dp box and shifted it with offset: Modifier.size is coerced
// to the parent constraints, so the image was stretched to 148x128
// (FillBounds) and the offset then pushed the top of the artwork out
// of the window (Mega Scizor showed only its legs, Mega Ampharos lost
// its head). Cropping + Fit cannot stretch or clip: uniform scale,
// centered, whole artwork visible. The detail header keeps
// PokemonArt/Fit over the reframed assets and is untouched by this.
private val trimmedBitmapCache=object:LruCache<Int,ImageBitmap>(24*1024*1024){override fun sizeOf(key:Int,value:ImageBitmap)=value.width*value.height*4}
@Composable fun PokemonArtTrimmed(id:Int,modifier:Modifier=Modifier,label:String?=null,alignment:Alignment=Alignment.Center){
    key(id){
    val context=LocalContext.current
    val cachedTrimmed=trimmedBitmapCache.get(id)
    val image by produceState<ImageBitmap?>(cachedTrimmed,id){if(cachedTrimmed==null)value=withContext(Dispatchers.IO){loadTrimmedBitmap(context,id,false)}}
    val ready=image
    if(ready!=null) Image(ready,label,modifier,contentScale=ContentScale.Fit,alignment=alignment) else Box(modifier,contentAlignment=Alignment.Center){LIcon(Icons.Rounded.CatchingPokemon,label,tint=solidBlend(MaterialTheme.colorScheme.onSurfaceVariant,MaterialTheme.colorScheme.surface,.4f),modifier=Modifier.size(44.dp))}
    }
}


// Shiny trim rects (art-trim-shiny.json, same [x,y,w,h] 256px coords as
// ArtTrimData, 1,330 entries). Loaded once, lazily; a missing id falls
// back to the full canvas.
private object ShinyTrimData{ @Volatile var map:Map<Int,IntArray>?=null
 fun get(context:android.content.Context):Map<Int,IntArray>{ map?.let{return it}; synchronized(this){ map?.let{return it}; val m=mutableMapOf<Int,IntArray>(); runCatching{ val root=org.json.JSONObject(context.assets.open("art-trim-shiny.json").bufferedReader().use{it.readText()}); val keys=root.keys(); while(keys.hasNext()){ val k=keys.next(); val a=root.getJSONArray(k); m[k.toInt()]=intArrayOf(a.getInt(0),a.getInt(1),a.getInt(2),a.getInt(3)) } }.getOrNull(); val done=m.toMap(); map=done; return done } } }
private val shinyTrimmedBitmapCache=object:LruCache<Int,ImageBitmap>(24*1024*1024){override fun sizeOf(key:Int,value:ImageBitmap)=value.width*value.height*4}
// High-res detail art (30/10/2026): assets/art-hd/<id>.webp (475 canvas,
// PokeAPI official artwork) and art-hd/shiny/<id>.webp (475 PokeAPI / 512
// TonOfDirt726 originals), used ONLY by the detail header (DetailArt) and
// the full-screen viewer (ArtViewer/ShinyArt). List/grid/team/spotlight/
// cards keep the 256px loaders untouched. Loaded lazily off the UI thread;
// the 256px trimmed bitmap shows first and stays if no HD asset exists
// (4 normal forms have none; 78 normal + 95 shiny HD files were held for
// framing mismatch and are not bundled -- see research/HD-ART-IMPORT-MANIFEST.json).
private val hdTrimmedBitmapCache=object:LruCache<Int,ImageBitmap>(16*1024*1024){override fun sizeOf(key:Int,value:ImageBitmap)=value.width*value.height*4}
private val hdShinyTrimmedBitmapCache=object:LruCache<Int,ImageBitmap>(16*1024*1024){override fun sizeOf(key:Int,value:ImageBitmap)=value.width*value.height*4}

// Detail framing (13:34 fix, 30/10/2026): ONE shared frame per Pokemon
// per resolution stage, from art-trim-detail.json -- measured content
// boxes (ImageMagick -trim, 256px coords) for every bundled file:
// normal/shiny at 256px and normal/shiny at HD. Root cause of the
// toggle jump and the cuts: each version was cropped to its OWN trim
// rect (787/1,330 normal-vs-shiny pairs differ by >1dp on screen), and
// the HD bitmaps were cropped with the 256px trim rect -- 143 HD
// normal files and 1,079/1,235 HD shiny files are cut by that rect
// (Mega Pidgeot 10073: HD content runs to y=459/475, the 256px trim
// window ends at y=416/475, so its feet are cut). The fix crops every
// version to its OWN measured content box (never another version's
// box: 0 cuts by construction) and draws it at a SHARED scale derived
// from the stage frame (union of that stage's normal+shiny boxes),
// bottom-center anchored: same scale, same baseline, same center for
// normal and shiny, at both stages. List/grid/team loaders are
// untouched -- this path is detail-only. The full-screen viewer
// (ArtViewer/ShinyArt) keeps its own full-canvas zoom/pan.
private data class DetailEntry(val n:IntArray,val s:IntArray?,val hn:IntArray?,val hs:IntArray?,val f256:IntArray,val fhd:IntArray?)
private object DetailTrimData{ @Volatile var map:Map<Int,DetailEntry>?=null
 fun get(context:android.content.Context):Map<Int,DetailEntry>{ map?.let{return it}; synchronized(this){ map?.let{return it}; val m=mutableMapOf<Int,DetailEntry>(); runCatching{ val root=org.json.JSONObject(context.assets.open("art-trim-detail.json").bufferedReader().use{it.readText()}); val keys=root.keys(); fun arr(o:org.json.JSONObject,k:String):IntArray?{ if(!o.has(k))return null; val a=o.getJSONArray(k); return intArrayOf(a.getInt(0),a.getInt(1),a.getInt(2),a.getInt(3)) }; while(keys.hasNext()){ val k=keys.next(); val o=root.getJSONObject(k); val n=arr(o,"n") ?: continue; m[k.toInt()]=DetailEntry(n,arr(o,"s"),arr(o,"hn"),arr(o,"hs"),arr(o,"f256") ?: n,arr(o,"fhd")) } }.getOrNull(); val done=m.toMap(); map=done; return done } } }
private val detailTrimmedBitmapCache=object:LruCache<Int,ImageBitmap>(24*1024*1024){override fun sizeOf(key:Int,value:ImageBitmap)=value.width*value.height*4}
private val detailShinyTrimmedBitmapCache=object:LruCache<Int,ImageBitmap>(24*1024*1024){override fun sizeOf(key:Int,value:ImageBitmap)=value.width*value.height*4}
// Detail-only loader: crops the requested asset to ITS OWN measured
// content box (256px coords, scaled by the decoded bitmap's canvas by
// cropToTrim). Returns null when the asset does not exist (honest
// fallback: caller keeps whatever it already shows). When no detail
// entry exists for the id, falls back to the shared list loader and
// reports the bitmap's own size as its frame (old Fit behaviour).
private fun loadDetailBitmap(context:android.content.Context,id:Int,shiny:Boolean,hd:Boolean):ImageBitmap?{
    val entry=DetailTrimData.get(context)[id]
    if(entry==null) return loadTrimmedBitmap(context,id,shiny)
    if(!hd){
        val cache=if(shiny) detailShinyTrimmedBitmapCache else detailTrimmedBitmapCache
        cache.get(id)?.let{return it}
        return runCatching{
            val full=context.assets.open(if(shiny)"art/shiny/$id.webp" else "art/$id.webp").use{BitmapFactory.decodeStream(it)?.asImageBitmap()} ?: return@runCatching null
            val rect=if(shiny)(entry.s ?: entry.n) else entry.n
            cropToTrim(full,rect).also{cache.put(id,it)}
        }.getOrNull()
    }
    val cache=if(shiny) hdShinyTrimmedBitmapCache else hdTrimmedBitmapCache
    cache.get(id)?.let{return it}
    return runCatching{
        val full=context.assets.open(if(shiny)"art-hd/shiny/$id.webp" else "art-hd/$id.webp").use{BitmapFactory.decodeStream(it)?.asImageBitmap()} ?: return@runCatching null
        val rect=if(shiny)(entry.hs ?: entry.s ?: entry.n) else (entry.hn ?: entry.n)
        cropToTrim(full,rect).also{cache.put(id,it)}
    }.getOrNull()
}


// Detail-header art (10:12 follow-up, HD upgrade 30/10/2026, framing
// fix 13:34): cropped to each version's OWN measured content box and
// drawn at the stage frame's shared scale, bottom-center anchored, so
// normal/shiny toggling never moves the art and no version is ever
// cropped. The 256px bitmap shows first; the HD asset replaces it
// when it exists, off the UI thread. Forms without an HD asset keep
// the 256px bitmap. No stretch: the drawn size is derived from the
// cropped bitmap's own aspect at the shared scale.
@Composable fun DetailArt(id:Int,shiny:Boolean,modifier:Modifier=Modifier,label:String?=null){
    key(id,shiny){
    val context=LocalContext.current
    var entryState by remember(id){mutableStateOf<DetailEntry?>(DetailTrimData.map?.get(id))}
    LaunchedEffect(id){entryState=withContext(Dispatchers.IO){DetailTrimData.get(context)[id]}}
    val entry=entryState
    val initial=if(shiny) detailShinyTrimmedBitmapCache.get(id) else detailTrimmedBitmapCache.get(id)
    val loaded by produceState<Pair<ImageBitmap,Boolean>?>(initial?.let{it to false},id,shiny){
        value=withContext(Dispatchers.IO){loadDetailBitmap(context,id,shiny,false)?.let{it to false}}
        withContext(Dispatchers.IO){loadDetailBitmap(context,id,shiny,true)}?.let{value=it to true}
    }
    val ready=loaded?.first
    if(ready!=null){
        val isHd=loaded?.second==true
        val own=if(entry!=null){ if(isHd){ if(shiny)(entry.hs ?: entry.s ?: entry.n) else (entry.hn ?: entry.n) } else { if(shiny)(entry.s ?: entry.n) else entry.n } } else intArrayOf(0,0,ready.width,ready.height)
        val frame=if(entry!=null){ if(isHd)(entry.fhd ?: entry.f256) else entry.f256 } else intArrayOf(0,0,ready.width,ready.height)
        BoxWithConstraints(modifier,contentAlignment=Alignment.BottomCenter){
            val scale=minOf(maxWidth.value/frame[2].toFloat(),maxHeight.value/frame[3].toFloat())
            Image(ready,label,Modifier.size((own[2]*scale).dp,(own[3]*scale).dp),contentScale=ContentScale.FillBounds)
        }
    } else Box(modifier,contentAlignment=Alignment.Center){LIcon(Icons.Rounded.CatchingPokemon,label,tint=solidBlend(MaterialTheme.colorScheme.onSurfaceVariant,MaterialTheme.colorScheme.surface,.4f),modifier=Modifier.size(44.dp))}
    }
}


// List/grid shiny art (10:30, global shiny toggle): same own-content-box
// cropping as PokemonArtTrimmed, but from art/shiny/<id>.webp. Loaded
// lazily per visible row only (produceState + IO, bounded LRU cache);
// rows without a real shiny asset are never routed here -- the caller
// falls back to the normal trimmed art, never a hue-shifted fake.
@Composable fun PokemonArtTrimmedShiny(id:Int,modifier:Modifier=Modifier,label:String?=null,alignment:Alignment=Alignment.Center){
    key(id){
    val context=LocalContext.current
    val cachedShiny=shinyTrimmedBitmapCache.get(id)
    val image by produceState<ImageBitmap?>(cachedShiny,id){if(cachedShiny==null)value=withContext(Dispatchers.IO){loadTrimmedBitmap(context,id,true)}}
    val ready=image
    if(ready!=null) Image(ready,label,modifier,contentScale=ContentScale.Fit,alignment=alignment) else Box(modifier,contentAlignment=Alignment.Center){LIcon(Icons.Rounded.CatchingPokemon,label,tint=solidBlend(MaterialTheme.colorScheme.onSurfaceVariant,MaterialTheme.colorScheme.surface,.4f),modifier=Modifier.size(44.dp))}
    }
}

@Composable fun PrismadexApp(){
    val context=LocalContext.current
    val repo=remember{PokemonRepository(context)}
    val damageEngine=remember{DamageEngine(context)}
    val store=remember{Store(context)}
    val drafts=remember{BuildDraftStore(context)}
    var pendingBuild by remember{mutableStateOf<Build?>(null)}
    var exitBuild by remember{mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    val writeMutex=remember{Mutex()}
    var catalog by remember{mutableStateOf<List<Pokemon>>(emptyList())}
    var state by remember{mutableStateOf(UserState())}
    var initialized by remember{mutableStateOf(false)}
    var failure by remember{mutableStateOf<String?>(null)}
    var screen by rememberSaveable{mutableStateOf("dex")}
    var requestedGame by rememberSaveable{mutableStateOf<Int?>(null)}
    var selectedId by rememberSaveable{mutableStateOf<Int?>(null)}
    var setPokemonId by rememberSaveable{mutableStateOf<Int?>(null)}
    var detailGame by rememberSaveable{mutableIntStateOf(23)}
    var detailTab by rememberSaveable{mutableStateOf("status")}
    var detailOrder by rememberSaveable{mutableStateOf(intArrayOf())}
    var detailDirection by remember{mutableIntStateOf(1)}
    var showTeamPicker by rememberSaveable{mutableStateOf(false)}
    var showCreateTeam by rememberSaveable{mutableStateOf(false)}
    var showAccount by rememberSaveable{mutableStateOf(false)}
    var showTrainerCard by rememberSaveable{mutableStateOf(false)}
    var showDummy by rememberSaveable{mutableStateOf(false)}
    var artViewerId by rememberSaveable{mutableStateOf<Int?>(null)}
    var shinyArt by rememberSaveable{mutableStateOf(false)}
    var requestedDefender by rememberSaveable{mutableStateOf<String?>(null)}
    var editingDummyId by rememberSaveable{mutableStateOf<String?>(null)}
    var email by remember{mutableStateOf<String?>(null)}
    var savedLabel by remember{mutableStateOf("Salvo no aparelho")}
    val snackbar=remember{SnackbarHostState()}
    val ui=LocalUiStrings.current
    val screenStates=rememberSaveableStateHolder()
    LaunchedEffect(Unit){runCatching{catalog=repo.loadCatalog();state=withContext(Dispatchers.IO){store.load()};email=withContext(Dispatchers.IO){store.currentEmail};initialized=true}.onFailure{failure="Não foi possível abrir os dados locais."}}
    LaunchedEffect(initialized, state.appIcon){ if(initialized) applyAppIcon(context, state.appIcon) }
    fun persist(next:UserState){state=next;savedLabel="Salvando…";scope.launch{writeMutex.withLock{runCatching{withContext(Dispatchers.IO){store.save(next)}}.onSuccess{savedLabel="Salvo no aparelho"}.onFailure{savedLabel="Falha ao salvar";snackbar.showSnackbar(ui.text(it.message?:"Não foi possível salvar."))}}}}
    fun favorite(p:Pokemon){persist(state.copy(favorites=if(p.id in state.favorites)state.favorites-p.id else state.favorites+p.id))}
    var removalTeamId by rememberSaveable{mutableStateOf<String?>(null)}
    var removalPokemonId by rememberSaveable{mutableStateOf<Int?>(null)}
    var removing by remember{mutableStateOf(false)}
    fun requestRemoval(p:Pokemon){removalTeamId=state.activeTeamId;removalPokemonId=p.id}
    // shiny=true from the detail (star state) and from the team picker
    // (sparkle above the +; 10:52 rule, extended 11:45):
    // star active + real shiny asset -> the member enters as shiny; every
    // other add path (picker, set screen) defaults to standard. An existing
    // build is kept, only its shiny flag is set.
    fun toggleTeam(p:Pokemon,shiny:Boolean=false){
        if(p.id in state.team){requestRemoval(p);return}
        if(!p.availableIn(state.activeTeam.gameId)){scope.launch{snackbar.showSnackbar(ui.text("Pokémon não listado para ${gameName(state.activeTeam.gameId)}."))};return}
        if(state.team.size==6){scope.launch{snackbar.showSnackbar(ui.text("Sua equipe já tem seis parceiros."))};return}
        if(state.team.mapNotNull{id->catalog.find{it.id==id}}.any{it.baseId==p.baseId}){scope.launch{snackbar.showSnackbar(ui.text("Já existe uma forma desta espécie na equipe."))};return}
        persist(state.updateTeam{val existing=it.builds[p.id];it.copy(members=it.members+p.id,builds=it.builds+(p.id to (existing?:Build()).copy(shiny=shiny)))})
        scope.launch{snackbar.showSnackbar(ui.text(if(shiny)"${p.name} shiny adicionado" else "${p.name} adicionado"))}
    }
    fun openDetail(p:Pokemon,order:List<Pokemon>,game:Int=23){
        detailGame=game
        detailOrder=order.map{it.id}.toIntArray()
        selectedId=p.id;detailTab="status";detailDirection=1
    }
    fun browseDetail(id:Int){
        if(pendingBuild!=null){scope.launch{snackbar.showSnackbar(ui.text("Salve ou descarte o rascunho antes de trocar de Pokémon."))};return}
        val order=detailOrder.toList()
        detailDirection=if(order.indexOf(id)<order.indexOf(selectedId)) -1 else 1
        selectedId=id
    }
    fun closePokemon(){if(pendingBuild!=null)exitBuild=true else selectedId=null}
    fun closeSet(){if(pendingBuild!=null)exitBuild=true else setPokemonId=null}
    fun openSet(p:Pokemon){if(pendingBuild!=null){scope.launch{snackbar.showSnackbar(ui.text("Salve ou descarte o rascunho antes de trocar de Pokémon."))};return};selectedId=null;setPokemonId=p.id}
    fun saveSet(p:Pokemon,build:Build,closeAfter:Boolean=false){
        val teamId=state.activeTeamId
        val draftKey=drafts.key(email,teamId,p.id)
        scope.launch{writeMutex.withLock{
            val result=repo.validateBuild(p,build,state.teams.first{it.id==teamId}.gameId)
            if(result.isFailure){snackbar.showSnackbar(ui.text(result.exceptionOrNull()?.message?:"Revise o set."));return@withLock}
            val next=state.copy(teams=state.teams.map{if(it.id==teamId)it.copy(builds=it.builds+(p.id to build)) else it})
            savedLabel="Salvando…"
            runCatching{withContext(Dispatchers.IO){store.save(next)}}.onSuccess{
                state=next;drafts.clear(draftKey);pendingBuild=null;savedLabel="Salvo no aparelho"
                if(closeAfter){exitBuild=false;selectedId=null;setPokemonId=null}
                snackbar.showSnackbar(ui.text("Set salvo no aparelho."))
            }.onFailure{savedLabel="Falha ao salvar";snackbar.showSnackbar(ui.text("Não foi possível salvar. Seu rascunho foi mantido."))}
        }}
    }
    val light=when(state.appearanceMode){"light"->true;"dark"->false;else->!isSystemInDarkTheme()}
    val selectedTheme=(AppThemes.firstOrNull{it.key==state.theme}?:AppThemes.first()).colors(light)
    val view=LocalView.current
    SideEffect{(view.context as? Activity)?.window?.let{window->
        WindowCompat.getInsetsController(window,view).apply{isAppearanceLightStatusBars=light;isAppearanceLightNavigationBars=light}
    }}
    val accent by animateColorAsState(selectedTheme.accent,tween(240),label="theme accent")
    val bg by animateColorAsState(selectedTheme.background,tween(240),label="theme background")
    val colors=opaqueThemeScheme(accent,bg,selectedTheme.surface,light)
    val drawerState=rememberDrawerState(DrawerValue.Closed)
    MaterialTheme(colorScheme=colors){
        val backdrop=rememberPrismaBackdrop()
        CompositionLocalProvider(LocalPrismaBackdrop provides backdrop, LocalContentColor provides colors.onBackground, LocalReduceMotion provides rememberReduceMotion()){
            ModalNavigationDrawer(drawerState=drawerState,gesturesEnabled=drawerState.isOpen,drawerContent={AppDrawer(screen,onNavigate={key->selectedId=null;setPokemonId=null;showDummy=false;showTrainerCard=false;screen=key;scope.launch{drawerState.close()}},onAccount={showAccount=true;scope.launch{drawerState.close()}})}){
            BoxWithConstraints(Modifier.fillMaxSize().background(bg).windowInsetsPadding(WindowInsets.safeDrawing).clipToBounds()){
                val expanded=maxWidth>=600.dp
                val fontScale=LocalDensity.current.fontScale
                val navigationHeight=if(fontScale>1.25f)(52+32*fontScale).dp else 72.dp
                val railWidth=if(fontScale>1.25f)(72*fontScale).dp.coerceAtMost(120.dp) else 72.dp
                val railVisible=expanded&&selectedId==null&&setPokemonId==null&&!showAccount&&!showTrainerCard&&!showDummy&&!showTeamPicker
                CompositionLocalProvider(LocalPrismaLayout provides PrismaLayout(expanded,if(expanded)16.dp else navigationHeight+20.dp)){
                Box(Modifier.fillMaxSize().padding(start=if(railVisible)railWidth+24.dp else 0.dp)){

                if(!initialized){Box(Modifier.align(Alignment.Center)){PrismaSplash(failure,accent,LocalReduceMotion.current)}}
                else{
                    val target=if(showTeamPicker)"team-picker" else if(showTrainerCard)"trainer-card" else if(showAccount)"account" else if(showDummy)"dummy/${editingDummyId?:"new"}" else if(setPokemonId!=null)"set/$setPokemonId" else selectedId?.let{"pokemon/$it"}?:screen
                    BackHandler(showTeamPicker||drawerState.isOpen||showTrainerCard||showAccount||showDummy||selectedId!=null||setPokemonId!=null||screen!="dex"||artViewerId!=null){when{artViewerId!=null->artViewerId=null;showTeamPicker->showTeamPicker=false;drawerState.isOpen->scope.launch{drawerState.close()};showTrainerCard->showTrainerCard=false;showAccount->showAccount=false;showDummy->showDummy=false;setPokemonId!=null->closeSet();selectedId!=null->closePokemon();else->screen="dex"}}
                    val reduceMotion=LocalReduceMotion.current
                    Box(Modifier.fillMaxSize().prismaBackdrop(backdrop)){
                        AnimatedContent(targetState=target,transitionSpec={
                            if(reduceMotion)(fadeIn(tween(120)) togetherWith fadeOut(tween(120)))
                            else{
                            val browsing=initialState.startsWith("pokemon/")&&targetState.startsWith("pokemon/")
                            val opening=targetState.startsWith("pokemon/")||targetState.startsWith("set/")||targetState=="account"
                            if(browsing)(slideInHorizontally(tween(280,easing=FastOutSlowInEasing)){it*detailDirection}+fadeIn(tween(180))) togetherWith (slideOutHorizontally(tween(280,easing=FastOutSlowInEasing)){-it*detailDirection}+fadeOut(tween(180)))
                            else
                            if(opening)(slideInHorizontally(tween(330,easing=FastOutSlowInEasing)){it/5}+fadeIn(tween(240))) togetherWith (fadeOut(tween(160))+scaleOut(tween(300),targetScale=.97f))
                            else (fadeIn(tween(220))+slideInHorizontally(tween(300)){ -it/12}) togetherWith fadeOut(tween(150))}
                        },label="native screen navigation"){route->
                            screenStates.SaveableStateProvider(route){when{
                                route=="team-picker"->TeamPokemonPicker(catalog,state,onAdd={p,shiny->if(p.id !in state.team)toggleTeam(p,shiny)},onClose={showTeamPicker=false})
                                route=="trainer-card"->TrainerCardCustomizationScreen(state=state,catalog=catalog,onState={persist(it)},onBack={showTrainerCard=false})
                                route=="account"->AccountScreen(store,writeMutex,email,state.theme,onBack={showAccount=false},onTheme={persist(state.copy(theme=it))},onAccountChanged={state=withContext(Dispatchers.IO){store.load()};email=withContext(Dispatchers.IO){store.currentEmail}},state=state,catalog=catalog,onState={persist(it)},onOpenTrainerCard={showTrainerCard=true})
                                route.startsWith("dummy/")->DummyEditorScreen(catalog,repo,state.dummies.find{it.id==editingDummyId},onBack={showDummy=false},onSave={dummy->scope.launch{val p=catalog.find{it.id==dummy.pokemonId};val checked=p?.let{repo.validateBuild(it,dummy.build)};if(checked?.isSuccess==true&&(state.dummies.size<50||state.dummies.any{it.id==dummy.id})){persist(state.copy(dummies=state.dummies.filterNot{it.id==dummy.id}+dummy));requestedDefender="dummy/${dummy.id}";showDummy=false;screen="calc";snackbar.showSnackbar(ui.text("Alvo de teste salvo no aparelho."))}else snackbar.showSnackbar(ui.text(checked?.exceptionOrNull()?.message?:"Limite de 50 alvos atingido."))}})
                                route=="calc"->DamageScreen(catalog,state,repo,damageEngine,onSelectTeam={persist(state.copy(activeTeamId=it))},onEdit={p,teamId->if(teamId!=null)persist(state.copy(activeTeamId=teamId));pendingBuild=null;detailOrder=catalog.sortedWith(compareBy({it.baseId},{it.id})).map{it.id}.toIntArray();selectedId=p.id;detailTab="status"},onDummy={editingDummyId=it;showDummy=true},onDeleteDummy={id->persist(state.copy(dummies=state.dummies.filterNot{it.id==id}))},requestedDefender=requestedDefender,onDefenderConsumed={requestedDefender=null},onChoosePokemon={requestedGame=state.activeTeam.gameId;screen="dex"})
                                route.startsWith("set/")->catalog.find{it.id==route.substringAfter('/').toIntOrNull()}?.let{p->TeamSetScreen(p,repo,state,onBack={closeSet()},onTeam={toggleTeam(p)},onBuild={build->saveSet(p,build)},draftKey=drafts.key(email,state.activeTeamId,p.id),onDraft={pendingBuild=it},onOpenDetail={detailGame=state.activeTeam.gameId;detailOrder=state.team.toIntArray();setPokemonId=null;selectedId=p.id;detailTab="status"},onArtViewer={artViewerId=it.id})}
                                route.startsWith("pokemon/")->catalog.find{it.id==route.substringAfter('/').toIntOrNull()}?.let{p->PokemonScreen(p,repo,state,detailTab,{detailTab=it},{favorite(p)},{shiny->toggleTeam(p,shiny)},onBuild={build->saveSet(p,build)},draftKey=drafts.key(email,state.activeTeamId,p.id),onDraft={pendingBuild=it},footerVisible=pendingBuild!=null&&detailTab=="status",catalog=catalog,referenceGame=detailGame,onReferenceGame={detailGame=it},onArtViewer={artViewerId=it.id},shinyArt=shinyArt,onShinyArt={shinyArt=it},
                                    onBack={closePokemon()},onOpenSet={openSet(it)},
                                    previousId=adjacentPokemonId(detailOrder.toList(),p.id,-1),nextId=adjacentPokemonId(detailOrder.toList(),p.id,1),
                                    canBrowse=pendingBuild==null,onBrowse={browseDetail(it)},onRelated={related->detailOrder=catalog.sortedWith(compareBy({it.baseId},{it.id})).map{it.id}.toIntArray();browseDetail(related.id)})}

                                route=="team"->TeamScreen(catalog,repo,state,onRemove={requestRemoval(it)},onOpen={openSet(it)},onAdd={showTeamPicker=true},onCalculate={screen="calc"},onState={persist(it)},creating=showCreateTeam,onCreatingChange={showCreateTeam=it})
                                route=="meta"->MetaScreen(catalog,state.battleFormat,onFormat={persist(state.copy(battleFormat=it))},onOpen={openDetail(it,catalog.sortedWith(compareBy({p->p.baseId},{p->p.id})))})
                                route=="items"->ItemCatalogScreen()
                                route=="abilities"->AbilitiesDexScreen(catalog,onOpenPokemon={openDetail(it,catalog.sortedWith(compareBy({p->p.baseId},{p->p.id})))})
                                route=="moves"->MovesDexScreen(catalog,repo,onOpenPokemon={openDetail(it,catalog.sortedWith(compareBy({p->p.baseId},{p->p.id})))})
                                route=="badges"->BadgeTrackerScreen(state,onState={persist(it)})
                                route=="settings"->SettingsScreen(state,email,onState={persist(it)},onAccount={showAccount=true},onSources={screen="sources"})
                                route=="sources"->SourcesScreen(catalog)
                                else->DexScreen(catalog,state,route=="favorites",requestedGame=requestedGame,onGameConsumed={requestedGame=null},onOpen={p,order,game->openDetail(p,order,game)},onFavorite={favorite(it)},onLayout={persist(state.copy(layout=it))})
                            }}
                        }
                    }
                    if(target=="team-picker"){ /* picker owns its title and close action */ }
                    else if(target.startsWith("pokemon/")||target.startsWith("set/")){
                        if(pendingBuild!=null&&(detailTab=="status"||target.startsWith("set/")))Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth().imePadding(),color=colors.surface,tonalElevation=6.dp){Row(Modifier.padding(horizontal=18.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){LText("Rascunho no aparelho",Modifier.weight(1f),fontSize=13.sp);Button(onClick={catalog.find{it.id==(selectedId?:setPokemonId)}?.let{p->pendingBuild?.let{saveSet(p,it)}}},enabled=(selectedId?:setPokemonId) in state.team){LText("Salvar set")}}}
                    }else if(target!="account"&&!target.startsWith("dummy/")&&!target.startsWith("set/")){
                        Box(Modifier.align(Alignment.TopCenter)){
                            val title=mapOf("dex" to "Pokédex","favorites" to "Favoritos","team" to "Equipes","calc" to "Calculadora","meta" to "Meta online","items" to "Itens","abilities" to "Habilidades","moves" to "Golpes","badges" to "Insígnias","settings" to "Configurações","sources" to "Champions")[screen]?:"Pokédex"
                            PrismaTopBar(title,accent,savedLabel,{scope.launch{drawerState.open()}},{showAccount=true},bg)
                        }
                        if(!expanded)PrismaNavigation(screen,{screen=it},Modifier.align(Alignment.BottomCenter).padding(horizontal=14.dp,vertical=10.dp).fillMaxWidth().height(navigationHeight))
                    }
                }
                if(initialized&&!showTeamPicker&&screen=="team"&&selectedId==null&&setPokemonId==null&&!showAccount&&!showTrainerCard&&!showDummy){
                    Box(Modifier.align(Alignment.TopEnd).padding(top=72.dp,end=22.dp).height(56.dp).widthIn(min=144.dp,max=280.dp).semantics{contentDescription=ui.text("Criar nova equipe")}.clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface,RoundedCornerShape(24.dp)).border(1.dp,MaterialTheme.colorScheme.outlineVariant,RoundedCornerShape(24.dp)).clickable{showCreateTeam=true},contentAlignment=Alignment.Center){
                        Row(Modifier.padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){LIcon(Icons.Rounded.Add,null,Modifier.size(24.dp),tint=accent);LText("Nova equipe",Modifier.padding(start=8.dp),fontSize=14.sp,color=accent,maxLines=1)}
                    }
                }
                val removalTeam=state.teams.find{it.id==removalTeamId}
                val removalPokemon=catalog.find{it.id==removalPokemonId}
                if(removalTeam!=null&&removalPokemon!=null){
                    AlertDialog(onDismissRequest={if(!removing){removalTeamId=null;removalPokemonId=null}},
                        title={LText("Remover ${removalPokemon.name}?")},
                        text={LText("O Pokémon e seu set serão removidos da equipe ${removalTeam.name}. Outras equipes e favoritos serão mantidos.")},
                        confirmButton={TextButton(enabled=!removing,onClick={
                            removing=true
                            val teamId=removalTeam.id;val pokemonId=removalPokemon.id;val owner=email
                            scope.launch{writeMutex.withLock{
                                val next=state.removeTeamMember(teamId,pokemonId)
                                runCatching{withContext(Dispatchers.IO){store.save(next)}}.onSuccess{
                                    state = next
                                    drafts.clear(drafts.key(owner,teamId,pokemonId))
                                    if(state.activeTeamId==teamId&&(selectedId==pokemonId||setPokemonId==pokemonId))pendingBuild=null
                                    removalTeamId=null;removalPokemonId=null;savedLabel="Salvo no aparelho"
                                }.onFailure{savedLabel="Falha ao salvar";snackbar.showSnackbar(ui.text("Não foi possível remover. Tente novamente."))}
                            };removing=false}
                        }){LText(if(removing)"Removendo…" else "Remover",color=MaterialTheme.colorScheme.error)}},
                        dismissButton={TextButton(enabled=!removing,onClick={removalTeamId=null;removalPokemonId=null}){LText("Cancelar")}})
                }
                if(exitBuild){AlertDialog(onDismissRequest={exitBuild=false},title={LText("Alterações não salvas")},text={LText("Seu rascunho está neste aparelho. Salve o set, descarte as alterações ou continue editando.")},confirmButton={TextButton(onClick={catalog.find{it.id==(selectedId?:setPokemonId)}?.let{p->pendingBuild?.let{saveSet(p,it,true)}}}){LText("Salvar e sair")}},dismissButton={Row{TextButton(onClick={(selectedId?:setPokemonId)?.let{drafts.clear(drafts.key(email,state.activeTeamId,it))};pendingBuild=null;exitBuild=false;selectedId=null;setPokemonId=null}){LText("Descartar")};TextButton(onClick={exitBuild=false}){LText("Continuar")}}})}
                SnackbarHost(snackbar,Modifier.align(Alignment.BottomCenter).imePadding().padding(bottom=if(expanded)20.dp else navigationHeight+28.dp))
                artViewerId?.let{id->catalog.find{it.id==id}?.let{p->ArtViewer(p,shinyArt,{shinyArt=it},{artViewerId=null})}}
                }
                if(railVisible)PrismaNavigation(screen,{screen=it},Modifier.align(Alignment.CenterStart).padding(start=12.dp,top=8.dp,bottom=8.dp).width(railWidth).fillMaxHeight(),vertical=true)
                }
            }
            }
        }
    }
}

@Composable internal fun DexScreen(catalog:List<Pokemon>,state:UserState,favoritesOnly:Boolean,onOpen:(Pokemon,List<Pokemon>,Int)->Unit,onFavorite:(Pokemon)->Unit,onLayout:(String)->Unit,requestedGame:Int?=null,onGameConsumed:()->Unit={}){
    var query by rememberSaveable(favoritesOnly){mutableStateOf("")}
    var selectedGame by rememberSaveable{mutableStateOf(0)}
    var generation by rememberSaveable{mutableStateOf(0)}
    var type by rememberSaveable{mutableStateOf<String?>(null)}
    var alphabetical by rememberSaveable{mutableStateOf(false)}
    var showFilterSheet by rememberSaveable{mutableStateOf(false)}
    var showShinyArt by rememberSaveable{mutableStateOf(false)}
    var megaOnly by rememberSaveable{mutableStateOf(false)}
    var gigantamaxOnly by rememberSaveable{mutableStateOf(false)}
    var alternateFormsOnly by rememberSaveable{mutableStateOf(false)}
    val filterSheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)
    fun clearAll(){query="";selectedGame=0;generation=0;type=null;alphabetical=false;showShinyArt=false;megaOnly=false;gigantamaxOnly=false;alternateFormsOnly=false}
    // The query has its own clear control inside the search field, so it is not counted here.
    // The game is only an active filter outside favorites, where it starts forced to Nacional.
    val activeFilterCount=(if(selectedGame!=0)1 else 0)+(if(generation!=0)1 else 0)+(if(type!=null)1 else 0)+(if(alphabetical)1 else 0)+(if(megaOnly)1 else 0)+(if(gigantamaxOnly)1 else 0)+(if(alternateFormsOnly)1 else 0)
    LaunchedEffect(requestedGame){if(requestedGame!=null){selectedGame=requestedGame;generation=0;type=null;query="";onGameConsumed()}}
    val context=LocalContext.current
    // Regional-first order (11:56): the regional dex map is loaded once
    // (bundled regional-dex.json, IO thread, soft-fails to empty) and
    // shared by the list and the team picker. An empty map keeps the
    // plain national order, so a missing file never breaks the list.
    val regionalDex by produceState<Map<Int, List<Int>>>(emptyMap(),catalog){value=withContext(Dispatchers.IO){RegionalDexData.get(context)}}
    val filterIndex by produceState<PokemonFilterIndex?>(null,catalog,regionalDex){value=withContext(Dispatchers.Default){PokemonFilterIndex(catalog,regionalDex)}}
    // Shiny availability, loaded ONCE (filenames only, IO thread): the
    // authoritative set is the files in art/shiny; the trim keys are
    // the fallback (id 10036 has a shiny file but no trim entry).
    val shinyIds by produceState<Set<Int>?>(null){value=withContext(Dispatchers.IO){runCatching{context.assets.list("art/shiny")?.mapNotNull{it.removeSuffix(".webp").toIntOrNull()}?.toSet()}.getOrNull()?.takeIf{it.isNotEmpty()} ?: ShinyTrimData.get(context).keys}}
    val gameLogos by rememberGameLogoBitmaps()
    val list by produceState<List<Pokemon>>(emptyList(),filterIndex,state.favorites,query,selectedGame,generation,type,alphabetical,favoritesOnly,megaOnly,gigantamaxOnly,alternateFormsOnly){
        value=withContext(Dispatchers.Default){filterIndex?.select(selectedGame,PokemonFilter(query=query,types=type?.let{setOf(it)}?:emptySet(),generation=generation,sort=if(alphabetical)PokemonSort.NAME else PokemonSort.NUMBER,megaOnly=megaOnly,gigantamaxOnly=gigantamaxOnly,alternateFormsOnly=alternateFormsOnly))?.filter{!favoritesOnly||it.id in state.favorites}.orEmpty()}
    }
    val scroll=rememberLazyGridState()
    // Prefetch (13:17 scroll fix): once scrolling settles (250ms), warm
    // the trimmed-art cache for the rows just ahead of the viewport, so
    // visible rows rarely decode on demand. Bounded (36 rows), IO-only,
    // cache-checked inside loadTrimmedBitmap; cancelled while scrolling.
    LaunchedEffect(scroll,list,showShinyArt,shinyIds){
        snapshotFlow{scroll.firstVisibleItemIndex}.collectLatest{first->
            delay(250)
            val ahead=list.drop(first.coerceAtLeast(0)).take(36)
            withContext(Dispatchers.IO){
                ahead.forEach{loadTrimmedBitmap(context,it.id,false)}
                if(showShinyArt){val ids=shinyIds; ahead.forEach{if(ids?.contains(it.id)==true)loadTrimmedBitmap(context,it.id,true)}}
            }
        }
    }
    val configuration=androidx.compose.ui.platform.LocalConfiguration.current
    val columns=if(state.layout=="list")1 else if(configuration.screenWidthDp>=840)4 else if(configuration.screenWidthDp>=600)3 else 2
    Box(Modifier.fillMaxSize()){
    LazyVerticalGrid(columns=GridCells.Fixed(columns),state=scroll,contentPadding=PaddingValues(start=18.dp,end=18.dp,top=16.dp,bottom=24.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.prismaViewport(maxWidth=1200.dp).testTag("pokedex-grid")){
        item(span={GridItemSpan(maxLineSpan)},key="catalogControls"){
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
                if(!favoritesOnly)LText("Busque por nome ou número da Pokédex.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value=query,onValueChange={query=it},placeholder={LText("Buscar Pokémon",style=MaterialTheme.typography.bodyLarge)},leadingIcon={LIcon(Icons.Rounded.Search,null)},trailingIcon={if(query.isNotEmpty())IconButton(onClick={query=""}){LIcon(Icons.Rounded.Close,"Limpar busca")}},singleLine=true,shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth())
            }
        }
        if(!favoritesOnly&&query.isBlank())item(span={GridItemSpan(maxLineSpan)},key="hero"){FeaturedPokemon(remember(catalog){catalog.filter{it.id==it.baseId}},{onOpen(it,if(it in list)list else catalog.sortedWith(compareBy({p->p.baseId},{p->p.id})),selectedGame.takeIf{it>0}?:23)})}
        item(span={GridItemSpan(maxLineSpan)},key="catalogFilters"){
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    LText("${list.size} Pokémon e formas",Modifier.weight(1f),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    FilterChip(alphabetical,{alphabetical=!alphabetical},label={LText(if(alphabetical)"Ordem: A–Z" else "Ordem: Nº")})
                    TextButton(onClick={showFilterSheet=true},modifier=Modifier.heightIn(min=48.dp)){LText(if(activeFilterCount>0)"Filtros ($activeFilterCount)" else "Filtros");LIcon(Icons.Rounded.ExpandMore,null)}
                    IconButton(onClick={onLayout(if(state.layout=="list")"grid" else "list")}){LIcon(if(state.layout=="list")Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.List,if(state.layout=="list")"Mostrar em grade" else "Mostrar em lista")}
                }
                if(activeFilterCount>0||showShinyArt){
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                        if(showShinyArt)FilterChip(true,{showShinyArt=false},label={LText("Arte shiny")},leadingIcon={LIcon(Icons.Rounded.Star,"Arte shiny")},trailingIcon={LIcon(Icons.Rounded.Close,"Desativar arte shiny")})
                        if(selectedGame!=0)FilterChip(true,{selectedGame=0},label={LText(if(selectedGame==25)"Champions" else gameName(selectedGame))},trailingIcon={LIcon(Icons.Rounded.Close,"Remover filtro de jogo")})
                        if(generation!=0)FilterChip(true,{generation=0},label={LText("Geração $generation")},trailingIcon={LIcon(Icons.Rounded.Close,"Remover filtro de geração")})
                        if(type!=null)FilterChip(true,{type=null},label={LText(typeNames.getValue(type!!))},leadingIcon={TypeSymbol(type!!)},trailingIcon={LIcon(Icons.Rounded.Close,"Remover filtro de tipo")})
                        if(alphabetical)FilterChip(true,{alphabetical=false},label={LText("Ordem: A–Z")},trailingIcon={LIcon(Icons.Rounded.Close,"Remover ordenação alfabética")})
                        if(megaOnly)FilterChip(true,{megaOnly=false},label={LText("Mega")},trailingIcon={LIcon(Icons.Rounded.Close,"Remover filtro Mega")})
                        if(gigantamaxOnly)FilterChip(true,{gigantamaxOnly=false},label={LText("Gigantamax")},trailingIcon={LIcon(Icons.Rounded.Close,"Remover filtro Gigantamax")})
                        if(alternateFormsOnly)FilterChip(true,{alternateFormsOnly=false},label={LText("Formas alternativas")},trailingIcon={LIcon(Icons.Rounded.Close,"Remover filtro de formas alternativas")})
                        TextButton(onClick={clearAll()}){LText("Limpar tudo")}
                    }
                }

            }
        }
        if(!favoritesOnly&&selectedGame==0&&generation==0&&type==null)item(span={GridItemSpan(maxLineSpan)},key="announced"){AnnouncedPokemonSection(query)}
        items(list,key={it.id},contentType={state.layout}){p->PokemonCard(p,state.layout,p.id in state.favorites,{onOpen(p,list,selectedGame.takeIf{it>0}?:23)},{onFavorite(p)},motionPlacement(LocalReduceMotion.current),showShiny=showShinyArt,hasShiny=shinyIds?.contains(p.id)==true)}
        if(list.isEmpty())item(span={GridItemSpan(maxLineSpan)},key="empty"){Column(horizontalAlignment=Alignment.CenterHorizontally){EmptyState(if(favoritesOnly&&query.isBlank())"Seus parceiros favoritos vivem aqui." else "Nenhum Pokémon encontrado.",if(favoritesOnly&&query.isBlank())"Toque no coração na Pokédex para adicionar favoritos." else "Experimente outro nome ou filtro.");if(query.isNotBlank()||activeFilterCount>0)TextButton(onClick={clearAll()}){LText("Limpar tudo")}}}
    }
    if(list.isNotEmpty())FastScrollOverlay(scroll,list,alphabetical,columns,LocalPrismaLayout.current.bottom,!favoritesOnly&&query.isBlank(),!favoritesOnly&&selectedGame==0&&generation==0&&type==null,Modifier.align(Alignment.TopEnd))
    if(showFilterSheet){
        ModalBottomSheet(onDismissRequest={showFilterSheet=false},sheetState=filterSheetState){
            Column(Modifier.padding(start=18.dp,end=18.dp,bottom=32.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(20.dp)){
                LText("Filtros",fontSize=20.sp,fontWeight=FontWeight.SemiBold)
                LText("A arte shiny mostra a versão alternativa de cada Pokémon, quando ela existe.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(!favoritesOnly){
                    // JOGO starts COLLAPSED (11:35): only the current
                    // selection shows; tapping expands the full logo
                    // grid, tapping again collapses it. Fresh false on
                    // every sheet open (sheet content leaves composition).
                    var gameFilterExpanded by remember{mutableStateOf(false)}
                    Row(Modifier.fillMaxWidth().heightIn(min=48.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).clickable{gameFilterExpanded=!gameFilterExpanded}.padding(horizontal=14.dp,vertical=10.dp).semantics{contentDescription=if(gameFilterExpanded)"Recolher filtro de jogo" else "Expandir filtro de jogo"},verticalAlignment=Alignment.CenterVertically){
                        LText("JOGO",fontSize=13.sp,letterSpacing=1.7.sp,color=MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Box(Modifier.weight(1f),contentAlignment=Alignment.Center){
                            val selectedLogo=gameLogos[selectedGame]
                            if(selectedGame==0)LText("Nacional · todos os jogos",fontSize=14.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                            else if(selectedLogo!=null)Image(selectedLogo,gameName(selectedGame),Modifier.height(40.dp).fillMaxWidth(),contentScale=ContentScale.Fit)
                            else LText(gameName(selectedGame),fontSize=14.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                        }
                        LIcon(Icons.Rounded.ExpandMore,if(gameFilterExpanded)"Recolher jogos" else "Expandir jogos",Modifier.rotate(if(gameFilterExpanded)180f else 0f),tint=MaterialTheme.colorScheme.primary)
                    }
                    AnimatedVisibility(gameFilterExpanded,enter=expandEnter(LocalReduceMotion.current),exit=shrinkExit(LocalReduceMotion.current)){
                        GameLogoGrid(selectedGame,{selectedGame=it},gameLogos)
                    }
                }
                ChoiceField("GERAÇÃO DA ESPÉCIE",if(generation==0)"Todas as gerações" else "Geração $generation",listOf("0" to "Todas as gerações")+(1..9).map{it.toString() to "Geração $it"}){generation=it.toInt()}
                LText("TIPO",fontSize=13.sp,letterSpacing=1.7.sp,color=MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    FilterChip(type==null,{type=null},label={LText("Todos")})
                    typeNames.keys.forEach{t->FilterChip(type==t,{type=if(type==t)null else t},label={LText(typeNames.getValue(t))},leadingIcon={TypeSymbol(t)})}
                }
                LText("FORMAS",fontSize=13.sp,letterSpacing=1.7.sp,color=MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    FilterChip(megaOnly,{megaOnly=!megaOnly},label={LText("Mega")})
                    FilterChip(gigantamaxOnly,{gigantamaxOnly=!gigantamaxOnly},label={LText("Gigantamax")})
                    FilterChip(alternateFormsOnly,{alternateFormsOnly=!alternateFormsOnly},label={LText("Formas alternativas")})
                }
                LText("Combinar Mega, Gigantamax e formas alternativas mostra a união das três; sozinha, cada uma restringe a lista.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){LText("Mostrar arte shiny",fontSize=15.sp);LText("Só Pokémon com shiny verificado mudam de arte.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                    Switch(showShinyArt,{showShinyArt=it})
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    TextButton(onClick={clearAll()},modifier=Modifier.heightIn(min=48.dp).weight(1f)){LText("Limpar tudo")}
                    Button(onClick={showFilterSheet=false},modifier=Modifier.heightIn(min=48.dp).weight(1f)){LText("Ver resultados")}
                }
            }
        }
    }
    }
}

// Fast-scroll thumb + bubble, in its OWN composable (13:17 scroll fix):
// reading scroll.layoutInfo / isScrollInProgress here used to live
// inline in DexScreen, so EVERY scroll frame recomposed the whole
// Pokedex screen (search, filters, header). Scoped here, only this
// small overlay recomposes while scrolling. Behaviour unchanged:
// one fraction drives thumb, jump target and bubble label.
@Composable private fun FastScrollOverlay(scroll:LazyGridState,list:List<Pokemon>,alphabetical:Boolean,columns:Int,bottomPadding:androidx.compose.ui.unit.Dp,showHero:Boolean,showAnnounced:Boolean,modifier:Modifier){
    val fastScrollScope=rememberCoroutineScope()
    var fastScrollDragging by remember{mutableStateOf(false)}
    var fastScrollFraction by remember{mutableStateOf(0f)}
    var fastScrollThumbVisible by remember{mutableStateOf(false)}
    // Root cause of the missing indicator (11:32): the old trigger was a
    // snapshotFlow on firstVisibleItemIndex/offset with drop(1) plus a
    // 900ms hide inside collectLatest, feeding a 6dp pill at 0.38 alpha
    // whose track spanned the FULL Box height — including the 72dp top
    // bar and the bottom navigation that paint OVER it. The thumb spent
    // most of its travel behind chrome and faded before it was looked
    // at. Visibility is now driven directly by the grid's own
    // isScrollInProgress snapshot state, read in composition: it turns
    // on the moment any scroll starts and only starts the 900ms hide
    // timer once the scroll has actually settled. No snapshotFlow.
    val fastScrollScrolling=scroll.isScrollInProgress
    LaunchedEffect(fastScrollScrolling){
        if(fastScrollScrolling)fastScrollThumbVisible=true
        else{delay(900); if(!fastScrollDragging)fastScrollThumbVisible=false}
    }
    LaunchedEffect(fastScrollDragging){if(fastScrollDragging)fastScrollThumbVisible=true}
    val fastScrollThumbAlpha by animateFloatAsState(
        targetValue=when{fastScrollDragging->1f; fastScrollThumbVisible->0.55f; else->0f},
        animationSpec=tween(200),label="fast scroll thumb")
    val headerCount=1+(if(showHero)1 else 0)+1+(if(showAnnounced)1 else 0)
    var fastScrollJob by remember{mutableStateOf<Job?>(null)}
    fun jumpToFraction(fraction:Float){
        val target=fastScrollTargetIndex(fraction,list.size)
        fastScrollFraction=fraction
        fastScrollJob?.cancel()
        fastScrollJob=fastScrollScope.launch{scroll.scrollToItem(headerCount+target)}
    }
    val gridLayoutInfo=scroll.layoutInfo
    val scrollFraction=fastScrollProgress(
        gridLayoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0,
        gridLayoutInfo.totalItemsCount,
        gridLayoutInfo.visibleItemsInfo.size)
    var dragTopPx by remember{mutableStateOf<Float?>(null)}
    BoxWithConstraints(modifier.fillMaxHeight().width(128.dp).padding(top=76.dp,bottom=bottomPadding+4.dp).zIndex(1f)){
        val trackHeightPx=constraints.maxHeight.toFloat()
        val thumbHeightPx=with(LocalDensity.current){64.dp.toPx()}
        val maxOffset=(trackHeightPx-thumbHeightPx).coerceAtLeast(0f)
        val thumbTopPx=dragTopPx ?: (scrollFraction*maxOffset)
        if(fastScrollDragging){
            val target=list[fastScrollTargetIndex(fastScrollFraction,list.size)]
            val label=fastScrollBubbleLabel(target,alphabetical)
            Box(Modifier.align(Alignment.TopStart).offset{IntOffset(0,thumbTopPx.roundToInt())}.width(112.dp).height(64.dp).padding(end=8.dp),contentAlignment=Alignment.CenterEnd){
                Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.primary,shadowElevation=6.dp,modifier=Modifier.sizeIn(minWidth=56.dp,minHeight=56.dp)){
                    Box(Modifier.padding(horizontal=14.dp,vertical=10.dp),contentAlignment=Alignment.Center){LText(label,fontSize=22.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onPrimary,maxLines=1)}
                }
            }
        }
        Box(
            Modifier.align(Alignment.TopEnd).offset{IntOffset(0,thumbTopPx.roundToInt())}.size(48.dp,64.dp).alpha(fastScrollThumbAlpha)
                .pointerInput(list,columns,headerCount,maxOffset){
                    awaitEachGesture{
                        val down=awaitFirstDown()
                        fastScrollDragging=true
                        var top=fastScrollProgress(
                            scroll.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0,
                            scroll.layoutInfo.totalItemsCount,
                            scroll.layoutInfo.visibleItemsInfo.size)*maxOffset
                        dragTopPx=top
                        jumpToFraction(if(maxOffset<=0f)0f else top/maxOffset)
                        try{
                            drag(down.id){change->
                                top=(top+(change.position-change.previousPosition).y).coerceIn(0f,maxOffset)
                                dragTopPx=top
                                jumpToFraction(if(maxOffset<=0f)0f else top/maxOffset)
                                change.consume()
                            }
                        }finally{
                            fastScrollDragging=false
                            dragTopPx=null
                        }
                    }
                },
            contentAlignment=Alignment.Center
        ){
            Box(Modifier.width(6.dp).height(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
    }
}

@Composable private fun FeaturedPokemon(pokemon:List<Pokemon>,onOpen:(Pokemon)->Unit){
    if(pokemon.isEmpty())return
    val byId=remember(pokemon){pokemon.associateBy{it.id}}
    var deck by rememberSaveable{mutableStateOf(pokemon.map{it.id}.shuffled().toIntArray())}
    var index by rememberSaveable{mutableIntStateOf(0)}
    fun next(){if(index+1<deck.size)index++ else {val last=deck.last();deck=pokemon.map{it.id}.shuffled().let{if(it.size>1&&it.first()==last)it.drop(1)+it.first() else it}.toIntArray();index=0}}
    var paused by rememberSaveable{mutableStateOf(false)}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(paused,pokemon){if(!paused)lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED){while(isActive){delay(8000);next()}}}
    val p=byId.getValue(deck[index%deck.size])
    // Spotlight redesign (23:32): a true hero — taller card, stronger
    // type-colour wash, larger art, and a bottom-left "Conhecer parceiro"
    // action that opens this Pokémon's own detail (the app has no separate
    // partner profile; the detail is the honest destination).
    BoxWithConstraints(Modifier.fillMaxWidth().heightIn(min=280.dp).clip(RoundedCornerShape(30.dp)).background(Brush.linearGradient(listOf(solidBlend(typeColor(p.types.first()),MaterialTheme.colorScheme.surface,.22f),MaterialTheme.colorScheme.surface)))){
        val stacked=LocalDensity.current.fontScale>1.25f&&maxWidth<400.dp
        val artWidth=(maxWidth*.44f).coerceIn(128.dp,200.dp)
        AnimatedContent(p,transitionSpec={(fadeIn(tween(500))+slideInHorizontally(tween(500)){it/8}) togetherWith fadeOut(tween(300))},label="rotating featured Pokémon",modifier=Modifier.fillMaxWidth()){featured->
            if(stacked){
                Column(Modifier.fillMaxWidth().clickable{onOpen(featured)}.padding(start=20.dp,end=20.dp,top=20.dp,bottom=56.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                    FeaturedIdentity(featured,Modifier.fillMaxWidth())
                    PokemonArt(featured.id,Modifier.align(Alignment.CenterHorizontally).size(200.dp),featured.name)
                }
            }else Row(Modifier.fillMaxWidth().clickable{onOpen(featured)}.padding(start=20.dp,end=12.dp,top=12.dp,bottom=56.dp),verticalAlignment=Alignment.CenterVertically){
                FeaturedIdentity(featured,Modifier.weight(1f))
                PokemonArt(featured.id,Modifier.width(artWidth).height(220.dp),featured.name)
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically){
            TextButton(onClick={onOpen(p)},modifier=Modifier.heightIn(min=48.dp)){LText("Conhecer parceiro",fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary)}
            Spacer(Modifier.weight(1f))
            IconButton(onClick={paused=!paused},modifier=Modifier.size(48.dp)){LIcon(if(paused)Icons.Rounded.PlayArrow else Icons.Rounded.Pause,if(paused)"Retomar destaque" else "Pausar destaque",Modifier.size(22.dp))}
            IconButton(onClick={next()},modifier=Modifier.size(48.dp)){LIcon(Icons.AutoMirrored.Rounded.ArrowForward,"Próximo destaque",Modifier.size(22.dp))}
        }
    }

}


@Composable private fun FeaturedIdentity(p:Pokemon,modifier:Modifier){
    Column(modifier,verticalArrangement=Arrangement.spacedBy(8.dp)){
        LText("Destaque · #${p.baseId.toString().padStart(4,'0')}",fontSize=13.sp,letterSpacing=1.2.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary)
        LText(p.name,fontSize=28.sp,lineHeight=32.sp,fontWeight=FontWeight.SemiBold)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){p.types.forEach{TypePill(it,true)}}
    }
}

@Composable private fun PokemonCard(p:Pokemon,layout:String,favorite:Boolean,onOpen:()->Unit,onFavorite:()->Unit,modifier:Modifier=Modifier,showShiny:Boolean=false,hasShiny:Boolean=false){
    val interaction=remember{MutableInteractionSource()};val pressed by interaction.collectIsPressedAsState();val scale by animateFloatAsState(pressTarget(pressed,LocalReduceMotion.current,.975f),spring(.8f,600f),label="card touch")
    val shape=RoundedCornerShape(22.dp)
    val ink=Color(0xFF1B1B26)
    if(layout=="list"){
        // List (08:34, supersedes the 08:28 containment): art FULLY
        // INSIDE the card, sized from its OWN trimmed content box
        // (PokemonArtTrimmed + art-trim.json) so each artwork fills
        // the 148x128dp art area as far as its own shape allows -
        // no overflow, no clipping, no distortion. One clipped card
        // box contains everything: texture, text on the left, heart
        // at the card's top-right corner (inside the card, never in
        // the middle between text and art).
        Box(modifier.fillMaxWidth().height(128.dp).then(if(scale==1f)Modifier else Modifier.graphicsLayer{scaleX=scale;scaleY=scale}).clip(RoundedCornerShape(18.dp)).background(typeColor(p.types.first())).clickable(interaction,null,onClick=onOpen)){
            CardTexture(typeColor(p.types.first()),Modifier.fillMaxSize())
            Box(Modifier.fillMaxWidth().padding(start=18.dp,end=140.dp,top=14.dp,bottom=14.dp),contentAlignment=Alignment.CenterStart){
                Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                    LText("#${p.baseId.toString().padStart(4,'0')}",fontSize=13.sp,color=solidBlend(ink,typeColor(p.types.first()),.72f))
                    LText(p.name,fontSize=20.sp,lineHeight=24.sp,fontWeight=FontWeight.Bold,color=ink,maxLines=1,overflow=TextOverflow.Ellipsis)
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){p.types.forEach{TypeChipOnColor(it)}}
                }
            }
            Box(Modifier.size(148.dp,128.dp).align(Alignment.CenterEnd).clipToBounds()){if(showShiny&&hasShiny)PokemonArtTrimmedShiny(p.id,Modifier.fillMaxSize(),p.name) else PokemonArtTrimmed(p.id,Modifier.fillMaxSize(),p.name)}
            IconButton(onClick=onFavorite,modifier=Modifier.align(Alignment.TopEnd).padding(top=4.dp,end=2.dp).size(48.dp)){LIcon(if(favorite)Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,"Favoritar ${p.name}",tint=ink,modifier=Modifier.size(20.dp))}
        }
    }else{
        // Grid redesign (23:31, art sizing 30/10): same clean language as the
        // list card — solid type-colour fill, dark ink, number + name + type
        // chips + art. Fixed 120dp art box, Fit, centered (no distortion,
        // no crop): every card uses the same box, so art scale is uniform
        // and only the species own shape varies. Name up to 2 lines, and
        // a chip area at least 64dp tall, vertically centered: two wrapped
        // chip rows need 64dp (30dp per row: 18dp icon + 12dp padding, +4dp
        // spacing). A fixed 60dp box made FlowRow drop the overflowing
        // second row, hiding the second type of long-name pairs
        // (e.g. Pedra+Terrestre). heightIn(min=...) lets the row grow when
        // needed; the grid row height follows the tallest card, so chips
        // stay aligned and no type is ever dropped. No field is dropped
        // vs the list.
        Box(modifier.fillMaxWidth().then(if(scale==1f)Modifier else Modifier.graphicsLayer{scaleX=scale;scaleY=scale}).clip(shape).background(typeColor(p.types.first())).clickable(interaction,null,onClick=onOpen)){
            // Texture (08:04): faint circle behind the art only; no dots
            // here (the number row occupies the top-left).
            CardTexture(typeColor(p.types.first()),Modifier.fillMaxSize(),dots=false,circleX=.5f,circleY=.35f,radiusFrac=.42f)
            Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Row(Modifier.padding(end=40.dp).heightIn(min=24.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    LText("#${p.baseId.toString().padStart(4,'0')}",fontSize=13.sp,lineHeight=18.sp,color=solidBlend(ink,typeColor(p.types.first()),.72f))
                }
                Box(Modifier.align(Alignment.CenterHorizontally).size(120.dp),contentAlignment=Alignment.Center){if(showShiny&&hasShiny)PokemonArtTrimmedShiny(p.id,Modifier.size(120.dp),p.name) else PokemonArt(p.id,Modifier.size(120.dp),p.name)}
                LText(p.name,fontSize=15.sp,lineHeight=20.sp,fontWeight=FontWeight.SemiBold,color=ink,minLines=2,maxLines=2,overflow=TextOverflow.Ellipsis)
                Box(Modifier.fillMaxWidth().heightIn(min=64.dp),contentAlignment=Alignment.CenterStart){
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){p.types.forEach{TypeChipOnColor(it)}}
                }
            }
            IconButton(onClick=onFavorite,modifier=Modifier.align(Alignment.TopEnd).size(48.dp)){LIcon(if(favorite)Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,"Favoritar ${p.name}",tint=ink,modifier=Modifier.size(19.dp))}
        }
    }
}

// Watermark species name (08:10): the BASE species name, never the form
// prefix. Audited 30/10 against data.json (1,306 records, 281 forms, all
// baseIds resolve): the base record's name, parentheticals stripped
// ("Meowstic (Male)" -> "Meowstic"), the last-word rule for variant
// colour/style bases ("Green Squawkabilly"/"Blue Squawkabilly" ->
// "Squawkabilly"; "Curly Tatsugiri" -> "Tatsugiri"), and a mismatch
// fallback to the Pokemon's own stripped name (covers the one data
// quirk found: "Maushold (Family of Three)" has baseId pointing at
// Oinkologne, and the Farfetch'd straight/curly apostrophe variant).
// The readable name/number text elsewhere keeps p.name unchanged.
private fun watermarkName(p:Pokemon,catalog:List<Pokemon>):String{
    val stripParen:(String)->String={it.substringBefore(" (").trim()}
    val raw=stripParen(catalog.firstOrNull{it.id==p.baseId}?.name?:p.name)
    val own=stripParen(p.name)
    val lastRaw=raw.substringAfterLast(' ');val lastOwn=own.substringAfterLast(' ')
    val species=when{
        raw.contains(own,ignoreCase=true)||own.contains(raw,ignoreCase=true)->raw
        raw.contains(' ')&&lastRaw.equals(lastOwn,ignoreCase=true)->lastRaw
        else->own.substringAfterLast(' ')
    }
    return species.uppercase()
}

// Texture drawn with the app's own Canvas primitives (08:04): the same
// geometry as the reference list cards - a large faint circle with a
// horizontal band and centre dot behind the art, plus a small dot grid
// near the top - never a bitmap copied from any reference image. The
// circle uses white at low alpha over the card's own type colour; the
// band/centre use the card colour itself. Faint by design so text,
// chips and art stay fully legible in light and dark.
@Composable private fun CardTexture(cardColor:Color,modifier:Modifier,dots:Boolean=true,circleX:Float=.80f,circleY:Float=.52f,radiusFrac:Float=.62f){
    Box(modifier.drawWithCache{
        val radius=size.height*radiusFrac
        val center=Offset(size.width*circleX,size.height*circleY)
        val bandPath=Path().apply{addOval(Rect(center-Offset(radius,radius),center+Offset(radius,radius)))}
        val dotSpacing=16.dp.toPx()
        val dotSoft=solidBlend(Color.White,cardColor,.13f)
        val dotStrong=solidBlend(Color.White,cardColor,.32f)
        onDrawBehind{
            drawCircle(dotSoft,radius,center)
            clipPath(bandPath){drawRect(cardColor,Offset(center.x-radius,center.y-radius*.14f),Size(radius*2f,radius*.28f))}
            drawCircle(cardColor,radius*.22f,center)
            drawCircle(dotSoft,radius*.22f,center,style=Stroke(width=2.dp.toPx()))
            if(dots){
                val startX=size.width*.30f
                for(row in 0..2)for(col in 0..4)drawCircle(dotStrong,2.5.dp.toPx(),Offset(startX+col*dotSpacing,10.dp.toPx()+row*14.dp.toPx()))
            }
        }
    })
}

// Expandable section header (08:05/08:06): 48dp target, chevron shows
// state; every section using it starts COLLAPSED on a fresh detail open.
@Composable private fun ExpandableSectionHeader(title:String,subtitle:String?,expanded:Boolean,onToggle:()->Unit){
    Surface(onClick=onToggle,shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth()){
        Row(Modifier.fillMaxWidth().heightIn(min=48.dp).padding(horizontal=14.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){
                LText(title,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
                subtitle?.let{LText(it,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            }
            LIcon(Icons.Rounded.ExpandMore,if(expanded)"Recolher $title" else "Expandir $title",Modifier.rotate(if(expanded)180f else 0f),tint=MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable private fun PokemonScreen(p:Pokemon,repo:PokemonRepository,state:UserState,tab:String,onTab:(String)->Unit,onFavorite:()->Unit,onTeam:(Boolean)->Unit,onBuild:(Build)->Unit,draftKey:String,onDraft:(Build?)->Unit,footerVisible:Boolean=false,catalog:List<Pokemon>,previousId:Int?,nextId:Int?,canBrowse:Boolean,onBrowse:(Int)->Unit,onRelated:(Pokemon)->Unit,referenceGame:Int,onReferenceGame:(Int)->Unit,onArtViewer:(Pokemon)->Unit={},shinyArt:Boolean=false,onShinyArt:(Boolean)->Unit={},onBack:()->Unit={},onOpenSet:(Pokemon)->Unit={}){
    val viewingGame=referenceGame
    var moves by remember(p.id){mutableStateOf<List<Move>>(emptyList())}
    val learnContext=LocalContext.current
    val learnRepository=remember{LearnsetRepository(learnContext)}
    val speciesRepository=remember{SpeciesProfileRepository(learnContext)}
    val speciesProfile by produceState<SpeciesProfile?>(null,p.id){value=speciesRepository.profile(p)}
    var learnMethods by remember(p.id){mutableStateOf<Map<Int,List<LearnMethod>>>(emptyMap())}
    var heldItems by remember(p.id){mutableStateOf<List<HeldItem>>(emptyList())}
    var loaded by remember(p.id){mutableStateOf(false)}
    val shinyExists=rememberArtExists(shinyArtPath(p.id))
    var query by rememberSaveable(p.id){mutableStateOf("")}
    var weaknessExpanded by remember(p.id){mutableStateOf(false)}
    var buildExpanded by remember(p.id){mutableStateOf(false)}
    var movesExpanded by remember(p.id){mutableStateOf(false)}
    LaunchedEffect(p.id,viewingGame){loaded=false;moves=repo.movesFor(p,viewingGame);heldItems=repo.heldItemsFor(p,state.activeTeam.gameId);learnMethods=learnRepository.methodsFor(p.id,viewingGame);loaded=true}
    val detailScroll=rememberLazyListState()
    LazyColumn(Modifier.prismaViewport(top=0.dp,bottom=if(footerVisible)88.dp else 16.dp).imePadding().pokemonSwipe(p.id,canBrowse,previousId,nextId,onBrowse),state=detailScroll,contentPadding=PaddingValues(top=8.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        item(key="detail-header"){
            // Squirtle-reference composition (07:43, primary model for the
            // detail, in our theme), revised 07:57 from the annotated
            // screenshot: Pokemon CENTRAL, the enlarged watermark (base
            // species name only, shifted right, fragment cropped at the
            // card edge, faint, behind the legible text only), exit is a
            // small UP ARROW next to the
            // #number, TYPE CHIPS under the name inside the card,
            // horizontal as originally (07:59 correction), and the
            // Pokemon STEPPING
            // ON the lower colour — most of the art on the type-colour
            // card, its feet/bottom crossing 36dp below the card onto the
            // background colour. Top-right column: shiny star, favorite
            // heart, team + / -. No browse buttons and no circular back
            // button. The card keeps the existing type-colour token and
            // dark ink; no light theme, colour, font or asset from the
            // reference.
            val ink=Color(0xFF1B1B26)
            Column(Modifier.padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Box(Modifier.fillMaxWidth().height(288.dp)){
                    Box(Modifier.fillMaxWidth().height(252.dp).align(Alignment.TopCenter).clip(RoundedCornerShape(24.dp)).background(typeColor(p.types.first()),RoundedCornerShape(24.dp))){
                        // Watermark name at the TOP of the header (07:50):
                        // faint background only (8% opacity), drawn before
                        // the legible column so number/name/star stay fully
                        // legible on top of it; clipped to one line.
                        // Watermark (08:10): base species name only (see
                        // watermarkName), enlarged + heavier, shifted right
                        // so only a fragment shows, cropped at the card
                        // edge by the clip on the card Box. Faint (8%) so
                        // legible text stays fully readable.
                        BoxWithConstraints(Modifier.fillMaxSize()){
                            LText(watermarkName(p,catalog),Modifier.offset(x=maxWidth*.52f,y=2.dp),fontSize=84.sp,lineHeight=92.sp,fontWeight=FontWeight.Black,letterSpacing=(-1).sp,color=solidBlend(ink,typeColor(p.types.first()),.08f),maxLines=1,overflow=TextOverflow.Clip)
                        }
                        Column(Modifier.fillMaxWidth().padding(start=20.dp,end=20.dp,top=16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                            // Exit (07:57): SMALL up arrow at the top-left,
                            // next to the #number, replacing the old circular
                            // back button. Small icon, full 48dp target.
                            Row(Modifier.padding(end=52.dp),verticalAlignment=Alignment.CenterVertically){
                                IconButton(onClick=onBack,modifier=Modifier.size(48.dp).offset(x=(-12).dp)){LIcon(Icons.Rounded.KeyboardArrowUp,"Voltar para a lista",tint=ink,modifier=Modifier.size(20.dp))}
                                LText("#${p.baseId.toString().padStart(4,'0')}  /  ${LocalUiStrings.current.text(if(p.isMega)"MEGA EVOLUÇÃO" else "POKÉDEX")}",fontSize=13.sp,letterSpacing=1.8.sp,color=solidBlend(ink,typeColor(p.types.first()),.72f),fontWeight=FontWeight.SemiBold)
                            }
                            LText(p.name,fontSize=30.sp,fontWeight=FontWeight.Bold,lineHeight=34.sp,letterSpacing=(-1).sp,color=ink,maxLines=2,overflow=TextOverflow.Ellipsis)
                            // Types OUT of the card (08:20 clarification, supersedes the 07:59
                            // keep-where-they-were correction): no type chip inside the header
                            // card. They render below the card, on the background (see below).
                        }
                        // Top-right column (07:52 + 07:57): shiny star
                        // on top, favorite heart DIRECTLY BELOW it, team
                        // + / - below the heart, same right-aligned
                        // column inside the header. No heart is centered
                        // below the art.
                        Column(Modifier.align(Alignment.TopEnd).padding(4.dp),horizontalAlignment=Alignment.CenterHorizontally){
                            IconButton(onClick={onShinyArt(!shinyArt)},modifier=Modifier.size(48.dp)){ShinySparkleIcon(shinyArt,if(shinyArt)"Ver arte normal de ${p.name}" else "Ver arte shiny de ${p.name}")}
                            IconButton(onClick=onFavorite,modifier=Modifier.size(48.dp)){LIcon(if(p.id in state.favorites)Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,"Favoritar ${p.name}",tint=if(p.id in state.favorites)MaterialTheme.colorScheme.primary else solidBlend(ink,typeColor(p.types.first()),.45f),modifier=Modifier.size(24.dp))}
                            // Team button (07:57): "+" to add to the team,
                            // "-" if already in the team; same onTeam handler
                            // and availability rule as the team action in
                            // the Sobre tab, so state stays identical.
                            IconButton(onClick={onTeam(shinyArt&&shinyExists==true)},enabled=p.availableIn(state.activeTeam.gameId)||p.id in state.team,modifier=Modifier.size(48.dp)){LIcon(if(p.id in state.team)Icons.Rounded.Remove else Icons.Rounded.Add,if(p.id in state.team)"Remover da equipe" else "Adicionar à equipe",tint=solidBlend(ink,typeColor(p.types.first()),.72f),modifier=Modifier.size(24.dp))}
                        }
                    }
                    // Central art, bottom-aligned: its lower 36dp sit on
                    // the background colour below the card. Tapping it
                    // still opens the full-screen art viewer (detail only).
                    Box(Modifier.align(Alignment.BottomCenter).size(188.dp).clickable{onArtViewer(p)},contentAlignment=Alignment.Center){DetailArt(p.id,shinyArt,Modifier.size(188.dp),p.name)}
                    // Shiny-unavailable text INSIDE the coloured panel, bottom-center (30/10):
                    // drawn after the art so it stays legible over the art's lower edge.
                    if(shinyArt&&shinyExists==false)Box(Modifier.align(Alignment.BottomCenter).offset(y=(-40).dp),contentAlignment=Alignment.Center){Surface(shape=CircleShape,color=ink){LText("Arte shiny não disponível",Modifier.padding(horizontal=10.dp,vertical=5.dp),fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=Color.White)}}
                }
                // Type chips BELOW the card, on the background (08:20): TypePill (onSurface
                // text on a type-colour pill) is the legible chip on the dark/light background;
                // TypeChipOnColor uses dark ink made for the pastel card and would be unreadable here.
                FlowRow(Modifier.padding(horizontal=6.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){p.types.forEach{TypePill(it,true)}}
                LText(when(p.championsSource){"official-regulation"->"Espécie/forma na lista oficial M-C · verificada em 28/10/2026.";"showdown-form"->"Forma de batalha ou variante · referência Champions do Showdown.";else->"Não disponível no Pokémon Champions."},Modifier.padding(horizontal=6.dp),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // Tab mapping (30/10, mockup-style tabs, our theme): Sobre =
        // height/weight + biology/national reference + general abilities +
        // Champions note + team action (old "Informações" minus stats,
        // weaknesses and evolution). Status (default) = base stats +
        // weaknesses + the full Golpes list + Seu set editor. Evolução =
        // the evolution line only. Nothing from the old three tabs is
        // dropped; Golpes and Seu set now live inside Status.
        item(key="detail-tabs"){Column(Modifier.padding(horizontal=24.dp)){
            Row(Modifier.fillMaxWidth()){
                listOf("about" to "Sobre","status" to "Status","evolution" to "Evolução").forEach{(key,label)->Column(Modifier.weight(1f).clickable{onTab(key)}.heightIn(min=48.dp).padding(vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)){
                    LText(label,fontSize=14.sp,color=if(key==tab)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,fontWeight=if(key==tab)FontWeight.SemiBold else FontWeight.Normal)
                    Box(Modifier.width(32.dp).height(3.dp).clip(CircleShape).background(if(key==tab)MaterialTheme.colorScheme.primary else Color.Transparent))
                }}
            }
            HorizontalDivider(color=MaterialTheme.colorScheme.surfaceVariant)
        }}
        if(tab=="about"){
            item{Column(Modifier.padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)){InfoTile("ALTURA",(p.height?.takeIf{it>0}?:speciesProfile?.heightMeters)?.let{"$it m"}?:"Não informado",Modifier.weight(1f));InfoTile("PESO",(p.weight?.takeIf{it>0}?:speciesProfile?.weightKg)?.let{"$it kg"}?:"Não informado",Modifier.weight(1f))}
                SpeciesProfileSection(p,catalog,onRelated)
                if(speciesProfile?.abilities.isNullOrEmpty()){SectionLabel("HABILIDADES GERAIS");p.abilities.forEach{AbilityNameButton(it,catalog,onRelated)}}
                LText("A lista oficial identifica espécies/formas elegíveis no regulamento. Megas e transformações usam a referência de batalha; não confunda ausência da página com ausência no jogo.",fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                ActionButton(if(p.id in state.team)"Remover da equipe" else "Adicionar à equipe",if(p.id in state.team)Icons.Rounded.Remove else Icons.Rounded.Add,p.availableIn(state.activeTeam.gameId)||p.id in state.team,{onTeam(shinyArt&&shinyExists==true)})
            }}
        }else if(tab=="evolution"){
            item{Column(Modifier.padding(horizontal=24.dp)){EvolutionSection(p,catalog,canBrowse,onRelated)}}
        }else{
            item{Column(Modifier.padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
                SectionLabel("ATRIBUTOS BASE")
                p.stats.forEachIndexed{i,v->Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){LText(listOf("HP","Ataque","Defesa","Atq. especial","Def. especial","Velocidade")[i],Modifier.weight(1f),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);LText("$v",fontSize=14.sp,fontWeight=FontWeight.SemiBold)}
                    LinearProgressIndicator(progress={v/255f},modifier=Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),color=typeColor(p.types.first()),trackColor=MaterialTheme.colorScheme.surfaceVariant)
                }}
            }}
            // Expandable sections (08:05/08:06): EVERYTHING long starts
            // COLLAPSED on a fresh detail open; only the header card +
            // base stats show without expanding. Nothing removed.
            item{Column(Modifier.padding(horizontal=24.dp)){ExpandableSectionHeader("FRAQUEZAS E RESISTÊNCIAS",null,weaknessExpanded){weaknessExpanded=!weaknessExpanded}}}
            if(weaknessExpanded)item{Column(Modifier.padding(horizontal=24.dp)){TypeAnalysisPanel(listOf(p),team=false)}}
            item{Column(Modifier.padding(horizontal=24.dp)){ExpandableSectionHeader("GOLPES","${moves.size} golpes · ${gameName(viewingGame)}",movesExpanded){movesExpanded=!movesExpanded}}}
            if(movesExpanded){
                item{Column(Modifier.padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){ChoiceField("JOGO DOS GOLPES",gameName(viewingGame),SupportedGames.map{it.id.toString() to it.name}){onReferenceGame(it.toInt())};OutlinedTextField(query,{query=it},leadingIcon={LIcon(Icons.Rounded.Search,null)},placeholder={LText("Buscar golpe")},singleLine=true,shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth());if(!loaded)LinearProgressIndicator(Modifier.fillMaxWidth())}}
                items(moves.filter{it.name.contains(query,true)},key={it.id}){move->Column(Modifier.padding(horizontal=24.dp)){MoveRow(move);LearnMethodLabel(learnMethods[move.id].orEmpty().ifEmpty{listOf(LearnMethod("unknown"))})}}
                if(loaded&&moves.isEmpty())item{EmptyState("Sem golpes registrados para este jogo.","Este Pokémon ainda não tem compatibilidade confirmada nesta base.")}
                item{LText(if(viewingGame==25)"Atributos e aprendizados de Champions: fonte Showdown, consulta de 28/10/2026. PP básico; não é a tabela de outros jogos." else "Poder, precisão e PP da referência geral; atributos escalares e categoria adaptados ao jogo. Efeitos textuais, alvo e flags são referência geral e ainda não certificados por versão.",Modifier.padding(horizontal=24.dp),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            }
            item{Column(Modifier.padding(horizontal=24.dp)){ExpandableSectionHeader("SEU SET",null,buildExpanded){buildExpanded=!buildExpanded}}}
            if(buildExpanded)item{BuildEditor(p,state.builds[p.id]?:Build(),p.id in state.team,moves,heldItems,{onTeam(shinyArt&&shinyExists==true)},onBuild,game=state.activeTeam.gameId,draftKey=draftKey,onDraft=onDraft)}
        }
    }
}

@Composable internal fun BuildEditor(p:Pokemon,existing:Build,inTeam:Boolean,moves:List<Move>,items:List<HeldItem>,onTeam:()->Unit,onSave:(Build)->Unit,game:Int=25,draftKey:String?=null,onDraft:(Build?)->Unit={}){
    val draftContext=LocalContext.current
    val draftStore=remember{BuildDraftStore(draftContext)}
    val initial=remember(p.id,existing,draftKey){draftKey?.let{draftStore.load(it,existing)}?:existing}
    var selectedMoves by remember(p.id,existing){mutableStateOf(initial.moves)}
    var selectedItem by remember(p.id,existing){mutableStateOf(initial.itemId)}
    LaunchedEffect(p.id,items){if(game==25&&p.isMega&&selectedItem==null&&existing.itemId==null)selectedItem=items.singleOrNull()?.id}
    var nature by remember(p.id,existing){mutableStateOf(initial.nature)}
    var ability by remember(p.id,existing){mutableStateOf(initial.ability)}
    var sps by remember(p.id,existing){mutableStateOf(initial.sps)}
    var gender by remember(p.id,existing){mutableStateOf(initial.gender)}
    val genderRepo=remember{SpeciesProfileRepository(draftContext)}
    val genderProfile by produceState<SpeciesProfile?>(null,p.id){value=genderRepo.profile(p)}
    var inspectedMove by remember(p.id){mutableStateOf<Move?>(null)}
    inspectedMove?.let{MoveInfoSheet(it,onDismiss={inspectedMove=null})}
    var searchMove by rememberSaveable(p.id){mutableStateOf("")}
    var searchItem by rememberSaveable(p.id){mutableStateOf("")}
    var picking by rememberSaveable(p.id){mutableStateOf("")}
    val editing=initial.copy(itemId=selectedItem,moves=selectedMoves,nature=nature,ability=ability,sps=sps,gender=gender)
    val changed=editing!=existing
    LaunchedEffect(editing,existing,draftKey){
        draftKey?.let{draftStore.save(it,existing,editing)}
        onDraft(editing.takeIf{changed})
    }
    Column(Modifier.fillMaxWidth().padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){LText("Seu set",fontSize=20.sp,fontWeight=FontWeight.SemiBold);LText(if(changed)"Rascunho no aparelho · não aplicado ao cálculo" else "Sem alterações pendentes",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            FilledTonalButton(onClick={onSave(editing)},enabled=inTeam&&sps.sum()<=66&&changed){LText("Salvar")}
        }
        if(!inTeam){LText("Adicione este parceiro à sua equipe para montar o set.",fontSize=14.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);ActionButton("Adicionar à equipe",Icons.Rounded.Add,p.availableIn(game),onTeam)}
        if(game==25)TrainingEditor(p,nature,ability,sps,{nature=it},{ability=it},{sps=it}) else LText("Equipe de planejamento: treinamento e itens específicos deste jogo ainda não estão incluídos. Os golpes abaixo são filtrados pelo jogo.",fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        val genderOptions=listOf("" to "Não definido")+when {
            genderProfile?.formGender=="female"||genderProfile?.genderRateEighths==8->listOf("female" to "Feminino")
            genderProfile?.formGender=="male"||genderProfile?.genderRateEighths==0->listOf("male" to "Masculino")
            genderProfile?.genderRateEighths== -1->emptyList()
            else->listOf("male" to "Masculino","female" to "Feminino")
        }
        ChoiceField("GÊNERO DO PARCEIRO",genderOptions.firstOrNull{it.first==gender}?.second?:"Não definido",genderOptions){gender=it.takeIf(String::isNotBlank)}
        AbilityEffect(ability)
        LText(if(game==25)"Item segurado" else "Itens · em preparação",fontSize=17.sp,fontWeight=FontWeight.SemiBold)
        Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface,onClick={if(game==25)picking=if(picking=="items")"" else "items"}){
            Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically){ItemArt(selectedItem,items.find{it.id==selectedItem}?.name,Modifier.size(40.dp));Column(Modifier.weight(1f).padding(horizontal=14.dp)){LText(items.find{it.id==selectedItem}?.name?:if(selectedItem!=null)"Item indisponível · selecione outro" else if(p.isMega&&game==25)"Escolha a Mega Stone" else "Sem item",fontSize=14.sp);LText("Toque para escolher",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};LIcon(Icons.Rounded.ExpandMore,"Escolher item")}
        }
        items.find{it.id==selectedItem}?.let{item->Text(descriptionText(item.description),fontSize=14.sp,lineHeight=21.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        AnimatedVisibility(picking=="items",enter=expandEnter(LocalReduceMotion.current),exit=shrinkExit(LocalReduceMotion.current)){
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedTextField(searchItem,{searchItem=it},singleLine=true,placeholder={LText("Buscar item segurado")},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp))
                if(!p.isMega||game!=25)TextButton(onClick={selectedItem=null;picking=""}){LText("Usar sem item")}
                LazyColumn(Modifier.heightIn(max=300.dp)){items(items.filter{it.name.contains(searchItem,true)},key={it.id}){item->Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable{selectedItem=item.id;picking=""}.padding(12.dp),verticalAlignment=Alignment.CenterVertically){RadioButton(selectedItem==item.id,null);ItemArt(item.id,item.name,Modifier.size(32.dp));Column(Modifier.weight(1f).padding(start=10.dp)){LText(item.name,fontSize=14.sp);Text(descriptionText(item.description),fontSize=13.sp,lineHeight=16.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=3,overflow=TextOverflow.Ellipsis)}}}}
            }
        }
        LText("Golpes  ${selectedMoves.size}/4",fontSize=17.sp,fontWeight=FontWeight.SemiBold)
        selectedMoves.forEach{id->val move=moves.find{it.id==id};Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface,RoundedCornerShape(18.dp)).padding(start=14.dp)){
            if(move!=null)TypeSymbol(move.type)
            LText(move?.name?:"Golpe indisponível · #$id",Modifier.weight(1f).padding(12.dp),fontSize=14.sp,color=if(move==null)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            if(move!=null)IconButton(onClick={inspectedMove=move}){LIcon(Icons.Rounded.Info,"Informações de ${move.name}",Modifier.size(20.dp))}
            IconButton(onClick={selectedMoves=selectedMoves-id}){LIcon(Icons.Rounded.Close,"Remover ${move?.name?:"golpe antigo"}",Modifier.size(20.dp))}
        }}
        OutlinedButton(onClick={picking=if(picking=="moves")"" else "moves"},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){LIcon(Icons.Rounded.Add,null,Modifier.size(18.dp));LText("Escolher golpes",Modifier.padding(8.dp))}
        AnimatedVisibility(picking=="moves",enter=expandEnter(LocalReduceMotion.current),exit=shrinkExit(LocalReduceMotion.current)){
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedTextField(searchMove,{searchMove=it},singleLine=true,placeholder={LText("Buscar golpe do jogo")},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp))
                LazyColumn(Modifier.heightIn(max=320.dp)){items(moves.filter{it.name.contains(searchMove,true)},key={it.id}){move->Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(enabled=move.id in selectedMoves||selectedMoves.size<4){selectedMoves=if(move.id in selectedMoves)selectedMoves-move.id else selectedMoves+move.id}.padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){Checkbox(move.id in selectedMoves,null,enabled=move.id in selectedMoves||selectedMoves.size<4);TypeSymbol(move.type);Column(Modifier.weight(1f).padding(start=9.dp)){LText(move.name,fontSize=14.sp);LText("${move.power?.takeIf{it>0}?:"—"} · ${move.accuracy?.let{"$it%"}?:"—"} · ${move.pp?:"—"} PP",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton(onClick={inspectedMove=move}){LIcon(Icons.Rounded.Info,"Informações de ${move.name}",Modifier.size(20.dp))}}}}
            }
        }
        LText("Selecione até quatro golpes registrados para ${gameName(game)}. Confira o regulamento antes de competir.",fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        ActionButton("Salvar set",Icons.Rounded.Check,inTeam&&sps.sum()<=66){onSave(editing)}
    }
}

// Dedicated set screen (team grid redesign, 30/10): the ONLY place where a
// team member's set is shown or edited from the team views — held item,
// moves, nature, ability and training, via the same BuildEditor (same
// fields, same validation, same on-device draft) used by the detail and
// dummy screens. Team grid/list cards show no set details; tapping one
// opens this screen. "Ver ficha completa" returns to the detail.
@Composable private fun TeamSetScreen(p:Pokemon,repo:PokemonRepository,state:UserState,onBack:()->Unit,onTeam:()->Unit,onBuild:(Build)->Unit,draftKey:String,onDraft:(Build?)->Unit,onOpenDetail:()->Unit,onArtViewer:(Pokemon)->Unit={}){
    var moves by remember(p.id){mutableStateOf<List<Move>>(emptyList())}
    var heldItems by remember(p.id){mutableStateOf<List<HeldItem>>(emptyList())}
    LaunchedEffect(p.id,state.activeTeam.gameId){moves=repo.movesFor(p,state.activeTeam.gameId);heldItems=repo.heldItemsFor(p,state.activeTeam.gameId)}
    val ink=Color(0xFF1B1B26)
    LazyColumn(Modifier.prismaViewport(top=0.dp,bottom=16.dp).imePadding(),contentPadding=PaddingValues(start=18.dp,end=18.dp,top=8.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        item(key="set-header"){
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    IconButton(onClick=onBack,modifier=Modifier.size(48.dp).offset(x=(-12).dp)){LIcon(Icons.Rounded.KeyboardArrowUp,"Voltar para a equipe",tint=MaterialTheme.colorScheme.onBackground,modifier=Modifier.size(20.dp))}
                    LText("SET · ${state.activeTeam.name} · ${gameName(state.activeTeam.gameId)}",fontSize=13.sp,letterSpacing=1.8.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,fontWeight=FontWeight.SemiBold)
                }
                Surface(shape=RoundedCornerShape(24.dp),color=typeColor(p.types.first())){Row(Modifier.fillMaxWidth().padding(20.dp),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
                        LText("#${p.baseId.toString().padStart(4,'0')}",fontSize=13.sp,letterSpacing=1.8.sp,color=solidBlend(ink,typeColor(p.types.first()),.72f),fontWeight=FontWeight.SemiBold)
                        LText(p.name,fontSize=28.sp,fontWeight=FontWeight.Bold,lineHeight=32.sp,color=ink,maxLines=2,overflow=TextOverflow.Ellipsis)
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){p.types.forEach{TypeChipOnColor(it)}}
                        LText(if(state.builds[p.id]==null)"Sem set salvo" else "Set salvo neste aparelho",fontSize=13.sp,color=solidBlend(ink,typeColor(p.types.first()),.78f))
                    }
                    Box(Modifier.size(132.dp).clickable{onArtViewer(p)},contentAlignment=Alignment.Center){if(state.builds[p.id]?.shiny==true)PokemonArtTrimmedShiny(p.id,Modifier.size(132.dp),p.name) else PokemonArtTrimmed(p.id,Modifier.size(132.dp),p.name)}
                }}
                TextButton(onClick=onOpenDetail,modifier=Modifier.heightIn(min=48.dp)){LText("Ver ficha completa",fontSize=14.sp,fontWeight=FontWeight.SemiBold);LIcon(Icons.AutoMirrored.Rounded.ArrowForward,"Ver ficha completa",Modifier.size(20.dp))}
            }
        }
        item(key="set-editor"){BuildEditor(p,state.builds[p.id]?:Build(),p.id in state.team,moves,heldItems,onTeam,onBuild,game=state.activeTeam.gameId,draftKey=draftKey,onDraft=onDraft)}
        item(key="set-note"){LText("O set é validado ao salvar, como antes. Confira o regulamento do jogo antes de competir.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
}

@Composable private fun TeamScreen(catalog:List<Pokemon>,repo:PokemonRepository,state:UserState,onRemove:(Pokemon)->Unit,onOpen:(Pokemon)->Unit,onAdd:()->Unit,onCalculate:()->Unit,onState:(UserState)->Unit,creating:Boolean,onCreatingChange:(Boolean)->Unit){
    val members=remember(catalog,state.team){catalog.filter{it.id in state.team}}
    val shareScope=rememberCoroutineScope();val shareContext=LocalContext.current;var shareError by remember{mutableStateOf<String?>(null)}
    var coverageMoves by remember{mutableStateOf<List<Move>>(emptyList())}
    LaunchedEffect(state.activeTeam,catalog){coverageMoves=members.flatMap{p->val ids=state.builds[p.id]?.moves.orEmpty();repo.movesFor(p,state.activeTeam.gameId).filter{it.id in ids}}}
    LazyColumn(Modifier.prismaViewport(top=140.dp),contentPadding=PaddingValues(start=22.dp,end=22.dp,top=8.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        item(key="library"){TeamLibraryHeader(state,onState)}
        if(state.activeTeam.gameId==25&&state.builds.values.mapNotNull{it.itemId}.let{it.distinct().size<it.size})item{LText("Regulamento M-C: itens segurados repetidos não são permitidos.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)}
        item(key="battle-format"){Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){FilterChip(state.activeTeam.format=="singles",{onState(state.updateTeam{it.copy(format="singles")})},label={LText("Singles · individual")});FilterChip(state.activeTeam.format=="doubles",{onState(state.updateTeam{it.copy(format="doubles")})},label={LText("Doubles · duplas")})}}
        item(key="team-view"){Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){FilterChip(state.teamView=="list",{onState(state.copy(teamView="list"))},label={LText("Lista")});FilterChip(state.teamView=="grid",{onState(state.copy(teamView="grid"))},label={LText("Grade · 6 de uma vez")});Spacer(Modifier.weight(1f));TextButton(onClick={shareScope.launch{shareError=runCatching{shareTeamCard(shareContext,state.activeTeam,catalog)}.exceptionOrNull()?.message}}){LText("Compartilhar")}};shareError?.let{LText("Não foi possível compartilhar: $it",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)};LText("A grade mostra os 6 parceiros de uma vez. Toque num parceiro para abrir a tela do set dele — item, golpes, nature e o resto só aparecem lá, sem poluir a grade ou a lista. A escolha fica salva neste aparelho.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
        if(state.teamView=="grid"){item(key="team-grid"){TeamGrid(members,state,onOpen,onRemove,onAdd)}}
        else{
        items(state.team,key={it}){id->catalog.find{it.id==id}?.let{p->Surface(onClick={onOpen(p)},modifier=motionPlacement(LocalReduceMotion.current),shape=RoundedCornerShape(26.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(16.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(72.dp),contentAlignment=Alignment.Center){if(state.builds[id]?.shiny==true)PokemonArtTrimmedShiny(id,Modifier.size(72.dp),p.name) else PokemonArtTrimmed(id,Modifier.size(72.dp),p.name)};Column(Modifier.weight(1f).padding(start=10.dp)){LText(p.name,fontSize=19.sp,fontWeight=FontWeight.SemiBold);FlowRow(horizontalArrangement=Arrangement.spacedBy(5.dp),verticalArrangement=Arrangement.spacedBy(4.dp),modifier=Modifier.padding(top=9.dp)){p.types.forEach{TypePill(it,true)}}};Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                IconButton(onClick={onOpen(p)},modifier=Modifier.size(48.dp)){LIcon(Icons.Rounded.Edit,"Editar set",tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(22.dp))}
                IconButton(onClick={onRemove(p)},modifier=Modifier.size(48.dp)){LIcon(Icons.Rounded.Remove,"Remover ${p.name} da equipe",tint=MaterialTheme.colorScheme.error,modifier=Modifier.size(22.dp))}
            }}
            FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp),modifier=Modifier.padding(top=10.dp)){p.stats.forEachIndexed{i,value->LText("${LocalUiStrings.current.text(listOf("HP","Ataque","Defesa","Atq. especial","Def. especial","Velocidade")[i])} $value",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
            LText(if(state.builds[id]==null)"Sem set — toque para abrir a tela do set" else "Set salvo — toque para abrir a tela do set",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=10.dp))
        }}}}
        }
        item(key="type-analysis"){TypeAnalysisPanel(members,coverageMoves)}
        if(state.team.isNotEmpty()&&state.activeTeam.gameId==25)item{ActionButton("Simular dano com estes sets",Icons.Rounded.AutoAwesome,true,onCalculate)}
        if(state.team.size<6)item{ActionButton("Escolher parceiro",Icons.Rounded.Add,true,onAdd)}
        item{LText("Equipe: ${state.activeTeam.name} · ${gameName(state.activeTeam.gameId)}. Confira as regras do jogo, formato e temporada antes de competir.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
    if(creating)NewTeamDialog(state,onState,onDismiss={onCreatingChange(false)})
}


@Composable private fun TeamGrid(members:List<Pokemon>,state:UserState,onOpen:(Pokemon)->Unit,onRemove:(Pokemon)->Unit,onAdd:()->Unit={}){
    // Uniform slots (30/10): every slot — filled or empty — has the same
    // fixed height (224dp); the art box (64dp, own-content-box crop + Fit,
    // no distortion/clip) and the chip area (64dp, as in the Pokédex grid
    // card) are fixed, so long type pairs wrapping to two lines cannot
    // change a card's height. Shiny members render their real shiny art.
    val ink=Color(0xFF1B1B26)
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        (0 until 3).forEach{r->
            Row(Modifier.height(224.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                (0 until 2).forEach{c->
                    val p=members.getOrNull(r*2+c)
                    if(p==null){
                        Surface(onClick=onAdd,shape=RoundedCornerShape(22.dp),border=BorderStroke(1.dp,solidBlend(MaterialTheme.colorScheme.onSurface,MaterialTheme.colorScheme.background,.25f)),color=solidBlend(MaterialTheme.colorScheme.surface,MaterialTheme.colorScheme.background,.45f),modifier=Modifier.weight(1f).fillMaxHeight()){
                            Column(Modifier.fillMaxSize().padding(14.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
                                LIcon(Icons.Rounded.Add,"Escolher parceiro",tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(28.dp))
                                LText("Escolher parceiro",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }else{
                        Surface(onClick={onOpen(p)},shape=RoundedCornerShape(22.dp),color=typeColor(p.types.first()),modifier=Modifier.weight(1f).fillMaxHeight()){
                            // Identity: same CardTexture dot/circle motif
                            // as the Pokédex cards, behind the art row.
                            // Sizes unchanged (224dp slot, 64dp art/chips).
                            Box(Modifier.fillMaxSize()){
                            CardTexture(typeColor(p.types.first()),Modifier.fillMaxSize(),dots=false,circleX=.85f,circleY=.30f,radiusFrac=.55f)
                            Column(Modifier.fillMaxSize().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                                Row(Modifier.height(64.dp),verticalAlignment=Alignment.CenterVertically){
                                    Box(Modifier.size(64.dp),contentAlignment=Alignment.Center){if(state.builds[p.id]?.shiny==true)PokemonArtTrimmedShiny(p.id,Modifier.size(64.dp),p.name) else PokemonArtTrimmed(p.id,Modifier.size(64.dp),p.name)}
                                    Spacer(Modifier.weight(1f))
                                    IconButton(onClick={onRemove(p)},modifier=Modifier.size(48.dp)){LIcon(Icons.Rounded.Remove,"Remover ${p.name} da equipe",tint=MaterialTheme.colorScheme.error,modifier=Modifier.size(20.dp))}
                                }
                                LText(p.name,style=MaterialTheme.typography.titleMedium,color=ink,maxLines=1,overflow=TextOverflow.Ellipsis)
                                Box(Modifier.height(64.dp),contentAlignment=Alignment.TopStart){FlowRow(horizontalArrangement=Arrangement.spacedBy(5.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){p.types.forEach{TypeChipOnColor(it)}}}
                                LText(if(state.builds[p.id]==null)"Sem set" else "Set salvo",style=MaterialTheme.typography.bodySmall,color=solidBlend(ink,typeColor(p.types.first()),.78f),maxLines=1,overflow=TextOverflow.Ellipsis)
                            }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun AccountScreen(store:Store,writeMutex:Mutex,email:String?,theme:String,onBack:()->Unit,onTheme:(String)->Unit,onAccountChanged:suspend ()->Unit,state:UserState,catalog:List<Pokemon>,onState:(UserState)->Unit,onOpenTrainerCard:()->Unit={}){
    val scope=rememberCoroutineScope();var mail by remember{mutableStateOf("")};var password by remember{mutableStateOf("")};var message by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)}
    Column(Modifier.prismaViewport(top=0.dp,bottom=0.dp).imePadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        IconButton(onClick=onBack,enabled=!busy){LIcon(Icons.AutoMirrored.Rounded.ArrowBack,"Voltar")}
        LText("Seu universo.\nDo seu jeito.",fontSize=32.sp,lineHeight=36.sp,fontWeight=FontWeight.Bold)
        SectionLabel("APARÊNCIA")
        ThemeSettingRow(theme){if(!busy)onTheme(it)}
        SectionLabel("CARTÃO DE TREINADOR")
        TrainerCard(state,catalog,styleKey=state.trainerCardStyle,backgroundKey=state.trainerCardBackground)
        OutlinedButton(onClick=onOpenTrainerCard,enabled=!busy,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){LText("Personalizar cartão",Modifier.padding(8.dp))}
        LText("O cartão mostra a equipe escolhida e as insígnias do jogo dela. Na tela de personalização dá para escolher estilo, fundo, nome, personagem, equipe e marcar as insígnias — e compartilhar o cartão como imagem. Nome, personagem, equipe e insígnias ficam só neste aparelho — limpar os dados do aplicativo apaga tudo.",fontSize=13.sp,lineHeight=20.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        SectionLabel("CONTA NESTE APARELHO")
        if(email!=null){LText(email,fontSize=16.sp);LText("Equipe, sets, favoritos e tema protegidos no armazenamento do aplicativo.",fontSize=13.sp,lineHeight=20.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);OutlinedButton(enabled=!busy,onClick={busy=true;scope.launch{writeMutex.withLock{withContext(Dispatchers.IO){store.logout()};onAccountChanged()};busy=false}}){LText("Sair da conta")}}
        else{
            LText("Use como visitante ou crie uma conta local para separar seus dados.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(mail,{mail=it},label={LText("E-mail")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth())
            OutlinedTextField(password,{password=it},label={LText("Senha")},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth())
            fun auth(register:Boolean){busy=true;scope.launch{writeMutex.withLock{val result=if(register)store.register(mail,password) else store.login(mail,password);password="";result.onSuccess{message="Conta aberta neste aparelho.";onAccountChanged()}.onFailure{message=it.message?:"Não foi possível entrar."}};busy=false}}
            ActionButton(if(busy)"Aguarde…" else "Entrar",Icons.Rounded.Login,!busy){auth(false)}
            OutlinedButton(onClick={auth(true)},enabled=!busy,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){LText("Criar conta local",Modifier.padding(8.dp))}
        }
        if(message.isNotEmpty())LText(message,fontSize=13.sp,color=MaterialTheme.colorScheme.primary)
        Surface(color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){LIcon(Icons.Rounded.OfflineBolt,null,tint=MaterialTheme.colorScheme.primary);LText("Pronto para ficar offline",fontWeight=FontWeight.SemiBold)};LText("Catálogo, artes e golpes já vêm no aplicativo. Equipe e favoritos funcionam sem internet.",fontSize=13.sp,lineHeight=20.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
        LText("Nesta versão, a conta existe apenas neste aparelho. Não há sincronização em nuvem, confirmação de e-mail ou recuperação de senha. Essas integrações ainda precisam ser implementadas.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun SourcesScreen(catalog:List<Pokemon>){
    LazyColumn(Modifier.prismaViewport(),contentPadding=PaddingValues(start=24.dp,end=24.dp,top=16.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){
        item{LText("Fontes e\ncobertura.",fontSize=34.sp,fontWeight=FontWeight.Bold,lineHeight=38.sp)}
        item(key="catalog-audit"){Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            LText("Nacional × Champions",fontSize=20.sp,fontWeight=FontWeight.SemiBold)
            LText("${catalog.map{it.baseId}.distinct().size} espécies · ${catalog.size} registros nacionais",fontSize=13.sp)
            LText("${catalog.filter{it.champions}.map{it.baseId}.distinct().size} espécies · ${catalog.count{it.champions}} registros Champions",fontSize=13.sp,color=MaterialTheme.colorScheme.primary)
            LText("Registros incluem formas e Mega Evoluções. Presença na base, compatibilidade no jogo e elegibilidade da temporada são verificações diferentes.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            for(gen in 1..9){val rows=catalog.filter{it.generation==gen};Row(Modifier.fillMaxWidth()){LText("Geração $gen",Modifier.weight(1f),fontSize=13.sp);LText("${rows.count{it.champions}} / ${rows.size} registros",fontSize=13.sp)}}
        }}}
        item(key="art-coverage"){ArtCoverageSection(catalog)}
        item{Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){LText("Créditos de arte",fontSize=20.sp,fontWeight=FontWeight.SemiBold);LText("Insígnias: Kanto a Unova, recriações de Stephen Griffiths (SteGriff/pokemon-badges), CC BY 3.0, a partir da Bulbapedia; Paldea, Kalos, Galar e variantes de Sinnoh/Unova: imagens PokeAPI/sprites (arte de Nintendo, Game Freak e The Pokémon Company, sob o risco de IP aceito pelo responsável do projeto; a aceitação na Play Store não é garantida). Sem insígnias inventadas para Alola, Legends ou Champions.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);LText("Artes shiny (1.330 de 1.353): 953 recolors de TonOfDirt726 (crédito dado aqui; a página verificada exibe CC BY 3.0) + 377 artes oficiais shiny do PokeAPI, forma exata (arte de Nintendo, Game Freak e The Pokémon Company). 23 registros sem shiny estático verificado mostram o estado indisponível, nunca recolor artificial. Fontes por registro em shiny-art-sources.json.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);LText("Sprites de treinador (1.500): Pokémon Showdown, cópias sem edição; 13 artistas creditados por sprite no seletor (não editar sem permissão) — arte de Nintendo, Game Freak e The Pokémon Company, sob o risco de IP aceito pelo responsável do projeto; a aceitação na Play Store não é garantida.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);LText("Arte de itens (1.010 de 2.223): sprites de bolsa dos Arquivos Bulbagarden, por nome exato; itens sem arte usam o marcador genérico. Arte de jogo, sem licença aberta de redistribuição alegada.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);LText("Artes dos iniciais anunciados (Browt, Pombon, Gecqua): reproduzidas da página Universo Nintendo (fan site, sem licença aberta de redistribuição alegada) — arte de Nintendo, Game Freak e The Pokémon Company, fora da aceitação de risco Showdown/PokeAPI; a aceitação na Play Store não é garantida. Fontes e hashes por arte em announced-art-sources.json.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);LText("Logos de jogos (filtro da Pokédex): arquivos dos Arquivos Bulbagarden, conferidos um a um (logo correto, transparente, em inglês) — arte e marcas de Nintendo, Game Freak e The Pokémon Company, sem licença aberta de redistribuição alegada e fora da aceitação de risco Showdown/PokeAPI; a aceitação na Play Store não é garantida. Red/Blue, Yellow, Gold/Silver e Crystal mantêm o rótulo em texto por não haver logo limpo. O logo de Winds/Waves da seção de anunciados é a imagem enviada pelo responsável do projeto.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);LText("Ícones do app (Sylveon e Flareon): artes enviadas pelo responsável do projeto, recortadas para o quadrado interno sem o fundo grafite — personagens de Nintendo, Game Freak e The Pokémon Company, sem licença aberta de redistribuição alegada; a aceitação na Play Store não é garantida. Ícones de pôster (Aggron, Pikachu, Starmie, Lucario, Cloyster, Sigilyph, Haxorus, Vaporeon, Infernape, Umbreon): artes de pôster enviadas pelo responsável do projeto, com o texto removido por preenchimento amostrado da arte vizinha, sem zoom e sem redesenho, no formato de quadrado arredondado — personagens de Nintendo, Game Freak e The Pokémon Company, sem licença aberta de redistribuição alegada; a aceitação na Play Store não é garantida. Ícones Ultra Ball, Great Ball, Pokédex e Glaceon: artes enviadas pelo responsável do projeto, separadas de suas imagens originais sem zoom e sem redesenho, no formato de quadrado arredondado — personagens e itens de Nintendo, Game Freak e The Pokémon Company, sem licença aberta de redistribuição alegada; a aceitação na Play Store não é garantida. Ícones Master Ball e Poké Ball: artes enviadas pelo responsável do projeto, separadas de suas imagens originais sem zoom e sem redesenho, no formato de quadrado arredondado — itens de Nintendo, Game Freak e The Pokémon Company, sem licença aberta de redistribuição alegada; a aceitação na Play Store não é garantida.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
        item{LText("Conheça sua equipe. Respeite o regulamento.",fontSize=14.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        listOf("Compatibilidade por jogo" to "A seleção Champions combina a lista oficial M-C com formas de batalha e variantes verificadas na referência Showdown. A página oficial não enumera todas as Megas. A ausência de uma forma em uma fonte não comprova que ela inexista no jogo.","Construa com clareza" to "Golpes, PP e itens usam os dados específicos de Champions; Mega Stones verificam a forma correta. A M-C proíbe itens repetidos nas ranqueadas. Os sets preservados de versões anteriores podem pedir revisão.","Seus dados, offline" to "Todas as artes e dados necessários à consulta estão incluídos no APK. Você não precisa baixar um pacote após instalar.","Verificação contínua" to "Consulte as notícias, regras e atualizações oficiais de Pokémon Champions antes de competir. Esta versão é um projeto de fã em desenvolvimento.").forEach{(title,body)->item{Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){LText(title,fontSize=18.sp,fontWeight=FontWeight.SemiBold);LText(body,fontSize=13.sp,lineHeight=21.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
    }
}

@Composable internal fun ActionButton(text:String,icon:ImageVector,enabled:Boolean=true,onClick:()->Unit){
    val interaction=remember{MutableInteractionSource()};val pressed by interaction.collectIsPressedAsState();val scale by animateFloatAsState(pressTarget(pressed,LocalReduceMotion.current,.96f),spring(.75f,500f),label="action touch")
    val shape=RoundedCornerShape(20.dp);val accent=MaterialTheme.colorScheme.primary
    Row(Modifier.fillMaxWidth().graphicsLayer{scaleX=scale;scaleY=scale;alpha=if(enabled)1f else .4f}.clip(shape).background(Brush.linearGradient(listOf(solidBlend(accent,MaterialTheme.colorScheme.surface,.30f),solidBlend(accent,MaterialTheme.colorScheme.surface,.09f),solidBlend(accent,MaterialTheme.colorScheme.surface,.18f)))).border(1.dp,Brush.linearGradient(listOf(solidBlend(Color.White,accent,.5f),solidBlend(accent,MaterialTheme.colorScheme.surface,.15f),solidBlend(Color.White,accent,.25f))),shape).clickable(interaction,null,enabled=enabled,onClick=onClick).heightIn(min=54.dp).padding(horizontal=18.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){LIcon(icon,null,Modifier.size(20.dp),tint=accent);Spacer(Modifier.width(10.dp));LText(text,style=MaterialTheme.typography.labelLarge)}
}
@Composable private fun InfoTile(label:String,value:String,modifier:Modifier){Column(modifier.background(MaterialTheme.colorScheme.surface,RoundedCornerShape(20.dp)).padding(18.dp)){LText(label,fontSize=13.sp,letterSpacing=1.6.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);LText(value,fontSize=17.sp,fontWeight=FontWeight.Medium,modifier=Modifier.padding(top=7.dp))}}
@Composable private fun SectionLabel(label:String){LText(label,fontSize=13.sp,letterSpacing=1.7.sp,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(top=8.dp))}
@Composable private fun EmptyState(title:String,subtitle:String){Column(Modifier.fillMaxWidth().padding(vertical=40.dp),horizontalAlignment=Alignment.CenterHorizontally){LIcon(Icons.Rounded.CatchingPokemon,null,Modifier.size(44.dp),tint=solidBlend(MaterialTheme.colorScheme.primary,MaterialTheme.colorScheme.surface,.5f));LText(title,fontSize=16.sp,modifier=Modifier.padding(top=18.dp));LText(subtitle,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=8.dp))}}
@Composable internal fun MoveRow(move:Move,modifier:Modifier=Modifier,learners:List<Pokemon> = emptyList(),onOpenLearner: ((Pokemon) -> Unit)? = null){
    var details by remember{mutableStateOf(false)}
    if(details)MoveInfoSheet(move,{details=false},learners,onOpenLearner)
    Surface(onClick={details=true},modifier=modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface){
        Column(Modifier.padding(17.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){TypeSymbol(move.type,Modifier.size(24.dp));LText(move.name,Modifier.weight(1f).padding(horizontal=10.dp),fontSize=16.sp,fontWeight=FontWeight.SemiBold);LIcon(Icons.Rounded.Info,"Informações de ${move.name}",Modifier.size(20.dp),tint=MaterialTheme.colorScheme.primary)}
            MoveFacts(move)
            if(move.summary.isNotBlank())Column(verticalArrangement=Arrangement.spacedBy(4.dp)){LText("Resumo",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(descriptionText(move.summary),fontSize=14.sp,lineHeight=20.sp)}
            LText("Ver efeito e propriedades",fontSize=13.sp,color=MaterialTheme.colorScheme.primary)
        }
    }
}
