package org.openrun.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.openrun.core.JumpPlan
import org.openrun.core.Program
import org.openrun.core.ProgramProgress

/** Everything the screens read and write about sessions and program progress. */
class RunRepository(private val db: OpenRunDatabase) {

    /**
     * Saves a finished session with its track and phases in one go. When it belongs to a program session, that
     * session counts as done (and stays done even if the same one is run again).
     */
    suspend fun saveRun(
        run: RunEntity,
        points: List<TrackPointEntity> = emptyList(),
        phases: List<PhaseResultEntity> = emptyList(),
    ): Long = db.withTransaction {
        val id = db.runs().insert(run)
        if (points.isNotEmpty()) db.tracks().insertAll(points.map { it.copy(runId = id) })
        if (phases.isNotEmpty()) db.tracks().insertPhases(phases.map { it.copy(runId = id) })
        if (run.programId != null && run.sessionIndex != null) {
            db.done().insertAll(listOf(DoneSessionEntity(run.programId, run.sessionIndex, id)))
        }
        id
    }

    /** Deleting a session also makes its program session to do again. */
    suspend fun deleteRun(id: Long) = db.withTransaction {
        db.done().deleteByRun(id)
        db.runs().delete(id)
    }

    suspend fun run(id: Long) = db.runs().get(id)
    suspend fun points(runId: Long) = db.tracks().pointsOf(runId)
    suspend fun phases(runId: Long) = db.tracks().phasesOf(runId)
    suspend fun setFeeling(id: Long, feeling: Int?) = db.runs().setFeeling(id, feeling)
    suspend fun setHealthConnectId(id: Long, hcId: String?) = db.runs().setHealthConnectId(id, hcId)

    fun history(): Flow<List<RunEntity>> = db.runs().observeAll()
    fun between(from: Long, to: Long): Flow<List<RunEntity>> = db.runs().observeBetween(from, to)
    fun totalKcal(): Flow<Double> = db.runs().observeTotalKcal()
    fun totals(): Flow<Totals> = db.runs().observeTotals()

    fun progress(program: Program): Flow<ProgramProgress> =
        db.done().observe(program.id).map { ProgramProgress(program.sessionCount, it.toSet()) }

    /** Applies the plan shown to the user when they pick a day of a program. */
    suspend fun applyJump(program: Program, plan: JumpPlan) = db.withTransaction {
        db.done().delete(program.id, plan.unmarkDone)
        db.done().insertAll(plan.markDone.map { DoneSessionEntity(program.id, it, null) })
    }

    suspend fun progressNow(program: Program): ProgramProgress =
        ProgramProgress(program.sessionCount, db.done().doneOf(program.id).toSet())
}
