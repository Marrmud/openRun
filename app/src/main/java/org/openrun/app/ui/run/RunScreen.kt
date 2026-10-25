package org.openrun.app.ui.run

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VoiceOverOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openrun.app.R
import org.openrun.app.run.RunUiState
import org.openrun.app.ui.components.WorkoutBar
import org.openrun.app.ui.theme.RunTheme
import org.openrun.core.PhaseKind
import org.openrun.core.ScreenStyle

data class RunActions(
    val onPause: () -> Unit,
    val onResume: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onRestartPhase: () -> Unit,
    val onToStart: () -> Unit,
    val onSave: () -> Unit,
    val onDiscard: () -> Unit,
    val onToggleVoice: () -> Unit,
)

/** The screen of a session in progress, in one of three styles. Everything is large and one tap away. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunScreen(state: RunUiState, voiceOn: Boolean, initialStyle: ScreenStyle, actions: RunActions) {
    var style by remember { mutableStateOf(initialStyle) }
    var menuOpen by remember { mutableStateOf(false) }
    var finishOpen by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    val isFree = state.workout == null
    // a free run has no intervals, so no timeline
    val styles = if (isFree) listOf(ScreenStyle.FOCUS, ScreenStyle.FULL) else ScreenStyle.entries.toList()
    val shownStyle = if (style in styles) style else ScreenStyle.FOCUS

    androidx.activity.compose.BackHandler(enabled = !finishOpen) { if (!locked) finishOpen = true }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp).padding(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                styles.forEachIndexed { i, st ->
                    SegmentedButton(
                        selected = st == shownStyle, onClick = { style = st },
                        shape = SegmentedButtonDefaults.itemShape(i, styles.size),
                    ) {
                        Text(stringResource(when (st) { ScreenStyle.FOCUS -> R.string.style_focus; ScreenStyle.TIMELINE -> R.string.style_timeline; ScreenStyle.FULL -> R.string.style_full }), maxLines = 1)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalIconButton(onClick = { locked = true }) { Icon(Icons.Filled.Lock, stringResource(R.string.lock)) }
                Text(
                    stateTitle(state), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                )
                FilledIconToggleButton(checked = voiceOn, onCheckedChange = { actions.onToggleVoice() }) {
                    Icon(if (voiceOn) Icons.Filled.RecordVoiceOver else Icons.Filled.VoiceOverOff, stringResource(R.string.voice))
                }
                Box {
                    FilledTonalIconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, null) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (!isFree) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.restart_interval)) }, onClick = { menuOpen = false; actions.onRestartPhase() })
                            DropdownMenuItem(text = { Text(stringResource(R.string.back_to_warmup)) }, onClick = { menuOpen = false; actions.onToStart() })
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.finish_menu), color = MaterialTheme.colorScheme.error) },
                            onClick = { menuOpen = false; finishOpen = true },
                        )
                    }
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (shownStyle) {
                    ScreenStyle.FOCUS -> FocusStyle(state)
                    ScreenStyle.TIMELINE -> TimelineStyle(state)
                    ScreenStyle.FULL -> FullStyle(state)
                }
            }

            Controls(state, actions, onFinish = { finishOpen = true })
        }

        if (locked) LockOverlay(onUnlock = { locked = false })
    }

    if (finishOpen) {
        FinishSheet(state, onSave = { finishOpen = false; actions.onSave() }, onDiscard = { finishOpen = false; actions.onDiscard() }, onDismiss = { finishOpen = false })
    }
}

@Composable
private fun stateTitle(s: RunUiState): String {
    val base = if (s.sessionIndex != null && s.programId != null) {
        val program = org.openrun.core.Programs.all.firstOrNull { it.id == s.programId }
        program?.locate(s.sessionIndex)?.let { (w, d) -> stringResource(R.string.week_day_short, w, d) } ?: stringResource(R.string.free_run)
    } else stringResource(R.string.free_run)
    return "$base · ${clock(s.elapsedMs)}"
}

// ---------------------------------------------------------------- styles

@Composable
private fun phaseColors(kind: PhaseKind?): Pair<Color, Color> {
    val c = RunTheme.colors
    return when (kind) {
        PhaseKind.RUN -> c.run to c.runContainer
        PhaseKind.WALK -> c.walk to c.walkContainer
        PhaseKind.WARMUP, PhaseKind.COOLDOWN -> c.calm to c.calmContainer
        null -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
    }
}

@Composable
private fun phaseName(kind: PhaseKind?): String = stringResource(when (kind) {
    PhaseKind.WARMUP -> R.string.phase_warmup; PhaseKind.RUN -> R.string.phase_run
    PhaseKind.WALK -> R.string.phase_walk; PhaseKind.COOLDOWN -> R.string.phase_cooldown
    null -> R.string.free_run
})

@Composable
private fun PhasePill(s: RunUiState) {
    val (color, container) = phaseColors(s.phase?.kind)
    val label = when {
        s.workoutFinished -> stringResource(R.string.run_done_title)
        s.paused -> stringResource(R.string.paused)
        s.autoPaused -> stringResource(R.string.auto_paused)
        else -> phaseName(s.phase?.kind) + if (s.phase?.kind == PhaseKind.RUN && s.runsTotal > 1) " · ${s.runsDone} / ${s.runsTotal}" else ""
    }
    Surface(shape = RoundedCornerShape(16.dp), color = container, contentColor = color) {
        Text(label.uppercase(), Modifier.padding(horizontal = 14.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun FocusStyle(s: RunUiState) {
    val (color, container) = phaseColors(s.phase?.kind)
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PhasePill(s)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val fraction = if (s.phaseMs > 0) (s.leftMs.toFloat() / s.phaseMs).coerceIn(0f, 1f) else 1f
            Canvas(Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp)) {
                val stroke = 28.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(container, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                if (s.workout != null) drawArc(color, -90f, 360f * fraction, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    clock(if (s.workout != null) s.leftMs + 999 else s.elapsedMs),
                    fontSize = 88.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-3).sp, maxLines = 1,
                )
                Text(stringResource(if (s.workout != null) R.string.remaining else R.string.elapsed), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val next = s.nextPhase
        if (next != null) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(14.dp).background(phaseColors(next.kind).first, CircleShape))
                    Text(stringResource(R.string.next_up, phaseName(next.kind), clock(next.seconds * 1000L)), fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            BigStat(stringResource(R.string.distance), "${kmText(s.distanceM)} km")
            BigStat(stringResource(if (s.workout != null) R.string.time else R.string.pace), if (s.workout != null) clock(s.elapsedMs) else paceText(s.paceNowSecPerKm))
        }
    }
}

@Composable
private fun BigStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TimelineStyle(s: RunUiState) {
    val w = s.workout ?: return
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PhasePill(s)
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                WorkoutBar(w, height = 56.dp, doneUpTo = s.phaseIndex)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(clock(s.elapsedMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.left_total, clock((s.plannedMs - s.elapsedMs).coerceAtLeast(0))), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text(stringResource(R.string.upcoming).uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            w.phases.forEachIndexed { i, p ->
                if (i < s.phaseIndex) return@forEachIndexed
                val (color, container) = phaseColors(p.kind)
                val current = i == s.phaseIndex
                Surface(shape = RoundedCornerShape(14.dp), color = if (current) container else MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(20.dp).background(color, CircleShape))
                        Text(phaseName(p.kind), fontWeight = if (current) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                        Text(clock(if (current) s.leftMs + 999 else p.seconds * 1000L), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FullStyle(s: RunUiState) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PhasePill(s)
        Tile(
            if (s.workout != null) stringResource(R.string.remaining_interval) else stringResource(R.string.time),
            clock(if (s.workout != null) s.leftMs + 999 else s.elapsedMs), "", big = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(stringResource(R.string.time), clock(s.elapsedMs), "", Modifier.weight(1f))
            Tile(stringResource(R.string.distance), kmText(s.distanceM), " km", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(stringResource(R.string.pace), paceText(s.paceNowSecPerKm), " /km", Modifier.weight(1f))
            Tile(stringResource(R.string.avg_pace), paceText(s.paceAvgSecPerKm), " /km", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(stringResource(R.string.calories), "%d".format(s.kcal.toInt()), " kcal", Modifier.weight(1f))
            if (s.workout != null) Tile(stringResource(R.string.intervals), "${s.runsDone} / ${s.runsTotal}", "", Modifier.weight(1f))
            else Tile(stringResource(R.string.gps), if (s.gpsOn) (s.gpsAccuracyM?.let { "±${it.toInt()} m" } ?: "…") else stringResource(R.string.gps_off), "", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Tile(label: String, value: String, unit: String, modifier: Modifier = Modifier, big: Boolean = false) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = if (big) 60.sp else 34.sp, fontWeight = FontWeight.Bold, letterSpacing = if (big) (-2).sp else (-1).sp, maxLines = 1)
                if (unit.isNotEmpty()) Text(unit, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 2.dp, bottom = 5.dp))
            }
        }
    }
}

// ---------------------------------------------------------------- controls, lock, finish

@Composable
private fun Controls(s: RunUiState, a: RunActions, onFinish: () -> Unit) {
    val isFree = s.workout == null
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!isFree) {
            FilledTonalIconButton(onClick = a.onPrevious, modifier = Modifier.size(60.dp), colors = IconButtonDefaults.filledTonalIconButtonColors()) {
                Icon(Icons.Filled.SkipPrevious, stringResource(R.string.previous_interval), Modifier.size(28.dp))
            }
        }
        when {
            s.workoutFinished -> Button(onClick = onFinish, modifier = Modifier.weight(1f).height(72.dp)) {
                Icon(Icons.Filled.Stop, null)
                Text(stringResource(R.string.finish), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
            }
            s.paused -> Button(onClick = a.onResume, modifier = Modifier.weight(1f).height(72.dp)) {
                Icon(Icons.Filled.PlayArrow, null)
                Text(stringResource(R.string.resume_run), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
            }
            else -> Button(onClick = a.onPause, modifier = Modifier.weight(1f).height(72.dp)) {
                Icon(Icons.Filled.Pause, null)
                Text(stringResource(R.string.pause), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (!isFree) {
            FilledTonalIconButton(onClick = a.onNext, modifier = Modifier.size(60.dp)) {
                Icon(Icons.Filled.SkipNext, stringResource(R.string.next_interval), Modifier.size(28.dp))
            }
        } else {
            FilledTonalIconButton(onClick = onFinish, modifier = Modifier.size(60.dp)) { Icon(Icons.Filled.Stop, stringResource(R.string.finish), Modifier.size(28.dp)) }
        }
    }
}

/** Blocks every touch, so a phone in a pocket or a wet hand changes nothing. Holding the button opens it again. */
@Composable
private fun LockOverlay(onUnlock: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xCC000000)).pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                shape = CircleShape, color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(96.dp).pointerInput(Unit) { detectTapGestures(onLongPress = { onUnlock() }) },
            ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Lock, null, Modifier.size(40.dp)) } }
            Text(stringResource(R.string.unlock_hint), color = Color.White, textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinishSheet(s: RunUiState, onSave: () -> Unit, onDiscard: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.finish_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            val percent = if (s.plannedMs > 0) (100 * s.elapsedMs / s.plannedMs).toInt().coerceAtMost(100) else null
            Text(
                if (percent != null) stringResource(R.string.finish_progress, clock(s.elapsedMs), (s.plannedMs / 60000).toInt(), percent, kmText(s.distanceM))
                else stringResource(R.string.finish_progress_free, clock(s.elapsedMs), kmText(s.distanceM)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(stringResource(R.string.save_and_finish)) }
            Button(
                onClick = onDiscard, modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
            ) { Text(stringResource(R.string.discard)) }
            androidx.compose.material3.TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.keep_running)) }
        }
    }
}
