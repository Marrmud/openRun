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
        item { Text(stringResource(R.string.tab_history), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp)) }
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    Total("${totals.runs}", stringResource(R.string.sessions_label))
                    Total(kmText1(totals.distanceM), "km")
                    Total(durationShort(totals.durationSec), stringResource(R.string.of_running))
                }
            }
        }
        if (items.isEmpty()) {
            item { Text(stringResource(R.string.history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp)) }
        } else {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    SegmentedButton(grouping == Grouping.DATE, { grouping = Grouping.DATE }, SegmentedButtonDefaults.itemShape(0, 2)) { Text(stringResource(R.string.by_date)) }
                    SegmentedButton(grouping == Grouping.PROGRAM, { grouping = Grouping.PROGRAM }, SegmentedButtonDefaults.itemShape(1, 2)) { Text(stringResource(R.string.by_program)) }
                }
            }
            when (grouping) {
                Grouping.DATE -> History.byMonth(items, zone).forEach { month ->
                    item(key = "m${month.year}-${month.month}") {
                        Text(
                            java.time.Month.of(month.month).getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase() + " ${month.year}",
                            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
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
private fun Total(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun RunRow(item: HistoryItem, onOpen: (Long) -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth().clickable { onOpen(item.id) }) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = CircleShape, color = if (item.completed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
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
                Text("${kmText(item.distanceM)} km", fontWeight = FontWeight.Bold)
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
