package org.openrun.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.first
import org.openrun.app.R
import org.openrun.app.ui.home.HomeScreen
import org.openrun.app.ui.program.ParcoursScreen
import org.openrun.app.ui.program.ProgramScreen
import org.openrun.app.ui.history.HistoryScreen
import org.openrun.app.ui.history.RunDetailScreen
import org.openrun.app.ui.run.RunRoute
import org.openrun.app.ui.run.SummaryScreen
import org.openrun.app.ui.settings.SettingsScreen
import org.openrun.app.ui.start.StartScreen
import org.openrun.core.Programs

private object Routes {
    const val HOME = "home"
    const val PARCOURS = "parcours"
    const val HISTORY = "history"
    const val START = "start?first={first}"
    const val PROGRAM = "program/{id}"
    const val SETTINGS = "settings"
    const val RUN = "run?programId={programId}&index={index}&skipWarmup={skipWarmup}&skipCooldown={skipCooldown}"

    const val SUMMARY = "summary/{runId}"
    const val DETAIL = "detail/{runId}"

    fun detail(runId: Long) = "detail/$runId"
    fun summary(runId: Long) = "summary/$runId"
    fun start(first: Boolean) = "start?first=$first"
    fun program(id: String) = "program/$id"
    fun run(programId: String?, index: Int, sw: Boolean, sc: Boolean) =
        "run?programId=${programId ?: ""}&index=$index&skipWarmup=$sw&skipCooldown=$sc"
}

private val ScreenIn = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
private val ScreenOut = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160))

private data class Tab(val route: String, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, R.string.tab_home, Icons.Rounded.Home),
    Tab(Routes.PARCOURS, R.string.tab_parcours, Icons.Rounded.Route),
    Tab(Routes.HISTORY, R.string.tab_history, Icons.Rounded.Insights),
)

