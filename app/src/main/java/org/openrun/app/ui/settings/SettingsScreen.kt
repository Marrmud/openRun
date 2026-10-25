package org.openrun.app.ui.settings

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openrun.app.R
import org.openrun.app.ui.SettingsViewModel
import org.openrun.core.AppSettings
import org.openrun.core.MusicMode
import org.openrun.core.ScreenStyle
import org.openrun.core.VibrationIntensity

@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 6.dp, end = 18.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
            // ---- voice and sounds ----
            Section(Icons.AutoMirrored.Filled.VolumeUp, R.string.section_voice)
            SwitchRow(R.string.voice, R.string.voice_sub, s.voice) { v -> vm.update { it.copy(voice = v) } }
            AnimatedVisibility(s.voice) {
                Column(Modifier.padding(start = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.voice_volume), modifier = Modifier.padding(end = 12.dp))
                        Slider(value = s.voiceVolume, onValueChange = { v -> vm.update { it.copy(voiceVolume = v) } }, modifier = Modifier.weight(1f))
                    }
                    SwitchRow(R.string.countdown, R.string.countdown_sub, s.countdown) { v -> vm.update { it.copy(countdown = v) } }
                    SwitchRow(R.string.encouragements, null, s.encouragements) { v -> vm.update { it.copy(encouragements = v) } }
                    SwitchRow(R.string.time_reminder, R.string.time_reminder_sub, s.timeReminder) { v -> vm.update { it.copy(timeReminder = v) } }
                }
            }
            SwitchRow(R.string.beeps, null, s.beeps) { v -> vm.update { it.copy(beeps = v) } }
            AnimatedVisibility(s.playsSound) { MusicBlock(s) { m -> vm.update { it.copy(musicMode = m) } } }

            // ---- vibrations ----
            Section(Icons.Filled.Vibration, R.string.section_vibrations)
            SwitchRow(R.string.vibrate_phone, null, s.vibratePhone) { v -> vm.update { it.copy(vibratePhone = v) } }
            AnimatedVisibility(s.vibratePhone) {
                Segmented(
                    options = VibrationIntensity.entries.toList(),
                    selected = s.vibrationIntensity,
                    label = { stringResource(when (it) { VibrationIntensity.LOW -> R.string.intensity_low; VibrationIntensity.MEDIUM -> R.string.intensity_medium; VibrationIntensity.HIGH -> R.string.intensity_high }) },
                    onSelect = { i -> vm.update { it.copy(vibrationIntensity = i) } },
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
                )
            }
            SwitchRow(R.string.vibrate_watch, null, s.vibrateWatch) { v -> vm.update { it.copy(vibrateWatch = v) } }

            // ---- session ----
            Section(Icons.Filled.DirectionsRun, R.string.section_session)
            SwitchRow(R.string.keep_screen_on, null, s.keepScreenOn) { v -> vm.update { it.copy(keepScreenOn = v) } }
            SwitchRow(R.string.treadmill, R.string.treadmill_sub, s.treadmill) { v -> vm.update { it.copy(treadmill = v) } }
            SwitchRow(R.string.auto_pause, null, s.autoPause) { v -> vm.update { it.copy(autoPause = v) } }
            StyleRow(s.defaultStyle) { st -> vm.update { it.copy(defaultStyle = st) } }
            StepperRow(R.string.weekly_goal, stringResource(R.string.goal_value, s.weeklyGoal), { vm.update { it.copy(weeklyGoal = it.weeklyGoal - 1) } }, { vm.update { it.copy(weeklyGoal = it.weeklyGoal + 1) } })

            // ---- health ----
            Section(Icons.Filled.Favorite, R.string.section_health)
            StepperRow(R.string.my_weight, "${s.weightKg.toInt()} kg", { vm.update { it.copy(weightKg = it.weightKg - 1) } }, { vm.update { it.copy(weightKg = it.weightKg + 1) } }, subtitle = R.string.my_weight_sub)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.health_connect), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.health_connect_soon), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Section(icon: ImageVector, @StringRes title: Int) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null)
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SwitchRow(@StringRes title: Int, @StringRes subtitle: Int?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(stringResource(subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun StepperRow(@StringRes title: Int, value: String, onMinus: () -> Unit, onPlus: () -> Unit, @StringRes subtitle: Int? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(stringResource(subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onMinus) { Icon(Icons.Filled.Remove, null) }
        Text(value, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
        IconButton(onClick = onPlus) { Icon(Icons.Filled.Add, null) }
    }
}

@Composable
private fun <T> Segmented(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { i, option ->
            SegmentedButton(
                selected = option == selected, onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(label(option), maxLines = 1) }
        }
    }
}

/** What the voice does to the music or video playing: the setting that decides whether audio focus is asked for. */
@Composable
private fun MusicBlock(s: AppSettings, onMode: (MusicMode) -> Unit) {
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.with_music), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Segmented(
            options = MusicMode.entries.toList(), selected = s.musicMode,
            label = { stringResource(when (it) { MusicMode.OVERLAY -> R.string.music_overlay; MusicMode.DUCK -> R.string.music_duck; MusicMode.PAUSE -> R.string.music_pause }) },
            onSelect = onMode,
        )
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Info, null, Modifier.padding(top = 2.dp))
                Text(
                    stringResource(when (s.musicMode) { MusicMode.OVERLAY -> R.string.music_overlay_note; MusicMode.DUCK -> R.string.music_duck_note; MusicMode.PAUSE -> R.string.music_pause_note }),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StyleRow(current: ScreenStyle, onSelect: (ScreenStyle) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val names = mapOf(
        ScreenStyle.FOCUS to R.string.style_focus, ScreenStyle.TIMELINE to R.string.style_timeline, ScreenStyle.FULL to R.string.style_full,
    )
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.default_style), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = { open = true }) { Text(stringResource(names.getValue(current))) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ScreenStyle.entries.forEach { st ->
                DropdownMenuItem(text = { Text(stringResource(names.getValue(st))) }, onClick = { open = false; onSelect(st) })
            }
        }
    }
}
