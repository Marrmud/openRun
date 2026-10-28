package org.openrun.app.ui.run

import android.provider.Settings
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VoiceOverOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VoiceOverOff
import androidx.compose.ui.unit.sp
import org.openrun.app.R
import org.openrun.app.run.RunUiState
import org.openrun.app.ui.components.LineColor
import org.openrun.app.ui.components.PhaseBadge
import org.openrun.app.ui.components.PillTabs
import org.openrun.app.ui.components.WorkoutBar
import org.openrun.app.ui.components.phaseIcon
import org.openrun.app.ui.theme.RunTheme
import org.openrun.core.PhaseKind
import org.openrun.core.ScreenStyle
import kotlin.math.cos
import kotlin.math.sin

data class RunActions(
    val onPause: () -> Unit,
    val onResume: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onRestartPhase: () -> Unit,
    val onToStart: () -> Unit,
    val onGoTo: (Int) -> Unit,
    val onUndoGoTo: () -> Unit,
    val onSave: () -> Unit,
    val onDiscard: () -> Unit,
    val onToggleVoice: () -> Unit,
)

/** The screen of a session in progress, in one of three styles. Everything is large and one tap away. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunScreen(state: RunUiState, voiceOn: Boolean, initialStyle: ScreenStyle, lockAtStart: Boolean, actions: RunActions) {
    var style by remember { mutableStateOf(initialStyle) }
    var menuOpen by remember { mutableStateOf(false) }
    var finishOpen by remember { mutableStateOf(false) }
    // locked from the start if asked: the phone goes straight into the pocket
    var locked by remember { mutableStateOf(lockAtStart) }
    // the lock shows itself for a moment (when set, or when the screen is touched), then lets the run screen be seen
    var lockShown by remember { mutableStateOf(lockAtStart) }
    var pokes by remember { mutableIntStateOf(0) }
    var sliding by remember { mutableStateOf(false) }
    LaunchedEffect(locked, lockShown, pokes, sliding) {
        if (locked && lockShown && !sliding) { delay(2500); lockShown = false }
    }
    val blur by animateDpAsState(if (locked && lockShown) 24.dp else 0.dp, tween(250), label = "lock blur")
    val isFree = state.workout == null
    // a free run has no intervals, so no timeline
    val styles = if (isFree) listOf(ScreenStyle.FOCUS, ScreenStyle.FULL) else ScreenStyle.entries.toList()
    val shownStyle = if (style in styles) style else ScreenStyle.FOCUS
    // the three styles are pages: swipe between them, or tap their tabs
    val pager = rememberPagerState(initialPage = styles.indexOf(shownStyle)) { styles.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager.currentPage) { style = styles[pager.currentPage] }
    fun show(st: ScreenStyle) { scope.launch { pager.animateScrollToPage(styles.indexOf(st)) } }
    // a phase touched in the timeline: the session goes there, with a few seconds to undo it
    var jumpedTo by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(jumpedTo) { if (jumpedTo != null) { delay(4000); jumpedTo = null } }
    val goTo: (Int) -> Unit = { i -> actions.onGoTo(i); jumpedTo = i }

    androidx.activity.compose.BackHandler(enabled = !finishOpen) { if (!locked) finishOpen = true }

    // the whole screen takes the color of the phase (D36): running or walking is seen at arm's length
    val screenColor by animateColorAsState(phaseLook(state.phase?.kind).background, tween(400), label = "phase background")
    Box(Modifier.fillMaxSize().background(screenColor)) {
        Column(Modifier.fillMaxSize().blur(blur).padding(horizontal = 18.dp).padding(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PillTabs(
                styles, shownStyle,
                label = { st -> stringResource(when (st) { ScreenStyle.FOCUS -> R.string.style_focus; ScreenStyle.TIMELINE -> R.string.style_timeline; ScreenStyle.FULL -> R.string.style_full }) },
                onSelect = { show(it) }, modifier = Modifier.padding(top = 8.dp),
                // a touch of ink over the green or yellow, so the white pill stands out
                track = RunTheme.colors.ink.copy(alpha = 0.08f),
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RoundButton(onClick = { locked = true; lockShown = true; pokes++ }) { Icon(Icons.Rounded.Lock, stringResource(R.string.lock)) }
                Text(
                    stateTitle(state), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                )
                RoundButton(onClick = { actions.onToggleVoice() }, on = voiceOn) {
                    Icon(if (voiceOn) Icons.Rounded.RecordVoiceOver else Icons.Rounded.VoiceOverOff, stringResource(R.string.voice))
                }
                Box {
                    RoundButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, null) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (!isFree) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.restart_interval)) }, onClick = { menuOpen = false; actions.onRestartPhase() })
                            DropdownMenuItem(text = { Text(stringResource(R.string.back_to_warmup)) }, onClick = { menuOpen = false; actions.onToStart() })
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.finish_menu), color = MaterialTheme.colorScheme.error) },
                            onClick = { menuOpen = false; finishOpen = true },
                        )
                    }
                }
            }

            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), pageSpacing = 18.dp, verticalAlignment = Alignment.Top) { page ->
                when (styles[page]) {
                    // tapping "Ensuite" opens the timeline, to see what comes after
                    ScreenStyle.FOCUS -> FocusStyle(state, onUpNext = if (ScreenStyle.TIMELINE in styles) ({ show(ScreenStyle.TIMELINE) }) else null)
                    ScreenStyle.TIMELINE -> TimelineStyle(state, onGoTo = goTo)
                    ScreenStyle.FULL -> FullStyle(state)
                }
            }

            Controls(state, actions, onFinish = { finishOpen = true })
        }

        jumpedTo?.let { i ->
            val w = state.workout
            val p = w?.phases?.getOrNull(i)
            if (p != null) {
                val nth = w.phases.take(i + 1).count { it.kind == p.kind }
                val total = if (p.kind == PhaseKind.WALK) w.walkCount else w.runCount
                Surface(
                    shape = RoundedCornerShape(14.dp), color = RunTheme.colors.ink, contentColor = Color.White, shadowElevation = 6.dp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(start = 18.dp, end = 18.dp, bottom = 100.dp).fillMaxWidth(),
                ) {
                    Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(phaseIcon(p.kind), null, Modifier.size(20.dp))
                        Text(stringResource(R.string.went_to, phaseName(p.kind) + phaseCount(p.kind, nth, total)), modifier = Modifier.weight(1f))
                        androidx.compose.material3.TextButton(onClick = { actions.onUndoGoTo(); jumpedTo = null }) {
                            Text(stringResource(R.string.undo), color = RunTheme.colors.walk, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (locked) LockOverlay(
            shown = lockShown,
            onTouch = { lockShown = true; pokes++ },
            onSliding = { sliding = it },
            onUnlock = { locked = false; lockShown = false },
        )
    }

    if (finishOpen) {
        FinishSheet(state, onSave = { finishOpen = false; actions.onSave() }, onDiscard = { finishOpen = false; actions.onDiscard() }, onDismiss = { finishOpen = false })
    }
}

@Composable
private fun stateTitle(s: RunUiState): String {
    val base = if (s.sessionIndex != null && s.programId != null) {
        val program = org.openrun.core.Programs.all.firstOrNull { it.id == s.programId }
        program?.locate(s.sessionIndex)?.let { (w, d) -> stringResource(R.string.week_day_short, w, d) } ?: stringResource(R.string.free_run)
    } else stringResource(R.string.free_run)
    return "$base · ${clock(s.elapsedMs)}"
}

// ---------------------------------------------------------------- styles

/** The color of the whole run screen, also painted under the status and navigation bars (see AppNav). */
@Composable
fun runScreenColor(s: RunUiState?): Color = phaseLook(s?.phase?.kind).background