@Composable
fun AppNav() {
    val app = LocalContext.current.openRun
    // The first screen depends on whether the question "where are you?" was answered; wait to know.
    val onboarded by produceState<Boolean?>(null) { value = app.settings.settings.first().onboarded }
    val ready = onboarded ?: return Box(Modifier.fillMaxSize())

    val nav = rememberNavController()
    // highlights asked per tab: one for the first visit of the launch, one more each time the open tab is tapped again
    val asked = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateMapOf(Routes.HOME to 1, Routes.PARCOURS to 1) }
    val played = androidx.compose.runtime.remember { mutableMapOf<String, Int>() }
    fun highlight(route: String) = org.openrun.app.ui.components.Highlight(asked[route] ?: 0) { id ->
        if ((played[route] ?: 0) < id) { played[route] = id; true } else false
    }
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    // Accueil, Parcours and Historique are pages of one screen: swipe between them; a tap on a tab fades to it
    val tabsPager = rememberPagerState { tabs.size }
    val tabsFade = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    // the run screen takes the color of the phase edge to edge, under the status and navigation bars too
    val live by org.openrun.app.run.RunSession.state.collectAsState()
    val onRun = route?.startsWith("run") == true
    val behind by androidx.compose.animation.animateColorAsState(
        if (onRun) org.openrun.app.ui.run.runScreenColor(live) else androidx.compose.material3.MaterialTheme.colorScheme.background,
        androidx.compose.animation.core.tween(400), label = "behind",
    )
    Scaffold(
        containerColor = behind,
        bottomBar = {
            if (route == Routes.HOME) {
                // white bar with a hairline on top, mint pill under the selected tab (mockup v2)
                androidx.compose.foundation.layout.Column {
                androidx.compose.material3.HorizontalDivider(color = org.openrun.app.ui.components.LineColor)
                NavigationBar(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLowest) {
                    tabs.forEachIndexed { i, tab ->
                        val selected = tabsPager.currentPage == i
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                // tapping the open tab again does not reload it: it shows the thing to do there
                                if (selected) asked[tab.route] = (asked[tab.route] ?: 0) + 1
                                else scope.launch {
                                    tabsFade.snapTo(0f)
                                    tabsPager.scrollToPage(i)
                                    tabsFade.animateTo(1f, androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
                                }
                            },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(stringResource(tab.label), fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else null) },
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
                }
            }
        },
    ) { padding ->
        // a short, quick cross-fade between screens (the default one is slow)
        NavHost(
            nav, startDestination = if (ready) Routes.HOME else Routes.START, modifier = Modifier.padding(padding),
            enterTransition = { ScreenIn }, exitTransition = { ScreenOut }, popEnterTransition = { ScreenIn }, popExitTransition = { ScreenOut },
        ) {
            composable(Routes.HOME) {
                HorizontalPager(tabsPager, Modifier.fillMaxSize().graphicsLayer { alpha = tabsFade.value }, verticalAlignment = Alignment.Top) { page ->
                    when (tabs[page].route) {
                        Routes.HOME -> {
                            val vm: HomeViewModel = viewModel(factory = factory(app) { HomeViewModel(it) })
                            val state by vm.state.collectAsStateWithLifecycle()
                            val active by org.openrun.app.run.RunSession.state.collectAsStateWithLifecycle()
                            val interrupted by vm.interrupted.collectAsStateWithLifecycle()
                            HomeScreen(
                                active = active,
                                onResume = { active?.let { nav.navigate(Routes.run(it.programId, it.sessionIndex ?: 0, false, false)) } },
                                interrupted = interrupted,
                                onSaveInterrupted = { vm.saveInterrupted { id -> nav.navigate(Routes.summary(id)) } },
                                onDiscardInterrupted = vm::discardInterrupted,
                                state = state,
                                onStart = { id, i, sw, sc -> nav.navigate(Routes.run(id, i, sw, sc)) },
                                onOtherSession = { nav.navigate(Routes.program(it)) },
                                onChooseStart = { nav.navigate(Routes.start(false)) },
                                onFreeRun = { nav.navigate(Routes.run(null, 0, false, false)) },
                                onSettings = { nav.navigate(Routes.SETTINGS) },
                                highlight = highlight(Routes.HOME),
                            )
                        }
                        Routes.PARCOURS -> {
                            val vm: ParcoursViewModel = viewModel(factory = factory(app) { ParcoursViewModel(it) })
                            val progress by vm.state.collectAsStateWithLifecycle()
                            val active by vm.activeProgramId.collectAsStateWithLifecycle()
                            ParcoursScreen(progress, active, onProgram = { nav.navigate(Routes.program(it)) }, onWhereAreYou = { nav.navigate(Routes.start(false)) }, highlight = highlight(Routes.PARCOURS))
                        }
                        else -> {
                            val vm: HistoryViewModel = viewModel(factory = factory(app) { HistoryViewModel(it) })
                            val items by vm.items.collectAsStateWithLifecycle()
                            HistoryScreen(items, onOpen = { nav.navigate(Routes.detail(it)) })
                        }
                    }
                }
            }
            composable(Routes.DETAIL, arguments = listOf(navArgument("runId") { type = NavType.LongType })) { entry ->
                RunDetailScreen(entry.arguments?.getLong("runId") ?: 0L, onBack = { nav.popBackStack() })
            }
            composable(
                Routes.START,
                // Opened with no argument on a first launch; every other way in passes first=false.
                arguments = listOf(navArgument("first") { type = NavType.BoolType; defaultValue = true }),
            ) { entry ->
                val first = entry.arguments?.getBoolean("first") ?: true
                val vm: StartViewModel = viewModel(factory = factory(app) { StartViewModel(it) })
                val current by vm.current.collectAsStateWithLifecycle()
                StartScreen(
                    currentProgramId = current.activeProgramId, onboarded = current.onboarded,
                    showBack = !first, onBack = { nav.popBackStack() },
                    onChoose = { id ->
                        vm.choose(id) {
                            if (first) nav.navigate(Routes.HOME) { popUpTo(0) } else nav.popBackStack(Routes.HOME, inclusive = false)
                        }
                    },
                )
            }
            composable(Routes.PROGRAM, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("id") ?: Programs.C25K
                val vm: ProgramViewModel = viewModel(key = id, factory = factory(app) { ProgramViewModel(it, id) })
                ProgramScreen(vm, onBack = { nav.popBackStack() }, onStart = { pid, i, sw, sc -> nav.navigate(Routes.run(pid, i, sw, sc)) })
            }
            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(factory = factory(app) { SettingsViewModel(it) })
                SettingsScreen(vm, onBack = { nav.popBackStack() })
            }
            composable(
                Routes.RUN,
                arguments = listOf(
                    navArgument("programId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("index") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("skipWarmup") { type = NavType.BoolType; defaultValue = false },
                    navArgument("skipCooldown") { type = NavType.BoolType; defaultValue = false },
                ),
            ) { entry ->
                val a = entry.arguments
                RunRoute(
                    programId = a?.getString("programId")?.takeIf { it.isNotEmpty() },
                    index = a?.getInt("index") ?: 0,
                    skipWarmup = a?.getBoolean("skipWarmup") ?: false,
                    skipCooldown = a?.getBoolean("skipCooldown") ?: false,
                    onSaved = { id -> nav.navigate(Routes.summary(id)) { popUpTo(Routes.HOME) } },
                    onClose = { nav.popBackStack(Routes.HOME, inclusive = false) },
                )
            }
            composable(Routes.SUMMARY, arguments = listOf(navArgument("runId") { type = NavType.LongType })) { entry ->
                SummaryScreen(entry.arguments?.getLong("runId") ?: 0L, onDone = { nav.popBackStack(Routes.HOME, inclusive = false) })
            }
        }
    }
}
