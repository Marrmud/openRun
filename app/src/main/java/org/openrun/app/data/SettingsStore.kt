package org.openrun.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.openrun.core.AppSettings
import org.openrun.core.MusicMode
import org.openrun.core.ScreenStyle
import org.openrun.core.ThemeMode
import org.openrun.core.VibrationIntensity

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings")

/** Reads and writes [AppSettings]. A missing or unreadable value falls back to the default, never to a crash. */
class SettingsStore(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.settingsDataStore)

    val settings: Flow<AppSettings> = store.data.map { read(it) }

    suspend fun update(change: (AppSettings) -> AppSettings) {
        store.edit { p -> write(p, change(read(p)).sanitized()) }
    }

    private object K {
        val voice = booleanPreferencesKey("voice")
        val voiceVolume = floatPreferencesKey("voice_volume")
        val countdown = booleanPreferencesKey("countdown")
        val encouragements = booleanPreferencesKey("encouragements")
        val timeReminder = booleanPreferencesKey("time_reminder")
        val beeps = booleanPreferencesKey("beeps")
        val vibratePhone = booleanPreferencesKey("vibrate_phone")
        val vibrationIntensity = stringPreferencesKey("vibration_intensity")
        val vibrateWatch = booleanPreferencesKey("vibrate_watch")
        val musicMode = stringPreferencesKey("music_mode")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val treadmill = booleanPreferencesKey("treadmill")
        val autoPause = booleanPreferencesKey("auto_pause")
        val defaultStyle = stringPreferencesKey("default_style")
        val weightKg = doublePreferencesKey("weight_kg")
        val theme = stringPreferencesKey("theme")
        val activeProgramId = stringPreferencesKey("active_program_id")
        val weeklyGoal = intPreferencesKey("weekly_goal")
        val onboarded = booleanPreferencesKey("onboarded")
    }

    private inline fun <reified E : Enum<E>> Preferences.enum(key: Preferences.Key<String>, default: E): E =
        this[key]?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default

    private fun read(p: Preferences): AppSettings {
        val d = AppSettings()
        return AppSettings(
            voice = p[K.voice] ?: d.voice,
            voiceVolume = p[K.voiceVolume] ?: d.voiceVolume,
            countdown = p[K.countdown] ?: d.countdown,
            encouragements = p[K.encouragements] ?: d.encouragements,
            timeReminder = p[K.timeReminder] ?: d.timeReminder,
            beeps = p[K.beeps] ?: d.beeps,
            vibratePhone = p[K.vibratePhone] ?: d.vibratePhone,
            vibrationIntensity = p.enum(K.vibrationIntensity, d.vibrationIntensity),
            vibrateWatch = p[K.vibrateWatch] ?: d.vibrateWatch,
            musicMode = p.enum(K.musicMode, d.musicMode),
            keepScreenOn = p[K.keepScreenOn] ?: d.keepScreenOn,
            treadmill = p[K.treadmill] ?: d.treadmill,
            autoPause = p[K.autoPause] ?: d.autoPause,
            defaultStyle = p.enum(K.defaultStyle, d.defaultStyle),
            weightKg = p[K.weightKg] ?: d.weightKg,
            theme = p.enum(K.theme, d.theme),
            activeProgramId = p[K.activeProgramId],
            weeklyGoal = p[K.weeklyGoal] ?: d.weeklyGoal,
            onboarded = p[K.onboarded] ?: d.onboarded,
        ).sanitized()
    }

    private fun write(p: androidx.datastore.preferences.core.MutablePreferences, s: AppSettings) {
        p[K.voice] = s.voice
        p[K.voiceVolume] = s.voiceVolume
        p[K.countdown] = s.countdown
        p[K.encouragements] = s.encouragements
        p[K.timeReminder] = s.timeReminder
        p[K.beeps] = s.beeps
        p[K.vibratePhone] = s.vibratePhone
        p[K.vibrationIntensity] = s.vibrationIntensity.name
        p[K.vibrateWatch] = s.vibrateWatch
        p[K.musicMode] = s.musicMode.name
        p[K.keepScreenOn] = s.keepScreenOn
        p[K.treadmill] = s.treadmill
        p[K.autoPause] = s.autoPause
        p[K.defaultStyle] = s.defaultStyle.name
        p[K.weightKg] = s.weightKg
        p[K.theme] = s.theme.name
        val program = s.activeProgramId
        if (program != null) p[K.activeProgramId] = program else p.remove(K.activeProgramId)
        p[K.weeklyGoal] = s.weeklyGoal
        p[K.onboarded] = s.onboarded
    }
}
