package org.openrun.app.ui.program

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openrun.app.R
import org.openrun.app.ui.programNameRes
import org.openrun.core.ProgramProgress
import org.openrun.core.Programs

/** The four programs, with how far each one is. Tapping one opens its weeks; nothing is locked. */
@Composable
fun ParcoursScreen(
    progress: Map<String, ProgramProgress>,
    activeProgramId: String?,
    onProgram: (String) -> Unit,
    onWhereAreYou: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.tab_parcours), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))

        Surface(
            shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onWhereAreYou),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.where_are_you), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.change_start_sub), style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
            }
        }

        Programs.all.forEach { program ->
            val p = progress[program.id]
            val done = p?.done?.size ?: 0
            val active = program.id == activeProgramId
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth().clickable { onProgram(program.id) },
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(programNameRes(program.id)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                pluralStringResource(R.plurals.weeks_count, program.weeks.size, program.weeks.size) + " · " +
                                    pluralStringResource(R.plurals.sessions_count, program.sessionCount, program.sessionCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        when {
                            p?.isFinished == true -> Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            active -> OutlinedButton(onClick = { onProgram(program.id) }) { Text(stringResource(R.string.resume)) }
                            else -> OutlinedButton(onClick = { onProgram(program.id) }) { Text(stringResource(R.string.here)) }
                        }
                    }
                    if (done > 0) {
                        LinearProgressIndicator(
                            progress = { done.toFloat() / program.sessionCount },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        )
                        Text(
                            stringResource(R.string.sessions_done, done, program.sessionCount),
                            style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
