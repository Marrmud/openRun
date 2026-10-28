package org.openrun.app.ui.program

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import org.openrun.app.ui.components.BrandProgress
import org.openrun.app.ui.components.BrandTitle
import org.openrun.app.ui.components.LineColor
import org.openrun.app.ui.components.Mascot
import org.openrun.app.ui.components.Mood
import org.openrun.app.R
import org.openrun.app.ui.ProgramViewModel
import org.openrun.app.ui.programNameRes
import org.openrun.app.ui.tipText
import org.openrun.core.JumpPlan
import org.openrun.core.Program
import org.openrun.core.ProgramProgress

/**
 * All the weeks of a program, each with its tip and its days. Tapping a day starts there: sessions skipped over are
 * marked done (or, going back, made to do again), after a confirmation.
 */
@Composable
fun ProgramScreen(vm: ProgramViewModel, onBack: () -> Unit, onStart: (programId: String, index: Int, skipWarmup: Boolean, skipCooldown: Boolean) -> Unit) {
    val program = vm.program
    val progress by vm.progress.collectAsState()
    val current = progress.currentIndex
    val currentWeek = current?.let { program.locate(it).first }

    // weeks open: the current one, then any the user opens; finished weeks start closed
    val open = remember(program.id, currentWeek) { mutableStateListOf<Int>().apply { currentWeek?.let { add(it) } } }
    var pending by remember { mutableStateOf<JumpPlan?>(null) }
    var sheetIndex by remember { mutableStateOf<Int?>(null) }

    fun pick(index: Int) {
        val plan = progress.planJump(index)
        if (plan.isNoop) sheetIndex = index else pending = plan
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 6.dp, end = 18.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
            BrandTitle(stringResource(programNameRes(program.id)), size = 24)
        }
        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(
                    pluralStringResource(R.plurals.weeks_count, program.weeks.size, program.weeks.size) + " · " +
                        stringResource(R.string.sessions_done, progress.done.size, program.sessionCount),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BrandProgress(progress.done.size.toFloat() / program.sessionCount.coerceAtLeast(1), modifier = Modifier.padding(top = 8.dp))
            }
            items(program.weeks, key = { it.number }) { week ->
                val first = program.indexOf(week.number, 1)
                val allDone = week.sessions.indices.all { (first + it) in progress.done }
                val expanded = week.number in open
                Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, border = androidx.compose.foundation.BorderStroke(1.dp, LineColor)) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(
                            Modifier.fillMaxWidth().clickable { if (expanded) open.remove(week.number) else open.add(week.number) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BrandTitle(stringResource(R.string.week_n, week.number), size = 20, modifier = Modifier.weight(1f))
                            if (allDone) Text("${week.sessions.size}/${week.sessions.size} ✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(end = 8.dp))
                            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                        }
                        if (expanded) {
                            val tip = tipText(week.tipKey)
                            if (tip.isNotEmpty()) {
                                // the mascot says the tip of the week, in the mint of the logo
                                Row(
                                    Modifier.padding(top = 10.dp, bottom = 12.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFE6F4F0)).padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Mascot(Mood.FRONT, 30.dp)
                                    Text(tip, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                week.sessions.forEachIndexed { d, workout ->
                                    DayButton(
                                        day = d + 1, minutes = workout.totalSeconds / 60,
                                        done = (first + d) in progress.done, current = (first + d) == current,
                                        onClick = { pick(first + d) }, modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pending?.let { plan ->
        val forward = plan.markDone.isNotEmpty()
        val n = if (forward) plan.markDone.size else plan.unmarkDone.size
        val (w, d) = program.locate(plan.target)
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.start_at, w, d)) },
            text = {
                Text(
                    pluralStringResource(if (forward) R.plurals.jump_forward else R.plurals.jump_back, n, n) + " " +
                        stringResource(R.string.jump_undo_hint),
                )
            },
            confirmButton = {
                Button(onClick = { vm.pick(plan) { sheetIndex = plan.target }; pending = null }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    sheetIndex?.let { index ->
        SessionSheet(
            program = program, index = index, onDismiss = { sheetIndex = null },
            onStart = { sw, sc -> sheetIndex = null; onStart(program.id, index, sw, sc) },
        )
    }
}

@Composable
private fun DayButton(day: Int, minutes: Int, done: Boolean, current: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val container = when {
        current -> MaterialTheme.colorScheme.primary
        done -> MaterialTheme.colorScheme.primaryContainer
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val content = if (current) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp), color = container, contentColor = content,
        border = if (!current && !done) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) {
        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (done && !current) Icon(Icons.Filled.CheckCircle, null, Modifier.padding(end = 4.dp), tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.day_n, day), fontWeight = FontWeight.Bold)
            }
            Text(stringResource(R.string.about_minutes, minutes), fontSize = 12.sp, color = if (current) Color(0xFFCDEBE4) else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
