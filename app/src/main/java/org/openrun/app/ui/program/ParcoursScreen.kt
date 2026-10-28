package org.openrun.app.ui.program

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openrun.app.R
import org.openrun.app.ui.components.BrandTitle
import org.openrun.app.ui.components.Label
import org.openrun.app.ui.components.LineColor
import org.openrun.app.ui.components.Highlight
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import org.openrun.app.ui.programNameRes
import org.openrun.app.ui.theme.RunTheme
import org.openrun.core.ProgramProgress
import org.openrun.core.Programs

/** The programs one after the other, as a path: done, the one in progress (green), the ones ahead. */
@Composable
fun ParcoursScreen(
    progress: Map<String, ProgramProgress>,
    activeProgramId: String?,
    onProgram: (String) -> Unit,
    onWhereAreYou: () -> Unit,
    /** First visit of the launch, or the Parcours tab tapped again: the program in progress fades in. */
    highlight: Highlight = Highlight.None,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BrandTitle(stringResource(R.string.tab_parcours), size = 26, modifier = Modifier.padding(top = 16.dp))

        Surface(
            onClick = onWhereAreYou, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, LineColor), modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Explore, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.where_are_you), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(stringResource(R.string.change_start_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
            }
        }

        Label(stringResource(R.string.your_path))
        Programs.all.forEach { program ->
            val p = progress[program.id]
            val done = p?.isFinished == true
            val active = program.id == activeProgramId && !done
            val subtitle = when {
                done -> pluralStringResource(R.plurals.weeks_count, program.weeks.size, program.weeks.size) + " · " + stringResource(R.string.finished)
                active && p != null -> stringResource(R.string.sessions_done, p.done.size, program.sessionCount)
                else -> pluralStringResource(R.plurals.weeks_count, program.weeks.size, program.weeks.size) + " · " +
                    pluralStringResource(R.plurals.sessions_count, program.sessionCount, program.sessionCount)
            }
            PathNode(
                title = stringResource(programNameRes(program.id)), subtitle = subtitle, done = done, current = active,
                first = program == Programs.all.first(), last = program == Programs.all.last(),
                action = when { active -> stringResource(R.string.resume); done -> null; else -> stringResource(R.string.here) },
                onClick = { onProgram(program.id) }, highlight = if (active) highlight else Highlight.None,
            )
        }
    }
}

/** One step of the path: a dot on the dotted line, and the card of the program. */
@Composable
private fun PathNode(title: String, subtitle: String, done: Boolean, current: Boolean, first: Boolean, last: Boolean, action: String?, onClick: () -> Unit, highlight: Highlight) {
    val c = RunTheme.colors
    val primary = MaterialTheme.colorScheme.primary
    val dash = MaterialTheme.colorScheme.outlineVariant
    // a light sheen sweeps once across the card of the program in progress: "you are here"
    val play = highlight.play()
    val sheen = remember(highlight.id) { Animatable(if (play) 0f else 1f) }
    LaunchedEffect(highlight.id) { if (play) sheen.animateTo(1f, tween(750, easing = FastOutSlowInEasing)) }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(Modifier.width(44.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                // the dotted line goes through every node, from the first to the last
                val x = 21.dp.toPx()
                drawLine(
                    dash, Offset(x, if (first) size.height / 2 else -6.dp.toPx()), Offset(x, if (last) size.height / 2 else size.height + 6.dp.toPx()),
                    strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
                )
            }
            val dot = Modifier.padding(end = 2.dp).size(22.dp).clip(CircleShape)
            when {
                done -> Box(dot.background(primary))
                current -> Box(dot.background(c.walk).border(4.dp, primary, CircleShape))
                else -> Box(dot.background(MaterialTheme.colorScheme.surface).border(4.dp, dash, CircleShape))
            }
        }
        Surface(
            onClick = onClick, shape = RoundedCornerShape(16.dp),
            color = if (current) primary else MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = if (current) Color.White else MaterialTheme.colorScheme.onSurface,
            border = if (current) null else BorderStroke(1.dp, LineColor),
            modifier = Modifier.weight(1f).padding(vertical = 6.dp).clip(RoundedCornerShape(16.dp)).drawWithContent {
                drawContent()
                val p = sheen.value
                if (p < 1f) {
                    val band = size.width * 0.45f
                    val x = -band + p * (size.width + band)
                    drawRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.38f), Color.Transparent), start = Offset(x, 0f), end = Offset(x + band, size.height)))
                }
            },
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(subtitle, fontSize = 12.sp, color = if (current) Color(0xFFCDEBE4) else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (action != null) {
                    Surface(
                        onClick = onClick, shape = RoundedCornerShape(17.dp),
                        color = if (current) c.walk else MaterialTheme.colorScheme.surfaceContainerLowest,
                        contentColor = if (current) c.ink else MaterialTheme.colorScheme.onSurface,
                        border = if (current) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Text(action, fontSize = 13.sp, fontWeight = if (current) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                    }
                }
            }
        }
    }
}
