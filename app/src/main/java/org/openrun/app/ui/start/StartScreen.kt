package org.openrun.app.ui.start

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import org.openrun.app.ui.components.BrandTitle
import org.openrun.app.ui.components.LineColor
import org.openrun.app.R
import org.openrun.core.Programs

private data class Level(val icon: ImageVector, @StringRes val title: Int, @StringRes val subtitle: Int, val programId: String?)

private val levels = listOf(
    Level(Icons.Filled.Weekend, R.string.level_zero, R.string.level_zero_sub, Programs.PRE_C25K),
    Level(Icons.AutoMirrored.Filled.DirectionsWalk, R.string.level_minute, R.string.level_minute_sub, Programs.C25K),
    Level(Icons.Filled.DirectionsRun, R.string.level_five_km, R.string.level_five_km_sub, Programs.BRIDGE_10K),
    Level(Icons.Filled.Bolt, R.string.level_ten_km, R.string.level_ten_km_sub, null),
)

/** "Où en es-tu ?": one question that picks where to begin. Nothing is locked, it can be changed any time. */
@Composable
fun StartScreen(currentProgramId: String?, onboarded: Boolean, showBack: Boolean, onBack: () -> Unit, onChoose: (String?) -> Unit) {
    // before the first answer, suggest the middle one; afterwards, show what was chosen (null = free run)
    var selected by remember(currentProgramId, onboarded) {
        mutableIntStateOf(if (onboarded) levels.indexOfFirst { it.programId == currentProgramId }.coerceAtLeast(0) else 1)
    }
    val primary = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxSize()) {
        if (showBack) {
            IconButton(onClick = onBack, modifier = Modifier.padding(start = 6.dp, top = 8.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
            }
        } else Spacer(Modifier.height(24.dp))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BrandTitle(stringResource(R.string.where_are_you), size = 26)
            Text(stringResource(R.string.where_are_you_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
            levels.forEachIndexed { i, level ->
                val on = i == selected
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (on) Color(0xFFE6F4F0) else MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = if (on) BorderStroke(2.dp, primary) else BorderStroke(1.dp, LineColor),
                    modifier = Modifier.fillMaxWidth().selectable(selected = on, role = Role.RadioButton, onClick = { selected = i }),
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(level.icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                            Text(stringResource(level.title), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(level.subtitle), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        // the round radio of the mockup: an outline, filled in green when chosen
                        Box(
                            Modifier.size(22.dp).border(2.dp, if (on) primary else MaterialTheme.colorScheme.outline, CircleShape).padding(5.dp)
                                .clip(CircleShape).background(if (on) primary else Color.Transparent),
                        )
                    }
                }
            }
        }
        Button(
            onClick = { onChoose(levels[selected].programId) },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(18.dp).height(56.dp),
        ) { Text(stringResource(R.string.lets_go), fontSize = 17.sp, fontWeight = FontWeight.Bold) }
    }
}
