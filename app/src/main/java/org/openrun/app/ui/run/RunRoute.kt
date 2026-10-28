package org.openrun.app.ui.run

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.openrun.app.R
import org.openrun.app.run.RunControl
import org.openrun.app.run.RunSession
import org.openrun.app.ui.openRun
import org.openrun.core.AppSettings

/**
 * Starts the session (after asking for the permissions it needs, once), shows it, and leaves when it ends:
 * to the summary if it was saved, back where we came from if it was thrown away.
 */
@Composable
fun RunRoute(programId: String?, index: Int, skipWarmup: Boolean, skipCooldown: Boolean, onSaved: (Long) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val app = context.openRun
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val settings by produceState<AppSettings?>(null) { app.settings.settings.collect { value = it } }
    val session by RunSession.state.collectAsStateWithLifecycle()
    val savedId by RunSession.savedRunId.collectAsStateWithLifecycle()

    var requested by rememberSaveable { mutableStateOf(false) }
    var hadSession by rememberSaveable { mutableStateOf(false) }

    fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
    fun startNow() { requested = true; RunControl.start(context, programId, index, skipWarmup, skipCooldown) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { startNow() }

    val s = settings
    if (s == null) return

    // the first time through: start at once if nothing is missing
    LaunchedEffect(s.treadmill) {
        if (session == null && !requested && (s.treadmill || granted(Manifest.permission.ACCESS_FINE_LOCATION))) startNow()
    }

    LaunchedEffect(session) { if (session != null) hadSession = true }
    LaunchedEffect(savedId) { savedId?.let { RunSession.savedRunId.value = null; onSaved(it) } }
    LaunchedEffect(session, savedId) { if (hadSession && session == null && savedId == null) onClose() }

    // keep the screen on while running, if asked
    val activity = context as? Activity
    // only while a session is on screen, not on the permission card before it
    val keepOn = s.keepScreenOn && session != null
    DisposableEffect(keepOn) {
        if (keepOn) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    val live = session
    if (live != null) {
        RunScreen(
            state = live, voiceOn = s.voice, initialStyle = s.defaultStyle, lockAtStart = s.lockAtStart,
            actions = RunActions(
                onPause = { RunControl.pause(context) }, onResume = { RunControl.resume(context) },
                onNext = { RunControl.next(context) }, onPrevious = { RunControl.previous(context) },
                onRestartPhase = { RunControl.restartPhase(context) }, onToStart = { RunControl.toStart(context) },
                onGoTo = { RunControl.goTo(context, it) }, onUndoGoTo = { RunControl.undoGoTo(context) },
                onSave = { RunControl.save(context) }, onDiscard = { RunControl.discard(context) },
                onToggleVoice = { scope.launch { app.settings.update { it.copy(voice = !it.voice) } } },
            ),
        )
    } else if (!requested) {
        PermissionCard(
            onAllow = {
                launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS))
            },
            onWithoutGps = { startNow() },
        )
    }
}

@Composable
private fun PermissionCard(onAllow: () -> Unit, onWithoutGps: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(stringResource(R.string.perm_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.perm_body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp, bottom = 24.dp))
        Button(onClick = onAllow, modifier = Modifier.fillMaxWidth().height(64.dp)) { Text(stringResource(R.string.allow)) }
        FilledTonalButton(onClick = onWithoutGps, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(56.dp)) { Text(stringResource(R.string.run_without_gps)) }
        TextButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.perm_privacy)) }
    }
}
