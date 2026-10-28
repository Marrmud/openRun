package org.openrun.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openrun.app.R
import org.openrun.app.ui.HomeState
import org.openrun.app.ui.components.BrandCard
import org.openrun.app.ui.components.GoButton
import org.openrun.app.ui.components.Highlight
import org.openrun.app.ui.components.BrandProgress
import org.openrun.app.ui.components.BrandTitle
import org.openrun.app.ui.components.Label
import org.openrun.app.ui.components.LineColor
import org.openrun.app.ui.components.Mascot
import org.openrun.app.ui.components.Mood
import org.openrun.app.ui.components.RankBadge
import org.openrun.app.ui.components.TipBubble
import org.openrun.app.ui.components.Wordmark
import org.openrun.app.ui.theme.RunTheme
import org.openrun.app.ui.tipText
import org.openrun.app.ui.components.WorkoutBar
import org.openrun.app.ui.components.rankName
import org.openrun.app.ui.components.rankProgressText
import org.openrun.app.ui.program.SessionSheet
import org.openrun.app.ui.programNameRes
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
    /** First visit of the launch, or the Accueil tab tapped again: the start button flashes. */
    highlight: Highlight = Highlight.None,
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
        Row(Modifier.fillMaxWidth().padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Wordmark(32.dp)
                Text(
                    LocalDate.now().let {
                        "${it.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar(Char::titlecase)} ${it.dayOfMonth} " +
                            it.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    },
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp),
                )
            }
            Surface(onClick = onSettings, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerLowest, border = BorderStroke(1.dp, LineColor), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings)) }
            }
        }

        // the chips go to the next line when they do not fit side by side (large text), and grow instead of clipping
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.streak > 0) {
                Surface(shape = RoundedCornerShape(17.dp), color = RunTheme.colors.walkContainer, contentColor = RunTheme.colors.onWalk) {
                    Row(Modifier.heightIn(min = 34.dp).padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.LocalFireDepartment, null, Modifier.size(18.dp), tint = Color(0xFFD98A00))
                        Text(pluralStringResource(R.plurals.week_streak, state.streak, state.streak), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Surface(shape = RoundedCornerShape(17.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Row(Modifier.heightIn(min = 34.dp).padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RankBadge(state.rank.tier, 26.dp)
                    Text("${rankName(state.rank)} · ${rankProgressText(state.rank)}", style = MaterialTheme.typography.labelLarge)
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
            // a session is running (the app was left during it): the card of the day becomes the way back to it
            active != null -> ActiveCard(active, onResume, highlight)
            program == null -> WelcomeCard(onChooseStart)
            progress?.currentIndex == null -> DoneCard(onChooseStart)
            else -> TodayCard(
                programId = program.id, index = progress.currentIndex!!, state = state,
                onStart = { id, i -> onStart(id, i, false, false) }, onPreview = { _, i -> preview = i }, onOther = onOtherSession, highlight = highlight,
            )
        }

        // free run: not in the mockup, kept as a clear green outline so it reads apart from the green card
        if (active == null) OutlinedButton(
            onClick = onFreeRun, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(26.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, contentColor = MaterialTheme.colorScheme.primary),
        ) {
            Icon(Icons.Rounded.DirectionsRun, null, Modifier.size(20.dp))
            Text(stringResource(R.string.free_run), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp))
        }

        if (program != null && progress?.currentIndex != null) {
            val tip = tipText(program.weeks[program.locate(progress.currentIndex!!).first - 1].tipKey)
            if (tip.isNotEmpty()) TipBubble(tip, title = stringResource(R.string.tip_of_week))
        }

        WeekCard(state)

        if (program != null && progress != null && !progress.isFinished) ProgramCard(program, progress)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun TodayCard(
    programId: String, index: Int, state: HomeState,
    onStart: (String, Int) -> Unit, onPreview: (String, Int) -> Unit, onOther: (String) -> Unit, highlight: Highlight,
) {
    val program = state.program!!
    val workout = program.sessions[index]
    val (week, day) = program.locate(index)
    HeroCard {
        Label(stringResource(R.string.today), color = Color(0xFFB2DFD8))
        BrandTitle(stringResource(R.string.week_day, week, day), size = 26, color = Color.White, modifier = Modifier.padding(top = 6.dp))
        Text(stringResource(programNameRes(program.id)), style = MaterialTheme.typography.bodyMedium, color = HeroMeta)
        WorkoutBar(workout, Modifier.padding(top = 16.dp, bottom = 8.dp), onDark = true)
        Text(stringResource(R.string.session_summary, workout.totalSeconds / 60, workout.runCount), style = MaterialTheme.typography.bodyMedium, color = HeroMeta)
        GoButton(stringResource(R.string.start), onClick = { onStart(programId, index) }, modifier = Modifier.padding(top = 16.dp), flash = highlight.play())
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { onPreview(programId, index) }) { Text(stringResource(R.string.see_session), color = Color.White, fontWeight = FontWeight.Bold) }
            TextButton(onClick = { onOther(programId) }) { Text(stringResource(R.string.other_session), color = Color.White, fontWeight = FontWeight.Bold) }
        }
    }
}

private val HeroMeta = Color(0xFFCDEBE4)

/** The session left running: its name, how far it is, and a white "Reprendre" that flashes like "Démarrer". */
@Composable
private fun ActiveCard(active: org.openrun.app.run.RunUiState, onResume: () -> Unit, highlight: Highlight) {
    val program = active.programId?.let { id -> org.openrun.core.Programs.all.firstOrNull { it.id == id } }
    val title = active.sessionIndex?.let { i -> program?.locate(i)?.let { (w, d) -> stringResource(R.string.week_day, w, d) } } ?: stringResource(R.string.free_run)
    HeroCard {
        Label(stringResource(R.string.session_running), color = Color(0xFFB2DFD8))
        BrandTitle(title, size = 26, color = Color.White, modifier = Modifier.padding(top = 6.dp))
        Text(
            org.openrun.app.ui.run.clock(active.elapsedMs) + " · " + org.openrun.app.ui.run.kmText(active.distanceM) + " km",
            style = MaterialTheme.typography.bodyMedium, color = HeroMeta,
        )
        GoButton(
            stringResource(R.string.resume_run), onClick = onResume, modifier = Modifier.padding(top = 16.dp), flash = highlight.play(),
            container = Color.White, content = MaterialTheme.colorScheme.primary,
        )
    }
}

/** The green card of the day, with the faint ring of the logo in its corner. */
@Composable
private fun HeroCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primary, contentColor = Color.White) {
        Box {
            Canvas(Modifier.matchParentSize()) {
                val r = 100.dp.toPx() - 13.dp.toPx()
                drawCircle(Color.White.copy(alpha = 0.06f), r, Offset(size.width + 60.dp.toPx() - 100.dp.toPx(), 100.dp.toPx() - 70.dp.toPx()), style = Stroke(26.dp.toPx()))
            }
            Column(Modifier.padding(20.dp), content = content)
        }
    }
}

