package org.openrun.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.openrun.app.R
import org.openrun.app.ui.theme.RunTheme
import org.openrun.core.PhaseKind
import org.openrun.core.Rank
import org.openrun.core.RankTier
import org.openrun.core.Workout

/** The whole session as coloured bars: tall for running, short for walking, shortest for warm-up and cool-down. Phases done fade out. */
@Composable
fun WorkoutBar(workout: Workout, modifier: Modifier = Modifier, height: Dp = 34.dp, doneUpTo: Int = 0, onDark: Boolean = false) {
    val c = RunTheme.colors
    Row(modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        workout.phases.forEachIndexed { i, p ->
            val (color, fraction) = when (p.kind) {
                // on the green card of the day, running is drawn white
                PhaseKind.RUN -> (if (onDark) Color.White else c.run) to 1f
                PhaseKind.WALK -> c.walk to 0.6f
                PhaseKind.WARMUP, PhaseKind.COOLDOWN -> (if (onDark) Color.White.copy(alpha = 0.35f) else c.calm) to 0.34f
            }
            Box(
                Modifier.weight(p.seconds.toFloat()).fillMaxHeight(fraction)
                    .alpha(if (i < doneUpTo) 0.25f else 1f)
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
    }
}

/** The fill and content colors of a phase (D36). */
@Composable
fun phaseColors(kind: PhaseKind): Pair<Color, Color> {
    val c = RunTheme.colors
    return when (kind) {
        PhaseKind.RUN -> c.run to c.onRun
        PhaseKind.WALK -> c.walk to c.onWalk
        PhaseKind.WARMUP, PhaseKind.COOLDOWN -> c.calmContainer to c.onCalm
    }
}

fun phaseIcon(kind: PhaseKind): ImageVector = when (kind) {
    PhaseKind.RUN -> Icons.Filled.DirectionsRun
    PhaseKind.WALK -> Icons.Filled.DirectionsWalk
    PhaseKind.WARMUP -> Icons.Filled.AccessibilityNew      // arms up: stretching before
    PhaseKind.COOLDOWN -> Icons.Filled.SelfImprovement     // seated, breathing: calming down after
}

/** The round badge of a phase, the same everywhere (preview, run screen, details), so the color is never alone. */
@Composable
fun PhaseBadge(kind: PhaseKind, size: Dp = 28.dp) {
    val (fill, content) = phaseColors(kind)
    Box(Modifier.size(size).background(fill, CircleShape), contentAlignment = Alignment.Center) {
        Icon(phaseIcon(kind), null, tint = content, modifier = Modifier.size(size * 0.64f))
    }
}

@Composable
fun rankName(rank: Rank): String {
    val tiers = stringArrayResource(R.array.rank_tiers)
    val roman = stringArrayResource(R.array.rank_divisions)
    return "${tiers[rank.tier.ordinal]} ${roman[rank.division - 1]}"
}

@Composable
fun rankProgressText(rank: Rank): String =
    stringResource(R.string.rank_progress, rank.kcalInDivision, rank.kcalPerDivision)

@Composable
fun cardContainer() = MaterialTheme.colorScheme.surfaceContainer
