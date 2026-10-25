package org.openrun.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A recorded session. Nullable columns are what only a watch (or a program) can fill. */
@Entity(tableName = "run")
data class RunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val durationSec: Int,
    val distanceM: Double,
    val kcal: Double,
    /** True when the figure comes from a watch, false when estimated from the weight. */
    val kcalFromWatch: Boolean = false,
    /** Null for a free run. */
    val programId: String? = null,
    val sessionIndex: Int? = null,
    val treadmill: Boolean = false,
    /** How it felt, 1 (hard) to 5 (great). */
    val feeling: Int? = null,
    val avgHr: Int? = null,
    val maxHr: Int? = null,
    val avgCadence: Int? = null,
    val elevationGainM: Double? = null,
    /** "phone" or "watch": what recorded it. */
    val source: String = "phone",
    /** Id of the session written to Health Connect, to never write it twice. */
    val healthConnectId: String? = null,
)

@Entity(
    tableName = "track_point",
    primaryKeys = ["runId", "idx"],
    foreignKeys = [ForeignKey(RunEntity::class, ["id"], ["runId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("runId")],
)
data class TrackPointEntity(
    val runId: Long,
    val idx: Int,
    val lat: Double,
    val lon: Double,
    val accuracyM: Float,
    val timeMs: Long,
    val altitudeM: Double? = null,
)

/** How one phase of a program session went: the "laps" of the detail screen. */
@Entity(
    tableName = "phase_result",
    primaryKeys = ["runId", "idx"],
    foreignKeys = [ForeignKey(RunEntity::class, ["id"], ["runId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("runId")],
)
data class PhaseResultEntity(
    val runId: Long,
    val idx: Int,
    /** A [org.openrun.core.PhaseKind] name. */
    val kind: String,
    val seconds: Int,
    val distanceM: Double,
    val avgHr: Int? = null,
)

/** A session of a program that counts as done: really run ([runId]) or skipped over when picking a later day. */
@Entity(tableName = "done_session", primaryKeys = ["programId", "sessionIndex"])
data class DoneSessionEntity(
    val programId: String,
    val sessionIndex: Int,
    val runId: Long? = null,
)

data class Totals(val runs: Int, val distanceM: Double, val durationSec: Long)
