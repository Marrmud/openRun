package org.openrun.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.openrun.core.Programs

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var db: OpenRunDatabase
    private lateinit var repo: RunRepository
    private val c25k = Programs.byId(Programs.C25K)

    @Before fun open() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, OpenRunDatabase::class.java).build()
        repo = RunRepository(db)
    }

    @After fun close() = db.close()

    private fun run(startedAt: Long = 1_000, kcal: Double = 300.0, session: Int? = null) = RunEntity(
        startedAt = startedAt, durationSec = 1860, distanceM = 3600.0, kcal = kcal,
        programId = if (session != null) Programs.C25K else null, sessionIndex = session,
    )

    private fun point(i: Int) = TrackPointEntity(0, i, 48.0 + i * 0.0001, 2.0, 5f, i * 1000L)

    @Test fun aSavedRunComesBackWithItsTrackAndPhases() = runBlocking {
        val id = repo.saveRun(
            run(),
            points = List(5) { point(it) },
            phases = listOf(PhaseResultEntity(0, 0, "WARMUP", 300, 400.0), PhaseResultEntity(0, 1, "RUN", 90, 250.0, 150)),
        )
        assertEquals(3600.0, repo.run(id)!!.distanceM, 0.0)
        assertEquals(List(5) { it }, repo.points(id).map { it.idx })
        assertTrue(repo.points(id).all { it.runId == id })
        assertEquals(listOf("WARMUP", "RUN"), repo.phases(id).map { it.kind })
        assertEquals(150, repo.phases(id)[1].avgHr)
    }

    @Test fun historyIsNewestFirst() = runBlocking {
        repo.saveRun(run(startedAt = 1_000)); repo.saveRun(run(startedAt = 3_000)); repo.saveRun(run(startedAt = 2_000))
        assertEquals(listOf(3_000L, 2_000L, 1_000L), repo.history().first().map { it.startedAt })
    }

    @Test fun betweenKeepsTheHalfOpenRange() = runBlocking {
        repo.saveRun(run(startedAt = 100)); repo.saveRun(run(startedAt = 200)); repo.saveRun(run(startedAt = 300))
        assertEquals(listOf(100L, 200L), repo.between(100, 300).first().map { it.startedAt })
    }

    @Test fun totalsAndKcalAddUp() = runBlocking {
        assertEquals(0.0, repo.totalKcal().first(), 0.0)
        assertEquals(0, repo.totals().first().runs)
        repo.saveRun(run(kcal = 300.0)); repo.saveRun(run(kcal = 212.5))
        assertEquals(512.5, repo.totalKcal().first(), 0.001)
        val t = repo.totals().first()
        assertEquals(2, t.runs); assertEquals(7200.0, t.distanceM, 0.0); assertEquals(3720L, t.durationSec)
    }

    @Test fun runningAProgramSessionMarksItDone() = runBlocking {
        repo.saveRun(run(session = 0))
        val p = repo.progress(c25k).first()
        assertEquals(setOf(0), p.done)
        assertEquals(1, p.currentIndex)
    }

    @Test fun aFreeRunDoesNotTouchProgress() = runBlocking {
        repo.saveRun(run())
        assertTrue(repo.progress(c25k).first().done.isEmpty())
    }

    @Test fun deletingARunMakesItsSessionToDoAgainAndRemovesItsTrack() = runBlocking {
        val id = repo.saveRun(run(session = 0), points = List(3) { point(it) })
        repo.saveRun(run(session = 1))
        repo.deleteRun(id)
        assertNull(repo.run(id))
        assertTrue(repo.points(id).isEmpty())
        assertEquals(setOf(1), repo.progress(c25k).first().done)
    }

    @Test fun jumpingForwardThenBackFollowsThePlans() = runBlocking {
        repo.saveRun(run(session = 0))
        var progress = repo.progressNow(c25k)
        val forward = progress.planJump(4)
        repo.applyJump(c25k, forward)
        progress = repo.progressNow(c25k)
        assertEquals(setOf(0, 1, 2, 3), progress.done)
        assertEquals(4, progress.currentIndex)

        repo.applyJump(c25k, progress.planJump(2))
        progress = repo.progressNow(c25k)
        assertEquals(setOf(0, 1), progress.done)
        assertEquals(2, progress.currentIndex)
    }

    @Test fun progressOfOneProgramIsKeptApartFromAnother() = runBlocking {
        repo.saveRun(run(session = 0))
        val other = Programs.byId(Programs.C210K)
        assertTrue(repo.progress(other).first().done.isEmpty())
    }

    @Test fun feelingAndHealthConnectIdCanBeSetAfterwards() = runBlocking {
        val id = repo.saveRun(run())
        assertNull(repo.run(id)!!.feeling)
        repo.setFeeling(id, 4); repo.setHealthConnectId(id, "hc-123")
        assertEquals(4, repo.run(id)!!.feeling)
        assertNotNull(repo.run(id)!!.healthConnectId)
        repo.setFeeling(id, null)
        assertNull(repo.run(id)!!.feeling)
    }
}
