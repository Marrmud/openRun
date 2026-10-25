package org.openrun.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {
    @Insert suspend fun insert(run: RunEntity): Long

    @Query("SELECT * FROM run WHERE id = :id") suspend fun get(id: Long): RunEntity?

    @Query("SELECT * FROM run ORDER BY startedAt DESC") fun observeAll(): Flow<List<RunEntity>>

    @Query("SELECT * FROM run WHERE startedAt >= :from AND startedAt < :to ORDER BY startedAt")
    fun observeBetween(from: Long, to: Long): Flow<List<RunEntity>>

    @Query("SELECT COALESCE(SUM(kcal), 0) FROM run") fun observeTotalKcal(): Flow<Double>

    @Query("SELECT COUNT(*) AS runs, COALESCE(SUM(distanceM), 0) AS distanceM, COALESCE(SUM(durationSec), 0) AS durationSec FROM run")
    fun observeTotals(): Flow<Totals>

    @Query("UPDATE run SET feeling = :feeling WHERE id = :id") suspend fun setFeeling(id: Long, feeling: Int?)

    @Query("UPDATE run SET healthConnectId = :hcId WHERE id = :id") suspend fun setHealthConnectId(id: Long, hcId: String?)

    @Query("DELETE FROM run WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface TrackDao {
    @Insert suspend fun insertAll(points: List<TrackPointEntity>)

    @Query("SELECT * FROM track_point WHERE runId = :runId ORDER BY idx")
    suspend fun pointsOf(runId: Long): List<TrackPointEntity>

    @Insert suspend fun insertPhases(phases: List<PhaseResultEntity>)

    @Query("SELECT * FROM phase_result WHERE runId = :runId ORDER BY idx")
    suspend fun phasesOf(runId: Long): List<PhaseResultEntity>
}

@Dao
interface DoneDao {
    @Query("SELECT sessionIndex FROM done_session WHERE programId = :programId")
    fun observe(programId: String): Flow<List<Int>>

    @Query("SELECT sessionIndex FROM done_session WHERE programId = :programId")
    suspend fun doneOf(programId: String): List<Int>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAll(done: List<DoneSessionEntity>)

    @Query("DELETE FROM done_session WHERE programId = :programId AND sessionIndex IN (:indices)")
    suspend fun delete(programId: String, indices: List<Int>)

    @Query("DELETE FROM done_session WHERE runId = :runId")
    suspend fun deleteByRun(runId: Long)
}
