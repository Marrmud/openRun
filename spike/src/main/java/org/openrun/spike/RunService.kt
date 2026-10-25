package org.openrun.spike

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** What the screen shows. Same process as the service, so plain fields are enough for a spike. */
object SpikeState {
    @Volatile var running = false
    @Volatile var startedAt = 0L
    @Volatile var phase = "-"
    @Volatile var phaseLeftMs = 0L
    @Volatile var phaseCount = 0
    @Volatile var fixes = 0
    @Volatile var distanceM = 0f
    @Volatile var maxGapMs = 0L
    @Volatile var lastGapMs = 0L
    @Volatile var lastAccuracy = -1f
    @Volatile var ttsReady = false
    val events = ArrayDeque<String>()
    @Synchronized fun event(s: String) { events.addLast(s); while (events.size > 14) events.removeFirst() }
    @Synchronized fun recent(): String = events.joinToString("\n")
}

object Prefs {
    const val FILE = "spike"
    const val VOICE = "voice"
    const val BEEPS = "beeps"
    const val VIB = "vib"
    const val MODE = "mode" // 0 overlay (no audio focus), 1 duck, 2 pause
    const val VOL = "voiceVol" // 0..100
    fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}

class RunService : Service(), LocationListener, TextToSpeech.OnInitListener {

    companion object {
        const val ACTION_START = "org.openrun.spike.START"
        const val ACTION_STOP = "org.openrun.spike.STOP"
        const val ACTION_SAY = "org.openrun.spike.SAY"
        const val CHANNEL = "run"
        // Short phases so a test shows many changes: warm-up once, then run/walk forever.
        const val WARMUP_MS = 10_000L
        const val RUN_MS = 20_000L
        const val WALK_MS = 20_000L
    }

    private val ui = Handler(Looper.getMainLooper())
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var audio: AudioManager
    private lateinit var lm: LocationManager
    private lateinit var vibrator: Vibrator
    private var tts: TextToSpeech? = null
    private var tone: ToneGenerator? = null
    private var wake: PowerManager.WakeLock? = null
    private var focus: AudioFocusRequest? = null
    private var lastLoc: Location? = null
    private var lastFixAt = 0L
    private var phaseIdx = -1
    private var phaseEndsAt = 0L
    private var pendingSay: String? = null

