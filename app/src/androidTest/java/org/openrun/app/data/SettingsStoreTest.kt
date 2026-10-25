package org.openrun.app.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.openrun.core.AppSettings
import org.openrun.core.MusicMode
import org.openrun.core.ScreenStyle
import org.openrun.core.ThemeMode
import java.io.File

@RunWith(AndroidJUnit4::class)
class SettingsStoreTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var file: File
    private lateinit var dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    private lateinit var store: SettingsStore

    @Before fun open() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        file = File(ctx.cacheDir, "test-${System.nanoTime()}.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
        store = SettingsStore(dataStore)
    }

    @After fun close() { scope.cancel(); file.delete() }

    @Test fun aFreshStoreGivesTheDefaults() = runBlocking {
        assertEquals(AppSettings(), store.settings.first())
    }

    @Test fun changesAreKept() = runBlocking {
        store.update { it.copy(voice = false, musicMode = MusicMode.PAUSE, defaultStyle = ScreenStyle.FULL, weightKg = 82.5) }
        val s = store.settings.first()
        assertFalse(s.voice)
        assertEquals(MusicMode.PAUSE, s.musicMode)
        assertEquals(ScreenStyle.FULL, s.defaultStyle)
        assertEquals(82.5, s.weightKg, 0.0)
        assertEquals(true, s.beeps) // untouched
    }

    @Test fun everythingRoundTrips() = runBlocking {
        val custom = AppSettings(
            voice = false, voiceVolume = 0.3f, countdown = false, encouragements = true, timeReminder = true,
            beeps = false, vibratePhone = false, vibrateWatch = false, musicMode = MusicMode.DUCK,
            keepScreenOn = false, treadmill = true, autoPause = false, defaultStyle = ScreenStyle.TIMELINE,
            weightKg = 64.0, theme = ThemeMode.SYSTEM, activeProgramId = "c25k", weeklyGoal = 4, onboarded = true,
            healthConnect = true,
        )
        store.update { custom }
        assertEquals(custom, store.settings.first())
    }

    @Test fun aValueOutOfRangeIsBroughtBack() = runBlocking {
        store.update { it.copy(voiceVolume = 5f, weightKg = 1.0, weeklyGoal = 99) }
        val s = store.settings.first()
        assertEquals(1f, s.voiceVolume, 0f)
        assertEquals(30.0, s.weightKg, 0.0)
        assertEquals(7, s.weeklyGoal)
    }

    @Test fun clearingTheActiveProgramRemovesIt() = runBlocking {
        store.update { it.copy(activeProgramId = "c25k") }
        assertEquals("c25k", store.settings.first().activeProgramId)
        store.update { it.copy(activeProgramId = null) }
        assertNull(store.settings.first().activeProgramId)
    }

    @Test fun anUnknownStoredNameFallsBackToTheDefault() = runBlocking {
        dataStore.edit { it[stringPreferencesKey("music_mode")] = "SOMETHING_FROM_THE_FUTURE" }
        assertEquals(MusicMode.OVERLAY, store.settings.first().musicMode)
    }
}
