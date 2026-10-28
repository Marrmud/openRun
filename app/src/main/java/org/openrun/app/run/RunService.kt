package org.openrun.app.run

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.openrun.app.MainActivity
import org.openrun.app.OpenRunApp
import org.openrun.app.R
import org.openrun.app.data.PhaseResultEntity
import org.openrun.app.data.RunEntity
import org.openrun.app.data.TrackPointEntity
import org.openrun.core.AppSettings
import org.openrun.core.Calories
import org.openrun.core.Cues
import org.openrun.core.PhaseKind
import org.openrun.core.Programs
import org.openrun.core.RunState
import org.openrun.core.Step
import org.openrun.core.Track
import org.openrun.core.TrackAccumulator
import org.openrun.core.TrackPoint
import org.openrun.core.Workout
import org.openrun.core.WorkoutRunner

/**
 * The session itself. It lives in a foreground service so it keeps counting, recording and speaking with the
 * screen locked. The screens only read [RunSession] and send commands through [RunControl].
 */
class RunService : Service(), LocationListener {

    companion object {
        const val ACTION_START = "org.openrun.app.START"
        const val ACTION_PAUSE = "org.openrun.app.PAUSE"
        const val ACTION_RESUME = "org.openrun.app.RESUME"
        const val ACTION_NEXT = "org.openrun.app.NEXT"
        const val ACTION_PREVIOUS = "org.openrun.app.PREVIOUS"
        const val ACTION_RESTART = "org.openrun.app.RESTART"
        const val ACTION_TO_START = "org.openrun.app.TO_START"
        const val ACTION_SAVE = "org.openrun.app.SAVE"
        const val ACTION_DISCARD = "org.openrun.app.DISCARD"
        const val ACTION_GO_TO = "org.openrun.app.GO_TO"
        const val ACTION_UNDO_GO_TO = "org.openrun.app.UNDO_GO_TO"
        const val EXTRA_PHASE = "phase"
        const val EXTRA_PROGRAM = "program"
        const val EXTRA_INDEX = "index"
        const val EXTRA_SKIP_WARMUP = "skipWarmup"
        const val EXTRA_SKIP_COOLDOWN = "skipCooldown"
        private const val CHANNEL = "run"
        private const val NOTIFICATION_ID = 1
        private const val AUTO_PAUSE_BELOW_MPS = 0.7f
        private const val AUTO_RESUME_ABOVE_MPS = 1.2f
        private const val AUTO_PAUSE_AFTER_MS = 6_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ticker: Job? = null
    private lateinit var app: OpenRunApp
    private lateinit var locationManager: LocationManager
    private var audio: RunAudio? = null
    private var wake: PowerManager.WakeLock? = null

    @Volatile private var settings = AppSettings()
    private var running = false

    // what is being run
    private var workout: Workout? = null
    private var programId: String? = null
    private var sessionIndex: Int? = null
    private var runState = RunState()
    private var startedWallMs = 0L

    // time (active time only counts when neither paused nor auto-paused)
    private var lastTickAt = 0L
    private var activeMs = 0L
    private var paused = false
    private var autoPaused = false
    private var lastNotificationAt = 0L
    private var lastCheckpointAt = 0L
    private var skipWarmup = false
    private var skipCooldown = false

    // GPS
    private var gpsOn = false
    private val acc = TrackAccumulator()
    private val points = ArrayList<TrackPoint>()
    private val altitudes = ArrayList<Double?>()
    private var lastAccuracy: Float? = null
    private var slowSince = 0L
    private var lastSpeed = 0f
    private var phaseSecMs = LongArray(1)
    private var phaseDistM = DoubleArray(1)
    private var lastPhaseForDistance = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        app = application as OpenRunApp
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> if (!running) start(intent) else Unit
            ACTION_PAUSE -> if (running) { paused = true; publish() }
            ACTION_RESUME -> if (running) { paused = false; autoPaused = false; lastTickAt = SystemClock.elapsedRealtime(); publish() }
            ACTION_NEXT -> if (running) move { WorkoutRunner.next(it, runState) }
            ACTION_PREVIOUS -> if (running) move { WorkoutRunner.previous(it, runState) }
            ACTION_RESTART -> if (running) move { WorkoutRunner.restartPhase(it, runState) }
            ACTION_TO_START -> if (running) move { WorkoutRunner.toStart(it) }
            ACTION_GO_TO -> if (running) {
                beforeJump = runState
                move { WorkoutRunner.goTo(it, intent.getIntExtra(EXTRA_PHASE, runState.phaseIndex)) }
            }
            // back exactly where the session was before the jump, time in the phase included
            ACTION_UNDO_GO_TO -> if (running) beforeJump?.let { back -> beforeJump = null; move { Step(back, listOf(back.phaseIndex)) } }
            ACTION_SAVE -> if (running) finish(save = true)
            ACTION_DISCARD -> if (running) finish(save = false)
        }
        return START_NOT_STICKY
    }

    // ---------- lifecycle ----------

    private fun start(intent: Intent) {
        // a foreground service has a few seconds to say so: do it before anything slow
        running = true
        goForeground(getString(R.string.app_name), "")

        programId = intent.getStringExtra(EXTRA_PROGRAM)?.takeIf { it.isNotEmpty() }
        sessionIndex = if (programId != null) intent.getIntExtra(EXTRA_INDEX, 0) else null
        val program = programId?.let { id -> Programs.all.firstOrNull { it.id == id } }
        skipWarmup = intent.getBooleanExtra(EXTRA_SKIP_WARMUP, false)
        skipCooldown = intent.getBooleanExtra(EXTRA_SKIP_COOLDOWN, false)
        workout = program?.sessions?.getOrNull(sessionIndex ?: 0)?.let { w ->
            w.let { if (skipWarmup) it.withoutWarmup() else it }.let { if (skipCooldown) it.withoutCooldown() else it }
        }
        if (program == null) { programId = null; sessionIndex = null }

        scope.launch {
            // a session left behind by a kill is saved before a new one starts: never lose a run
            app.recovery.pending()?.let { app.recovery.save(it) }
            settings = app.settings.settings.first()
            // keep following changes, so the voice switch on the run screen acts at once
            launch { app.settings.settings.collect { settings = it } }
            begin()
        }
    }

    private fun begin() {
        val w = workout
        phaseSecMs = LongArray(w?.phases?.size ?: 1)
        phaseDistM = DoubleArray(w?.phases?.size ?: 1)
        runState = RunState()
        activeMs = 0; paused = false; autoPaused = false
        startedWallMs = System.currentTimeMillis()
        acc.reset(); points.clear(); altitudes.clear()
        RunSession.savedRunId.value = null

        wake = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "openrun:run").apply { acquire(6 * 60 * 60 * 1000L) }
        audio = RunAudio(this)

        gpsOn = false
        if (!settings.treadmill && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this, Looper.getMainLooper())
                gpsOn = true
            } catch (_: SecurityException) {}
        }

        lastTickAt = SystemClock.elapsedRealtime()
        lastCheckpointAt = lastTickAt
        if (w != null) announcePhase(0, first = true) else { audio?.startSignal(settings); audio?.say(getString(R.string.run_go), settings) }
        publish()
        ticker = scope.launch { while (isActive && running) { tick(); delay(tickDelayMs()) } }
    }

    private fun stopSession() {
        running = false
        ticker?.cancel()
        try { locationManager.removeUpdates(this) } catch (_: SecurityException) {}
        audio?.release(); audio = null
        wake?.let { if (it.isHeld) it.release() }; wake = null
        RunSession.state.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (running) stopSession()
        scope.cancel()
        super.onDestroy()
    }

    // ---------- the clock ----------

    private fun tick() {
        val now = SystemClock.elapsedRealtime()
        val delta = (now - lastTickAt).coerceAtLeast(0)
        lastTickAt = now
        updateAutoPause(now)

        if (!paused && !autoPaused) {
            val prevElapsed = activeMs
            activeMs += delta
            val w = workout
            if (w == null) {
                phaseSecMs[0] += delta
                if (settings.timeReminder) Cues.reminderCrossed(prevElapsed, activeMs)?.let { audio?.say(getString(R.string.reminder_minutes, it), settings) }
            } else if (!runState.finished) {
                val prevLeft = WorkoutRunner.leftMs(w, runState)
                val prevPos = WorkoutRunner.elapsedMs(w, runState)
                phaseSecMs[runState.phaseIndex] += delta
                val step = WorkoutRunner.tick(w, runState, delta)
                runState = step.state
                step.entered.forEach { announcePhase(it) }
                if (runState.finished) {
                    announceEnd()
                } else if (step.entered.isEmpty() && settings.countdown) {
                    Cues.countdownCrossed(prevLeft, WorkoutRunner.leftMs(w, runState)).forEach { audio?.countdown(it, settings) }
                }
                val planned = w.totalSeconds * 1000L
                if (settings.encouragements && Cues.halfwayCrossed(prevPos, WorkoutRunner.elapsedMs(w, runState), planned)) {
                    audio?.say(getString(R.string.encourage_half), settings)
                }
                if (settings.timeReminder) Cues.reminderCrossed(prevElapsed, activeMs)?.let { audio?.say(getString(R.string.reminder_minutes, it), settings) }
            }
        }
        publish()
        if (now - lastNotificationAt >= 1000) { lastNotificationAt = now; updateNotification() }
        if (now - lastCheckpointAt >= 15_000) { lastCheckpointAt = now; writeCheckpoint() }
    }

    /** The plan is over: said, buzzed and, if asked, a few notes (D38). */
    private fun announceEnd() {
        audio?.endSignal(settings)
        audio?.say(getString(R.string.run_done_voice), settings)
        audio?.buzzEnd(settings)
    }

    /**
     * Four readings a second are enough, except in the last seconds of a phase: there the 3, 2, 1 beeps would land up
     * to a quarter of a second late, unevenly (3 and 2 too close, 2 and 1 too far). So the clock reads every 50 ms there.
     */
    private fun tickDelayMs(): Long {
        val w = workout ?: return 250
        if (runState.finished || paused || autoPaused) return 250
        return if (WorkoutRunner.leftMs(w, runState) <= 3_600) 50 else 250
    }

    private fun announcePhase(index: Int, first: Boolean = false) {
        val w = workout ?: return
        val kind = w.phases[index].kind
        val name = getString(when (kind) {
            PhaseKind.WARMUP -> R.string.phase_warmup; PhaseKind.RUN -> R.string.phase_run
            PhaseKind.WALK -> R.string.phase_walk; PhaseKind.COOLDOWN -> R.string.phase_cooldown
        })
        audio?.phaseChange(kind, name, settings, first)
        if (settings.encouragements && kind == PhaseKind.RUN && w.phases.drop(index + 1).none { it.kind == PhaseKind.RUN } && w.runCount > 1) {
            audio?.say(getString(R.string.encourage_last_run), settings)
        }
    }

    /** Where the session was before a phase was touched in the timeline, for « Annuler ». */
    private var beforeJump: RunState? = null

    private fun move(change: (Workout) -> Step) {
        val w = workout ?: return
        val step = change(w)
        runState = step.state
        step.entered.forEach { announcePhase(it) }
        if (runState.finished) announceEnd()
        publish()
    }

    // ---------- GPS ----------

    override fun onLocationChanged(loc: Location) {
        if (!running) return
        lastAccuracy = loc.accuracy
        if (paused) return
        val p = TrackPoint(loc.latitude, loc.longitude, loc.accuracy, loc.time)
        val added = acc.add(p)
        points += p
        altitudes += if (loc.hasAltitude()) loc.altitude else null
        if (added > 0 && !autoPaused) phaseDistM[phaseIndexNow()] += added
        lastSpeed = if (loc.hasSpeed()) loc.speed else lastSpeed
    }

    private fun phaseIndexNow() = if (workout == null) 0 else runState.phaseIndex.coerceIn(0, phaseDistM.lastIndex)

    /** Auto-pause only for a free run: in a program, walking phases are part of the plan. */
    private fun updateAutoPause(now: Long) {
        if (!settings.autoPause || workout != null || !gpsOn || paused) { autoPaused = false; slowSince = 0; return }
        if (!autoPaused) {
            if (lastSpeed < AUTO_PAUSE_BELOW_MPS && points.isNotEmpty()) {
                if (slowSince == 0L) slowSince = now
                if (now - slowSince >= AUTO_PAUSE_AFTER_MS) autoPaused = true
            } else slowSince = 0
        } else if (lastSpeed > AUTO_RESUME_ABOVE_MPS) { autoPaused = false; slowSince = 0 }
    }

    // ---------- state published to the screens ----------

    /** The session as the saver wants it. [copy] gives it its own lists, for what is written while the run goes on. */
    private fun currentInput(copy: Boolean): SessionInput {
        val w = workout
        return SessionInput(
            programId = programId, sessionIndex = sessionIndex, workout = w,
            startedWallMs = startedWallMs, activeMs = activeMs, workoutFinished = w != null && runState.finished,
            positionMs = w?.let { WorkoutRunner.elapsedMs(it, runState) } ?: 0L,
            phaseSecMs = if (copy) phaseSecMs.copyOf() else phaseSecMs,
            phaseDistM = if (copy) phaseDistM.copyOf() else phaseDistM,
            points = if (copy) ArrayList(points) else points,
            altitudes = if (copy) ArrayList(altitudes) else altitudes,
            gpsOn = gpsOn, weightKg = settings.weightKg, distanceM = acc.distanceM,
        )
    }

    private fun kcalNow(): Double = SessionSaver.kcal(currentInput(copy = false))

    /** Written every few seconds: if the app is killed during a run, the session is offered for saving next time. */
    private fun writeCheckpoint() {
        val i = currentInput(copy = true)
        val cp = Checkpoint(
            i.programId, i.sessionIndex, skipWarmup, skipCooldown, i.startedWallMs, i.activeMs, i.workoutFinished,
            i.positionMs, i.phaseSecMs, i.phaseDistM, i.points, i.altitudes, i.gpsOn, i.weightKg, System.currentTimeMillis(),
        )
        scope.launch(Dispatchers.IO) { RunCheckpoints.write(this@RunService, cp) }
    }

    private fun publish() {
        val w = workout
        val phases = w?.phases
        val idx = runState.phaseIndex
        val distance = acc.distanceM
        RunSession.state.value = RunUiState(
            workout = w, programId = programId, sessionIndex = sessionIndex,
            phaseIndex = idx,
            phase = phases?.getOrNull(idx),
            nextPhase = phases?.getOrNull(idx + 1),
            runsDone = phases?.take(idx + 1)?.count { it.kind == PhaseKind.RUN } ?: 0,
            runsTotal = w?.runCount ?: 0,
            walksDone = phases?.take(idx + 1)?.count { it.kind == PhaseKind.WALK } ?: 0,
            walksTotal = w?.walkCount ?: 0,
            leftMs = if (w != null) WorkoutRunner.leftMs(w, runState) else 0,
            phaseMs = phases?.getOrNull(idx)?.let { it.seconds * 1000L } ?: 0,
            elapsedMs = activeMs,
            plannedMs = (w?.totalSeconds ?: 0) * 1000L,
            distanceM = distance,
            paceNowSecPerKm = if (gpsOn) Track.recentPaceSecPerKm(points.takeLast(40)) else null,
            paceAvgSecPerKm = if (gpsOn) Track.paceSecPerKm(distance, activeMs / 1000.0) else null,
            kcal = kcalNow(),
            paused = paused, autoPaused = autoPaused,
            workoutFinished = w != null && runState.finished,
            gpsOn = gpsOn, gpsAccuracyM = lastAccuracy,
            treadmill = settings.treadmill || !gpsOn,
        )
    }

    // ---------- saving ----------

    private fun finish(save: Boolean) {
        val bundle = SessionSaver.bundle(currentInput(copy = false))
        // stop counting first, so nothing moves while the database is written
        running = false
        ticker?.cancel()
        scope.launch {
            if (save) RunSession.savedRunId.value = app.runs.saveRun(bundle.run, bundle.points, bundle.phases, completesSession = bundle.completesSession)
            RunCheckpoints.clear(this@RunService) // the session is in the database now, or was thrown away on purpose
            stopSession()
            // after the session is safely stored and the screen is moving on: Health Connect, if the user asked for it
            // (in the app's scope: this service is about to be destroyed along with its own)
            if (save && settings.healthConnect) app.appScope.launch { app.health.syncPending() }
        }
    }

    // ---------- notification ----------

    private fun goForeground(title: String, text: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW))
        val type = if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        startForeground(NOTIFICATION_ID, notification(title, text), type)
    }

    private fun notification(title: String, text: String): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val toggle = PendingIntent.getService(
            this, 1, Intent(this, RunService::class.java).setAction(if (paused) ACTION_RESUME else ACTION_PAUSE), PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL)
            .setContentTitle(title).setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true).setOnlyAlertOnce(true).setContentIntent(open)
            .addAction(Notification.Action.Builder(null, getString(if (paused) R.string.resume_run else R.string.pause), toggle).build())
            .build()
    }

    private fun updateNotification() {
        val s = RunSession.state.value ?: return
        val name = s.phase?.let {
            getString(when (it.kind) {
                PhaseKind.WARMUP -> R.string.phase_warmup; PhaseKind.RUN -> R.string.phase_run
                PhaseKind.WALK -> R.string.phase_walk; PhaseKind.COOLDOWN -> R.string.phase_cooldown
            })
        } ?: getString(R.string.free_run)
        val time = if (s.workout != null) s.leftMs / 1000 else s.elapsedMs / 1000
        val text = "%s · %d:%02d".format(name, time / 60, time % 60) + if (paused) " · " + getString(R.string.paused) else ""
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, notification(getString(R.string.app_name), text))
    }
}
