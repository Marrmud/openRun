package org.openrun.app.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.openrun.app.OpenRunApp
import org.openrun.app.data.PhaseResultEntity
import org.openrun.app.data.RunEntity
import org.openrun.app.data.TrackPointEntity
import org.openrun.core.PhaseKind
import org.openrun.core.Programs
import kotlin.math.cos
import kotlin.math.sin

/**
 * A development tool, only in the debug build: puts three demo sessions in the app's real database so the history,
 * the detail and the summary can be looked at without running outside. Trigger it with
 *
 *   adb shell am broadcast -n org.openrun.app/.debug.DemoSeedReceiver
 */
class DemoSeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val repo = (context.applicationContext as OpenRunApp).runs
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val day = 24 * 3600 * 1000L
                val now = System.currentTimeMillis()

                // 1. a completed program session, with a watch: a loop of about 3.6 km, heart rate, laps
                val workout = Programs.byId(Programs.C25K).sessions[0]
                val phases = workout.phases.mapIndexed { i, p ->
                    val metersPerSec = if (p.kind == PhaseKind.RUN) 2.8 else 1.4
                    PhaseResultEntity(0, i, p.kind.name, p.seconds, p.seconds * metersPerSec, avgHr = if (p.kind == PhaseKind.RUN) 150 + i else 120)
                }
                repo.saveRun(
                    RunEntity(
                        startedAt = now - 5 * day, durationSec = 1800, distanceM = 3560.0, kcal = 262.0, kcalFromWatch = true,
                        programId = Programs.C25K, sessionIndex = 0, avgHr = 148, maxHr = 171, avgCadence = 162,
                        elevationGainM = 14.0, source = "watch",
                    ),
                    points = loop(1800, 570.0, now - 5 * day), phases = phases,
                )
                // 2. a free run, phone only
                repo.saveRun(
                    RunEntity(startedAt = now - 3 * day, durationSec = 1260, distanceM = 2100.0, kcal = 150.0),
                    points = loop(1260, 330.0, now - 3 * day),
                )
                // 3. a program session ended early: keeps its program, does not complete it
                repo.saveRun(
                    RunEntity(startedAt = now - day, durationSec = 900, distanceM = 1500.0, kcal = 110.0, programId = Programs.C25K, sessionIndex = 1),
                    points = loop(900, 240.0, now - day), completesSession = false,
                )
            } finally {
                pending.finish()
            }
        }
    }

    private fun loop(seconds: Int, radiusM: Double, startMs: Long): List<TrackPointEntity> {
        val lat0 = 48.8566
        val lon0 = 2.3522
        return List(seconds) { i ->
            val a = 2 * Math.PI * i / seconds
            // a squashed loop with a bump, so it does not look like a perfect circle
            val dx = radiusM * cos(a) * 1.25
            val dy = radiusM * sin(a) * 0.85 + 40 * sin(3 * a)
            TrackPointEntity(0, i, lat0 + dy / 111_195.0, lon0 + dx / (111_195.0 * cos(Math.toRadians(lat0))), 5f, startMs + i * 1000L, 35.0 + 3 * sin(a))
        }
    }
}
