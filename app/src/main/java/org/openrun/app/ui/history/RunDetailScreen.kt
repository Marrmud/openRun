package org.openrun.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.openrun.app.R
import org.openrun.app.data.PhaseResultEntity
import org.openrun.app.data.RunEntity
import org.openrun.app.data.TrackPointEntity
import org.openrun.app.ui.components.TrackView
import org.openrun.app.ui.openRun
import org.openrun.app.ui.run.clock
import org.openrun.app.ui.run.kmText
import org.openrun.app.ui.run.kmText1
import org.openrun.app.ui.run.paceText
import org.openrun.app.ui.components.PhaseBadge
import org.openrun.core.HistoryItem
import org.openrun.core.PhaseKind
import org.openrun.core.Track
import org.openrun.core.TrackPoint

private data class Detail(val run: RunEntity, val points: List<TrackPointEntity>, val phases: List<PhaseResultEntity>, val completed: Boolean)

/** One session: its route, its figures, how each phase went, and what the watch added when there is one. */
@Composable
fun RunDetailScreen(runId: Long, onBack: () -> Unit) {
    val app = LocalContext.current.openRun
    val scope = rememberCoroutineScope()
    val detail by produceState<Detail?>(null, runId) {
        val run = app.runs.run(runId)
        if (run != null) value = Detail(run, app.runs.points(runId), app.runs.phases(runId), app.runs.isCompleted(runId))
    }
    var confirmDelete by remember { mutableStateOf(false) }
    val d = detail ?: return
    val run = d.run

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
            Column(Modifier.weight(1f)) {
                val item = HistoryItem(run.id, run.startedAt, run.programId, run.sessionIndex, d.completed, run.durationSec, run.distanceM, run.kcal, false)
                Text(runTitle(item), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(runSubtitle(item), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TrackView(d.points.map { TrackPoint(it.lat, it.lon, it.accuracyM, it.timeMs) })

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tile(stringResource(R.string.time), clock(run.durationSec * 1000L), Modifier.weight(1f))
                Tile(stringResource(R.string.distance), kmText(run.distanceM) + " km", Modifier.weight(1f))
                Tile(stringResource(R.string.avg_pace), paceText(Track.paceSecPerKm(run.distanceM, run.durationSec.toDouble())), Modifier.weight(1f))
            }
            if (run.distanceM >= 100) {
                Text(stringResource(R.string.laps_equiv, kmText1(run.distanceM), "%.0f".format(Track.laps(run.distanceM))), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tile(stringResource(R.string.calories), "${run.kcal.toInt()} kcal", Modifier.weight(1f))
                if (run.treadmill) Tile(stringResource(R.string.gps), stringResource(R.string.gps_off), Modifier.weight(1f))
                if (run.elevationGainM != null) Tile(stringResource(R.string.elevation), "+${run.elevationGainM.toInt()} m", Modifier.weight(1f))
            }

            if (run.source == "watch" || run.avgHr != null) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Watch, null, tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.watch_data), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    run.avgHr?.let { Tile(stringResource(R.string.hr_avg), "$it", Modifier.weight(1f)) }
                    run.maxHr?.let { Tile(stringResource(R.string.hr_max), "$it", Modifier.weight(1f)) }
                    run.avgCadence?.let { Tile(stringResource(R.string.cadence), "$it", Modifier.weight(1f)) }
                }
            }

            if (d.phases.isNotEmpty()) {
                Text(stringResource(R.string.laps).uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        var runNumber = 0
                        var walkNumber = 0
                        d.phases.forEach { p ->
                            val kind = runCatching { PhaseKind.valueOf(p.kind) }.getOrDefault(PhaseKind.RUN)
                            if (kind == PhaseKind.RUN) runNumber++
                            if (kind == PhaseKind.WALK) walkNumber++
                            PhaseLine(kind, when (kind) { PhaseKind.RUN -> runNumber; PhaseKind.WALK -> walkNumber; else -> null }, p)
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        scope.launch {
                            app.runs.deleteRun(runId)
                            app.appScope.launch { app.health.delete(runId) }
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun PhaseLine(kind: PhaseKind, number: Int?, p: PhaseResultEntity) {
    val name = when (kind) {
        PhaseKind.WARMUP -> stringResource(R.string.phase_warmup)
        PhaseKind.COOLDOWN -> stringResource(R.string.phase_cooldown)
        PhaseKind.WALK -> stringResource(R.string.phase_walk) + " $number"
        PhaseKind.RUN -> stringResource(R.string.phase_run) + " $number"
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        PhaseBadge(kind, size = 24.dp)
        Text(name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.4f).padding(start = 8.dp))
        Text(clock(p.seconds * 1000L), modifier = Modifier.weight(1f))
        Text(if (p.distanceM > 0) "${kmText(p.distanceM)} km" else "–", modifier = Modifier.weight(1.1f))
        Text(paceText(Track.paceSecPerKm(p.distanceM, p.seconds.toDouble())), modifier = Modifier.weight(0.9f))
    }
}

@Composable
private fun Tile(label: String, value: String, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
