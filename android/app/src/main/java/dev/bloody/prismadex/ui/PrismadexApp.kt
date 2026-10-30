@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package dev.bloody.prismadex.ui

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.*
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Lilac=Color(0xFFC4B5FD)
private val Ink=Color(0xFF10121B)
private val Muted=Color(0xFFABAABE)
private val bitmapCache=object:LruCache<Int,ImageBitmap>(18*1024*1024){override fun sizeOf(key:Int,value:ImageBitmap)=value.width*value.height*4}

@Composable fun PokemonArt(id:Int,modifier:Modifier=Modifier,label:String?=null){
    key(id){
    val assets=LocalContext.current.assets
    val image by produceState<ImageBitmap?>(bitmapCache.get(id),id){value=withContext(Dispatchers.IO){bitmapCache.get(id)?:runCatching{assets.open("art/$id.webp").use{BitmapFactory.decodeStream(it)?.asImageBitmap()}}.getOrNull()?.also{bitmapCache.put(id,it)}}}
    val ready=image
    if(ready!=null) Image(ready,label,modifier) else Box(modifier,contentAlignment=Alignment.Center){LIcon(Icons.Rounded.CatchingPokemon,label,tint=MaterialTheme.colorScheme.onSurfaceVariant.copy(.4f),modifier=Modifier.size(44.dp))}
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
    var detailGame by rememberSaveable{mutableIntStateOf(23)}
    var detailTab by rememberSaveable{mutableStateOf("info")}
    var detailOrder by rememberSaveable{mutableStateOf(intArrayOf())}
    var detailDirection by remember{mutableIntStateOf(1)}
    var showTeamPicker by rememberSaveable{mutableStateOf(false)}
    var showCreateTeam by rememberSaveable{mutableStateOf(false)}
    var showAccount by rememberSaveable{mutableStateOf(false)}
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
    fun persist(next:UserState){state=next;savedLabel="Salvando…";scope.launch{writeMutex.withLock{runCatching{withContext(Dispatchers.IO){store.save(next)}}.onSuccess{savedLabel="Salvo no aparelho"}.onFailure{savedLabel="Falha ao salvar";snackbar.showSnackbar(ui.text(it.message?:"Não foi possível salvar."))}}}}
    fun favorite(p:Pokemon){persist(state.copy(favorites=if(p.id in state.favorites)state.favorites-p.id else state.favorites+p.id))}
    var removalTeamId by rememberSaveable{mutableStateOf<String?>(null)}
    var removalPokemonId by rememberSaveable{mutableStateOf<Int?>(null)}
    var removing by remember{mutableStateOf(false)}
    fun requestRemoval(p:Pokemon){removalTeamId=state.activeTeamId;removalPokemonId=p.id}
    fun toggleTeam(p:Pokemon){
        if(p.id in state.team){requestRemoval(p);return}
        if(!p.availableIn(state.activeTeam.gameId)){scope.launch{snackbar.showSnackbar(ui.text("Pokémon não listado para ${gameName(state.activeTeam.gameId)}."))};return}
        if(state.team.size==6){scope.launch{snackbar.showSnackbar(ui.text("Sua equipe já tem seis parceiros."))};return}
        if(state.team.mapNotNull{id->catalog.find{it.id==id}}.any{it.baseId==p.baseId}){scope.launch{snackbar.showSnackbar(ui.text("Já existe uma forma desta espécie na equipe."))};return}
        persist(state.updateTeam{it.copy(members=it.members+p.id)})
    }
    fun openDetail(p:Pokemon,order:List<Pokemon>,game:Int=23){
        detailGame=game
        detailOrder=order.map{it.id}.toIntArray()
        selectedId=p.id;detailTab="info";detailDirection=1
    }
    fun browseDetail(id:Int){
        if(pendingBuild!=null){scope.launch{snackbar.showSnackbar(ui.text("Salve ou descarte o rascunho antes de trocar de Pokémon."))};return}
        val order=detailOrder.toList()
        detailDirection=if(order.indexOf(id)<order.indexOf(selectedId)) -1 else 1
        selectedId=id
    }
    fun closePokemon(){if(pendingBuild!=null)exitBuild=true else selectedId=null}
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
                if(closeAfter){exitBuild=false;selectedId=null}
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
    val colors=if(light)lightColorScheme(primary=accent,background=bg,surface=selectedTheme.surface,onSurface=Color(0xFF28263A),onBackground=Color(0xFF28263A),onSurfaceVariant=Color(0xFF625A75)) else darkColorScheme(primary=accent,background=bg,surface=selectedTheme.surface,surfaceVariant=selectedTheme.surface.copy(alpha=.85f),onBackground=Color(0xFFF2F0FA),onSurface=Color(0xFFF2F0FA),onSurfaceVariant=Color(0xFFB5B1C5))
    val drawerState=rememberDrawerState(DrawerValue.Closed)
    MaterialTheme(colorScheme=colors){
        val backdrop=rememberPrismaBackdrop()
        CompositionLocalProvider(LocalPrismaBackdrop provides backdrop, LocalContentColor provides colors.onBackground, LocalReduceMotion provides rememberReduceMotion()){
            ModalNavigationDrawer(drawerState=drawerState,gesturesEnabled=drawerState.isOpen,drawerContent={AppDrawer(screen,onNavigate={key->selectedId=null;showDummy=false;screen=key;scope.launch{drawerState.close()}},onAccount={showAccount=true;scope.launch{drawerState.close()}})}){
            BoxWithConstraints(Modifier.fillMaxSize().background(bg).windowInsetsPadding(WindowInsets.safeDrawing).clipToBounds()){
                val expanded=maxWidth>=600.dp
                val fontScale=LocalDensity.current.fontScale
                val navigationHeight=if(fontScale>1.25f)(52+32*fontScale).dp else 72.dp
                val railWidth=if(fontScale>1.25f)(72*fontScale).dp.coerceAtMost(120.dp) else 72.dp
                val railVisible=expanded&&selectedId==null&&!showAccount&&!showDummy&&!showTeamPicker
                CompositionLocalProvider(LocalPrismaLayout provides PrismaLayout(expanded,if(expanded)16.dp else navigationHeight+20.dp)){
                Box(Modifier.fillMaxSize().padding(start=if(railVisible)railWidth+24.dp else 0.dp)){

                if(!initialized){Column(Modifier.align(Alignment.Center),horizontalAlignment=Alignment.CenterHorizontally){LIcon(Icons.Rounded.CatchingPokemon,null,tint=accent,modifier=Modifier.size(44.dp));Spacer(Modifier.height(20.dp));LText(failure?:"Abrindo seu universo…",color=MaterialTheme.colorScheme.onBackground)}}
                else{
                    val target=if(showTeamPicker)"team-picker" else if(showAccount)"account" else if(showDummy)"dummy/${editingDummyId?:"new"}" else selectedId?.let{"pokemon/$it"}?:screen
                    BackHandler(showTeamPicker||drawerState.isOpen||showAccount||showDummy||selectedId!=null||screen!="dex"||artViewerId!=null){when{artViewerId!=null->artViewerId=null;showTeamPicker->showTeamPicker=false;drawerState.isOpen->scope.launch{drawerState.close()};showAccount->showAccount=false;showDummy->showDummy=false;selectedId!=null->closePokemon();else->screen="dex"}}
                    val reduceMotion=LocalReduceMotion.current
                    Box(Modifier.fillMaxSize().prismaBackdrop(backdrop)){
                        AnimatedContent(targetState=target,transitionSpec={
                            if(reduceMotion)(fadeIn(tween(120)) togetherWith fadeOut(tween(120)))
                            else{
                            val browsing=initialState.startsWith("pokemon/")&&targetState.startsWith("pokemon/")
                            val opening=targetState.startsWith("pokemon/")||targetState=="account"
                            if(browsing)(slideInHorizontally(tween(280,easing=FastOutSlowInEasing)){it*detailDirection}+fadeIn(tween(180))) togetherWith (slideOutHorizontally(tween(280,easing=FastOutSlowInEasing)){-it*detailDirection}+fadeOut(tween(180)))
                            else
                            if(opening)(slideInHorizontally(tween(330,easing=FastOutSlowInEasing)){it/5}+fadeIn(tween(240))) togetherWith (fadeOut(tween(160))+scaleOut(tween(300),targetScale=.97f))
                            else (fadeIn(tween(220))+slideInHorizontally(tween(300)){ -it/12}) togetherWith fadeOut(tween(150))}
                        },label="native screen navigation"){route->
                            screenStates.SaveableStateProvider(route){when{
                                route=="team-picker"->TeamPokemonPicker(catalog,state,onAdd={p->if(p.id !in state.team)toggleTeam(p)},onClose={showTeamPicker=false})
                                route=="account"->AccountScreen(store,writeMutex,email,state.theme,onBack={showAccount=false},onTheme={persist(state.copy(theme=it))},onAccountChanged={state=withContext(Dispatchers.IO){store.load()};email=withContext(Dispatchers.IO){store.currentEmail}})
                                route.startsWith("dummy/")->DummyEditorScreen(catalog,repo,state.dummies.find{it.id==editingDummyId},onBack={showDummy=false},onSave={dummy->scope.launch{val p=catalog.find{it.id==dummy.pokemonId};val checked=p?.let{repo.validateBuild(it,dummy.build)};if(checked?.isSuccess==true&&(state.dummies.size<50||state.dummies.any{it.id==dummy.id})){persist(state.copy(dummies=state.dummies.filterNot{it.id==dummy.id}+dummy));requestedDefender="dummy/${dummy.id}";showDummy=false;screen="calc";snackbar.showSnackbar(ui.text("Alvo de teste salvo no aparelho."))}else snackbar.showSnackbar(ui.text(checked?.exceptionOrNull()?.message?:"Limite de 50 alvos atingido."))}})
                                route=="calc"->DamageScreen(catalog,state,repo,damageEngine,onSelectTeam={persist(state.copy(activeTeamId=it))},onEdit={p,teamId->if(teamId!=null)persist(state.copy(activeTeamId=teamId));pendingBuild=null;detailOrder=catalog.sortedWith(compareBy({it.baseId},{it.id})).map{it.id}.toIntArray();selectedId=p.id;detailTab="build"},onDummy={editingDummyId=it;showDummy=true},onDeleteDummy={id->persist(state.copy(dummies=state.dummies.filterNot{it.id==id}))},requestedDefender=requestedDefender,onDefenderConsumed={requestedDefender=null},onChoosePokemon={requestedGame=state.activeTeam.gameId;screen="dex"})
                                route.startsWith("pokemon/")->catalog.find{it.id==route.substringAfter('/').toIntOrNull()}?.let{p->PokemonScreen(p,repo,state,detailTab,{detailTab=it},{favorite(p)},{toggleTeam(p)},onBuild={build->saveSet(p,build)},draftKey=drafts.key(email,state.activeTeamId,p.id),onDraft={pendingBuild=it},footerVisible=pendingBuild!=null&&detailTab=="build",catalog=catalog,referenceGame=detailGame,onReferenceGame={detailGame=it},onArtViewer={artViewerId=it.id},shinyArt=shinyArt,onShinyArt={shinyArt=it},
                                    previousId=adjacentPokemonId(detailOrder.toList(),p.id,-1),nextId=adjacentPokemonId(detailOrder.toList(),p.id,1),
                                    canBrowse=pendingBuild==null,onBrowse={browseDetail(it)},onRelated={related->detailOrder=catalog.sortedWith(compareBy({it.baseId},{it.id})).map{it.id}.toIntArray();browseDetail(related.id)})}

                                route=="team"->TeamScreen(catalog,repo,state,onRemove={requestRemoval(it)},onOpen={detailGame=state.activeTeam.gameId;detailOrder=state.team.toIntArray();selectedId=it.id;detailTab="build"},onAdd={showTeamPicker=true},onCalculate={screen="calc"},onState={persist(it)},creating=showCreateTeam,onCreatingChange={showCreateTeam=it},onArt={artViewerId=it.id})
                                route=="meta"->MetaScreen(catalog,state.battleFormat,onFormat={persist(state.copy(battleFormat=it))},onOpen={openDetail(it,catalog.sortedWith(compareBy({p->p.baseId},{p->p.id})))})
                                route=="items"->ItemCatalogScreen()
                                route=="abilities"->AbilitiesDexScreen(catalog,onOpenPokemon={openDetail(it,catalog.sortedWith(compareBy({p->p.baseId},{p->p.id})))})
                                route=="moves"->MovesDexScreen(catalog,repo,onOpenPokemon={openDetail(it,catalog.sortedWith(compareBy({p->p.baseId},{p->p.id})))})
                                route=="badges"->BadgeTrackerScreen(state,onState={persist(it)})
                                route=="settings"->SettingsScreen(state,email,onState={persist(it)},onAccount={showAccount=true},onSources={screen="sources"})
                                route=="sources"->SourcesScreen(catalog)
                                else->DexScreen(catalog,state,route=="favorites",requestedGame=requestedGame,onGameConsumed={requestedGame=null},onOpen={p,order,game->openDetail(p,order,game)},onFavorite={favorite(it)},onLayout={persist(state.copy(layout=it))},onArt={artViewerId=it.id})
                            }}
                        }
                    }
                    if(target=="team-picker"){ /* picker owns its title and close action */ }
                    else if(target.startsWith("pokemon/")){
                        FloatingTopBar(title=catalog.find{it.id==selectedId}?.name?:"Pokémon",onBack={closePokemon()},onFavorite={catalog.find{it.id==selectedId}?.let{favorite(it)}},isFavorite=selectedId in state.favorites)
                        if(pendingBuild!=null&&detailTab=="build")Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth().imePadding(),color=colors.surface,tonalElevation=6.dp){Row(Modifier.padding(horizontal=18.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){LText("Rascunho no aparelho",Modifier.weight(1f),fontSize=13.sp);Button(onClick={catalog.find{it.id==selectedId}?.let{p->pendingBuild?.let{saveSet(p,it)}}},enabled=selectedId in state.team){LText("Salvar set")}}}
                    }else if(target!="account"&&!target.startsWith("dummy/")){
                        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().background(bg).padding(horizontal=18.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
                            LiquidGlass(Modifier.size(48.dp),radius=24.dp,tint=MaterialTheme.colorScheme.surface.copy(.3f),onClick={scope.launch{drawerState.open()}}){LIcon(Icons.Rounded.Menu,"Abrir menu lateral",Modifier.align(Alignment.Center),tint=accent)}
                            Spacer(Modifier.width(10.dp));PrismaMark(Modifier.size(28.dp));Spacer(Modifier.width(8.dp))
                            val title=mapOf("dex" to "Pokédex","favorites" to "Favoritos","team" to "Equipes","calc" to "Calculadora","meta" to "Meta online","items" to "Itens","abilities" to "Habilidades","moves" to "Golpes","badges" to "Insígnias","settings" to "Configurações","sources" to "Champions")[screen]?:"Pokédex"
                            Column(Modifier.weight(1f)){
                                LText(title,style=MaterialTheme.typography.titleLarge,fontSize=20.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                                if(!savedLabel.startsWith("Salvo"))LText(savedLabel,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            LiquidGlass(Modifier.size(48.dp),radius=24.dp,tint=MaterialTheme.colorScheme.surface.copy(.3f),onClick={showAccount=true}){LIcon(Icons.Rounded.PersonOutline,"Minha conta",Modifier.align(Alignment.Center),tint=accent)}
                        }
                        if(!expanded)PrismaNavigation(screen,{screen=it},Modifier.align(Alignment.BottomCenter).padding(horizontal=14.dp,vertical=10.dp).fillMaxWidth().height(navigationHeight))
                    }
                }
                if(initialized&&!showTeamPicker&&screen=="team"&&selectedId==null&&!showAccount&&!showDummy){
                    LiquidGlass(Modifier.align(Alignment.TopEnd).padding(top=72.dp,end=22.dp).height(56.dp).widthIn(min=144.dp,max=280.dp).semantics{contentDescription=ui.text("Criar nova equipe")},radius=24.dp,tint=MaterialTheme.colorScheme.surface.copy(.4f),onClick={showCreateTeam=true}){
                        Row(Modifier.align(Alignment.Center).padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){LIcon(Icons.Rounded.Add,null,Modifier.size(24.dp),tint=accent);LText("Nova equipe",Modifier.padding(start=8.dp),fontSize=14.sp,color=accent,maxLines=1)}
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
                                    if(state.activeTeamId==teamId&&selectedId==pokemonId)pendingBuild=null
                                    removalTeamId=null;removalPokemonId=null;savedLabel="Salvo no aparelho"
                                }.onFailure{savedLabel="Falha ao salvar";snackbar.showSnackbar(ui.text("Não foi possível remover. Tente novamente."))}
                            };removing=false}
                        }){LText(if(removing)"Removendo…" else "Remover",color=MaterialTheme.colorScheme.error)}},
                        dismissButton={TextButton(enabled=!removing,onClick={removalTeamId=null;removalPokemonId=null}){LText("Cancelar")}})
                }
                if(exitBuild){AlertDialog(onDismissRequest={exitBuild=false},title={LText("Alterações não salvas")},text={LText("Seu rascunho está neste aparelho. Salve o set, descarte as alterações ou continue editando.")},confirmButton={TextButton(onClick={catalog.find{it.id==selectedId}?.let{p->pendingBuild?.let{saveSet(p,it,true)}}}){LText("Salvar e sair")}},dismissButton={Row{TextButton(onClick={selectedId?.let{drafts.clear(drafts.key(email,state.activeTeamId,it))};pendingBuild=null;exitBuild=false;selectedId=null}){LText("Descartar")};TextButton(onClick={exitBuild=false}){LText("Continuar")}}})}
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

@Composable private fun FloatingTopBar(title:String,onBack:()->Unit,onFavorite:()->Unit,isFavorite:Boolean){
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(14.dp),verticalAlignment=Alignment.CenterVertically){
        LiquidGlass(Modifier.size(48.dp),radius=24.dp,onClick=onBack){LIcon(Icons.AutoMirrored.Rounded.ArrowBack,"Voltar",Modifier.align(Alignment.Center))}
        Spacer(Modifier.width(12.dp));LText(title,Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
        LiquidGlass(Modifier.size(48.dp),radius=24.dp,onClick=onFavorite){LIcon(if(isFavorite)Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,"Favoritar Pokémon",Modifier.align(Alignment.Center),tint=MaterialTheme.colorScheme.primary)}
    }
}

@Composable internal fun DexScreen(catalog:List<Pokemon>,state:UserState,favoritesOnly:Boolean,onOpen:(Pokemon,List<Pokemon>,Int)->Unit,onFavorite:(Pokemon)->Unit,onLayout:(String)->Unit,requestedGame:Int?=null,onGameConsumed:()->Unit={},onArt:(Pokemon)->Unit={}){
    var query by rememberSaveable(favoritesOnly){mutableStateOf("")}
    var selectedGame by rememberSaveable{mutableStateOf(0)}
    var generation by rememberSaveable{mutableStateOf(0)}
    var type by rememberSaveable{mutableStateOf<String?>(null)}
    var alphabetical by rememberSaveable{mutableStateOf(false)}
    var filtersExpanded by rememberSaveable{mutableStateOf(false)}
    fun clearAll(){query="";selectedGame=0;generation=0;type=null;alphabetical=false}
    // The query has its own clear control inside the search field, so it is not counted here.
    // The game is only an active filter outside favorites, where it starts forced to Nacional.
    val activeFilterCount=(if(selectedGame!=0)1 else 0)+(if(generation!=0)1 else 0)+(if(type!=null)1 else 0)+(if(alphabetical)1 else 0)
    LaunchedEffect(requestedGame){if(requestedGame!=null){selectedGame=requestedGame;generation=0;type=null;query="";onGameConsumed()}}
    val filterIndex by produceState<PokemonFilterIndex?>(null,catalog){value=withContext(Dispatchers.Default){PokemonFilterIndex(catalog)}}
    val list by produceState<List<Pokemon>>(emptyList(),filterIndex,state.favorites,query,selectedGame,generation,type,alphabetical,favoritesOnly){
        value=withContext(Dispatchers.Default){filterIndex?.select(selectedGame,PokemonFilter(query=query,types=type?.let{setOf(it)}?:emptySet(),generation=generation,sort=if(alphabetical)PokemonSort.NAME else PokemonSort.NUMBER))?.filter{!favoritesOnly||it.id in state.favorites}.orEmpty()}
    }
    val scroll=rememberLazyGridState()
    val configuration=androidx.compose.ui.platform.LocalConfiguration.current
    val columns=if(state.layout=="list")1 else if(configuration.screenWidthDp>=840)4 else if(configuration.screenWidthDp>=600)3 else 2
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
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    LText("${list.size} Pokémon e formas",Modifier.weight(1f),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    FilterChip(alphabetical,{alphabetical=!alphabetical},label={LText(if(alphabetical)"Ordem: A–Z" else "Ordem: Nº")})
                    TextButton(onClick={filtersExpanded=!filtersExpanded}){LText(if(activeFilterCount>0)"Filtros ($activeFilterCount)" else "Filtros");LIcon(Icons.Rounded.ExpandMore,null,Modifier.rotate(if(filtersExpanded)180f else 0f))}
                    IconButton(onClick={onLayout(if(state.layout=="list")"grid" else "list")}){LIcon(if(state.layout=="list")Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.List,if(state.layout=="list")"Mostrar em grade" else "Mostrar em lista")}
                }
                if(activeFilterCount>0&&!filtersExpanded){
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                        if(selectedGame!=0)FilterChip(true,{selectedGame=0},label={LText(if(selectedGame==25)"Champions" else gameName(selectedGame))},trailingIcon={LIcon(Icons.Rounded.Close,"Remover filtro de jogo")})
                        if(generation!=0)FilterChip(true,{generation=0},label={LText("Geração $generation")},trailingIcon={LIcon(Icons.Rounded.Close,"Remover filtro de geração")})
                        if(type!=null)FilterChip(true,{type=null},label={LText(typeNames.getValue(type!!))},leadingIcon={TypeSymbol(type!!)},trailingIcon={LIcon(Icons.Rounded.Close,"Remover filtro de tipo")})
                        if(alphabetical)FilterChip(true,{alphabetical=false},label={LText("Ordem: A–Z")},trailingIcon={LIcon(Icons.Rounded.Close,"Remover ordenação alfabética")})
                        TextButton(onClick={clearAll()}){LText("Limpar tudo")}
                    }
                }
                AnimatedVisibility(filtersExpanded,enter=expandEnter(LocalReduceMotion.current),exit=shrinkExit(LocalReduceMotion.current)){Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
                if(!favoritesOnly)ChoiceField("JOGO",if(selectedGame==0)"Nacional · todos os jogos" else gameName(selectedGame),listOf("0" to "Nacional · todos os jogos")+SupportedGames.map{it.id.toString() to it.name}){selectedGame=it.toInt()}
                ChoiceField("GERAÇÃO DA ESPÉCIE",if(generation==0)"Todas as gerações" else "Geração $generation",listOf("0" to "Todas as gerações")+(1..9).map{it.toString() to "Geração $it"}){generation=it.toInt()}
                LText("TIPO",fontSize=13.sp,letterSpacing=1.7.sp,color=MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    FilterChip(type==null,{type=null},label={LText("Todos")})
                    typeNames.keys.forEach{t->FilterChip(type==t,{type=if(type==t)null else t},label={LText(typeNames.getValue(t))},leadingIcon={TypeSymbol(t)})}
                }
                    TextButton(onClick={clearAll()}){LText("Limpar tudo")}
                }}

            }
        }
        if(!favoritesOnly&&selectedGame==0&&generation==0&&type==null)item(span={GridItemSpan(maxLineSpan)},key="announced"){AnnouncedPokemonSection(query)}
        items(list,key={it.id},contentType={state.layout}){p->PokemonCard(p,state.layout,p.id in state.favorites,{onOpen(p,list,selectedGame.takeIf{it>0}?:23)},{onFavorite(p)},onArt={onArt(p)},motionPlacement(LocalReduceMotion.current))}
        if(list.isEmpty())item(span={GridItemSpan(maxLineSpan)},key="empty"){Column(horizontalAlignment=Alignment.CenterHorizontally){EmptyState(if(favoritesOnly&&query.isBlank())"Seus parceiros favoritos vivem aqui." else "Nenhum Pokémon encontrado.",if(favoritesOnly&&query.isBlank())"Toque no coração na Pokédex para adicionar favoritos." else "Experimente outro nome ou filtro.");if(query.isNotBlank()||activeFilterCount>0)TextButton(onClick={clearAll()}){LText("Limpar tudo")}}}
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
    BoxWithConstraints(Modifier.fillMaxWidth().heightIn(min=280.dp).clip(RoundedCornerShape(30.dp)).background(Brush.linearGradient(listOf(typeColor(p.types.first()).copy(.22f),MaterialTheme.colorScheme.surface)))){
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

@Composable private fun PokemonCard(p:Pokemon,layout:String,favorite:Boolean,onOpen:()->Unit,onFavorite:()->Unit,onArt:()->Unit={},modifier:Modifier=Modifier){
    val interaction=remember{MutableInteractionSource()};val pressed by interaction.collectIsPressedAsState();val scale by animateFloatAsState(pressTarget(pressed,LocalReduceMotion.current,.975f),spring(.8f,600f),label="card touch")
    val shape=RoundedCornerShape(25.dp)
    val ink=Color(0xFF1B1B26)
    if(layout=="list"){
        // Clean format (reference list): solid type-colour card, number + name +
        // type chips, art on the right. Dark ink on the pastel type colours
        // (existing theme tokens) keeps labels legible in light and dark.
        Box(modifier.fillMaxWidth().graphicsLayer{scaleX=scale;scaleY=scale}.clip(shape).background(typeColor(p.types.first())).clickable(interaction,null,onClick=onOpen)){
            Row(Modifier.fillMaxWidth().padding(start=18.dp,end=8.dp,top=14.dp,bottom=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){
                    LText("#${p.baseId.toString().padStart(4,'0')}",fontSize=13.sp,color=ink.copy(.72f))
                    LText(p.name,fontSize=20.sp,lineHeight=24.sp,fontWeight=FontWeight.Bold,color=ink,maxLines=1,overflow=TextOverflow.Ellipsis)
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){p.types.forEach{TypeChipOnColor(it)}}
                    if(!p.champions)LText("Compatibilidade por verificar",fontSize=13.sp,color=ink.copy(.78f))
                }
                Box(Modifier.size(100.dp).clickable{onArt()},contentAlignment=Alignment.Center){PokemonArt(p.id,Modifier.size(100.dp),p.name)}
                IconButton(onClick=onFavorite,modifier=Modifier.size(48.dp)){LIcon(if(favorite)Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,"Favoritar ${p.name}",tint=ink,modifier=Modifier.size(20.dp))}
            }
        }
    }else{
        // Grid redesign (23:31): same clean language as the list card — solid
        // type-colour fill, dark ink, number + name + type chips + art. Fixed
        // 104dp art (no aspect-ratio distortion), name up to 2 lines, and a
        // reserved chip area so every card in a row has the same height at
        // font scale 1.0. No field is dropped vs the list card.
        Box(modifier.fillMaxWidth().graphicsLayer{scaleX=scale;scaleY=scale}.clip(shape).background(typeColor(p.types.first())).clickable(interaction,null,onClick=onOpen)){
            Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Row(Modifier.padding(end=40.dp).heightIn(min=24.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    LText("#${p.baseId.toString().padStart(4,'0')}",fontSize=13.sp,lineHeight=18.sp,color=ink.copy(.72f))
                    if(!p.champions)LIcon(Icons.Rounded.Info,"Compatibilidade com Champions por verificar",Modifier.size(16.dp),tint=ink.copy(.72f))
                }
                Box(Modifier.align(Alignment.CenterHorizontally).size(104.dp).clickable{onArt()},contentAlignment=Alignment.Center){PokemonArt(p.id,Modifier.size(104.dp),p.name)}
                LText(p.name,fontSize=15.sp,lineHeight=20.sp,fontWeight=FontWeight.SemiBold,color=ink,minLines=2,maxLines=2,overflow=TextOverflow.Ellipsis)
                Box(Modifier.fillMaxWidth().heightIn(min=60.dp),contentAlignment=Alignment.TopStart){
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){p.types.forEach{TypeChipOnColor(it)}}
                }
                if(!p.champions)LText("Compatibilidade por verificar",fontSize=12.sp,color=ink.copy(.78f))
            }
            IconButton(onClick=onFavorite,modifier=Modifier.align(Alignment.TopEnd).size(48.dp)){LIcon(if(favorite)Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,"Favoritar ${p.name}",tint=ink,modifier=Modifier.size(19.dp))}
        }
    }
}

@Composable private fun PokemonScreen(p:Pokemon,repo:PokemonRepository,state:UserState,tab:String,onTab:(String)->Unit,onFavorite:()->Unit,onTeam:()->Unit,onBuild:(Build)->Unit,draftKey:String,onDraft:(Build?)->Unit,footerVisible:Boolean=false,catalog:List<Pokemon>,previousId:Int?,nextId:Int?,canBrowse:Boolean,onBrowse:(Int)->Unit,onRelated:(Pokemon)->Unit,referenceGame:Int,onReferenceGame:(Int)->Unit,onArtViewer:(Pokemon)->Unit={},shinyArt:Boolean=false,onShinyArt:(Boolean)->Unit={}){
    val viewingGame=if(tab=="build")state.activeTeam.gameId else referenceGame
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
    LaunchedEffect(p.id,viewingGame){loaded=false;moves=repo.movesFor(p,viewingGame);heldItems=repo.heldItemsFor(p,state.activeTeam.gameId);learnMethods=learnRepository.methodsFor(p.id,viewingGame);loaded=true}
    val detailScroll=rememberLazyListState()
    LazyColumn(Modifier.prismaViewport(top=80.dp,bottom=if(footerVisible)88.dp else 16.dp).imePadding().pokemonSwipe(p.id,tab!="build"&&canBrowse,previousId,nextId,onBrowse),state=detailScroll,contentPadding=PaddingValues(top=8.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        if(tab!="build")item(key="browse-controls"){
            PokemonBrowseControls(previousId?.let{id->catalog.find{it.id==id}},nextId?.let{id->catalog.find{it.id==id}},canBrowse,onBrowse)
        }
        item(key="detail-header"){
            // Clean format, our theme: solid type-colour header (existing type
            // token), faint name watermark, art, number + name + type chips.
            // The content below keeps the app background/surface in light and
            // dark — no forced white sheet from the mockups.
            val ink=Color(0xFF1B1B26)
            Box(Modifier.padding(horizontal=18.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(typeColor(p.types.first())).clipToBounds()){
                LText(p.name.uppercase(),Modifier.padding(start=10.dp,top=10.dp),fontSize=64.sp,lineHeight=70.sp,fontWeight=FontWeight.Bold,color=ink.copy(.10f),maxLines=1,overflow=TextOverflow.Clip)
                Column(Modifier.fillMaxWidth().padding(start=20.dp,end=20.dp,top=20.dp,bottom=20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    LText("#${p.baseId.toString().padStart(4,'0')}  /  ${LocalUiStrings.current.text(if(p.isMega)"MEGA EVOLUÇÃO" else "POKÉDEX")}",fontSize=13.sp,letterSpacing=1.8.sp,color=ink.copy(.72f),fontWeight=FontWeight.SemiBold)
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)){
                            LText(p.name,fontSize=30.sp,fontWeight=FontWeight.Bold,lineHeight=34.sp,letterSpacing=(-1).sp,color=ink,maxLines=2,overflow=TextOverflow.Ellipsis)
                            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){p.types.forEach{TypeChipOnColor(it)}}
                        }
                        Box(Modifier.size(132.dp).clickable{onArtViewer(p)},contentAlignment=Alignment.Center){ShinyArt(p.id,shinyArt,Modifier.size(132.dp),p.name)}
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        FilterChip(!shinyArt,{onShinyArt(false)},label={LText("Normal")})
                        FilterChip(shinyArt,{onShinyArt(true)},label={LText("Shiny")})
                    }
                    if(shinyArt&&shinyExists==false)LText("Arte shiny não disponível para este Pokémon.",fontSize=13.sp,color=ink.copy(.78f))
                    LText(when(p.championsSource){"official-regulation"->"Espécie/forma na lista oficial M-C · verificada em 28/10/2026.";"showdown-form"->"Forma de batalha ou variante · referência Champions do Showdown.";else->"Compatibilidade desta forma com Champions ainda por verificar."},fontSize=13.sp,color=ink.copy(.78f))
                }
            }
        }
        item(key="detail-tabs"){Row(Modifier.padding(horizontal=22.dp).fillMaxWidth().background(MaterialTheme.colorScheme.surface,RoundedCornerShape(20.dp)).padding(5.dp)){
            listOf("info" to "Informações","moves" to "Golpes","build" to "Seu set").forEach{(key,label)->Box(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if(key==tab)MaterialTheme.colorScheme.primary.copy(.16f) else Color.Transparent).clickable{onTab(key)}.heightIn(min=48.dp).padding(vertical=13.dp),contentAlignment=Alignment.Center){LText(label,fontSize=13.sp,color=if(key==tab)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,fontWeight=FontWeight.SemiBold)}}
        }}
        if(tab=="info"){
            item{Column(Modifier.padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)){InfoTile("ALTURA",(p.height?.takeIf{it>0}?:speciesProfile?.heightMeters)?.let{"$it m"}?:"Não informado",Modifier.weight(1f));InfoTile("PESO",(p.weight?.takeIf{it>0}?:speciesProfile?.weightKg)?.let{"$it kg"}?:"Não informado",Modifier.weight(1f))}
                SectionLabel("ATRIBUTOS BASE")
                p.stats.forEachIndexed{i,v->Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){LText(listOf("HP","Ataque","Defesa","Atq. especial","Def. especial","Velocidade")[i],Modifier.weight(1f),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);LText("$v",fontSize=14.sp,fontWeight=FontWeight.SemiBold)}
                    LinearProgressIndicator(progress={v/255f},modifier=Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),color=typeColor(p.types.first()),trackColor=MaterialTheme.colorScheme.surfaceVariant)
                }}
                TypeAnalysisPanel(listOf(p),team=false)
                EvolutionSection(p,catalog,canBrowse,onRelated)
                SpeciesProfileSection(p,catalog,onRelated)
                if(speciesProfile?.abilities.isNullOrEmpty()){SectionLabel("HABILIDADES GERAIS");p.abilities.forEach{AbilityNameButton(it,catalog,onRelated)}}
                LText("A lista oficial identifica espécies/formas elegíveis no regulamento. Megas e transformações usam a referência de batalha; não confunda ausência da página com ausência no jogo.",fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                ActionButton(if(p.id in state.team)"Remover da equipe" else "Adicionar à equipe",if(p.id in state.team)Icons.Rounded.Remove else Icons.Rounded.Add,p.availableIn(state.activeTeam.gameId)||p.id in state.team,onTeam)
            }}
        }else if(tab=="moves"){
            item{Column(Modifier.padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){ChoiceField("JOGO DOS GOLPES",gameName(viewingGame),SupportedGames.map{it.id.toString() to it.name}){onReferenceGame(it.toInt())};SectionLabel("GOLPES · ${gameName(viewingGame)}");LText("${moves.size} golpes · ${gameName(viewingGame)}",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);OutlinedTextField(query,{query=it},leadingIcon={LIcon(Icons.Rounded.Search,null)},placeholder={LText("Buscar golpe")},singleLine=true,shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth());if(!loaded)LinearProgressIndicator(Modifier.fillMaxWidth())}}
            items(moves.filter{it.name.contains(query,true)},key={it.id}){move->Column(Modifier.padding(horizontal=24.dp)){MoveRow(move);LearnMethodLabel(learnMethods[move.id].orEmpty().ifEmpty{listOf(LearnMethod("unknown"))})}}
            if(loaded&&moves.isEmpty())item{EmptyState("Sem golpes registrados para este jogo.","Este Pokémon ainda não tem compatibilidade confirmada nesta base.")}
            item{LText(if(viewingGame==25)"Atributos e aprendizados de Champions: fonte Showdown, consulta de 28/10/2026. PP básico; não é a tabela de outros jogos." else "Poder, precisão e PP da referência geral; atributos escalares e categoria adaptados ao jogo. Efeitos textuais, alvo e flags são referência geral e ainda não certificados por versão.",Modifier.padding(horizontal=24.dp),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        }else{
            item{BuildEditor(p,state.builds[p.id]?:Build(),p.id in state.team,moves,heldItems,onTeam,onBuild,game=state.activeTeam.gameId,draftKey=draftKey,onDraft=onDraft)}
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
    var inspectedMove by remember(p.id){mutableStateOf<Move?>(null)}
    inspectedMove?.let{MoveInfoSheet(it,onDismiss={inspectedMove=null})}
    var searchMove by rememberSaveable(p.id){mutableStateOf("")}
    var searchItem by rememberSaveable(p.id){mutableStateOf("")}
    var picking by rememberSaveable(p.id){mutableStateOf("")}
    val editing=Build(selectedItem,selectedMoves,nature,ability,sps)
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
        ActionButton("Salvar set",Icons.Rounded.Check,inTeam&&sps.sum()<=66){onSave(Build(selectedItem,selectedMoves,nature,ability,sps))}
    }
}

@Composable private fun TeamScreen(catalog:List<Pokemon>,repo:PokemonRepository,state:UserState,onRemove:(Pokemon)->Unit,onOpen:(Pokemon)->Unit,onAdd:()->Unit,onCalculate:()->Unit,onState:(UserState)->Unit,creating:Boolean,onCreatingChange:(Boolean)->Unit,onArt:(Pokemon)->Unit={}){
    val members=remember(catalog,state.team){catalog.filter{it.id in state.team}}
    val shareScope=rememberCoroutineScope();val shareContext=LocalContext.current;var shareError by remember{mutableStateOf<String?>(null)}
    var coverageMoves by remember{mutableStateOf<List<Move>>(emptyList())}
    LaunchedEffect(state.activeTeam,catalog){coverageMoves=members.flatMap{p->val ids=state.builds[p.id]?.moves.orEmpty();repo.movesFor(p,state.activeTeam.gameId).filter{it.id in ids}}}
    LazyColumn(Modifier.prismaViewport(top=140.dp),contentPadding=PaddingValues(start=22.dp,end=22.dp,top=8.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        item(key="library"){TeamLibraryHeader(state,onState)}
        if(state.activeTeam.gameId==25&&state.builds.values.mapNotNull{it.itemId}.let{it.distinct().size<it.size})item{LText("Regulamento M-C: itens segurados repetidos não são permitidos.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)}
        item(key="battle-format"){Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){FilterChip(state.activeTeam.format=="singles",{onState(state.updateTeam{it.copy(format="singles")})},label={LText("Singles · individual")});FilterChip(state.activeTeam.format=="doubles",{onState(state.updateTeam{it.copy(format="doubles")})},label={LText("Doubles · duplas")})}}
        item(key="team-view"){Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){FilterChip(state.teamView=="list",{onState(state.copy(teamView="list"))},label={LText("Lista")});FilterChip(state.teamView=="grid",{onState(state.copy(teamView="grid"))},label={LText("Grade · 6 de uma vez")});Spacer(Modifier.weight(1f));TextButton(onClick={shareScope.launch{shareError=runCatching{shareTeamCard(shareContext,state.activeTeam,catalog)}.exceptionOrNull()?.message}}){LText("Compartilhar")}};shareError?.let{LText("Não foi possível compartilhar: $it",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)};LText("A grade mostra os 6 parceiros de uma vez; a lista mostra os sets completos. A escolha fica salva neste aparelho.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
        if(state.teamView=="grid"){item(key="team-grid"){TeamGrid(members,state,onOpen,onRemove,onArt,onAdd)}}
        else{
        items(state.team,key={it}){id->catalog.find{it.id==id}?.let{p->val build=state.builds[id];var review by remember(id,build,state.activeTeam.gameId){mutableStateOf<String?>(null)};var itemName by remember(id,build,state.activeTeam.gameId){mutableStateOf<String?>(null)};var moveNames by remember(id,build,state.activeTeam.gameId){mutableStateOf<List<Move>>(emptyList())};LaunchedEffect(id,build,state.activeTeam.gameId){review=if(!p.availableIn(state.activeTeam.gameId))"Compatibilidade por revisar" else build?.let{repo.validateBuild(p,it,state.activeTeam.gameId).exceptionOrNull()?.message};itemName=repo.heldItemsFor(p,state.activeTeam.gameId).find{it.id==build?.itemId}?.name;moveNames=repo.movesFor(p,state.activeTeam.gameId).filter{it.id in (build?.moves?:emptyList())}};Surface(onClick={onOpen(p)},modifier=motionPlacement(LocalReduceMotion.current),shape=RoundedCornerShape(26.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(16.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(72.dp).clickable{onArt(p)},contentAlignment=Alignment.Center){PokemonArt(id,Modifier.size(72.dp),p.name)};Column(Modifier.weight(1f).padding(start=10.dp)){LText(p.name,fontSize=19.sp,fontWeight=FontWeight.SemiBold);FlowRow(horizontalArrangement=Arrangement.spacedBy(5.dp),verticalArrangement=Arrangement.spacedBy(4.dp),modifier=Modifier.padding(top=9.dp)){p.types.forEach{TypePill(it,true)}}};Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                IconButton(onClick={onOpen(p)},modifier=Modifier.size(48.dp)){LIcon(Icons.Rounded.Edit,"Editar set",tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(22.dp))}
                IconButton(onClick={onRemove(p)},modifier=Modifier.size(48.dp)){LIcon(Icons.Rounded.Remove,"Remover ${p.name} da equipe",tint=MaterialTheme.colorScheme.error,modifier=Modifier.size(22.dp))}
            }}
            FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp),modifier=Modifier.padding(top=10.dp)){p.stats.forEachIndexed{i,value->LText("${LocalUiStrings.current.text(listOf("HP","Ataque","Defesa","Atq. especial","Def. especial","Velocidade")[i])} $value",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
            review?.let{LText("Revisar set: $it",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(top=10.dp))}
            HorizontalDivider(Modifier.padding(vertical=13.dp),color=MaterialTheme.colorScheme.onSurface.copy(.07f));Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){ItemArt(build?.itemId,itemName,Modifier.size(28.dp));LText(if(build==null)"Toque para escolher item e golpes" else itemName?:"Sem item",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};if(moveNames.isNotEmpty()){FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(9.dp),modifier=Modifier.padding(top=14.dp)){moveNames.forEach{move->Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){TypeSymbol(move.type,Modifier.size(14.dp));LText(move.name,style=MaterialTheme.typography.bodySmall)}}}}
        }}}}
        }
        item(key="type-analysis"){TypeAnalysisPanel(members,coverageMoves)}
        if(state.team.isNotEmpty()&&state.activeTeam.gameId==25)item{ActionButton("Simular dano com estes sets",Icons.Rounded.AutoAwesome,true,onCalculate)}
        if(state.team.size<6)item{ActionButton("Escolher parceiro",Icons.Rounded.Add,true,onAdd)}
        item{LText("Equipe: ${state.activeTeam.name} · ${gameName(state.activeTeam.gameId)}. Confira as regras do jogo, formato e temporada antes de competir.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
    if(creating)NewTeamDialog(state,onState,onDismiss={onCreatingChange(false)})
}


@Composable private fun TeamGrid(members:List<Pokemon>,state:UserState,onOpen:(Pokemon)->Unit,onRemove:(Pokemon)->Unit,onArt:(Pokemon)->Unit={},onAdd:()->Unit={}){
    // Grid redesign (23:31): same clean language as the Pokédex list/grid
    // cards — solid type-colour fill, dark ink, art + name + type chips +
    // set summary. Same fields as the list rows; only the layout changes.
    // Six slots always (3x2): empty slots open the picker.
    val ink=Color(0xFF1B1B26)
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        (0 until 3).forEach{r->
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                (0 until 2).forEach{c->
                    val p=members.getOrNull(r*2+c)
                    if(p==null){
                        Surface(onClick=onAdd,shape=RoundedCornerShape(22.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.onSurface.copy(.25f)),color=MaterialTheme.colorScheme.surface.copy(.45f),modifier=Modifier.weight(1f).fillMaxHeight()){
                            Column(Modifier.fillMaxWidth().padding(14.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
                                LIcon(Icons.Rounded.Add,"Escolher parceiro",tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(28.dp))
                                LText("Escolher parceiro",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }else{
                        Surface(onClick={onOpen(p)},shape=RoundedCornerShape(22.dp),color=typeColor(p.types.first()),modifier=Modifier.weight(1f)){
                            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                                Row(verticalAlignment=Alignment.CenterVertically){
                                    Box(Modifier.size(64.dp).clickable{onArt(p)},contentAlignment=Alignment.Center){PokemonArt(p.id,Modifier.size(64.dp),p.name)}
                                    Spacer(Modifier.weight(1f))
                                    IconButton(onClick={onRemove(p)},modifier=Modifier.size(48.dp)){LIcon(Icons.Rounded.Remove,"Remover ${p.name} da equipe",tint=MaterialTheme.colorScheme.error,modifier=Modifier.size(20.dp))}
                                }
                                LText(p.name,style=MaterialTheme.typography.titleMedium,color=ink,maxLines=1,overflow=TextOverflow.Ellipsis)
                                FlowRow(horizontalArrangement=Arrangement.spacedBy(5.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){p.types.forEach{TypeChipOnColor(it)}}
                                val build=state.builds[p.id]
                                LText(if(build==null)"Sem set" else "${build.moves.size}/4 golpes${if(build.itemId!=null)" · com item" else ""}",style=MaterialTheme.typography.bodySmall,color=ink.copy(.78f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun AccountScreen(store:Store,writeMutex:Mutex,email:String?,theme:String,onBack:()->Unit,onTheme:(String)->Unit,onAccountChanged:suspend ()->Unit){
    val scope=rememberCoroutineScope();var mail by remember{mutableStateOf("")};var password by remember{mutableStateOf("")};var message by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)}
    Column(Modifier.prismaViewport(top=0.dp,bottom=0.dp).imePadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        IconButton(onClick=onBack,enabled=!busy){LIcon(Icons.AutoMirrored.Rounded.ArrowBack,"Voltar")}
        LText("Seu universo.\nDo seu jeito.",fontSize=32.sp,lineHeight=36.sp,fontWeight=FontWeight.Bold)
        SectionLabel("APARÊNCIA")
        ThemePicker(theme){if(!busy)onTheme(it)}
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
        item{LText("Conheça sua equipe. Respeite o regulamento.",fontSize=14.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        listOf("Compatibilidade por jogo" to "A seleção Champions combina a lista oficial M-C com formas de batalha e variantes verificadas na referência Showdown. A página oficial não enumera todas as Megas. A ausência de uma forma em uma fonte não comprova que ela inexista no jogo.","Construa com clareza" to "Golpes, PP e itens usam os dados específicos de Champions; Mega Stones verificam a forma correta. A M-C proíbe itens repetidos nas ranqueadas. Os sets preservados de versões anteriores podem pedir revisão.","Seus dados, offline" to "Todas as artes e dados necessários à consulta estão incluídos no APK. Você não precisa baixar um pacote após instalar.","Verificação contínua" to "Consulte as notícias, regras e atualizações oficiais de Pokémon Champions antes de competir. Esta versão é um projeto de fã em desenvolvimento.").forEach{(title,body)->item{Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){LText(title,fontSize=18.sp,fontWeight=FontWeight.SemiBold);LText(body,fontSize=13.sp,lineHeight=21.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
    }
}

@Composable internal fun ActionButton(text:String,icon:ImageVector,enabled:Boolean=true,onClick:()->Unit){
    val interaction=remember{MutableInteractionSource()};val pressed by interaction.collectIsPressedAsState();val scale by animateFloatAsState(pressTarget(pressed,LocalReduceMotion.current,.96f),spring(.75f,500f),label="action touch")
    val shape=RoundedCornerShape(20.dp);val accent=MaterialTheme.colorScheme.primary
    Row(Modifier.fillMaxWidth().graphicsLayer{scaleX=scale;scaleY=scale;alpha=if(enabled)1f else .4f}.clip(shape).background(Brush.linearGradient(listOf(accent.copy(.30f),accent.copy(.09f),accent.copy(.18f)))).border(1.dp,Brush.linearGradient(listOf(Color.White.copy(.5f),accent.copy(.15f),Color.White.copy(.25f))),shape).clickable(interaction,null,enabled=enabled,onClick=onClick).heightIn(min=54.dp).padding(horizontal=18.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){LIcon(icon,null,Modifier.size(20.dp),tint=accent);Spacer(Modifier.width(10.dp));LText(text,style=MaterialTheme.typography.labelLarge)}
}
@Composable private fun InfoTile(label:String,value:String,modifier:Modifier){Column(modifier.background(MaterialTheme.colorScheme.surface,RoundedCornerShape(20.dp)).padding(18.dp)){LText(label,fontSize=13.sp,letterSpacing=1.6.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);LText(value,fontSize=17.sp,fontWeight=FontWeight.Medium,modifier=Modifier.padding(top=7.dp))}}
@Composable private fun SectionLabel(label:String){LText(label,fontSize=13.sp,letterSpacing=1.7.sp,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(top=8.dp))}
@Composable private fun EmptyState(title:String,subtitle:String){Column(Modifier.fillMaxWidth().padding(vertical=40.dp),horizontalAlignment=Alignment.CenterHorizontally){LIcon(Icons.Rounded.CatchingPokemon,null,Modifier.size(44.dp),tint=MaterialTheme.colorScheme.primary.copy(.5f));LText(title,fontSize=16.sp,modifier=Modifier.padding(top=18.dp));LText(subtitle,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=8.dp))}}
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
