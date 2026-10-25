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
        workout = program?.sessions?.getOrNull(sessionIndex ?: 0)?.let { w ->
            w.let { if (intent.getBooleanExtra(EXTRA_SKIP_WARMUP, false)) it.withoutWarmup() else it }
                .let { if (intent.getBooleanExtra(EXTRA_SKIP_COOLDOWN, false)) it.withoutCooldown() else it }
        }
        if (program == null) { programId = null; sessionIndex = null }

        scope.launch {
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
        if (w != null) announcePhase(0) else audio?.say(getString(R.string.run_go), settings)
        publish()
        ticker = scope.launch { while (isActive && running) { tick(); delay(250) } }
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
                    audio?.say(getString(R.string.run_done_voice), settings); audio?.buzzEnd(settings)
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
    }

    private fun announcePhase(index: Int) {
        val w = workout ?: return
        val kind = w.phases[index].kind
        val name = getString(when (kind) {
            PhaseKind.WARMUP -> R.string.phase_warmup; PhaseKind.RUN -> R.string.phase_run
            PhaseKind.WALK -> R.string.phase_walk; PhaseKind.COOLDOWN -> R.string.phase_cooldown
        })
        audio?.phaseChange(kind, name, settings)
        if (settings.encouragements && kind == PhaseKind.RUN && w.phases.drop(index + 1).none { it.kind == PhaseKind.RUN } && w.runCount > 1) {
            audio?.say(getString(R.string.encourage_last_run), settings)
        }
    }

    private fun move(change: (Workout) -> Step) {
        val w = workout ?: return
        val step = change(w)
        runState = step.state
        step.entered.forEach { announcePhase(it) }
        if (runState.finished) { audio?.say(getString(R.string.run_done_voice), settings); audio?.buzzEnd(settings) }
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

    private fun kcalNow(): Double {
        val w = workout
        val segments = if (w == null) {
            listOf(Calories.Segment(true, phaseSecMs[0] / 1000.0, if (gpsOn && phaseDistM[0] > 0) phaseDistM[0] else null))
        } else w.phases.indices.filter { phaseSecMs[it] > 0 }.map {
            Calories.Segment(w.phases[it].kind == PhaseKind.RUN, phaseSecMs[it] / 1000.0, if (gpsOn && phaseDistM[it] > 0) phaseDistM[it] else null)
        }
        return Calories.kcal(settings.weightKg, segments)
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
        val w = workout
        val finished = w != null && runState.finished
        val position = w?.let { WorkoutRunner.elapsedMs(it, runState) } ?: 0L
        val planned = (w?.totalSeconds ?: 0) * 1000L
        val counts = w != null && Cues.countsAsDone(position, planned, finished)
        val run = RunEntity(
            startedAt = startedWallMs,
            durationSec = (activeMs / 1000).toInt(),
            distanceM = acc.distanceM,
            kcal = kcalNow(),
            programId = programId,
            sessionIndex = sessionIndex,
            treadmill = !gpsOn,
        )
        val trackEntities = points.mapIndexed { i, p -> TrackPointEntity(0, i, p.lat, p.lon, p.accuracyM, p.timeMs, altitudes.getOrNull(i)) }
        val phaseEntities = w?.phases?.indices?.filter { phaseSecMs[it] > 0 }?.map {
            PhaseResultEntity(0, it, w.phases[it].kind.name, (phaseSecMs[it] / 1000).toInt(), phaseDistM[it])
        }.orEmpty()
        // stop counting first, so nothing moves while the database is written
        running = false
        ticker?.cancel()
        scope.launch {
            if (save) RunSession.savedRunId.value = app.runs.saveRun(run, trackEntities, phaseEntities, completesSession = counts)
            stopSession()
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
