package org.openrun.app.ui.program

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openrun.app.R
import org.openrun.app.ui.components.PhaseBadge
import org.openrun.app.ui.components.WorkoutBar
import org.openrun.app.ui.theme.RunTheme
import org.openrun.app.ui.tipText
import org.openrun.core.PhaseKind
import org.openrun.core.Program

/** The session in detail, with the choice to skip the warm-up or the cool-down. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSheet(program: Program, index: Int, onDismiss: () -> Unit, onStart: (skipWarmup: Boolean, skipCooldown: Boolean) -> Unit) {
    val full = program.sessions[index]
    val (week, day) = program.locate(index)
    var skipWarmup by remember { mutableStateOf(false) }
    var skipCooldown by remember { mutableStateOf(false) }
    val shown = full.let { if (skipWarmup) it.withoutWarmup() else it }.let { if (skipCooldown) it.withoutCooldown() else it }
    val c = RunTheme.colors

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.week_day, week, day), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.session_minutes, shown.totalSeconds / 60), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            val tip = tipText(program.weeks[week - 1].tipKey)
            if (tip.isNotEmpty()) {
                Surface(shape = RoundedCornerShape(12.dp), color = c.goldContainer) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Lightbulb, null, tint = c.gold)
                        Text(tip, style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color(0xFF3B2200))
                    }
                }
            }

            WorkoutBar(shown)

            PhaseSummary(shown)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.skip_warmup), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.skip_warmup_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = skipWarmup, onCheckedChange = { skipWarmup = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.skip_cooldown), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = skipCooldown, onCheckedChange = { skipCooldown = it })
            }

            Button(onClick = { onStart(skipWarmup, skipCooldown) }, modifier = Modifier.fillMaxWidth().height(72.dp)) {
                Icon(Icons.Filled.PlayArrow, null)
                Text(stringResource(R.string.begin), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** Warm-up, then "Course 1 min 30 ×6", "Marche 2 min ×6", then cool-down: short enough to read in a glance. */
@Composable
private fun PhaseSummary(workout: org.openrun.core.Workout) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val phases = workout.phases
    data class Line(val kind: PhaseKind, val label: String)
    val lines = buildList {
        phases.firstOrNull()?.takeIf { it.kind == PhaseKind.WARMUP }?.let { add(Line(PhaseKind.WARMUP, durationText(context, it.seconds))) }
        val middle = phases.filter { it.kind == PhaseKind.RUN || it.kind == PhaseKind.WALK }
        for (kind in listOf(PhaseKind.RUN, PhaseKind.WALK)) {
            val ofKind = middle.filter { it.kind == kind }.map { it.seconds }
            if (ofKind.isEmpty()) continue
            val text = if (ofKind.distinct().size == 1) durationText(context, ofKind.first()) + (if (ofKind.size > 1) " ×${ofKind.size}" else "")
            else ofKind.joinToString(" · ") { durationText(context, it) }
            add(Line(kind, text))
        }
        phases.lastOrNull()?.takeIf { it.kind == PhaseKind.COOLDOWN }?.let { add(Line(PhaseKind.COOLDOWN, durationText(context, it.seconds))) }
    }
    Column {
        lines.forEach { l ->
            val name = when (l.kind) {
                PhaseKind.WARMUP -> R.string.phase_warmup; PhaseKind.RUN -> R.string.phase_run
                PhaseKind.WALK -> R.string.phase_walk; PhaseKind.COOLDOWN -> R.string.phase_cooldown
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                PhaseBadge(l.kind)
                Text(stringResource(name), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 10.dp))
                Text(l.label, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun durationText(context: android.content.Context, seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return when {
        m == 0 -> context.getString(R.string.duration_s, s)
        s == 0 -> context.getString(R.string.duration_m, m)
        else -> context.getString(R.string.duration_ms, m, s)
    }
}