/** Where the program stands: sessions done out of all, as the yellow bar of a milestone. */
@Composable
private fun ProgramCard(program: org.openrun.core.Program, progress: org.openrun.core.ProgramProgress) {
    BrandCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.Flag, null, tint = Color(0xFFD98A00))
            Column(Modifier.weight(1f)) {
                Text(stringResource(programNameRes(program.id)), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(stringResource(R.string.sessions_done, progress.done.size, progress.sessionCount), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        BrandProgress(progress.done.size.toFloat() / progress.sessionCount.coerceAtLeast(1), yellow = true, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun WelcomeCard(onChoose: () -> Unit) {
    HeroCard {
        BrandTitle(stringResource(R.string.welcome_title), size = 26, color = Color.White)
        Text(stringResource(R.string.welcome_body), style = MaterialTheme.typography.bodyMedium, color = HeroMeta, modifier = Modifier.padding(top = 6.dp))
        GoButton(stringResource(R.string.choose_start), onClick = onChoose, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun DoneCard(onChoose: () -> Unit) {
    HeroCard {
        Mascot(Mood.HAPPY, 72.dp, legs = true, modifier = Modifier.align(Alignment.CenterHorizontally))
        BrandTitle(stringResource(R.string.program_done_title), size = 26, color = Color.White, modifier = Modifier.padding(top = 8.dp))
        Text(stringResource(R.string.program_done_body), style = MaterialTheme.typography.bodyMedium, color = HeroMeta, modifier = Modifier.padding(top = 6.dp))
        GoButton(stringResource(R.string.choose_next_program), onClick = onChoose, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun WeekCard(state: HomeState) {
    BrandCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label(stringResource(R.string.this_week), modifier = Modifier.weight(1f))
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

@Composable
private fun DayDot(done: Boolean, today: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val base = Modifier.size(34.dp)
    when {
        done -> Box(base.clip(CircleShape).background(primary), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
        }
        // today: green ring with a soft halo
        today -> Box(
            base.border(BorderStroke(4.dp, MaterialTheme.colorScheme.primaryContainer), CircleShape).padding(4.dp)
                .border(BorderStroke(2.dp, primary), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Box(Modifier.size(4.dp).clip(CircleShape).background(primary)) }
        else -> Box(base.border(BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant), CircleShape))
    }
}
