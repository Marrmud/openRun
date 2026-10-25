package org.openrun.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openrun.app.R
import org.openrun.core.Program

/** The run screen comes in T4. This only shows what would start, so the chain Home -> Program -> Start can be checked. */
@Composable
fun RunPlaceholder(program: Program?, index: Int, skipWarmup: Boolean, skipCooldown: Boolean, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 6.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
        }
        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (program == null) {
                Text(stringResource(R.string.free_run), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            } else {
                val (w, d) = program.locate(index)
                val workout = program.sessions[index].let { if (skipWarmup) it.withoutWarmup() else it }.let { if (skipCooldown) it.withoutCooldown() else it }
                Text(stringResource(R.string.week_day, w, d), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.session_minutes, workout.totalSeconds / 60))
                Text(stringResource(R.string.phases_count, workout.phases.size))
            }
            Text(stringResource(R.string.run_coming), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun HistoryPlaceholder() {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text(stringResource(R.string.tab_history), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.history_coming), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
    }
}
