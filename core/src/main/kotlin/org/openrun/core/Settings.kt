package org.openrun.core

enum class MusicMode { OVERLAY, DUCK, PAUSE }
enum class VibrationIntensity { LOW, MEDIUM, HIGH }
enum class ScreenStyle { FOCUS, TIMELINE, FULL }
enum class ThemeMode { LIGHT, DARK, SYSTEM }

/** Everything the settings screen can change. The defaults are what a first launch gets. */
data class AppSettings(
    val voice: Boolean = true,
    val voiceVolume: Float = 0.8f,
    val countdown: Boolean = true,
    val encouragements: Boolean = false,
    val timeReminder: Boolean = false,
    val beeps: Boolean = true,
    val vibratePhone: Boolean = true,
    val vibrationIntensity: VibrationIntensity = VibrationIntensity.MEDIUM,
    val vibrateWatch: Boolean = true,
    val musicMode: MusicMode = MusicMode.OVERLAY,
    val keepScreenOn: Boolean = true,
    val treadmill: Boolean = false,
    val autoPause: Boolean = true,
    val defaultStyle: ScreenStyle = ScreenStyle.FOCUS,
    val weightKg: Double = 70.0,
    val theme: ThemeMode = ThemeMode.LIGHT,
    val activeProgramId: String? = null,
    val weeklyGoal: Int = 3,
    val onboarded: Boolean = false,
) {
    /** The "with your music" setting only means something when something is played over it. */
    val playsSound: Boolean get() = voice || beeps

    /**
     * Whether a session may ask Android for audio focus. Never when everything is muted or when the music is left
     * alone: this is the rule that keeps a video playing (the bug that sent us away from other apps).
     */
    val requestsAudioFocus: Boolean get() = playsSound && musicMode != MusicMode.OVERLAY

    fun sanitized(): AppSettings = copy(
        voiceVolume = voiceVolume.coerceIn(0f, 1f),
        weightKg = weightKg.coerceIn(30.0, 250.0),
        weeklyGoal = weeklyGoal.coerceIn(1, 7),
    )
}
