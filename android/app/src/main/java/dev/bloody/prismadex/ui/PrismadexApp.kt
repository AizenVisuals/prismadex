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
    if(ready!=null) Image(ready,label,modifier) else Box(modifier,contentAlignment=Alignment.Center){Icon(Icons.Rounded.CatchingPokemon,label,tint=MaterialTheme.colorScheme.onSurfaceVariant.copy(.4f),modifier=Modifier.size(44.dp))}
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
    var detailTab by rememberSaveable{mutableStateOf("info")}
    var showCreateTeam by rememberSaveable{mutableStateOf(false)}
    var showAccount by rememberSaveable{mutableStateOf(false)}
    var showDummy by rememberSaveable{mutableStateOf(false)}
    var requestedDefender by rememberSaveable{mutableStateOf<String?>(null)}
    var editingDummyId by rememberSaveable{mutableStateOf<String?>(null)}
    var email by remember{mutableStateOf<String?>(null)}
    var savedLabel by remember{mutableStateOf("Salvo no aparelho")}
    val snackbar=remember{SnackbarHostState()}
    val screenStates=rememberSaveableStateHolder()
    LaunchedEffect(Unit){runCatching{catalog=repo.loadCatalog();state=withContext(Dispatchers.IO){store.load()};email=withContext(Dispatchers.IO){store.currentEmail};initialized=true}.onFailure{failure="Não foi possível abrir os dados locais."}}
    fun persist(next:UserState){state=next;savedLabel="Salvando…";scope.launch{writeMutex.withLock{runCatching{withContext(Dispatchers.IO){store.save(next)}}.onSuccess{savedLabel="Salvo no aparelho"}.onFailure{savedLabel="Falha ao salvar";snackbar.showSnackbar(it.message?:"Não foi possível salvar.")}}}}
    fun favorite(p:Pokemon){persist(state.copy(favorites=if(p.id in state.favorites)state.favorites-p.id else state.favorites+p.id))}
    fun toggleTeam(p:Pokemon){
        if(p.id in state.team){persist(state.updateTeam{it.copy(members=it.members-p.id,builds=it.builds-p.id)});return}
        if(!p.availableIn(state.activeTeam.gameId)){scope.launch{snackbar.showSnackbar("Pokémon não listado para ${gameName(state.activeTeam.gameId)}.")};return}
        if(state.team.size==6){scope.launch{snackbar.showSnackbar("Sua equipe já tem seis parceiros.")};return}
        if(state.team.mapNotNull{id->catalog.find{it.id==id}}.any{it.baseId==p.baseId}){scope.launch{snackbar.showSnackbar("Já existe uma forma desta espécie na equipe.")};return}
        persist(state.updateTeam{it.copy(members=it.members+p.id)})
    }
    fun closePokemon(){if(pendingBuild!=null)exitBuild=true else selectedId=null}
    fun saveSet(p:Pokemon,build:Build,closeAfter:Boolean=false){
        val teamId=state.activeTeamId
        val draftKey=drafts.key(email,teamId,p.id)
        scope.launch{writeMutex.withLock{
            val result=repo.validateBuild(p,build,state.teams.first{it.id==teamId}.gameId)
            if(result.isFailure){snackbar.showSnackbar(result.exceptionOrNull()?.message?:"Revise o set.");return@withLock}
            val next=state.copy(teams=state.teams.map{if(it.id==teamId)it.copy(builds=it.builds+(p.id to build)) else it})
            savedLabel="Salvando…"
            runCatching{withContext(Dispatchers.IO){store.save(next)}}.onSuccess{
                state=next;drafts.clear(draftKey);pendingBuild=null;savedLabel="Salvo no aparelho"
                if(closeAfter){exitBuild=false;selectedId=null}
                snackbar.showSnackbar("Set salvo no aparelho.")
            }.onFailure{savedLabel="Falha ao salvar";snackbar.showSnackbar("Não foi possível salvar. Seu rascunho foi mantido.")}
        }}
    }
    val light=when(state.appearanceMode){"light"->true;"dark"->false;else->!isSystemInDarkTheme()}
    val selectedTheme=(AppThemes.firstOrNull{it.key==state.theme}?:AppThemes.first()).colors(light)
    val view=LocalView.current
    SideEffect{(view.context as? Activity)?.window?.let{window->
        WindowCompat.getInsetsController(window,view).apply{isAppearanceLightStatusBars=light;isAppearanceLightNavigationBars=light}
    }}
    val accent=selectedTheme.accent
    val bg=selectedTheme.background
    val colors=if(light)lightColorScheme(primary=accent,background=bg,surface=selectedTheme.surface,onSurface=Color(0xFF28263A),onBackground=Color(0xFF28263A),onSurfaceVariant=Color(0xFF625A75)) else darkColorScheme(primary=accent,background=bg,surface=selectedTheme.surface,surfaceVariant=selectedTheme.surface.copy(alpha=.85f),onBackground=Color(0xFFF2F0FA),onSurface=Color(0xFFF2F0FA),onSurfaceVariant=Color(0xFFB5B1C5))
    val drawerState=rememberDrawerState(DrawerValue.Closed)
    MaterialTheme(colorScheme=colors){
        val backdrop=rememberPrismaBackdrop()
        CompositionLocalProvider(LocalPrismaBackdrop provides backdrop, LocalContentColor provides colors.onBackground){
            ModalNavigationDrawer(drawerState=drawerState,gesturesEnabled=drawerState.isOpen,drawerContent={AppDrawer(screen,onNavigate={key->selectedId=null;showDummy=false;screen=key;scope.launch{drawerState.close()}},onAccount={showAccount=true;scope.launch{drawerState.close()}})}){
            Box(Modifier.fillMaxSize().background(bg).windowInsetsPadding(WindowInsets.safeDrawing).clipToBounds()){
                if(!initialized){Column(Modifier.align(Alignment.Center),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Rounded.CatchingPokemon,null,tint=accent,modifier=Modifier.size(44.dp));Spacer(Modifier.height(20.dp));Text(failure?:"Abrindo seu universo…",color=MaterialTheme.colorScheme.onBackground)}}
                else{
                    val target=if(showAccount)"account" else if(showDummy)"dummy/${editingDummyId?:"new"}" else selectedId?.let{"pokemon/$it"}?:screen
                    BackHandler(drawerState.isOpen||showAccount||showDummy||selectedId!=null||screen!="dex"){when{drawerState.isOpen->scope.launch{drawerState.close()};showAccount->showAccount=false;showDummy->showDummy=false;selectedId!=null->closePokemon();else->screen="dex"}}
                    Box(Modifier.fillMaxSize().prismaBackdrop(backdrop)){
                        AnimatedContent(targetState=target,transitionSpec={
                            val opening=targetState.startsWith("pokemon/")||targetState=="account"
                            if(opening)(slideInHorizontally(tween(330,easing=FastOutSlowInEasing)){it/5}+fadeIn(tween(240))) togetherWith (fadeOut(tween(160))+scaleOut(tween(300),targetScale=.97f))
                            else (fadeIn(tween(220))+slideInHorizontally(tween(300)){ -it/12}) togetherWith fadeOut(tween(150))
                        },label="native screen navigation"){route->
                            screenStates.SaveableStateProvider(route){when{
                                route=="account"->AccountScreen(store,writeMutex,email,state.theme,onBack={showAccount=false},onTheme={persist(state.copy(theme=it))},onAccountChanged={state=withContext(Dispatchers.IO){store.load()};email=withContext(Dispatchers.IO){store.currentEmail}})
                                route.startsWith("dummy/")->DummyEditorScreen(catalog,repo,state.dummies.find{it.id==editingDummyId},onBack={showDummy=false},onSave={dummy->scope.launch{val p=catalog.find{it.id==dummy.pokemonId};val checked=p?.let{repo.validateBuild(it,dummy.build)};if(checked?.isSuccess==true&&(state.dummies.size<50||state.dummies.any{it.id==dummy.id})){persist(state.copy(dummies=state.dummies.filterNot{it.id==dummy.id}+dummy));requestedDefender="dummy/${dummy.id}";showDummy=false;screen="calc";snackbar.showSnackbar("Alvo de teste salvo no aparelho.")}else snackbar.showSnackbar(checked?.exceptionOrNull()?.message?:"Limite de 50 alvos atingido.")}})
                                route=="calc"->DamageScreen(catalog,state,repo,damageEngine,onSelectTeam={persist(state.copy(activeTeamId=it))},onEdit={p,teamId->if(teamId!=null)persist(state.copy(activeTeamId=teamId));pendingBuild=null;selectedId=p.id;detailTab="build"},onDummy={editingDummyId=it;showDummy=true},onDeleteDummy={id->persist(state.copy(dummies=state.dummies.filterNot{it.id==id}))},requestedDefender=requestedDefender,onDefenderConsumed={requestedDefender=null},onChoosePokemon={val team=state.teams.firstOrNull{it.gameId==25};if(team!=null){persist(state.copy(activeTeamId=team.id));requestedGame=25;screen="dex"}else{screen="team";showCreateTeam=true}})
                                route.startsWith("pokemon/")->catalog.find{it.id==route.substringAfter('/').toIntOrNull()}?.let{p->PokemonScreen(p,repo,state,detailTab,{detailTab=it},{favorite(p)},{toggleTeam(p)},onBuild={build->saveSet(p,build)},draftKey=drafts.key(email,state.activeTeamId,p.id),onDraft={pendingBuild=it})}

                                route=="team"->TeamScreen(catalog,repo,state,onOpen={selectedId=it.id;detailTab="build"},onAdd={requestedGame=state.activeTeam.gameId;screen="dex"},onCalculate={screen="calc"},onState={persist(it)},creating=showCreateTeam,onCreatingChange={showCreateTeam=it})
                                route=="meta"->MetaScreen(catalog,state.battleFormat,onFormat={persist(state.copy(battleFormat=it))},onOpen={selectedId=it.id;detailTab="info"})
                                route=="settings"->SettingsScreen(state,email,onState={persist(it)},onAccount={showAccount=true},onSources={screen="sources"})
                                route=="sources"->SourcesScreen(catalog)
                                else->DexScreen(catalog,state,route=="favorites",requestedGame=requestedGame,onGameConsumed={requestedGame=null},onOpen={selectedId=it.id;detailTab="info"},onFavorite={favorite(it)},onLayout={persist(state.copy(layout=it))})
                            }}
                        }
                    }
                    if(target.startsWith("pokemon/")){
                        FloatingTopBar(title=catalog.find{it.id==selectedId}?.name?:"Pokémon",onBack={closePokemon()},onFavorite={catalog.find{it.id==selectedId}?.let{favorite(it)}},isFavorite=selectedId in state.favorites)
                        if(pendingBuild!=null&&detailTab=="build")Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth().imePadding(),color=colors.surface,tonalElevation=6.dp){Row(Modifier.padding(horizontal=18.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Text("Rascunho no aparelho",Modifier.weight(1f),fontSize=13.sp);Button(onClick={catalog.find{it.id==selectedId}?.let{p->pendingBuild?.let{saveSet(p,it)}}},enabled=selectedId in state.team){Text("Salvar set")}}}
                    }else if(target!="account"&&!target.startsWith("dummy/")){
                        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().background(bg).padding(horizontal=18.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
                            LiquidGlass(Modifier.size(48.dp),radius=24.dp,tint=MaterialTheme.colorScheme.surface.copy(.3f),onClick={scope.launch{drawerState.open()}}){Icon(Icons.Rounded.Menu,"Abrir menu lateral",Modifier.align(Alignment.Center),tint=accent)}
                            Spacer(Modifier.width(10.dp));PrismaMark(Modifier.size(28.dp));Spacer(Modifier.width(8.dp))
                            val title=mapOf("dex" to "Pokédex","favorites" to "Favoritos","team" to "Equipes","calc" to "Calculadora","meta" to "Meta online","settings" to "Configurações","sources" to "Champions")[screen]?:"Pokédex"
                            Column(Modifier.weight(1f)){
                                Text(title,style=MaterialTheme.typography.titleLarge,fontSize=20.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                                if(!savedLabel.startsWith("Salvo"))Text(savedLabel,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            LiquidGlass(Modifier.size(48.dp),radius=24.dp,tint=MaterialTheme.colorScheme.surface.copy(.3f),onClick={showAccount=true}){Icon(Icons.Rounded.PersonOutline,"Minha conta",Modifier.align(Alignment.Center),tint=accent)}
                        }
                        LiquidGlass(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal=14.dp,vertical=10.dp).fillMaxWidth().height(72.dp),radius=30.dp,tint=MaterialTheme.colorScheme.surface.copy(alpha=if(light).6f else .35f)){
                            Row(Modifier.fillMaxSize().padding(6.dp)){
                                listOf(Triple("dex","Pokédex",Icons.Rounded.CatchingPokemon),Triple("favorites","Favoritos",Icons.Rounded.FavoriteBorder),Triple("team","Equipe",Icons.Rounded.Hexagon),Triple("calc","Danos",Icons.Rounded.AutoAwesome),Triple("meta","Meta",Icons.Rounded.Public)).forEach{(key,label,icon)->
                                    val active=screen==key
                                    val tint by animateColorAsState(if(active)accent else MaterialTheme.colorScheme.onSurfaceVariant,label="nav tint")
                                    val navInteraction=remember{MutableInteractionSource()};val pressed by navInteraction.collectIsPressedAsState();val size by animateFloatAsState(if(pressed).94f else 1f,spring(stiffness=600f),label="nav press")
                                    Column(Modifier.weight(1f).fillMaxHeight().graphicsLayer{scaleX=size;scaleY=size}.clip(RoundedCornerShape(24.dp)).background(if(active)accent.copy(.17f) else Color.Transparent).clickable(navInteraction,null){screen=key}.padding(vertical=7.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Icon(icon,label,tint=tint,modifier=Modifier.size(23.dp));Spacer(Modifier.height(3.dp));Text(label,modifier=Modifier.fillMaxWidth().padding(horizontal=4.dp),fontSize=12.sp,lineHeight=16.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center,color=tint,maxLines=1,overflow=TextOverflow.Ellipsis)}
                                }
                            }
                        }
                    }
                }
                if(initialized&&screen=="team"&&selectedId==null&&!showAccount&&!showDummy){
                    LiquidGlass(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end=24.dp,bottom=104.dp).height(56.dp).width(144.dp).semantics{contentDescription="Criar nova equipe"},radius=24.dp,tint=MaterialTheme.colorScheme.surface.copy(.4f),onClick={showCreateTeam=true}){
                        Row(Modifier.align(Alignment.Center),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Rounded.Add,null,Modifier.size(24.dp),tint=accent);Text("Nova equipe",Modifier.padding(start=8.dp),fontSize=14.sp,color=accent)}
                    }
                }
                if(exitBuild){AlertDialog(onDismissRequest={exitBuild=false},title={Text("Alterações não salvas")},text={Text("Seu rascunho está neste aparelho. Salve o set, descarte as alterações ou continue editando.")},confirmButton={TextButton(onClick={catalog.find{it.id==selectedId}?.let{p->pendingBuild?.let{saveSet(p,it,true)}}}){Text("Salvar e sair")}},dismissButton={Row{TextButton(onClick={selectedId?.let{drafts.clear(drafts.key(email,state.activeTeamId,it))};pendingBuild=null;exitBuild=false;selectedId=null}){Text("Descartar")};TextButton(onClick={exitBuild=false}){Text("Continuar")}}})}
                SnackbarHost(snackbar,Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom=100.dp))
            }
            }
        }
    }
}

