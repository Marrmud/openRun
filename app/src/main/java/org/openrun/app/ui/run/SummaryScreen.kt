package org.openrun.app.ui.run

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
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import org.openrun.app.R
import org.openrun.app.data.RunEntity
import org.openrun.app.ui.components.RankBadge
import org.openrun.app.ui.components.rankName
import org.openrun.app.ui.components.rankProgressText
import org.openrun.app.ui.openRun
import org.openrun.core.Rank
import org.openrun.core.Ranks
import org.openrun.core.Track

/** The short version of the end of a session. T5 adds the track, the feeling, the watch data and the rank-up. */
@Composable
fun SummaryScreen(runId: Long, onDone: () -> Unit) {
    val app = LocalContext.current.openRun
    val data by produceState<Pair<RunEntity, Rank>?>(null, runId) {
        val run = app.runs.run(runId)
        if (run != null) value = run to Ranks.forTotal(app.runs.totalKcal().first())
    }
    val (run, rank) = data ?: return
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                RankBadge(rank.tier, 64.dp)
                Text(stringResource(R.string.well_done), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
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
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RankBadge(rank.tier, 44.dp)
                        Column(Modifier.weight(1f)) {
                            Text(rankName(rank), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.kcal_gained, run.kcal.toInt(), rank.kcalToNext), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    LinearProgressIndicator(progress = { rank.progress }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                    Text(rankProgressText(rank), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
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
