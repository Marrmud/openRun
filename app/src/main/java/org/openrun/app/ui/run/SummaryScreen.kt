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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.style.TextAlign
import org.openrun.app.ui.components.BrandCard
import org.openrun.app.ui.components.BrandProgress
import org.openrun.app.ui.components.BrandTitle
import org.openrun.app.ui.components.Label
import org.openrun.app.ui.components.LineColor
import org.openrun.app.ui.components.Mascot
import org.openrun.app.ui.components.Mood
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

// the feeling is picked with the faces of the mascot (1 = very hard … 5 = easy)
private val feelings = listOf(Mood.TIRED, Mood.MEH, Mood.FRONT, Mood.HAPPY, Mood.COOL)

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
            // the mascot celebrates, standing and happy
            Column(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Mascot(Mood.HAPPY, 92.dp, legs = true)
                BrandTitle(stringResource(R.string.well_done), size = 30, modifier = Modifier.padding(top = 6.dp))
                if (run.treadmill) Text(stringResource(R.string.gps_off), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, fontSize = 13.sp,
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

            BrandCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RankBadge(d.rank.tier, 54.dp)
                    Column(Modifier.weight(1f)) {
                        BrandTitle(rankName(d.rank), size = 18)
                        Text(stringResource(R.string.kcal_gained, run.kcal.toInt(), d.rank.kcalToNext), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                BrandProgress(d.rank.progress, yellow = true, modifier = Modifier.padding(top = 10.dp))
                Text(rankProgressText(d.rank), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }

            Label(stringResource(R.string.how_was_it))
            val captions = stringArrayResource(R.array.feelings)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                feelings.forEachIndexed { i, mood ->
                    val on = feeling == i + 1
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            onClick = { feeling = i + 1; scope.launch { app.runs.setFeeling(runId, i + 1) } },
                            shape = RoundedCornerShape(20.dp), color = if (on) Color(0xFFE6F4F0) else MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = if (on) BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, LineColor),
                            modifier = Modifier.size(62.dp),
                        ) { Box(contentAlignment = Alignment.Center) { Mascot(mood, 40.dp) } }
                        Text(captions[i], fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Button(onClick = onDone, shape = RoundedCornerShape(34.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp).height(68.dp)) {
            Text(stringResource(R.string.finish), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, border = BorderStroke(1.dp, LineColor), modifier = modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, maxLines = 1, color = RunTheme.colors.ink)
        }
    }
}
