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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
    Column(Modifier.fillMaxSize()) {
        if (showBack) {
            IconButton(onClick = onBack, modifier = Modifier.padding(start = 6.dp, top = 8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
            }
        } else Spacer(Modifier.height(24.dp))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.where_are_you), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.where_are_you_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            levels.forEachIndexed { i, level ->
                val on = i == selected
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    border = if (on) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.fillMaxWidth().selectable(selected = on, role = Role.RadioButton, onClick = { selected = i }),
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(44.dp)) {
                            Icon(level.icon, null, Modifier.padding(10.dp))
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                            Text(stringResource(level.title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(level.subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RadioButton(selected = on, onClick = null)
                    }
                }
            }
        }
        Button(
            onClick = { onChoose(levels[selected].programId) },
            modifier = Modifier.fillMaxWidth().padding(18.dp).height(64.dp),
        ) { Text(stringResource(R.string.lets_go), style = MaterialTheme.typography.titleMedium) }
    }
}