@Composable private fun FloatingTopBar(title:String,onBack:()->Unit,onFavorite:()->Unit,isFavorite:Boolean){
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(14.dp),verticalAlignment=Alignment.CenterVertically){
        LiquidGlass(Modifier.size(48.dp),radius=24.dp,onClick=onBack){Icon(Icons.AutoMirrored.Rounded.ArrowBack,"Voltar",Modifier.align(Alignment.Center))}
        Spacer(Modifier.width(12.dp));Text(title,Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
        LiquidGlass(Modifier.size(48.dp),radius=24.dp,onClick=onFavorite){Icon(if(isFavorite)Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,"Favoritar Pokémon",Modifier.align(Alignment.Center),tint=MaterialTheme.colorScheme.primary)}
    }
}

@Composable internal fun DexScreen(catalog:List<Pokemon>,state:UserState,favoritesOnly:Boolean,onOpen:(Pokemon)->Unit,onFavorite:(Pokemon)->Unit,onLayout:(String)->Unit,requestedGame:Int?=null,onGameConsumed:()->Unit={}){
    var query by rememberSaveable(favoritesOnly){mutableStateOf("")}
    var selectedGame by rememberSaveable{mutableStateOf(if(favoritesOnly)0 else 25)}
    var generation by rememberSaveable{mutableStateOf(0)}
    var type by rememberSaveable{mutableStateOf<String?>(null)}
    var alphabetical by rememberSaveable{mutableStateOf(false)}
    var filtersExpanded by rememberSaveable{mutableStateOf(false)}
    LaunchedEffect(requestedGame){if(requestedGame!=null){selectedGame=requestedGame;generation=0;type=null;query="";onGameConsumed()}}
    val list=remember(catalog,state.favorites,query,selectedGame,generation,type,alphabetical,favoritesOnly){catalog.filter{(!favoritesOnly||it.id in state.favorites)&&(selectedGame==0||it.availableIn(selectedGame))&&(generation==0||it.generation==generation)&&(type==null||type in it.types)&&it.matchesQuery(query)}.let{if(alphabetical)it.sortedBy{p->p.name} else it.sortedWith(compareBy({p->p.baseId},{p->p.id}))}}
    val scroll=rememberLazyGridState()
    val configuration=androidx.compose.ui.platform.LocalConfiguration.current
    val columns=if(state.layout=="list")1 else if(configuration.screenWidthDp>=600)3 else 2
    LazyVerticalGrid(columns=GridCells.Fixed(columns),state=scroll,contentPadding=PaddingValues(start=18.dp,end=18.dp,top=88.dp,bottom=120.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.fillMaxSize().testTag("pokedex-grid")){
        item(span={GridItemSpan(maxLineSpan)},key="catalogControls"){
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
                OutlinedTextField(value=query,onValueChange={query=it},placeholder={Text("Buscar Pokémon",fontSize=16.sp)},leadingIcon={Icon(Icons.Rounded.Search,null)},trailingIcon={if(query.isNotEmpty())IconButton(onClick={query=""}){Icon(Icons.Rounded.Close,"Limpar busca")}},singleLine=true,shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Text("${list.size} Pokémon e formas",Modifier.weight(1f),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick={filtersExpanded=!filtersExpanded}){Text("Filtros");Icon(Icons.Rounded.ExpandMore,null)}
                    IconButton(onClick={onLayout(if(state.layout=="list")"grid" else "list")}){Icon(if(state.layout=="list")Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.List,if(state.layout=="list")"Mostrar em grade" else "Mostrar em lista")}
                }
                if(!favoritesOnly){Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selectedGame==25,{selectedGame=25},label={Text("Champions")},leadingIcon={Icon(Icons.Rounded.AutoAwesome,null,Modifier.size(16.dp))});FilterChip(selectedGame==0,{selectedGame=0},label={Text("Nacional")});IconButton(onClick={alphabetical=!alphabetical}){Icon(Icons.Rounded.SortByAlpha,"Alternar ordenação alfabética")}}}
                AnimatedVisibility(filtersExpanded){Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
                ChoiceField("JOGO",if(selectedGame==0)"Nacional · todos os jogos" else gameName(selectedGame),listOf("0" to "Nacional · todos os jogos")+SupportedGames.map{it.id.toString() to it.name}){selectedGame=it.toInt()}
                ChoiceField("GERAÇÃO DA ESPÉCIE",if(generation==0)"Todas as gerações" else "Geração $generation",listOf("0" to "Todas as gerações")+(1..9).map{it.toString() to "Geração $it"}){generation=it.toInt()}
                LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    item{FilterChip(type==null,{type=null},label={Text("Todos")})}
                    items(typeNames.keys.toList()){t->FilterChip(type==t,{type=if(type==t)null else t},label={Text(typeNames.getValue(t),fontSize=13.sp)},leadingIcon={TypeSymbol(t)})}
                }
                    TextButton(onClick={generation=0;type=null;selectedGame=25;alphabetical=false}){Text("Limpar filtros")}
                }}

            }
        }
        if(!favoritesOnly&&query.isBlank()&&!filtersExpanded)item(span={GridItemSpan(maxLineSpan)},key="hero"){FeaturedPokemon(catalog.filter{it.id==it.baseId},onOpen)}
        if(list.isEmpty())item(span={GridItemSpan(maxLineSpan)},key="empty"){Text("Nenhum Pokémon encontrado. Limpe a busca ou ajuste os filtros.",Modifier.padding(16.dp),fontSize=15.sp)}
        items(list,key={it.id},contentType={state.layout}){p->PokemonCard(p,state.layout,p.id in state.favorites,{onOpen(p)},{onFavorite(p)})}
        if(list.isEmpty())item(span={GridItemSpan(maxLineSpan)}){EmptyState(if(favoritesOnly)"Seus parceiros favoritos vivem aqui." else "Nenhum Pokémon encontrado.","Experimente outro nome ou filtro.")}
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
    Box(Modifier.fillMaxWidth().heightIn(min=150.dp).clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(typeColor(p.types.first()).copy(.14f),MaterialTheme.colorScheme.surface)))){
        AnimatedContent(p,transitionSpec={(fadeIn(tween(500))+slideInHorizontally(tween(500)){it/8}) togetherWith fadeOut(tween(300))},label="rotating featured Pokémon",modifier=Modifier.fillMaxWidth()){featured->
            Row(Modifier.fillMaxWidth().clickable{onOpen(featured)}.padding(start=18.dp,end=12.dp,top=14.dp,bottom=48.dp),verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){
                    Text("Destaque · #${featured.baseId.toString().padStart(4,'0')}",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(featured.name,fontSize=21.sp,lineHeight=25.sp,fontWeight=FontWeight.SemiBold,maxLines=2,modifier=Modifier.padding(vertical=5.dp))
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp)){featured.types.forEach{TypePill(it,true)}}
                }
                PokemonArt(featured.id,Modifier.size(104.dp),featured.name)
            }
        }
        Row(Modifier.align(Alignment.BottomEnd),verticalAlignment=Alignment.CenterVertically){IconButton(onClick={paused=!paused},modifier=Modifier.size(48.dp)){Icon(if(paused)Icons.Rounded.PlayArrow else Icons.Rounded.Pause,if(paused)"Retomar destaque" else "Pausar destaque",Modifier.size(22.dp))};IconButton(onClick={next()},modifier=Modifier.size(48.dp)){Icon(Icons.AutoMirrored.Rounded.ArrowForward,"Próximo destaque",Modifier.size(22.dp))}}
    }

}

