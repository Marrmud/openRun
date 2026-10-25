package org.openrun.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
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

/** The whole session as coloured bars: tall for running, short for walking, shortest for warm-up and cool-down. */
@Composable
fun WorkoutBar(workout: Workout, modifier: Modifier = Modifier, height: Dp = 34.dp, doneUpTo: Int = 0) {
    val c = RunTheme.colors
    Row(modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        workout.phases.forEachIndexed { i, p ->
            val (color, fraction) = when (p.kind) {
                PhaseKind.RUN -> c.run to 1f
                PhaseKind.WALK -> c.walk to 0.55f
                PhaseKind.WARMUP, PhaseKind.COOLDOWN -> c.calm to 0.4f
            }
            Box(
                Modifier.weight(p.seconds.toFloat()).fillMaxHeight(fraction)
                    .alpha(if (i < doneUpTo) 0.35f else 1f)
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
    }
}

private val RankColors = mapOf(
    RankTier.FER to Color(0xFF78858F), RankTier.BRONZE to Color(0xFFB0703A), RankTier.ARGENT to Color(0xFF8F9BA6),
    RankTier.OR to Color(0xFFD19A00), RankTier.PLATINE to Color(0xFF2FA6B5), RankTier.DIAMANT to Color(0xFF4A7DF0),
)

/** A shield in the colour of the tier, with a runner on it. */
@Composable
fun RankBadge(tier: RankTier, size: Dp = 40.dp, dimmed: Boolean = false) {
    Box(Modifier.size(size).alpha(if (dimmed) 0.35f else 1f), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Shield, null, tint = RankColors.getValue(tier), modifier = Modifier.size(size))
        Icon(Icons.Filled.DirectionsRun, null, tint = Color.White, modifier = Modifier.size(size * 0.5f))
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
