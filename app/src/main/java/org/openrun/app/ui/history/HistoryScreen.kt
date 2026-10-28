package org.openrun.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.graphics.Color
import org.openrun.app.ui.components.BrandTitle
import org.openrun.app.ui.components.LineColor
import org.openrun.app.ui.components.PillTabs
import org.openrun.app.ui.theme.Fredoka
import org.openrun.app.ui.theme.RunTheme
import org.openrun.app.R
import org.openrun.app.ui.programNameRes
import org.openrun.app.ui.run.clock
import org.openrun.app.ui.run.kmText
import org.openrun.app.ui.run.kmText1
import org.openrun.app.ui.run.paceText
import org.openrun.core.History
import org.openrun.core.HistoryItem
import org.openrun.core.Programs
import org.openrun.core.Track
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class Grouping { DATE, PROGRAM }

/** Every session, newest first, by date like a phone's own history or by program. A watch marks detailed ones. */
@Composable
fun HistoryScreen(items: List<HistoryItem>?, onOpen: (Long) -> Unit) {
    if (items == null) return
    var grouping by remember { mutableStateOf(Grouping.DATE) }
    val totals = History.totals(items)
    val zone = ZoneId.systemDefault()

    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { BrandTitle(stringResource(R.string.tab_history), size = 26, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)) }
        item {
            // the totals on the ink of the logo
            Surface(shape = RoundedCornerShape(28.dp), color = RunTheme.colors.ink, contentColor = Color.White) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(vertical = 18.dp, horizontal = 8.dp)) {
                    Total("${totals.runs}", stringResource(R.string.sessions_label), Modifier.weight(1f))
                    VerticalDivider(color = Color.White.copy(alpha = 0.12f))
                    Total(kmText1(totals.distanceM), "km", Modifier.weight(1f))
                    VerticalDivider(color = Color.White.copy(alpha = 0.12f))
                    Total(durationShort(totals.durationSec), stringResource(R.string.of_running), Modifier.weight(1f))
                }
            }
        }
        if (items.isEmpty()) {
            item { Text(stringResource(R.string.history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp)) }
        } else {
            item {
                PillTabs(
                    Grouping.entries.toList(), grouping,
                    label = { stringResource(if (it == Grouping.DATE) R.string.by_date else R.string.by_program) },
                    onSelect = { grouping = it }, modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            when (grouping) {
                Grouping.DATE -> History.byMonth(items, zone).forEach { month ->
                    item(key = "m${month.year}-${month.month}") {
                        Text(
                            java.time.Month.of(month.month).getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase() + " ${month.year}",
                            fontSize = 12.sp, letterSpacing = 0.8.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(month.items, key = { "r${it.id}" }) { RunRow(it, onOpen) }
                }
                Grouping.PROGRAM -> History.byProgram(items).forEach { group ->
                    item(key = "g${group.programId}") {
                        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(group.programId?.let { programNameRes(it) } ?: R.string.free_runs),
                                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                            )
                            val program = group.programId?.let { id -> Programs.all.firstOrNull { it.id == id } }
                            Text(
                                (if (program != null) "${group.completedCount} / ${program.sessionCount} · " else "") + kmText1(group.distanceM) + " km",
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(group.items, key = { "p${it.id}" }) { RunRow(it, onOpen) }
                }
            }
        }
    }
}

@Composable
private fun Total(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 28.sp)
        Text(label, fontSize = 12.sp, color = Color(0xFFB2DFD8))
    }
}

@Composable
private fun RunRow(item: HistoryItem, onOpen: (Long) -> Unit) {
    Surface(onClick = { onOpen(item.id) }, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, border = BorderStroke(1.dp, LineColor), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(13.dp), color = if (item.completed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (item.completed) Icons.Filled.Check else Icons.Filled.DirectionsRun, null, Modifier.padding(9.dp),
                    tint = if (item.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(runTitle(item), fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(runSubtitle(item), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (item.hasWatchData) Icon(Icons.Filled.Watch, stringResource(R.string.watch_data), Modifier.padding(start = 6.dp).size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${kmText(item.distanceM)} km", fontWeight = FontWeight.Bold, color = RunTheme.colors.ink)
                Text("${clock(item.durationSec * 1000L)} · ${paceText(Track.paceSecPerKm(item.distanceM, item.durationSec.toDouble()))}/km", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun runTitle(item: HistoryItem): String {
    val program = item.programId?.let { id -> Programs.all.firstOrNull { it.id == id } }
    val index = item.sessionIndex
    if (program == null || index == null || index !in 0 until program.sessionCount) return stringResource(R.string.free_run)
    val (w, d) = program.locate(index)
    return stringResource(R.string.week_day, w, d)
}

/** The date, and "partielle" next to it when a program session was cut short. */
@Composable
fun runSubtitle(item: HistoryItem): String {
    val program = item.programId?.let { id -> Programs.all.firstOrNull { it.id == id } }
    val partial = program != null && item.sessionIndex != null && !item.completed
    return dateText(item.startedAt) + if (partial) " · " + stringResource(R.string.partial) else ""
}

fun dateText(epochMs: Long): String =
    DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", Locale.getDefault()).format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
        .replaceFirstChar(Char::titlecase)

private fun durationShort(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h} h %02d".format(m) else "$m min"
}
