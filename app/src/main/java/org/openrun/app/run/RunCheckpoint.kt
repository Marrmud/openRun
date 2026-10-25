package org.openrun.app.run

import android.content.Context
import org.json.JSONArray
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.openrun.app.OpenRunApp
import org.openrun.core.Programs
import org.openrun.core.TrackAccumulator
import org.openrun.core.TrackPoint
import java.io.File

/**
 * A copy of the session in progress, written every few seconds, so that if Android kills the app in the middle of a
 * run the session is not lost: it is offered for saving at the next launch.
 */
data class Checkpoint(
    val programId: String?,
    val sessionIndex: Int?,
    val skipWarmup: Boolean,
    val skipCooldown: Boolean,
    val startedWallMs: Long,
    val activeMs: Long,
    val workoutFinished: Boolean,
    val positionMs: Long,
    val phaseSecMs: LongArray,
    val phaseDistM: DoubleArray,
    val points: List<TrackPoint>,
    val altitudes: List<Double?>,
    val gpsOn: Boolean,
    val weightKg: Double,
    val savedAtMs: Long,
) {
    /** The session as the saver wants it: the plan is rebuilt from the program, the distance from the points. */
    fun toInput(): SessionInput {
        val program = programId?.let { id -> Programs.all.firstOrNull { it.id == id } }
        val workout = program?.sessions?.getOrNull(sessionIndex ?: -1)?.let { w ->
            w.let { if (skipWarmup) it.withoutWarmup() else it }.let { if (skipCooldown) it.withoutCooldown() else it }
        }
        val acc = TrackAccumulator().also { a -> points.forEach { a.add(it) } }
        return SessionInput(
            programId = if (workout != null) programId else null, sessionIndex = if (workout != null) sessionIndex else null,
            workout = workout, startedWallMs = startedWallMs, activeMs = activeMs, workoutFinished = workoutFinished,
            positionMs = positionMs, phaseSecMs = phaseSecMs, phaseDistM = phaseDistM,
            points = points, altitudes = altitudes, gpsOn = gpsOn, weightKg = weightKg, distanceM = acc.distanceM,
        )
    }
}

object RunCheckpoints {
    private fun file(context: Context) = File(context.filesDir, "run-checkpoint.json")

    fun write(context: Context, cp: Checkpoint) {
        val json = JSONObject().apply {
            put("programId", cp.programId ?: JSONObject.NULL)
            put("sessionIndex", cp.sessionIndex ?: JSONObject.NULL)
            put("skipWarmup", cp.skipWarmup); put("skipCooldown", cp.skipCooldown)
            put("startedWallMs", cp.startedWallMs); put("activeMs", cp.activeMs)
            put("workoutFinished", cp.workoutFinished); put("positionMs", cp.positionMs)
            put("phaseSecMs", JSONArray(cp.phaseSecMs.toList()))
            put("phaseDistM", JSONArray(cp.phaseDistM.toList()))
            put("gpsOn", cp.gpsOn); put("weightKg", cp.weightKg); put("savedAtMs", cp.savedAtMs)
            put("points", JSONArray().apply {
                cp.points.forEachIndexed { i, p ->
                    put(JSONArray().apply {
                        put(p.lat); put(p.lon); put(p.accuracyM.toDouble()); put(p.timeMs); put(cp.altitudes.getOrNull(i) ?: JSONObject.NULL)
                    })
                }
            })
        }
        // write beside, then rename: a kill in the middle of the write leaves the previous copy intact
        val tmp = File(context.filesDir, "run-checkpoint.json.tmp")
        tmp.writeText(json.toString())
        if (!tmp.renameTo(file(context))) { file(context).delete(); tmp.renameTo(file(context)) }
    }

    fun read(context: Context): Checkpoint? {
        val f = file(context)
        if (!f.exists()) return null
        return try {
            val j = JSONObject(f.readText())
            val pts = j.getJSONArray("points")
            val points = ArrayList<TrackPoint>(pts.length())
            val alts = ArrayList<Double?>(pts.length())
            for (i in 0 until pts.length()) {
                val a = pts.getJSONArray(i)
                points += TrackPoint(a.getDouble(0), a.getDouble(1), a.getDouble(2).toFloat(), a.getLong(3))
                alts += if (a.isNull(4)) null else a.getDouble(4)
            }
            fun longs(name: String) = j.getJSONArray(name).let { arr -> LongArray(arr.length()) { arr.getLong(it) } }
            fun doubles(name: String) = j.getJSONArray(name).let { arr -> DoubleArray(arr.length()) { arr.getDouble(it) } }
            Checkpoint(
                programId = if (j.isNull("programId")) null else j.getString("programId"),
                sessionIndex = if (j.isNull("sessionIndex")) null else j.getInt("sessionIndex"),
                skipWarmup = j.getBoolean("skipWarmup"), skipCooldown = j.getBoolean("skipCooldown"),
                startedWallMs = j.getLong("startedWallMs"), activeMs = j.getLong("activeMs"),
                workoutFinished = j.getBoolean("workoutFinished"), positionMs = j.getLong("positionMs"),
                phaseSecMs = longs("phaseSecMs"), phaseDistM = doubles("phaseDistM"),
                points = points, altitudes = alts, gpsOn = j.getBoolean("gpsOn"),
                weightKg = j.getDouble("weightKg"), savedAtMs = j.getLong("savedAtMs"),
            )
        } catch (_: Exception) {
            null // a copy that cannot be read is worth less than a clean start
        }
    }

    fun clear(context: Context) {
        file(context).delete()
        File(context.filesDir, "run-checkpoint.json.tmp").delete()
    }
}

/** What to do with a session found interrupted: save it as it was, or throw it away. */
class RunRecovery(private val app: OpenRunApp) {
    fun pending(): Checkpoint? = RunCheckpoints.read(app)

    /** Returns the id of the saved session. A session with no time at all is simply dropped. */
    suspend fun save(cp: Checkpoint): Long? {
        RunCheckpoints.clear(app)
        if (cp.activeMs < 1000) return null
        val bundle = SessionSaver.bundle(cp.toInput())
        val id = app.runs.saveRun(bundle.run, bundle.points, bundle.phases, completesSession = bundle.completesSession)
        if (app.settings.settings.first().healthConnect) app.health.syncPending()
        return id
    }

    fun discard() = RunCheckpoints.clear(app)
}