/** How a phase looks on the run screen (D36). A free run looks like running. */
private data class PhaseLook(val fill: Color, val pill: Color, val onPill: Color, val container: Color, val knob: Color, val background: Color)

@Composable
private fun phaseLook(kind: PhaseKind?): PhaseLook {
    val c = RunTheme.colors
    return when (kind) {
        PhaseKind.WALK -> PhaseLook(c.walk, c.walk, c.onWalk, c.walkContainer, c.ink, c.walkBackground)
        PhaseKind.WARMUP, PhaseKind.COOLDOWN -> PhaseLook(c.calm, c.calmContainer, c.onCalm, c.calmContainer, c.walk, c.calmBackground)
        PhaseKind.RUN, null -> PhaseLook(c.run, c.run, c.onRun, c.runContainer, c.walk, c.runBackground)
    }
}

@Composable
private fun phaseName(kind: PhaseKind?): String = stringResource(when (kind) {
    PhaseKind.WARMUP -> R.string.phase_warmup; PhaseKind.RUN -> R.string.phase_run
    PhaseKind.WALK -> R.string.phase_walk; PhaseKind.COOLDOWN -> R.string.phase_cooldown
    null -> R.string.free_run
})

/** "3 / 6" for the runs and for the walks (D40), nothing for the warm-up and the cool-down. */
private fun phaseCount(kind: PhaseKind?, done: Int, total: Int): String = when (kind) {
    PhaseKind.RUN, PhaseKind.WALK -> if (total > 1) " · $done / $total" else ""
    else -> ""
}

