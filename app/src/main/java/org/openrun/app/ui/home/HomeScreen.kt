package org.openrun.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openrun.app.R
import org.openrun.app.ui.HomeState
import org.openrun.app.ui.components.RankBadge
import org.openrun.app.ui.components.WorkoutBar
import org.openrun.app.ui.components.rankName
import org.openrun.app.ui.components.rankProgressText
import org.openrun.app.ui.program.SessionSheet
import org.openrun.app.ui.programNameRes
import org.openrun.app.ui.theme.RunTheme
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    state: HomeState?,
    onStart: (programId: String, index: Int, skipWarmup: Boolean, skipCooldown: Boolean) -> Unit,
    onOtherSession: (programId: String) -> Unit,
    onChooseStart: () -> Unit,
    onFreeRun: () -> Unit,
    onSettings: () -> Unit,
    active: org.openrun.app.run.RunUiState? = null,
    onResume: () -> Unit = {},
    interrupted: org.openrun.app.ui.HomeViewModel.Interrupted? = null,
    onSaveInterrupted: () -> Unit = {},
    onDiscardInterrupted: () -> Unit = {},
) {
    if (state == null) { Box(Modifier.fillMaxSize()); return }
    var preview by remember { mutableStateOf<Int?>(null) }
    val previewProgram = state.program
    if (preview != null && previewProgram != null) {
        SessionSheet(
            program = previewProgram, index = preview!!, onDismiss = { preview = null },
            onStart = { sw, sc -> val i = preview!!; preview = null; onStart(previewProgram.id, i, sw, sc) },
        )
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    LocalDate.now().let {
                        "${it.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar(Char::titlecase)} ${it.dayOfMonth} " +
                            it.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    },
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, stringResource(R.string.settings)) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.streak > 0) {
                AssistChip(
                    onClick = {},
                    label = { Text(pluralStringResource(R.plurals.week_streak, state.streak, state.streak)) },
                    leadingIcon = { Icon(Icons.Filled.LocalFireDepartment, null, Modifier.size(18.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = RunTheme.colors.goldContainer,
                        labelColor = Color(0xFF3B2200), leadingIconContentColor = RunTheme.colors.gold,
                    ),
                    border = null,
                )
            }
            AssistChip(
                onClick = {},
                label = { Text("${rankName(state.rank)} · ${rankProgressText(state.rank)}") },
                leadingIcon = { RankBadge(state.rank.tier, 24.dp) },
            )
        }

        if (active != null) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.session_running), fontWeight = FontWeight.Bold)
                        Text(org.openrun.app.ui.run.clock(active.elapsedMs), style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(
                        onClick = onResume,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimary, contentColor = MaterialTheme.colorScheme.primary),
                    ) { Text(stringResource(R.string.resume_run)) }
                }
            }
        }
        if (interrupted != null && active == null) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
                Column(Modifier.padding(18.dp)) {
                    Text(stringResource(R.string.interrupted_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(
                            R.string.interrupted_body, org.openrun.app.ui.run.clock(interrupted.checkpoint.activeMs),
                            org.openrun.app.ui.run.kmText(interrupted.distanceM),
                        ),
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp),
                    )
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onSaveInterrupted) { Text(stringResource(R.string.keep_it)) }
                        TextButton(onClick = onDiscardInterrupted) { Text(stringResource(R.string.throw_away), color = MaterialTheme.colorScheme.onErrorContainer) }
                    }
                }
            }
        }

        val program = state.program
        val progress = state.progress
        when {
            program == null -> WelcomeCard(onChooseStart)
            progress?.currentIndex == null -> DoneCard(onChooseStart)
            else -> TodayCard(
                programId = program.id, index = progress.currentIndex!!, state = state,
                onStart = { id, i -> onStart(id, i, false, false) }, onPreview = { _, i -> preview = i }, onOther = onOtherSession,
            )
        }

        FilledTonalButton(onClick = onFreeRun, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(stringResource(R.string.free_run))
        }

        WeekCard(state)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun TodayCard(
    programId: String, index: Int, state: HomeState,
    onStart: (String, Int) -> Unit, onPreview: (String, Int) -> Unit, onOther: (String) -> Unit,
) {
    val program = state.program!!
    val workout = program.sessions[index]
    val (week, day) = program.locate(index)
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer, contentColor = onContainer) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.today).uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.week_day, week, day), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
            Text(stringResource(programNameRes(program.id)), style = MaterialTheme.typography.bodyMedium)
            WorkoutBar(workout, Modifier.padding(top = 16.dp, bottom = 8.dp))
            Text(
                stringResource(R.string.session_summary, workout.totalSeconds / 60, workout.runCount),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = { onStart(programId, index) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(72.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, null)
                Text(stringResource(R.string.start), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { onPreview(programId, index) }) { Text(stringResource(R.string.see_session), color = onContainer) }
                TextButton(onClick = { onOther(programId) }) { Text(stringResource(R.string.other_session), color = onContainer) }
            }
        }
    }
}

@Composable
private fun WelcomeCard(onChoose: () -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.welcome_body), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            Button(onClick = onChoose, modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(64.dp)) {
                Text(stringResource(R.string.choose_start), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun DoneCard(onChoose: () -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.program_done_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.program_done_body), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            Button(onClick = onChoose, modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(64.dp)) {
                Text(stringResource(R.string.choose_next_program), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun WeekCard(state: HomeState) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.this_week).uppercase(), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${state.runsThisWeek} / ${state.settings.weeklyGoal}", fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                state.week.forEach { d ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(d.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        DayDot(d.done, d.today)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDot(done: Boolean, today: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val base = Modifier.size(32.dp).clip(CircleShape)
    if (done) {
        Box(base.background(primary), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
        }
    } else {
        Box(base.border(BorderStroke(2.dp, if (today) primary else MaterialTheme.colorScheme.outlineVariant), CircleShape))
    }
}
