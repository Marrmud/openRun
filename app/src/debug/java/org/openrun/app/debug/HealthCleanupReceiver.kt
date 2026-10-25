package org.openrun.app.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.openrun.app.OpenRunApp

/**
 * A development tool, only in the debug build: removes from Health Connect what was written for a session id, for
 * the case where the session itself is already gone from the app.
 *
 *   adb shell am broadcast -n org.openrun.app/.debug.HealthCleanupReceiver --el runId 1
 */
class HealthCleanupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val runId = intent.getLongExtra("runId", -1)
        if (runId < 0) return
        val pending = goAsync()
        val app = context.applicationContext as OpenRunApp
        CoroutineScope(Dispatchers.IO).launch {
            try { app.health.delete(runId) } finally { pending.finish() }
        }
    }
}
