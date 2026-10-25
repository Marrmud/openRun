package org.openrun.app.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import org.openrun.app.R
import org.openrun.app.data.RunEntity
import org.openrun.app.data.RunRepository
import org.openrun.core.HealthExport
import org.openrun.core.Programs
import org.openrun.core.TrackPoint
import java.time.Instant
import java.time.ZoneId

/**
 * Writes sessions to Health Connect, from where Google Health reads them. Each session has a fixed id, so writing
 * one twice replaces it. A session that could not be written (no permission, Health Connect busy) stays pending and
 * is tried again at the next opportunity.
 */
class HealthConnectSync(private val context: Context, private val repo: RunRepository) {

    companion object {
        /** Without these three there is nothing worth writing. */
        val REQUIRED: Set<String> = setOf(
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(DistanceRecord::class),
            HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
        )

        /** Optional: the route on the session. Without it, the session is written without its track. */
        const val ROUTE = HealthPermission.PERMISSION_WRITE_EXERCISE_ROUTE

        val ALL: Set<String> = REQUIRED + ROUTE
    }

    private val client: HealthConnectClient? by lazy {
        if (isAvailable()) runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull() else null
    }

    fun sdkStatus(): Int = HealthConnectClient.getSdkStatus(context)
    fun isAvailable(): Boolean = sdkStatus() == HealthConnectClient.SDK_AVAILABLE

    suspend fun grantedPermissions(): Set<String> =
        runCatching { client?.permissionController?.getGrantedPermissions() }.getOrNull().orEmpty()

    suspend fun hasRequiredPermissions(): Boolean = grantedPermissions().containsAll(REQUIRED)

    /** Writes every session still pending. Returns how many were written. */
    suspend fun syncPending(): Int {
        if (client == null || !hasRequiredPermissions()) return 0
        var written = 0
        for (run in repo.pendingForHealth()) {
            if (write(run)) written++
        }
        return written
    }

    private suspend fun write(run: RunEntity): Boolean {
        val c = client ?: return false
        val granted = grantedPermissions()
        val points = repo.points(run.id).map { TrackPoint(it.lat, it.lon, it.accuracyM, it.timeMs) }
        val (startMs, endMs) = HealthExport.window(run.startedAt, run.durationSec, points)
        val start = Instant.ofEpochMilli(startMs)
        val end = Instant.ofEpochMilli(endMs)
        val offset = ZoneId.systemDefault().rules.getOffset(start)
        val endOffset = ZoneId.systemDefault().rules.getOffset(end)
        val device = Device(type = Device.TYPE_PHONE)
        fun meta(part: String) = Metadata.activelyRecorded(device = device, clientRecordId = HealthExport.clientRecordId(run.id, part))

        val route = if (!run.treadmill && ROUTE in granted) {
            val locations = HealthExport.routePoints(points, startMs, endMs)
            if (locations.isEmpty()) null else ExerciseRoute(
                locations.map {
                    ExerciseRoute.Location(
                        time = Instant.ofEpochMilli(it.timeMs), latitude = it.lat, longitude = it.lon,
                        horizontalAccuracy = Length.meters(it.accuracyM.toDouble()),
                    )
                },
            )
        } else null

        val records = buildList {
            add(
                ExerciseSessionRecord(
                    startTime = start, startZoneOffset = offset, endTime = end, endZoneOffset = endOffset,
                    metadata = meta("session"),
                    exerciseType = if (run.treadmill) ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL else ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                    title = title(run), exerciseRoute = route,
                ),
            )
            if (run.distanceM >= 1) add(DistanceRecord(start, offset, end, endOffset, Length.meters(run.distanceM), meta("distance")))
            if (run.kcal > 0) add(ActiveCaloriesBurnedRecord(start, offset, end, endOffset, Energy.kilocalories(run.kcal), meta("calories")))
        }
        return try {
            c.insertRecords(records)
            repo.setHealthConnectId(run.id, HealthExport.clientRecordId(run.id))
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Removes what was written for a session that is deleted in openRun. Best effort. */
    suspend fun delete(runId: Long) {
        val c = client ?: return
        // the first list is Health Connect's own ids, the second the ids we gave: ours are the second ones
        runCatching {
            c.deleteRecords(ExerciseSessionRecord::class, emptyList(), listOf(HealthExport.clientRecordId(runId, "session")))
            c.deleteRecords(DistanceRecord::class, emptyList(), listOf(HealthExport.clientRecordId(runId, "distance")))
            c.deleteRecords(ActiveCaloriesBurnedRecord::class, emptyList(), listOf(HealthExport.clientRecordId(runId, "calories")))
        }.onFailure { android.util.Log.w("openrun-health", "delete failed for run $runId", it) }
    }

    private fun title(run: RunEntity): String {
        val program = run.programId?.let { id -> Programs.all.firstOrNull { it.id == id } }
        val index = run.sessionIndex
        if (program == null || index == null || index !in 0 until program.sessionCount) return context.getString(R.string.hc_title_free)
        val (w, d) = program.locate(index)
        return context.getString(R.string.hc_title_program, w, d)
    }
}
