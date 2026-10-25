package org.openrun.app.ui.run

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.openrun.app.R
import org.openrun.app.data.RunEntity
import org.openrun.app.ui.components.RankBadge
import org.openrun.app.ui.components.TrackView
import org.openrun.app.ui.components.rankName
import org.openrun.app.ui.components.rankProgressText
import org.openrun.app.ui.openRun
import org.openrun.app.ui.theme.RunTheme
import org.openrun.core.Rank
import org.openrun.core.Ranks
import org.openrun.core.Track
import org.openrun.core.TrackPoint

private data class SummaryData(val run: RunEntity, val points: List<TrackPoint>, val rank: Rank, val promotion: Rank?)

private val feelings = listOf("😫", "😓", "🙂", "😄", "🔥")

/** The end of a session: the route, the figures, where the rank stands (and a promotion if there was one), the feeling. */
@Composable
fun SummaryScreen(runId: Long, onDone: () -> Unit) {
    val app = LocalContext.current.openRun
    val scope = rememberCoroutineScope()
    val data by produceState<SummaryData?>(null, runId) {
        val run = app.runs.run(runId) ?: return@produceState
        val total = app.runs.totalKcal().first()
        value = SummaryData(
            run, app.runs.points(runId).map { TrackPoint(it.lat, it.lon, it.accuracyM, it.timeMs) },
            Ranks.forTotal(total), Ranks.promotion(total - run.kcal, total),
        )
    }
    var feeling by remember { mutableStateOf<Int?>(null) }
    val d = data ?: return
    val run = d.run

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                RankBadge(d.rank.tier, 64.dp)
                Column {
                    Text(stringResource(R.string.well_done), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    if (run.treadmill) Text(stringResource(R.string.gps_off), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (!run.treadmill) TrackView(d.points)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat(stringResource(R.string.time), clock(run.durationSec * 1000L), Modifier.weight(1f))
                Stat(stringResource(R.string.distance), kmText(run.distanceM) + " km", Modifier.weight(1f))
                Stat(stringResource(R.string.avg_pace), paceText(Track.paceSecPerKm(run.distanceM, run.durationSec.toDouble())), Modifier.weight(1f))
            }
            if (run.distanceM >= 100) {
                Text(
                    stringResource(R.string.laps_equiv, kmText1(run.distanceM), "%.0f".format(Track.laps(run.distanceM))),
                    modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            d.promotion?.let { up ->
                Surface(shape = RoundedCornerShape(20.dp), color = RunTheme.colors.goldContainer) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        RankBadge(up.tier, 48.dp)
                        Column {
                            Text(stringResource(R.string.new_rank), style = MaterialTheme.typography.labelLarge, color = androidx.compose.ui.graphics.Color(0xFF3B2200))
                            Text(rankName(up), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF3B2200))
                        }
                    }
                }
            }

            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RankBadge(d.rank.tier, 44.dp)
                        Column(Modifier.weight(1f)) {
                            Text(rankName(d.rank), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.kcal_gained, run.kcal.toInt(), d.rank.kcalToNext), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    LinearProgressIndicator(progress = { d.rank.progress }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                    Text(rankProgressText(d.rank), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
                }
            }

            Text(stringResource(R.string.how_was_it).uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                feelings.forEachIndexed { i, emoji ->
                    val on = feeling == i + 1
                    Surface(
                        onClick = { feeling = i + 1; scope.launch { app.runs.setFeeling(runId, i + 1) } },
                        shape = CircleShape, color = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        border = if (on) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.size(58.dp),
                    ) { Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(emoji, fontSize = 26.sp) } }
                }
            }
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp).height(72.dp)) {
            Text(stringResource(R.string.finish), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