@Composable
private fun PhasePill(s: RunUiState) {
    val look = phaseLook(s.phase?.kind)
    val (container, content) = if (s.workoutFinished) RunTheme.colors.ink to Color.White else look.pill to look.onPill
    val count = when (s.phase?.kind) {
        PhaseKind.WALK -> phaseCount(PhaseKind.WALK, s.walksDone, s.walksTotal)
        else -> phaseCount(s.phase?.kind, s.runsDone, s.runsTotal)
    }
    val label = when {
        s.workoutFinished -> stringResource(R.string.run_done_title)
        s.paused -> stringResource(R.string.paused)
        s.autoPaused -> stringResource(R.string.auto_paused)
        else -> phaseName(s.phase?.kind) + count
    }
    Surface(shape = RoundedCornerShape(16.dp), color = container, contentColor = content) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                s.workoutFinished -> Icon(Icons.Filled.Flag, null, Modifier.size(20.dp), tint = RunTheme.colors.walk)
                s.phase != null -> Icon(phaseIcon(s.phase.kind), null, Modifier.size(20.dp))
            }
            Text(label.uppercase(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun FocusStyle(s: RunUiState, onUpNext: (() -> Unit)?) {
    val look = phaseLook(s.phase?.kind)
    val track = RunTheme.colors.ink.copy(alpha = 0.07f)
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PhasePill(s)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            // the arc fills up as the phase goes by, and the dot rides its end (the sun of the logo)
            val done = when {
                s.workoutFinished -> 1f
                s.phaseMs > 0 -> (1f - s.leftMs.toFloat() / s.phaseMs).coerceIn(0f, 1f)
                else -> 0f
            }
            Canvas(Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp)) {
                val stroke = 28.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                if (s.workout != null) {
                    drawArc(look.fill, -90f, 360f * done, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    if (!s.workoutFinished) {
                        val angle = Math.toRadians(-90.0 + 360.0 * done)
                        val radius = arcSize.width / 2
                        val knob = Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
                        drawCircle(look.background, 17.dp.toPx(), knob)
                        drawCircle(look.knob, 13.dp.toPx(), knob)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (s.workoutFinished) {
                    Icon(Icons.Filled.Check, null, Modifier.size(96.dp), tint = RunTheme.colors.run)
                    Text(stringResource(R.string.run_done_bravo), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        clock(if (s.workout != null) s.leftMs + 999 else s.elapsedMs),
                        fontSize = 88.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-3).sp, maxLines = 1,
                    )
                    Text(stringResource(if (s.workout != null) R.string.remaining else R.string.elapsed), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        val next = s.nextPhase
        if (next != null || s.workoutFinished) {
            Surface(
                shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, border = BorderStroke(1.dp, LineColor),
                modifier = Modifier.fillMaxWidth().then(if (next != null && onUpNext != null) Modifier.clickable(onClick = onUpNext) else Modifier),
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (next != null) {
                        PhaseBadge(next.kind)
                        Text(stringResource(R.string.next_up, phaseName(next.kind), clock(next.seconds * 1000L)), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (onUpNext != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        PhaseBadge(PhaseKind.RUN)
                        Text(stringResource(R.string.runs_all_done, s.runsTotal), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            BigStat(stringResource(R.string.distance), "${kmText(s.distanceM)} km")
            BigStat(stringResource(if (s.workout != null) R.string.time else R.string.pace), if (s.workout != null) clock(s.elapsedMs) else paceText(s.paceNowSecPerKm))
        }
        // a little air between the figures and the buttons
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun BigStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TimelineStyle(s: RunUiState, onGoTo: (Int) -> Unit) {
    val w = s.workout ?: return
    val c = RunTheme.colors
    // the list starts one phase before the current one, so what was just done stays in sight
    val list = rememberLazyListState(initialFirstVisibleItemIndex = (s.phaseIndex - 1).coerceAtLeast(0))
    LaunchedEffect(s.phaseIndex) { list.animateScrollToItem((s.phaseIndex - 1).coerceAtLeast(0)) }
    val fadeTo by animateColorAsState(phaseLook(s.phase?.kind).background, tween(400), label = "fade")
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PhasePill(s)
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 14.dp)) {
                TimelineBar(w, s.phaseIndex, s.workoutFinished, onGoTo)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp).padding(horizontal = 14.dp)) {
                    Text(clock(s.elapsedMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.left_total, clock((s.plannedMs - s.elapsedMs).coerceAtLeast(0))), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

            }
        }
        Text(stringResource(R.string.session).uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(
            state = list, verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(top = 18.dp, bottom = 40.dp),
            modifier = Modifier.fillMaxWidth().weight(1f).drawWithContent {
                drawContent()
                // the list fades into the screen at both ends: there is more above and below
                val h = 46.dp.toPx()
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, fadeTo), startY = size.height - h, endY = size.height), Offset(0f, size.height - h), Size(size.width, h))
                val top = 26.dp.toPx()
                drawRect(Brush.verticalGradient(listOf(fadeTo, Color.Transparent), startY = 0f, endY = top), Offset.Zero, Size(size.width, top))
            },
        ) {
            itemsIndexed(w.phases) { i, p ->
                val look = phaseLook(p.kind)
                val current = i == s.phaseIndex && !s.workoutFinished
                val past = i < s.phaseIndex || s.workoutFinished
                val nth = w.phases.take(i + 1).count { it.kind == p.kind }
                val total = if (p.kind == PhaseKind.WALK) w.walkCount else w.runCount
                Surface(
                    onClick = { if (!current) onGoTo(i) },
                    shape = RoundedCornerShape(14.dp), color = if (current) look.container else MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = if (current) BorderStroke(2.dp, look.fill) else null,
                    modifier = Modifier.fillMaxWidth().alpha(if (past) 0.5f else 1f),
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (past) {
                            Box(Modifier.size(28.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else PhaseBadge(p.kind)
                        Text(phaseName(p.kind) + phaseCount(p.kind, nth, total), fontWeight = if (current) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                        Text(clock(if (current) s.leftMs + 999 else p.seconds * 1000L), fontWeight = FontWeight.SemiBold, color = if (current) c.ink else LocalContentColor.current)
                    }
                }
            }
        }
    }
}

/**
 * The session as bars, each as wide as its phase is long, but never under 16 dp so it can be touched (D42). When it
 * does not fit, it scrolls sideways, fades at its edges and keeps the current phase in the middle. A hint below says
 * what can be done with it.
 */
@Composable
private fun TimelineBar(w: org.openrun.core.Workout, current: Int, finished: Boolean, onTap: (Int) -> Unit) {
    val c = RunTheme.colors
    val gap = 3.dp
    val minWidth = 16.dp
    val edge = 14.dp
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val inner = maxWidth - edge * 2
        val perSecond = (inner - gap * (w.phases.size - 1)) / w.totalSeconds.coerceAtLeast(1)
        val widths = w.phases.map { maxOf(minWidth, perSecond * it.seconds) }
        val total = widths.fold(0.dp) { a, b -> a + b } + gap * (w.phases.size - 1)
        val scrolls = total > inner + 0.5.dp
        val scroll = rememberScrollState()
        val density = LocalDensity.current
        LaunchedEffect(current, scrolls) {
            if (!scrolls) return@LaunchedEffect
            val before = widths.take(current).fold(0.dp) { a, b -> a + b } + gap * current
            val target = with(density) { (before + widths[current.coerceAtMost(widths.lastIndex)] / 2 - inner / 2).roundToPx() }
            scroll.animateScrollTo(target.coerceIn(0, scroll.maxValue))
        }
        val fade = with(density) { edge.toPx() }
        Column {
        Row(
            Modifier.fillMaxWidth().height(56.dp)
                .then(
                    if (scrolls) Modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
                        drawContent()
                        val f = (fade / size.width).coerceIn(0f, 0.5f)
                        drawRect(Brush.horizontalGradient(0f to Color.Transparent, f to Color.Black, 1f - f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                    } else Modifier,
                )
                .horizontalScroll(scroll, enabled = scrolls).padding(horizontal = edge),
            horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Bottom,
        ) {
            w.phases.forEachIndexed { i, p ->
                val (color, fraction) = when (p.kind) {
                    PhaseKind.RUN -> c.run to 1f
                    PhaseKind.WALK -> c.walk to 0.6f
                    PhaseKind.WARMUP, PhaseKind.COOLDOWN -> c.calm to 0.34f
                }
                // the whole height is touchable, even above a short bar
                Box(
                    Modifier.width(widths[i]).fillMaxHeight()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (i != current || finished) onTap(i) },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(Modifier.fillMaxWidth().fillMaxHeight(fraction).alpha(if (i < current || finished) 0.25f else 1f).background(color, RoundedCornerShape(4.dp)))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (scrolls) Icon(Icons.Rounded.Swipe, null, Modifier.size(16.dp).padding(end = 4.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(if (scrolls) R.string.timeline_hint_scroll else R.string.timeline_hint), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        }
    }
}

@Composable
private fun FullStyle(s: RunUiState) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PhasePill(s)
        val look = phaseLook(s.phase?.kind)
        Tile(
            if (s.workout != null) stringResource(R.string.remaining_interval) else stringResource(R.string.time),
            clock(if (s.workout != null) s.leftMs + 999 else s.elapsedMs), "", big = true,
            container = look.pill, content = look.onPill,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(stringResource(R.string.time), clock(s.elapsedMs), "", Modifier.weight(1f))
            Tile(stringResource(R.string.distance), kmText(s.distanceM), " km", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(stringResource(R.string.pace), paceText(s.paceNowSecPerKm), " /km", Modifier.weight(1f))
            Tile(stringResource(R.string.avg_pace), paceText(s.paceAvgSecPerKm), " /km", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(stringResource(R.string.calories), "%d".format(s.kcal.toInt()), " kcal", Modifier.weight(1f))
            if (s.workout != null) Tile(stringResource(R.string.intervals), "${s.runsDone} / ${s.runsTotal}", "", Modifier.weight(1f))
            else Tile(stringResource(R.string.gps), if (s.gpsOn) (s.gpsAccuracyM?.let { "±${it.toInt()} m" } ?: "…") else stringResource(R.string.gps_off), "", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Tile(
    label: String, value: String, unit: String, modifier: Modifier = Modifier, big: Boolean = false,
    container: Color = MaterialTheme.colorScheme.surfaceContainerLowest, content: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(shape = RoundedCornerShape(14.dp), color = container, contentColor = content, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = LocalContentColor.current.copy(alpha = 0.75f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = if (big) 60.sp else 34.sp, fontWeight = FontWeight.Bold, letterSpacing = if (big) (-2).sp else (-1).sp, maxLines = 1)
                if (unit.isNotEmpty()) Text(unit, color = LocalContentColor.current.copy(alpha = 0.75f), modifier = Modifier.padding(start = 2.dp, bottom = 5.dp))
            }
        }
    }
}

// ---------------------------------------------------------------- controls, lock, finish

@Composable
private fun Controls(s: RunUiState, a: RunActions, onFinish: () -> Unit) {
    val isFree = s.workout == null
    if (s.workoutFinished) {
        FinishedButton(onClick = a.onSave)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!isFree) {
            RoundButton(onClick = a.onPrevious, size = 60.dp) {
                Icon(Icons.Rounded.SkipPrevious, stringResource(R.string.previous_interval), Modifier.size(28.dp))
            }
        }
        when {
            s.paused -> Button(onClick = a.onResume, shape = RoundedCornerShape(36.dp), modifier = Modifier.weight(1f).height(72.dp)) {
                Icon(Icons.Filled.PlayArrow, null)
                Text(stringResource(R.string.resume_run), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
            }
            else -> Button(onClick = a.onPause, shape = RoundedCornerShape(36.dp), modifier = Modifier.weight(1f).height(72.dp)) {
                Icon(Icons.Filled.Pause, null)
                Text(stringResource(R.string.pause), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (!isFree) {
            RoundButton(onClick = a.onNext, size = 60.dp) {
                Icon(Icons.Rounded.SkipNext, stringResource(R.string.next_interval), Modifier.size(28.dp))
            }
        } else {
            RoundButton(onClick = onFinish, size = 60.dp) { Icon(Icons.Rounded.Stop, stringResource(R.string.finish), Modifier.size(28.dp)) }
        }
    }
}

/**
 * Nothing left to run: the only button, yellow, with a soft halo pulsing around it so the end cannot be missed (D37).
 * It saves straight away and opens the summary. No pulse when the system asks for fewer animations.
 */
@Composable
private fun FinishedButton(onClick: () -> Unit) {
    val c = RunTheme.colors
    val context = LocalContext.current
    val still = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val pulse = if (still) null else rememberInfiniteTransition(label = "finish pulse").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1800, easing = LinearOutSlowInEasing)), label = "halo",
    )
    val haloMax = with(LocalDensity.current) { 16.dp.toPx() }
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = c.walk, contentColor = c.ink),
        modifier = Modifier.fillMaxWidth().height(72.dp).drawBehind {
            val t = pulse?.value ?: 0.25f
            val spread = haloMax * t
            val alpha = if (pulse == null) 0.4f else 0.8f * (1f - t)
            drawRoundRect(
                c.walk.copy(alpha = alpha), Offset(-spread, -spread), Size(size.width + 2 * spread, size.height + 2 * spread),
                CornerRadius(size.height / 2 + spread),
            )
        },
    ) {
        Icon(Icons.Filled.CheckCircle, null)
        Text(stringResource(R.string.finish_and_summary), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}

/** The white round button of the run screen (lock, voice, menu, previous, next); mint when [on]. */
@Composable
private fun RoundButton(onClick: () -> Unit, size: Dp = 44.dp, on: Boolean = false, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick, shape = CircleShape,
        color = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        border = if (on) null else BorderStroke(1.dp, LineColor), modifier = Modifier.size(size),
    ) { Box(contentAlignment = Alignment.Center) { content() } }
}

/**
 * Blocks every touch, so a phone in a pocket or a wet hand changes nothing. It stays invisible over the run screen and
 * shows itself, as frosted glass over the blurred screen, when set or when the screen is touched; sliding opens it.
 */
@Composable
private fun LockOverlay(shown: Boolean, onTouch: () -> Unit, onSliding: (Boolean) -> Unit, onUnlock: () -> Unit) {
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(250), label = "lock")
    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val e = awaitPointerEvent(PointerEventPass.Initial)
                    if (e.type == PointerEventType.Press) onTouch()
                }
            }
        },
    ) {
        Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }.background(RunTheme.colors.ink.copy(alpha = 0.35f)))
        Column(
            Modifier.align(Alignment.Center).padding(horizontal = 24.dp).graphicsLayer { this.alpha = alpha }
                .fillMaxWidth().clip(RoundedCornerShape(36.dp))
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.12f))))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0.20f))), RoundedCornerShape(36.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)).border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Lock, null, Modifier.size(30.dp), tint = Color.White) }
            Text(stringResource(R.string.screen_locked), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            SlideToUnlock(onSliding = onSliding, onUnlock = onUnlock)
        }
    }
}

