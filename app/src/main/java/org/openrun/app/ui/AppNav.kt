package org.openrun.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

    fun summary(runId: Long) = "summary/$runId"
    fun start(first: Boolean) = "start?first=$first"
    fun program(id: String) = "program/$id"
    fun run(programId: String?, index: Int, sw: Boolean, sc: Boolean) =
        "run?programId=${programId ?: ""}&index=$index&skipWarmup=$sw&skipCooldown=$sc"
}

private data class Tab(val route: String, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, R.string.tab_home, Icons.Filled.Home),
    Tab(Routes.PARCOURS, R.string.tab_parcours, Icons.Filled.Route),
    Tab(Routes.HISTORY, R.string.tab_history, Icons.Filled.Timeline),
)

@Composable
fun AppNav() {
    val app = LocalContext.current.openRun
    // The first screen depends on whether the question "where are you?" was answered; wait to know.
    val onboarded by produceState<Boolean?>(null) { value = app.settings.settings.first().onboarded }
    val ready = onboarded ?: return Box(Modifier.fillMaxSize())

    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        bottomBar = {
            if (route in tabs.map { it.route }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.navigate(tab.route) { popUpTo(Routes.HOME); launchSingleTop = true } },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = if (ready) Routes.HOME else Routes.START, modifier = Modifier.padding(padding)) {
            composable(Routes.HOME) {
                val vm: HomeViewModel = viewModel(factory = factory(app) { HomeViewModel(it) })
                val state by vm.state.collectAsStateWithLifecycle()
                HomeScreen(
                    state = state,
                    onStart = { id, i, sw, sc -> nav.navigate(Routes.run(id, i, sw, sc)) },
                    onOtherSession = { nav.navigate(Routes.program(it)) },
                    onChooseStart = { nav.navigate(Routes.start(false)) },
                    onFreeRun = { nav.navigate(Routes.run(null, 0, false, false)) },
                    onSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.PARCOURS) {
                val vm: ParcoursViewModel = viewModel(factory = factory(app) { ParcoursViewModel(it) })
                val progress by vm.state.collectAsStateWithLifecycle()
                val active by vm.activeProgramId.collectAsStateWithLifecycle()
                ParcoursScreen(progress, active, onProgram = { nav.navigate(Routes.program(it)) }, onWhereAreYou = { nav.navigate(Routes.start(false)) })
            }
            composable(Routes.HISTORY) { HistoryPlaceholder() }
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
