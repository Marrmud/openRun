package org.openrun.app.run

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import org.openrun.core.AppSettings
import org.openrun.core.MusicMode
import org.openrun.core.PhaseKind
import org.openrun.core.VibrationIntensity
import java.util.Locale

/**
 * Everything the session says, beeps or buzzes. The rule that matters (validated in the T0 spike): audio focus is
 * asked for only when [AppSettings.requestsAudioFocus] says so, so a muted session or "Superposer" never touches
 * the music or video playing.
 */
class RunAudio(private val context: Context) : TextToSpeech.OnInitListener {
    private val main = Handler(Looper.getMainLooper())
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val vibrator: Vibrator = if (android.os.Build.VERSION.SDK_INT >= 31)
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    else @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var ttsReady = false
    private var pendingSpeech: Pair<String, Float>? = null
    private val tone: ToneGenerator? = try { ToneGenerator(AudioManager.STREAM_MUSIC, 80) } catch (_: RuntimeException) { null }

    private var focus: AudioFocusRequest? = null
    private var activeOps = 0

    private val speechAttrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()

    override fun onInit(status: Int) {
        val t = tts ?: return
        if (status != TextToSpeech.SUCCESS) return
        t.language = Locale.getDefault().takeIf { t.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE } ?: Locale.FRANCE
        t.setAudioAttributes(speechAttrs)
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) { main.post { endOp() } }
            @Deprecated("Deprecated in Java") override fun onError(id: String?) { main.post { endOp() } }
        })
        ttsReady = true
        pendingSpeech?.let { (text, vol) -> pendingSpeech = null; speakNow(text, vol) }
    }

    // ---------- focus: only when asked for ----------

    private fun beginOp(s: AppSettings) {
        activeOps++
        if (!s.requestsAudioFocus || focus != null) return
        val gain = if (s.musicMode == MusicMode.DUCK) AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK else AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        val req = AudioFocusRequest.Builder(gain).setAudioAttributes(speechAttrs).setOnAudioFocusChangeListener { }.build()
        focus = req
        audio.requestAudioFocus(req)
    }

    private fun endOp() {
        activeOps = (activeOps - 1).coerceAtLeast(0)
        if (activeOps == 0) focus?.let { audio.abandonAudioFocusRequest(it); focus = null }
    }

    // ---------- voice ----------

    fun say(text: String, s: AppSettings) {
        if (!s.voice) return
        if (!ttsReady) { pendingSpeech = text to s.voiceVolume; return }
        beginOp(s)
        speakNow(text, s.voiceVolume)
    }

    private fun speakNow(text: String, volume: Float) {
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f)) }
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "u${SystemClock.elapsedRealtime()}")
        if (result != TextToSpeech.SUCCESS) main.post { endOp() }
    }

    // ---------- beeps and buzzes ----------

    fun beep(s: AppSettings, long: Boolean = false) {
        if (!s.beeps) return
        beginOp(s)
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP, if (long) 350 else 120)
        main.postDelayed({ endOp() }, if (long) 700 else 450)
    }

    fun phaseChange(kind: PhaseKind, text: String, s: AppSettings) {
        beep(s, long = true)
        buzz(kind, s)
        say(text, s)
    }

    /** One digit of the countdown: a short beep if beeps are on, else the number spoken. */
    fun countdown(n: Int, s: AppSettings) {
        if (s.beeps) beep(s) else say(n.toString(), s)
    }

    private fun buzz(kind: PhaseKind, s: AppSettings) {
        if (!s.vibratePhone) return
        val timings = when (kind) {
            PhaseKind.RUN -> longArrayOf(0, 600)
            PhaseKind.WALK -> longArrayOf(0, 150, 120, 150)
            PhaseKind.WARMUP, PhaseKind.COOLDOWN -> longArrayOf(0, 350)
        }
        vibrate(timings, s.vibrationIntensity)
    }

    fun buzzEnd(s: AppSettings) {
        if (s.vibratePhone) vibrate(longArrayOf(0, 200, 120, 200, 120, 400), s.vibrationIntensity)
    }

    private fun vibrate(timings: LongArray, intensity: VibrationIntensity) {
        val amp = when (intensity) { VibrationIntensity.LOW -> 80; VibrationIntensity.MEDIUM -> 160; VibrationIntensity.HIGH -> 255 }
        val effect = if (vibrator.hasAmplitudeControl()) {
            // even slots (pauses) get 0, odd slots (buzzes) the chosen strength
            VibrationEffect.createWaveform(timings, IntArray(timings.size) { if (it % 2 == 1) amp else 0 }, -1)
        } else VibrationEffect.createWaveform(timings, -1)
        vibrator.vibrate(effect)
    }

    fun release() {
        tts?.stop(); tts?.shutdown(); tts = null
        tone?.release()
        main.removeCallbacksAndMessages(null)
        focus?.let { audio.abandonAudioFocusRequest(it) }; focus = null
        activeOps = 0
    }
}
