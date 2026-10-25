package org.openrun.app.run

import org.openrun.app.data.PhaseResultEntity
import org.openrun.app.data.RunEntity
import org.openrun.app.data.TrackPointEntity
import org.openrun.core.Calories
import org.openrun.core.Cues
import org.openrun.core.PhaseKind
import org.openrun.core.TrackPoint
import org.openrun.core.Workout

/** Everything needed to turn a session, finished or interrupted, into what the database stores. */
data class SessionInput(
    val programId: String?,
    val sessionIndex: Int?,
    val workout: Workout?,
    val startedWallMs: Long,
    val activeMs: Long,
    val workoutFinished: Boolean,
    /** Where the session got to in its plan, skipped phases counting as covered. 0 for a free run. */
    val positionMs: Long,
    val phaseSecMs: LongArray,
    val phaseDistM: DoubleArray,
    val points: List<TrackPoint>,
    val altitudes: List<Double?>,
    val gpsOn: Boolean,
    val weightKg: Double,
    val distanceM: Double,
)

data class SaveBundle(val run: RunEntity, val points: List<TrackPointEntity>, val phases: List<PhaseResultEntity>, val completesSession: Boolean)

object SessionSaver {
    fun kcal(i: SessionInput): Double {
        val w = i.workout
        val segments = if (w == null) {
            listOf(Calories.Segment(true, i.phaseSecMs[0] / 1000.0, if (i.gpsOn && i.phaseDistM[0] > 0) i.phaseDistM[0] else null))
        } else w.phases.indices.filter { i.phaseSecMs[it] > 0 }.map {
            Calories.Segment(w.phases[it].kind == PhaseKind.RUN, i.phaseSecMs[it] / 1000.0, if (i.gpsOn && i.phaseDistM[it] > 0) i.phaseDistM[it] else null)
        }
        return Calories.kcal(i.weightKg, segments)
    }

    fun bundle(i: SessionInput): SaveBundle {
        val w = i.workout
        val planned = (w?.totalSeconds ?: 0) * 1000L
        val completes = w != null && Cues.countsAsDone(i.positionMs, planned, i.workoutFinished)
        val run = RunEntity(
            startedAt = i.startedWallMs,
            durationSec = (i.activeMs / 1000).toInt(),
            distanceM = i.distanceM,
            kcal = kcal(i),
            programId = i.programId,
            sessionIndex = i.sessionIndex,
            treadmill = !i.gpsOn,
        )
        val points = i.points.mapIndexed { n, p -> TrackPointEntity(0, n, p.lat, p.lon, p.accuracyM, p.timeMs, i.altitudes.getOrNull(n)) }
        val phases = w?.phases?.indices?.filter { i.phaseSecMs[it] > 0 }?.map {
            PhaseResultEntity(0, it, w.phases[it].kind.name, (i.phaseSecMs[it] / 1000).toInt(), i.phaseDistM[it])
        }.orEmpty()
        return SaveBundle(run, points, phases, completes)
    }
}