@Composable private fun PokemonCard(p:Pokemon,layout:String,favorite:Boolean,onOpen:()->Unit,onFavorite:()->Unit){
    val interaction=remember{MutableInteractionSource()};val pressed by interaction.collectIsPressedAsState();val scale by animateFloatAsState(if(pressed).975f else 1f,spring(.8f,600f),label="card touch")
    val shape=RoundedCornerShape(25.dp)
    Box(Modifier.fillMaxWidth().graphicsLayer{scaleX=scale;scaleY=scale}.clip(shape).background(Brush.linearGradient(listOf(typeColor(p.types.first()).copy(.075f),MaterialTheme.colorScheme.surface))).border(1.dp,Color.White.copy(.07f),shape)){
        if(layout=="list"){
            Row(Modifier.fillMaxWidth().clickable(interaction,null,onClick=onOpen).padding(12.dp).padding(end=28.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                PokemonArt(p.id,Modifier.size(76.dp),p.name)
                Column(Modifier.weight(1f)){Text("#${p.baseId.toString().padStart(4,'0')}",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(p.name,fontSize=16.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(vertical=5.dp));Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){p.types.forEach{TypePill(it,true)}};if(!p.champions)Text("Compatibilidade por verificar",fontSize=13.sp,color=Color(0xFFE6BE99),modifier=Modifier.padding(top=5.dp))}
            }
        }else{
            // Reserve the same two type rows for every card. FlowRow made card height
            // depend on label width, font scale and whether a Pokémon had one or two types.
            val typeRowHeight=with(LocalDensity.current){maxOf(17.dp,16.sp.toDp())+12.dp}
            Column(Modifier.fillMaxWidth().clickable(interaction,null,onClick=onOpen).padding(12.dp)){
                Row(Modifier.padding(top=4.dp,end=28.dp).heightIn(min=24.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    Text("#${p.baseId.toString().padStart(4,'0')}",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    if(!p.champions)Icon(Icons.Rounded.Info,"Compatibilidade com Champions por verificar",Modifier.size(16.dp),tint=MaterialTheme.colorScheme.primary)
                }
                PokemonArt(p.id,Modifier.fillMaxWidth().heightIn(max=116.dp).aspectRatio(1.35f),p.name)
                Text(p.name,fontSize=15.sp,lineHeight=20.sp,fontWeight=FontWeight.SemiBold,minLines=2,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=4.dp,bottom=8.dp))
                Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
                    repeat(2){slot->
                        Box(Modifier.fillMaxWidth().height(typeRowHeight),contentAlignment=Alignment.CenterStart){
                            p.types.getOrNull(slot)?.let{TypePill(it,true)}
                        }
                    }
                }
            }
        }
        IconButton(onClick=onFavorite,modifier=Modifier.align(Alignment.TopEnd).size(48.dp)){Icon(if(favorite)Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,"Favoritar ${p.name}",tint=if(favorite)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(19.dp))}
    }
}

@Composable private fun PokemonScreen(p:Pokemon,repo:PokemonRepository,state:UserState,tab:String,onTab:(String)->Unit,onFavorite:()->Unit,onTeam:()->Unit,onBuild:(Build)->Unit,draftKey:String,onDraft:(Build?)->Unit){
    var moves by remember(p.id){mutableStateOf<List<Move>>(emptyList())}
    var heldItems by remember(p.id){mutableStateOf<List<HeldItem>>(emptyList())}
    var loaded by remember(p.id){mutableStateOf(false)}
    var query by rememberSaveable(p.id){mutableStateOf("")}
    LaunchedEffect(p.id,state.activeTeam.gameId){loaded=false;moves=repo.movesFor(p,state.activeTeam.gameId);heldItems=repo.heldItemsFor(p,state.activeTeam.gameId);loaded=true}
    val detailScroll=rememberLazyListState()
    LazyColumn(Modifier.fillMaxSize(),state=detailScroll,contentPadding=PaddingValues(top=80.dp,bottom=110.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        if(tab!="build")item(key="portrait"){Box(Modifier.fillMaxWidth().height(268.dp).background(Brush.radialGradient(listOf(typeColor(p.types.first()).copy(.16f),Color.Transparent)))){PokemonArt(p.id,Modifier.fillMaxSize().padding(18.dp),p.name)}}
        item(key="identity"){Column(Modifier.padding(horizontal=24.dp)){
            Text("#${p.baseId.toString().padStart(4,'0')}  /  ${if(p.isMega)"MEGA EVOLUÇÃO" else "POKÉDEX"}",fontSize=13.sp,letterSpacing=1.8.sp,color=MaterialTheme.colorScheme.primary)
            Text(p.name,fontSize=if(tab=="build")24.sp else 30.sp,fontWeight=FontWeight.Bold,lineHeight=34.sp,letterSpacing=(-1).sp,modifier=Modifier.padding(top=8.dp,bottom=12.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){p.types.forEach{TypePill(it)}}
            Text(when(p.championsSource){"official-regulation"->"Espécie/forma na lista oficial M-C · verificada em 28/10/2026.";"showdown-form"->"Forma de batalha ou variante · referência Champions do Showdown.";else->"Compatibilidade desta forma com Champions ainda por verificar."},fontSize=13.sp,color=if(p.champions)MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFE6BE99),modifier=Modifier.padding(top=10.dp))
        }}
        item(key="detail-tabs"){Row(Modifier.padding(horizontal=22.dp).fillMaxWidth().background(MaterialTheme.colorScheme.surface,RoundedCornerShape(20.dp)).padding(5.dp)){
            listOf("info" to "Informações","moves" to "Golpes","build" to "Seu set").forEach{(key,label)->Box(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if(key==tab)MaterialTheme.colorScheme.primary.copy(.16f) else Color.Transparent).clickable{onTab(key)}.heightIn(min=48.dp).padding(vertical=13.dp),contentAlignment=Alignment.Center){Text(label,fontSize=13.sp,color=if(key==tab)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,fontWeight=FontWeight.SemiBold)}}
        }}
        if(tab=="info"){
            item{Column(Modifier.padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)){InfoTile("ALTURA",p.height?.takeIf{it>0}?.let{"$it m"}?:"Não informado",Modifier.weight(1f));InfoTile("PESO",p.weight?.takeIf{it>0}?.let{"$it kg"}?:"Não informado",Modifier.weight(1f))}
                SectionLabel("ATRIBUTOS BASE")
                p.stats.forEachIndexed{i,v->Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){Text(listOf("HP","Ataque","Defesa","Atq. especial","Def. especial","Velocidade")[i],fontSize=13.sp,modifier=Modifier.width(94.dp),color=MaterialTheme.colorScheme.onSurfaceVariant);Text("$v",fontSize=13.sp,modifier=Modifier.width(28.dp),fontWeight=FontWeight.SemiBold);LinearProgressIndicator(progress={v/255f},modifier=Modifier.weight(1f).height(6.dp).clip(CircleShape),color=MaterialTheme.colorScheme.primary,trackColor=MaterialTheme.colorScheme.surfaceVariant)}}
                SectionLabel("HABILIDADES GERAIS")
                p.abilities.forEach{Text(it.replace('-',' '),fontSize=15.sp)}
                Text("A lista oficial identifica espécies/formas elegíveis no regulamento. Megas e transformações usam a referência de batalha; não confunda ausência da página com ausência no jogo.",fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                ActionButton(if(p.id in state.team)"Remover da equipe" else "Adicionar à equipe",if(p.id in state.team)Icons.Rounded.Remove else Icons.Rounded.Add,p.availableIn(state.activeTeam.gameId)||p.id in state.team,onTeam)
            }}
        }else if(tab=="moves"){
            item{Column(Modifier.padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){SectionLabel("GOLPES · ${gameName(state.activeTeam.gameId)}");Text("${moves.size} golpes · ${gameName(state.activeTeam.gameId)}",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);OutlinedTextField(query,{query=it},leadingIcon={Icon(Icons.Rounded.Search,null)},placeholder={Text("Buscar golpe")},singleLine=true,shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth());if(!loaded)LinearProgressIndicator(Modifier.fillMaxWidth())}}
            items(moves.filter{it.name.contains(query,true)},key={it.id}){move->MoveRow(move,Modifier.padding(horizontal=24.dp))}
            if(loaded&&moves.isEmpty())item{EmptyState("Sem golpes registrados para este jogo.","Este Pokémon ainda não tem compatibilidade confirmada nesta base.")}
            item{Text(if(state.activeTeam.gameId==25)"Atributos e aprendizados de Champions: fonte Showdown, consulta de 28/10/2026. PP básico; não é a tabela de outros jogos." else "Poder, precisão e PP da referência geral; diferenças históricas por versão ainda precisam de validação.",Modifier.padding(horizontal=24.dp),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
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
            Column(Modifier.weight(1f)){Text("Seu set",fontSize=20.sp,fontWeight=FontWeight.SemiBold);Text(if(changed)"Rascunho no aparelho · não aplicado ao cálculo" else "Sem alterações pendentes",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            FilledTonalButton(onClick={onSave(editing)},enabled=inTeam&&sps.sum()<=66&&changed){Text("Salvar")}
        }
        if(!inTeam){Text("Adicione este parceiro à sua equipe para montar o set.",fontSize=14.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);ActionButton("Adicionar à equipe",Icons.Rounded.Add,p.availableIn(game),onTeam)}
        if(game==25)TrainingEditor(p,nature,ability,sps,{nature=it},{ability=it},{sps=it}) else Text("Equipe de planejamento: treinamento e itens específicos deste jogo ainda não estão incluídos. Os golpes abaixo são filtrados pelo jogo.",fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if(game==25)"Item segurado" else "Itens · em preparação",fontSize=17.sp,fontWeight=FontWeight.SemiBold)
        Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface,onClick={if(game==25)picking=if(picking=="items")"" else "items"}){
            Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Rounded.Inventory2,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(24.dp));Column(Modifier.weight(1f).padding(horizontal=14.dp)){Text(items.find{it.id==selectedItem}?.name?:if(selectedItem!=null)"Item indisponível · selecione outro" else if(p.isMega&&game==25)"Escolha a Mega Stone" else "Sem item",fontSize=14.sp);Text("Toque para escolher",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.Rounded.ExpandMore,"Escolher item")}
        }
        AnimatedVisibility(picking=="items",enter=expandVertically(tween(240))+fadeIn(),exit=shrinkVertically(tween(180))+fadeOut()){
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedTextField(searchItem,{searchItem=it},singleLine=true,placeholder={Text("Buscar item segurado")},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp))
                if(!p.isMega||game!=25)TextButton(onClick={selectedItem=null;picking=""}){Text("Usar sem item")}
                LazyColumn(Modifier.heightIn(max=300.dp)){items(items.filter{it.name.contains(searchItem,true)},key={it.id}){item->Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable{selectedItem=item.id;picking=""}.padding(12.dp),verticalAlignment=Alignment.CenterVertically){RadioButton(selectedItem==item.id,null);Column(Modifier.padding(start=10.dp)){Text(item.name,fontSize=14.sp);Text(item.description,fontSize=13.sp,lineHeight=16.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=3,overflow=TextOverflow.Ellipsis)}}}}
            }
        }
        Text("Golpes  ${selectedMoves.size}/4",fontSize=17.sp,fontWeight=FontWeight.SemiBold)
        selectedMoves.forEach{id->val move=moves.find{it.id==id};Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface,RoundedCornerShape(18.dp)).padding(start=14.dp)){
            if(move!=null)TypeSymbol(move.type)
            Text(move?.name?:"Golpe indisponível · #$id",Modifier.weight(1f).padding(12.dp),fontSize=14.sp,color=if(move==null)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            IconButton(onClick={selectedMoves=selectedMoves-id}){Icon(Icons.Rounded.Close,"Remover ${move?.name?:"golpe antigo"}",Modifier.size(20.dp))}
        }}
        OutlinedButton(onClick={picking=if(picking=="moves")"" else "moves"},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Icon(Icons.Rounded.Add,null,Modifier.size(18.dp));Text("Escolher golpes",Modifier.padding(8.dp))}
        AnimatedVisibility(picking=="moves",enter=expandVertically(tween(240))+fadeIn(),exit=shrinkVertically(tween(180))+fadeOut()){
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedTextField(searchMove,{searchMove=it},singleLine=true,placeholder={Text("Buscar golpe do jogo")},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp))
                LazyColumn(Modifier.heightIn(max=320.dp)){items(moves.filter{it.name.contains(searchMove,true)},key={it.id}){move->Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(enabled=move.id in selectedMoves||selectedMoves.size<4){selectedMoves=if(move.id in selectedMoves)selectedMoves-move.id else selectedMoves+move.id}.padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){Checkbox(move.id in selectedMoves,null,enabled=move.id in selectedMoves||selectedMoves.size<4);TypeSymbol(move.type);Text(move.name,Modifier.padding(start=9.dp),fontSize=14.sp)}}}
            }
        }
        Text("Selecione até quatro golpes registrados para ${gameName(game)}. Confira o regulamento antes de competir.",fontSize=13.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        ActionButton("Salvar set",Icons.Rounded.Check,inTeam&&sps.sum()<=66){onSave(Build(selectedItem,selectedMoves,nature,ability,sps))}
    }
}