/**
 * A glass track with a round thumb: slide it to the end to open, let go before and it springs back. It opens as soon as
 * the thumb reaches the end, or on release past 85 %; a cancelled drag counts as a release, so it never stays stuck.
 */
@Composable
private fun SlideToUnlock(onSliding: (Boolean) -> Unit, onUnlock: () -> Unit) {
    val scope = rememberCoroutineScope()
    var x by remember { mutableFloatStateOf(0f) }
    var opened by remember { mutableStateOf(false) }
    val unlock by rememberUpdatedState(onUnlock)
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(32.dp)).background(Color.White.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(32.dp)).padding(4.dp),
    ) {
        val thumb = 56.dp
        val max = with(LocalDensity.current) { (maxWidth - thumb).toPx() }.coerceAtLeast(1f)
        fun open() { if (!opened) { opened = true; onSliding(false); unlock() } }
        fun release() {
            if (opened) return
            onSliding(false)
            if (x >= max * 0.85f) open()
            else scope.launch { animate(x, 0f, animationSpec = spring(dampingRatio = 0.7f)) { v, _ -> x = v } }
        }
        Text(
            stringResource(R.string.unlock_hint), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1,
            modifier = Modifier.align(Alignment.Center).padding(start = thumb).graphicsLayer { alpha = 1f - (x / max).coerceIn(0f, 1f) },
        )
        Box(
            Modifier.offset { IntOffset(x.roundToInt(), 0) }.size(thumb).clip(CircleShape).background(Color.White.copy(alpha = 0.92f))
                .pointerInput(max) {
                    detectHorizontalDragGestures(
                        onDragStart = { onSliding(true) },
                        onDragEnd = { release() },
                        onDragCancel = { release() },
                    ) { change, delta ->
                        change.consume()
                        x = (x + delta).coerceIn(0f, max)
                        if (x >= max * 0.98f) open()
                    }
                },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.LockOpen, null, tint = RunTheme.colors.ink) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinishSheet(s: RunUiState, onSave: () -> Unit, onDiscard: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.finish_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            val percent = if (s.plannedMs > 0) (100 * s.elapsedMs / s.plannedMs).toInt().coerceAtMost(100) else null
            Text(
                if (percent != null) stringResource(R.string.finish_progress, clock(s.elapsedMs), (s.plannedMs / 60000).toInt(), percent, kmText(s.distanceM))
                else stringResource(R.string.finish_progress_free, clock(s.elapsedMs), kmText(s.distanceM)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(stringResource(R.string.save_and_finish)) }
            Button(
                onClick = onDiscard, modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
            ) { Text(stringResource(R.string.discard)) }
            androidx.compose.material3.TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.keep_running)) }
        }
    }
}
