package org.openrun.core

/** Sessions are numbered from 0 across the whole program, week after week. */
data class Week(val number: Int, val tipKey: String, val sessions: List<Workout>)

data class Program(val id: String, val weeks: List<Week>) {
    val sessions: List<Workout> by lazy { weeks.flatMap { it.sessions } }
    val sessionCount: Int get() = sessions.size

    /** (week number from 1, day from 1) of a session index. */
    fun locate(index: Int): Pair<Int, Int> {
        var left = index
        for (w in weeks) {
            if (left < w.sessions.size) return w.number to left + 1
            left -= w.sessions.size
        }
        throw IndexOutOfBoundsException("no session $index in $id")
    }

    fun indexOf(week: Int, day: Int): Int {
        require(week in 1..weeks.size) { "no week $week in $id" }
        require(day in 1..weeks[week - 1].sessions.size) { "no day $day in week $week" }
        return weeks.take(week - 1).sumOf { it.sessions.size } + day - 1
    }
}

/**
 * The four programs of the MVP. Written for openRun: the shape (run/walk intervals growing week by week) is the
 * classic one every couch-to-5K follows, the numbers are ours. Tips are keys, the app turns them into text.
 */
object Programs {
    const val PRE_C25K = "pre_c25k"
    const val C25K = "c25k"
    const val BRIDGE_10K = "bridge_10k"
    const val C210K = "c210k"

    /** Three identical sessions of intervals: warm-up, [reps] x (run, walk), cool-down. */
    private fun intervals(n: Int, tip: String, runSec: Int, walkSec: Int, reps: Int) =
        Week(n, tip, List(3) { workout { warmup(); repeat(reps) { run(runSec); walk(walkSec) }; cooldown() } })

    /** A session made of one list of (run seconds, walk seconds after it); the last walk is dropped. */
    private fun blocks(vararg b: Pair<Int, Int>) = workout {
        warmup()
        b.forEachIndexed { i, (r, w) -> run(r); if (i < b.lastIndex) walk(w) }
        cooldown()
    }

    /** A continuous run of [minutes] between the warm-up and the cool-down. */
    private fun continuous(minutes: Int) = workout { warmup(); run(minutes * 60); cooldown() }

    private fun runWeek(n: Int, tip: String, vararg minutes: Int) = Week(n, tip, minutes.map { continuous(it) })

    private val pre = Program(PRE_C25K, listOf(
        intervals(1, "pre_c25k.w1", runSec = 30, walkSec = 90, reps = 10),
        intervals(2, "pre_c25k.w2", runSec = 45, walkSec = 90, reps = 9),
        intervals(3, "pre_c25k.w3", runSec = 60, walkSec = 120, reps = 7),
    ))

    private val c25kWeeks = listOf(
        intervals(1, "c25k.w1", runSec = 60, walkSec = 90, reps = 8),
        intervals(2, "c25k.w2", runSec = 90, walkSec = 120, reps = 6),
        Week(3, "c25k.w3", List(3) {
            workout { warmup(); repeat(2) { run(90); walk(90); run(180); walk(180) }; cooldown() }
        }),
        Week(4, "c25k.w4", List(3) { blocks(180 to 90, 300 to 150, 180 to 90, 300 to 0) }),
        Week(5, "c25k.w5", listOf(
            blocks(300 to 180, 300 to 180, 300 to 0),
            blocks(480 to 300, 480 to 0),
            continuous(20),
        )),
        Week(6, "c25k.w6", listOf(
            blocks(300 to 180, 480 to 180, 300 to 0),
            blocks(600 to 180, 600 to 0),
            continuous(25),
        )),
        runWeek(7, "c25k.w7", 25, 25, 25),
        runWeek(8, "c25k.w8", 28, 28, 28),
        runWeek(9, "c25k.w9", 30, 30, 30),
    )

    private val c25k = Program(C25K, c25kWeeks)

    private val bridge = Program(BRIDGE_10K, listOf(
        runWeek(1, "bridge_10k.w1", 30, 30, 35),
        runWeek(2, "bridge_10k.w2", 30, 35, 40),
        runWeek(3, "bridge_10k.w3", 35, 35, 45),
        runWeek(4, "bridge_10k.w4", 35, 40, 50),
        runWeek(5, "bridge_10k.w5", 40, 40, 55),
        runWeek(6, "bridge_10k.w6", 40, 45, 60),
    ))

    /** The nine weeks of the 5 km program, then five weeks that stretch the long run to an hour. */
    private val c210k = Program(C210K, c25kWeeks + listOf(
        runWeek(10, "c210k.w10", 35, 35, 40),
        runWeek(11, "c210k.w11", 35, 40, 45),
        runWeek(12, "c210k.w12", 40, 40, 50),
        runWeek(13, "c210k.w13", 40, 45, 55),
        runWeek(14, "c210k.w14", 40, 45, 60),
    ))

    val all: List<Program> = listOf(pre, c25k, bridge, c210k)

    fun byId(id: String): Program = all.first { it.id == id }
}
