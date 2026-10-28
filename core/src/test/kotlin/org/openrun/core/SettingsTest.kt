package org.openrun.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTest {
    @Test fun aFirstLaunchLeavesTheMusicAloneAndSpeaks() {
        val s = AppSettings()
        assertTrue(s.voice)
        assertEquals(MusicMode.OVERLAY, s.musicMode)
        assertFalse(s.requestsAudioFocus)
    }

    @Test fun overlayNeverAsksForFocus() {
        assertFalse(AppSettings(musicMode = MusicMode.OVERLAY).requestsAudioFocus)
    }

    @Test fun duckAndPauseAskForFocusOnlyWhenSomethingPlays() {
        for (mode in listOf(MusicMode.DUCK, MusicMode.PAUSE)) {
            assertTrue(AppSettings(musicMode = mode).requestsAudioFocus)
            assertTrue(AppSettings(musicMode = mode, voice = false).requestsAudioFocus) // the beeps still play
            assertFalse(AppSettings(musicMode = mode, voice = false, beeps = false).requestsAudioFocus)
        }
    }

    @Test fun everythingMutedNeverTouchesTheMusicWhateverTheMode() {
        for (mode in MusicMode.entries) {
            val s = AppSettings(voice = false, beeps = false, musicMode = mode)
            assertFalse(s.playsSound)
            assertFalse(s.requestsAudioFocus)
        }
    }

    @Test fun vibrationsDoNotInfluenceTheMusic() {
        val s = AppSettings(voice = false, beeps = false, vibratePhone = true, vibrateWatch = true, musicMode = MusicMode.PAUSE)
        assertFalse(s.requestsAudioFocus)
    }

    @Test fun beepsAreLoudByDefault() {
        val s = AppSettings()
        assertEquals(1f, s.beepVolume, 0f)
        assertTrue(s.startEndSignal)
    }

    @Test fun sanitizedKeepsValuesInRange() {
        val s = AppSettings(voiceVolume = 3f, weightKg = 5.0, weeklyGoal = 0).sanitized()
        assertEquals(1f, s.voiceVolume, 0f)
        assertEquals(30.0, s.weightKg, 0.0)
        assertEquals(1, s.weeklyGoal)
        assertEquals(250.0, AppSettings(weightKg = 900.0).sanitized().weightKg, 0.0)
        assertEquals(0f, AppSettings(voiceVolume = -1f).sanitized().voiceVolume, 0f)
        assertEquals(7, AppSettings(weeklyGoal = 12).sanitized().weeklyGoal)
        assertEquals(1f, AppSettings(beepVolume = 2f).sanitized().beepVolume, 0f)
        assertEquals(0f, AppSettings(beepVolume = -1f).sanitized().beepVolume, 0f)
    }
}