    private val tick = object : Runnable {
        override fun run() {
            if (!SpikeState.running) return
            val now = SystemClock.elapsedRealtime()
            if (now >= phaseEndsAt) nextPhase(now)
            SpikeState.phaseLeftMs = (phaseEndsAt - now).coerceAtLeast(0)
            ui.postDelayed(this, 250)
        }
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs.sp(this)
        audio = getSystemService(AUDIO_SERVICE) as AudioManager
        lm = getSystemService(LOCATION_SERVICE) as LocationManager
        vibrator = if (android.os.Build.VERSION.SDK_INT >= 31)
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopRun(); return START_NOT_STICKY }
            ACTION_SAY -> { say("Course", force = true); return START_NOT_STICKY }
            else -> startRun()
        }
        return START_NOT_STICKY
    }

    // ---------- lifecycle ----------

    private fun startRun() {
        if (SpikeState.running) return
        goForeground()
        SpikeState.running = true
        SpikeState.startedAt = SystemClock.elapsedRealtime()
        SpikeState.fixes = 0; SpikeState.distanceM = 0f; SpikeState.maxGapMs = 0; SpikeState.lastGapMs = 0
        SpikeState.phaseCount = 0
        log("START voice=${prefs.getBoolean(Prefs.VOICE, true)} beeps=${prefs.getBoolean(Prefs.BEEPS, true)} " +
            "vib=${prefs.getBoolean(Prefs.VIB, true)} mode=${prefs.getInt(Prefs.MODE, 0)}")

        wake = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "openrun:spike").apply { acquire(4 * 60 * 60 * 1000L) }

        tts = TextToSpeech(this, this)
        tone = try { ToneGenerator(AudioManager.STREAM_MUSIC, 80) } catch (e: RuntimeException) { log("tone init failed $e"); null }

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this, Looper.getMainLooper())
                log("GPS updates requested")
            } catch (e: SecurityException) { log("GPS refused: $e") }
        } else log("no location permission")

        phaseIdx = -1
        phaseEndsAt = 0
        ui.post(tick)
    }

    private fun stopRun() {
        if (SpikeState.running) log("STOP fixes=${SpikeState.fixes} dist=${"%.0f".format(SpikeState.distanceM)}m maxGap=${SpikeState.maxGapMs}ms phases=${SpikeState.phaseCount}")
        SpikeState.running = false
        ui.removeCallbacksAndMessages(null)
        try { lm.removeUpdates(this) } catch (_: SecurityException) {}
        abandonFocus()
        tts?.shutdown(); tts = null
        tone?.release(); tone = null
        wake?.let { if (it.isHeld) it.release() }; wake = null
        lastLoc = null; lastFixAt = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() { if (SpikeState.running) stopRun(); super.onDestroy() }

    private fun goForeground() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Course", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, CHANNEL)
            .setContentTitle("openRun spike")
            .setContentText("Enregistrement en cours")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(open)
            .build()
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
    }

    // ---------- workout ----------

    private fun nextPhase(now: Long) {
        phaseIdx++
        SpikeState.phaseCount = phaseIdx + 1
        val (name, ms) = when {
            phaseIdx == 0 -> "Échauffement" to WARMUP_MS
            phaseIdx % 2 == 1 -> "Course" to RUN_MS
            else -> "Marche" to WALK_MS
        }
        phaseEndsAt = now + ms
        SpikeState.phase = name
        log("PHASE $name")
        announce(name)
    }

    private fun announce(name: String) {
        if (prefs.getBoolean(Prefs.BEEPS, true)) tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
        if (prefs.getBoolean(Prefs.VIB, true)) {
            val pattern = if (name == "Course") longArrayOf(0, 500) else longArrayOf(0, 150, 100, 150)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
        if (prefs.getBoolean(Prefs.VOICE, true)) say(name)
    }

    // ---------- voice and audio focus ----------

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) { log("TTS init failed ($status)"); return }
        val t = tts ?: return
        t.language = Locale.FRANCE
        t.setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { log("tts start $id") }
            override fun onDone(id: String?) { log("tts done $id"); ui.post { abandonFocus() } }
            @Deprecated("Deprecated in Java") override fun onError(id: String?) { log("tts error $id"); ui.post { abandonFocus() } }
        })
        SpikeState.ttsReady = true
        log("TTS ready")
        pendingSay?.let { pendingSay = null; say(it) }
    }

    private fun say(text: String, force: Boolean = false) {
        if (!force && !prefs.getBoolean(Prefs.VOICE, true)) return
        val t = tts
        if (t == null || !SpikeState.ttsReady) { pendingSay = text; if (t == null && force) { tts = TextToSpeech(this, this) }; return }
        requestFocusForSpeech()
        val vol = prefs.getInt(Prefs.VOL, 80) / 100f
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, vol) }
        t.speak(text, TextToSpeech.QUEUE_FLUSH, params, "u${SystemClock.elapsedRealtime()}")
    }

    /** Mode 0 asks for nothing: that is what must keep a YouTube video playing. */
    private fun requestFocusForSpeech() {
        val mode = prefs.getInt(Prefs.MODE, 0)
        if (mode == 0) { log("focus: none requested (overlay)"); return }
        val gain = if (mode == 1) AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK else AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        val req = AudioFocusRequest.Builder(gain)
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener { }
            .build()
        focus = req
        val r = audio.requestAudioFocus(req)
        log("focus: requested ${if (mode == 1) "MAY_DUCK" else "TRANSIENT"} -> $r")
    }

    private fun abandonFocus() {
        focus?.let { audio.abandonAudioFocusRequest(it); log("focus: abandoned") }
        focus = null
    }

    // ---------- GPS ----------

    override fun onLocationChanged(loc: Location) {
        val now = SystemClock.elapsedRealtime()
        if (lastFixAt != 0L) {
            val gap = now - lastFixAt
            SpikeState.lastGapMs = gap
            if (gap > SpikeState.maxGapMs) SpikeState.maxGapMs = gap
            if (gap > 5000) log("GPS gap ${gap}ms")
        }
        lastFixAt = now
        SpikeState.lastAccuracy = loc.accuracy
        lastLoc?.let { if (loc.accuracy <= 30f) SpikeState.distanceM += it.distanceTo(loc) }
        if (loc.accuracy <= 30f) lastLoc = loc
        SpikeState.fixes++
        if (SpikeState.fixes == 1) log("first fix acc=${loc.accuracy}")
    }

    @Deprecated("Deprecated in Java") override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}

    // ---------- log ----------

    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private fun log(s: String) {
        val line = "${fmt.format(Date())} $s"
        android.util.Log.i("openrun-spike", s)
        SpikeState.event(line)
        try { File(filesDir, "spike.log").appendText(line + "\n") } catch (_: Exception) {}
    }
}
