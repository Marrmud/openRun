package org.openrun.app.run

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import org.openrun.core.Phase
import org.openrun.core.PhaseKind
import org.openrun.core.Workout

/** What the run screen shows, published by the service several times a second. */
data class RunUiState(
    val workout: Workout?,              // null for a free run
    val programId: String?,
    val sessionIndex: Int?,
    val phaseIndex: Int,
    val phase: Phase?,
    val nextPhase: Phase?,
    val runsDone: Int,                  // running intervals started so far (the "3" of 3 / 6)
    val runsTotal: Int,
    val walksDone: Int,                 // the same for walks (D40: "Marche · 3 / 6")
    val walksTotal: Int,
    val leftMs: Long,                   // time left in the phase; 0 for a free run
    val phaseMs: Long,
    val elapsedMs: Long,                // active time, pauses excluded
    val plannedMs: Long,
    val distanceM: Double,
    val paceNowSecPerKm: Double?,
    val paceAvgSecPerKm: Double?,
    val kcal: Double,
    val paused: Boolean,
    val autoPaused: Boolean,
    val workoutFinished: Boolean,       // the plan is over; the runner decides when to stop
    val gpsOn: Boolean,
    val gpsAccuracyM: Float?,
    val treadmill: Boolean,
)

/** The one live session of the process, written by [RunService], read by the screens. */
object RunSession {
    val state = MutableStateFlow<RunUiState?>(null)

    /** Set when a session was saved, so the screen can open its summary. Cleared by whoever consumes it. */
    val savedRunId = MutableStateFlow<Long?>(null)

    val isActive: Boolean get() = state.value != null
}

object RunControl {
    private fun send(context: Context, action: String, block: Intent.() -> Unit = {}) {
        val intent = Intent(context, RunService::class.java).setAction(action).apply(block)
        if (action == RunService.ACTION_START) context.startForegroundService(intent) else context.startService(intent)
    }

    fun start(context: Context, programId: String?, index: Int, skipWarmup: Boolean, skipCooldown: Boolean) =
        send(context, RunService.ACTION_START) {
            putExtra(RunService.EXTRA_PROGRAM, programId)
            putExtra(RunService.EXTRA_INDEX, index)
            putExtra(RunService.EXTRA_SKIP_WARMUP, skipWarmup)
            putExtra(RunService.EXTRA_SKIP_COOLDOWN, skipCooldown)
        }

    fun pause(context: Context) = send(context, RunService.ACTION_PAUSE)
    fun resume(context: Context) = send(context, RunService.ACTION_RESUME)
    fun next(context: Context) = send(context, RunService.ACTION_NEXT)
    fun previous(context: Context) = send(context, RunService.ACTION_PREVIOUS)
    fun restartPhase(context: Context) = send(context, RunService.ACTION_RESTART)
    fun toStart(context: Context) = send(context, RunService.ACTION_TO_START)
    fun goTo(context: Context, phase: Int) = send(context, RunService.ACTION_GO_TO) { putExtra(RunService.EXTRA_PHASE, phase) }
    fun undoGoTo(context: Context) = send(context, RunService.ACTION_UNDO_GO_TO)
    fun save(context: Context) = send(context, RunService.ACTION_SAVE)
    fun discard(context: Context) = send(context, RunService.ACTION_DISCARD)
}

fun PhaseKind.isRunning() = this == PhaseKind.RUN
