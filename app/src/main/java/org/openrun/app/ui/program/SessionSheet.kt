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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openrun.app.R
import org.openrun.app.ui.components.BrandTitle
import org.openrun.app.ui.components.GoButton
import org.openrun.app.ui.components.LineColor
import org.openrun.app.ui.components.PhaseBadge
import org.openrun.app.ui.components.TipBubble
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

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BrandTitle(stringResource(R.string.week_day, week, day))
            Text(stringResource(R.string.session_minutes, shown.totalSeconds / 60), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            val tip = tipText(program.weeks[week - 1].tipKey)
            if (tip.isNotEmpty()) TipBubble(tip, mascotSize = 34.dp, mint = true)

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

            GoButton(stringResource(R.string.begin), onClick = { onStart(skipWarmup, skipCooldown) })
        }
    }
}

/**
 * The session as it is run (D42): the warm-up, the block that repeats in its order with "2 ×" on it, what is left, the
 * cool-down. One way to show simple sessions ("6 × run 1 min 30, walk 2 min") and mixed ones alike.
 */
@Composable
private fun PhaseSummary(workout: org.openrun.core.Workout) {
    val l = org.openrun.core.SessionLayouts.of(workout)
    Column {
        l.warmup?.let { PhaseLine(it) }
        if (l.times > 1) {
            Box(Modifier.fillMaxWidth().padding(top = 13.dp, bottom = 4.dp)) {
                Surface(
                    shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LineColor), modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        l.block.forEachIndexed { i, ph ->
                            if (i > 0) androidx.compose.material3.HorizontalDivider(color = LineColor)
                            PhaseLine(ph)
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(9.dp), color = RunTheme.colors.ink, contentColor = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp).offset(y = (-11).dp),
                ) { Text("${l.times} ×", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)) }
            }
        } else l.block.forEach { PhaseLine(it) }
        l.rest.forEach { PhaseLine(it) }
        l.cooldown?.let { PhaseLine(it) }
    }
}

@Composable
private fun PhaseLine(phase: org.openrun.core.Phase) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val name = when (phase.kind) {
        PhaseKind.WARMUP -> R.string.phase_warmup; PhaseKind.RUN -> R.string.phase_run
        PhaseKind.WALK -> R.string.phase_walk; PhaseKind.COOLDOWN -> R.string.phase_cooldown
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        PhaseBadge(phase.kind)
        Text(stringResource(name), fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f).padding(start = 10.dp))
        Text(durationText(context, phase.seconds), fontWeight = FontWeight.SemiBold)
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