@Composable private fun TeamScreen(catalog:List<Pokemon>,repo:PokemonRepository,state:UserState,onOpen:(Pokemon)->Unit,onAdd:()->Unit,onCalculate:()->Unit,onState:(UserState)->Unit,creating:Boolean,onCreatingChange:(Boolean)->Unit){
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(start=22.dp,end=22.dp,top=88.dp,bottom=120.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        item(key="library"){TeamLibraryHeader(state,onState)}
        item(key="battle-format"){Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){FilterChip(state.activeTeam.format=="singles",{onState(state.updateTeam{it.copy(format="singles")})},label={Text("Singles")});FilterChip(state.activeTeam.format=="doubles",{onState(state.updateTeam{it.copy(format="doubles")})},label={Text("Doubles")})}}
        items(state.team,key={it}){id->catalog.find{it.id==id}?.let{p->val build=state.builds[id];var review by remember(id,build,state.activeTeam.gameId){mutableStateOf<String?>(null)};var itemName by remember(id,build,state.activeTeam.gameId){mutableStateOf<String?>(null)};var moveNames by remember(id,build,state.activeTeam.gameId){mutableStateOf<List<Move>>(emptyList())};LaunchedEffect(id,build,state.activeTeam.gameId){review=if(!p.availableIn(state.activeTeam.gameId))"Compatibilidade por revisar" else build?.let{repo.validateBuild(p,it,state.activeTeam.gameId).exceptionOrNull()?.message};itemName=repo.heldItemsFor(p,state.activeTeam.gameId).find{it.id==build?.itemId}?.name;moveNames=repo.movesFor(p,state.activeTeam.gameId).filter{it.id in (build?.moves?:emptyList())}};Surface(onClick={onOpen(p)},shape=RoundedCornerShape(26.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().padding(16.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){PokemonArt(id,Modifier.size(86.dp),p.name);Column(Modifier.weight(1f).padding(start=10.dp)){Text(p.name,fontSize=19.sp,fontWeight=FontWeight.SemiBold);Row(horizontalArrangement=Arrangement.spacedBy(5.dp),modifier=Modifier.padding(top=9.dp)){p.types.forEach{TypePill(it,true)}}};Icon(Icons.Rounded.Edit,"Editar set",tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(20.dp))}
            review?.let{Text("Revisar set: $it",fontSize=13.sp,lineHeight=16.sp,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(top=10.dp))}
            HorizontalDivider(Modifier.padding(vertical=13.dp),color=MaterialTheme.colorScheme.onSurface.copy(.07f));Text(if(build==null)"Toque para escolher item e golpes" else itemName?:"Sem item",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);if(moveNames.isNotEmpty()){FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(9.dp),modifier=Modifier.padding(top=14.dp)){moveNames.forEach{move->Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){TypeSymbol(move.type,Modifier.size(14.dp));Text(move.name,fontSize=13.sp)}}}}
        }}}}
        if(state.team.isNotEmpty()&&state.activeTeam.gameId==25)item{ActionButton("Simular dano com estes sets",Icons.Rounded.AutoAwesome,true,onCalculate)}
        if(state.team.size<6)item{ActionButton("Escolher parceiro",Icons.Rounded.Add,true,onAdd)}
        item{Text("Equipe: ${state.activeTeam.name} · ${gameName(state.activeTeam.gameId)}. Confira as regras do jogo, formato e temporada antes de competir.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
    if(creating)NewTeamDialog(state,onState,onDismiss={onCreatingChange(false)})
}

@Composable private fun AccountScreen(store:Store,writeMutex:Mutex,email:String?,theme:String,onBack:()->Unit,onTheme:(String)->Unit,onAccountChanged:suspend ()->Unit){
    val scope=rememberCoroutineScope();var mail by remember{mutableStateOf("")};var password by remember{mutableStateOf("")};var message by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        IconButton(onClick=onBack,enabled=!busy){Icon(Icons.AutoMirrored.Rounded.ArrowBack,"Voltar")}
        Text("Seu universo.\nDo seu jeito.",fontSize=32.sp,lineHeight=36.sp,fontWeight=FontWeight.Bold)
        SectionLabel("APARÊNCIA")
        ThemePicker(theme){if(!busy)onTheme(it)}
        SectionLabel("CONTA NESTE APARELHO")
        if(email!=null){Text(email,fontSize=16.sp);Text("Equipe, sets, favoritos e tema protegidos no armazenamento do aplicativo.",fontSize=13.sp,lineHeight=20.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);OutlinedButton(enabled=!busy,onClick={busy=true;scope.launch{writeMutex.withLock{withContext(Dispatchers.IO){store.logout()};onAccountChanged()};busy=false}}){Text("Sair da conta")}}
        else{
            Text("Use como visitante ou crie uma conta local para separar seus dados.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(mail,{mail=it},label={Text("E-mail")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth())
            OutlinedTextField(password,{password=it},label={Text("Senha")},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth())
            fun auth(register:Boolean){busy=true;scope.launch{writeMutex.withLock{val result=if(register)store.register(mail,password) else store.login(mail,password);password="";result.onSuccess{message="Conta aberta neste aparelho.";onAccountChanged()}.onFailure{message=it.message?:"Não foi possível entrar."}};busy=false}}
            ActionButton(if(busy)"Aguarde…" else "Entrar",Icons.Rounded.Login,!busy){auth(false)}
            OutlinedButton(onClick={auth(true)},enabled=!busy,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Text("Criar conta local",Modifier.padding(8.dp))}
        }
        if(message.isNotEmpty())Text(message,fontSize=13.sp,color=MaterialTheme.colorScheme.primary)
        Surface(color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){Icon(Icons.Rounded.OfflineBolt,null,tint=MaterialTheme.colorScheme.primary);Text("Pronto para ficar offline",fontWeight=FontWeight.SemiBold)};Text("Catálogo, artes e golpes já vêm no aplicativo. Equipe e favoritos funcionam sem internet.",fontSize=13.sp,lineHeight=20.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
        Text("Nesta versão, a conta existe apenas neste aparelho. Não há sincronização em nuvem, confirmação de e-mail ou recuperação de senha. Essas integrações ainda precisam ser implementadas.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun SourcesScreen(catalog:List<Pokemon>){
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(start=24.dp,end=24.dp,top=88.dp,bottom=120.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){
        item{Text("Central\nChampions.",fontSize=34.sp,fontWeight=FontWeight.Bold,lineHeight=38.sp)}
        item(key="catalog-audit"){Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text("Nacional × Champions",fontSize=20.sp,fontWeight=FontWeight.SemiBold)
            Text("${catalog.map{it.baseId}.distinct().size} espécies · ${catalog.size} registros nacionais",fontSize=13.sp)
            Text("${catalog.filter{it.champions}.map{it.baseId}.distinct().size} espécies · ${catalog.count{it.champions}} registros Champions",fontSize=13.sp,color=MaterialTheme.colorScheme.primary)
            Text("Registros incluem formas e Mega Evoluções. Presença na base, compatibilidade no jogo e elegibilidade da temporada são verificações diferentes.",fontSize=13.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            for(gen in 1..9){val rows=catalog.filter{it.generation==gen};Row(Modifier.fillMaxWidth()){Text("Geração $gen",Modifier.weight(1f),fontSize=13.sp);Text("${rows.count{it.champions}} / ${rows.size} registros",fontSize=13.sp)}}
        }}}
        item{Text("Conheça sua equipe. Respeite o regulamento.",fontSize=14.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        listOf("Compatibilidade por jogo" to "A seleção Champions combina a lista oficial M-C com formas de batalha e variantes verificadas na referência Showdown. A página oficial não enumera todas as Megas. A ausência de uma forma em uma fonte não comprova que ela inexista no jogo.","Construa com clareza" to "Golpes, PP e itens usam os dados específicos de Champions; Mega Stones verificam a forma correta. A M-C proíbe itens repetidos nas ranqueadas. Os sets preservados de versões anteriores podem pedir revisão.","Seus dados, offline" to "Todas as artes e dados necessários à consulta estão incluídos no APK. Você não precisa baixar um pacote após instalar.","Verificação contínua" to "Consulte as notícias, regras e atualizações oficiais de Pokémon Champions antes de competir. Esta versão é um projeto de fã em desenvolvimento.").forEach{(title,body)->item{Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(title,fontSize=18.sp,fontWeight=FontWeight.SemiBold);Text(body,fontSize=13.sp,lineHeight=21.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
    }
}

@Composable internal fun ActionButton(text:String,icon:ImageVector,enabled:Boolean=true,onClick:()->Unit){
    val interaction=remember{MutableInteractionSource()};val pressed by interaction.collectIsPressedAsState();val scale by animateFloatAsState(if(pressed).96f else 1f,spring(.75f,500f),label="action touch")
    val shape=RoundedCornerShape(20.dp);val accent=MaterialTheme.colorScheme.primary
    Row(Modifier.fillMaxWidth().graphicsLayer{scaleX=scale;scaleY=scale;alpha=if(enabled)1f else .4f}.clip(shape).background(Brush.linearGradient(listOf(accent.copy(.30f),accent.copy(.09f),accent.copy(.18f)))).border(1.dp,Brush.linearGradient(listOf(Color.White.copy(.5f),accent.copy(.15f),Color.White.copy(.25f))),shape).clickable(interaction,null,enabled=enabled,onClick=onClick).heightIn(min=54.dp).padding(horizontal=18.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){Icon(icon,null,Modifier.size(20.dp),tint=accent);Spacer(Modifier.width(10.dp));Text(text,fontSize=14.sp,fontWeight=FontWeight.SemiBold)}
}
@Composable private fun InfoTile(label:String,value:String,modifier:Modifier){Column(modifier.background(MaterialTheme.colorScheme.surface,RoundedCornerShape(20.dp)).padding(18.dp)){Text(label,fontSize=13.sp,letterSpacing=1.6.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(value,fontSize=17.sp,fontWeight=FontWeight.Medium,modifier=Modifier.padding(top=7.dp))}}
@Composable private fun SectionLabel(label:String){Text(label,fontSize=13.sp,letterSpacing=1.7.sp,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(top=8.dp))}
@Composable private fun EmptyState(title:String,subtitle:String){Column(Modifier.fillMaxWidth().padding(vertical=40.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Rounded.CatchingPokemon,null,Modifier.size(44.dp),tint=MaterialTheme.colorScheme.primary.copy(.5f));Text(title,fontSize=16.sp,modifier=Modifier.padding(top=18.dp));Text(subtitle,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=8.dp))}}
@Composable private fun MoveRow(move:Move,modifier:Modifier=Modifier){Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface,RoundedCornerShape(20.dp)).padding(17.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Row(verticalAlignment=Alignment.CenterVertically){TypeSymbol(move.type,Modifier.size(20.dp));Text(move.name,Modifier.weight(1f).padding(start=10.dp),fontSize=16.sp,fontWeight=FontWeight.SemiBold);TypePill(move.type,true)};Row(horizontalArrangement=Arrangement.spacedBy(23.dp)){listOf("PODER" to (move.power?.toString()?:"—"),"PRECISÃO" to (move.accuracy?.toString()?:"—"),"PP" to (move.pp?.toString()?:"—"),"CLASSE" to when(move.category){"physical"->"Físico";"special"->"Especial";else->"Status"}).forEach{(label,value)->Column{Text(label,fontSize=12.sp,letterSpacing=.8.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(value,fontSize=13.sp,modifier=Modifier.padding(top=4.dp))}}}}
}
