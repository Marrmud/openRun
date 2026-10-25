package org.openrun.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.openrun.app.OpenRunApp
import org.openrun.core.AppSettings
import org.openrun.core.JumpPlan
import org.openrun.core.Program
import org.openrun.core.ProgramProgress
import org.openrun.core.Programs
import org.openrun.core.Rank
import org.openrun.core.Ranks
import org.openrun.core.Weeks
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

val Context.openRun: OpenRunApp get() = applicationContext as OpenRunApp

/** The type must be spelled out for the initializer, hence the reified parameter: a bare ViewModel would match none. */
inline fun <reified VM : ViewModel> factory(app: OpenRunApp, crossinline create: (OpenRunApp) -> VM): ViewModelProvider.Factory =
    viewModelFactory { initializer<VM> { create(app) } }

private fun <T> Flow<T>.hot(scope: kotlinx.coroutines.CoroutineScope, initial: T): StateFlow<T> =
    stateIn(scope, SharingStarted.WhileSubscribed(5_000), initial)

// ---------- Home ----------

data class WeekDay(val date: LocalDate, val done: Boolean, val today: Boolean)

data class HomeState(
    val settings: AppSettings,
    val rank: Rank,
    val streak: Int,
    val runsThisWeek: Int,
    val week: List<WeekDay>,
    val program: Program?,
    val progress: ProgramProgress?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val app: OpenRunApp) : ViewModel() {
    private val zone = ZoneId.systemDefault()

    private val activeProgress: Flow<Pair<Program, ProgramProgress>?> =
        app.settings.settings.map { it.activeProgramId }.distinctUntilChanged().flatMapLatest { id ->
            if (id == null) flowOf(null)
            else Programs.all.firstOrNull { it.id == id }?.let { p -> app.runs.progress(p).map { p to it } } ?: flowOf(null)
        }

    val state: StateFlow<HomeState?> = combine(
        app.settings.settings, app.runs.totalKcal(), app.runs.history(), activeProgress,
    ) { settings, kcal, runs, active ->
        val today = LocalDate.now(zone)
        val dates = runs.map { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
        HomeState(
            settings = settings,
            rank = Ranks.forTotal(kcal),
            streak = Weeks.streak(dates, settings.weeklyGoal, today),
            runsThisWeek = Weeks.runsInWeekOf(today, dates),
            week = Weeks.daysOf(today).map { WeekDay(it, it in dates, it == today) },
            program = active?.first,
            progress = active?.second,
        )
    }.hot(viewModelScope, null)
}

// ---------- History ----------

class HistoryViewModel(app: OpenRunApp) : ViewModel() {
    val items: StateFlow<List<org.openrun.core.HistoryItem>?> = app.runs.historyItems().map<List<org.openrun.core.HistoryItem>, List<org.openrun.core.HistoryItem>?> { it }.hot(viewModelScope, null)
}

// ---------- Programs ----------

class ParcoursViewModel(private val app: OpenRunApp) : ViewModel() {
    val state: StateFlow<Map<String, ProgramProgress>> = combine(
        Programs.all.map { p -> app.runs.progress(p).map { p.id to it } }
    ) { pairs -> pairs.toMap() }.hot(viewModelScope, emptyMap())

    val activeProgramId: StateFlow<String?> =
        app.settings.settings.map { it.activeProgramId }.hot(viewModelScope, null)
}

class ProgramViewModel(private val app: OpenRunApp, programId: String) : ViewModel() {
    val program: Program = Programs.byId(programId)
    val progress: StateFlow<ProgramProgress> =
        app.runs.progress(program).hot(viewModelScope, ProgramProgress(program.sessionCount))

    /** Picking a day starts this program: apply what was confirmed, and make it the active one. */
    fun pick(plan: JumpPlan, then: () -> Unit) {
        viewModelScope.launch {
            app.runs.applyJump(program, plan)
            app.settings.update { it.copy(activeProgramId = program.id, onboarded = true) }
            then()
        }
    }
}

// ---------- Start ("Où en es-tu ?") ----------

class StartViewModel(private val app: OpenRunApp) : ViewModel() {
    val current: StateFlow<AppSettings> = app.settings.settings.hot(viewModelScope, AppSettings())

    fun choose(programId: String?, then: () -> Unit) {
        viewModelScope.launch {
            app.settings.update { it.copy(activeProgramId = programId, onboarded = true) }
            then()
        }
    }
}

// ---------- Settings ----------

class SettingsViewModel(private val app: OpenRunApp) : ViewModel() {
    val settings: StateFlow<AppSettings> = app.settings.settings.hot(viewModelScope, AppSettings())
    fun update(change: (AppSettings) -> AppSettings) { viewModelScope.launch { app.settings.update(change) } }
}
