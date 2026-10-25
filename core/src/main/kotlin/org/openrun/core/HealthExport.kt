package org.openrun.core

/** The decisions behind writing a session to Health Connect, kept apart from the Android API so they can be tested. */
object HealthExport {
    private const val MAX_ROUTE_ACCURACY_M = 50f

    /** The same id every time for the same session, so writing it again replaces it instead of doubling it. */
    fun clientRecordId(runId: Long, part: String = "session") = "openrun-run-$runId-$part"

    /**
     * Start and end of the session in wall-clock time. The end is the active duration after the start, pushed back
     * to the last GPS fix when pauses made the fixes run past it: Health Connect refuses a route outside its session.
     */
    fun window(startedAtMs: Long, durationSec: Int, points: List<TrackPoint>): Pair<Long, Long> {
        val byDuration = startedAtMs + durationSec * 1000L
        val lastFix = points.filter { it.timeMs >= startedAtMs }.maxOfOrNull { it.timeMs } ?: byDuration
        return startedAtMs to maxOf(byDuration, lastFix, startedAtMs + 1000)
    }

    /** The fixes worth sending: precise enough, inside the window, in time order with no two at the same instant. */
    fun routePoints(points: List<TrackPoint>, startMs: Long, endMs: Long): List<TrackPoint> =
        points.filter { it.accuracyM <= MAX_ROUTE_ACCURACY_M && it.timeMs in startMs..endMs }
            .sortedBy { it.timeMs }
            .distinctBy { it.timeMs }
            .let { if (it.size >= 2) it else emptyList() }
}
