package org.openrun.core

enum class RankTier { FER, BRONZE, ARGENT, OR, PLATINE, DIAMANT }

/**
 * [division] counts down like in a game: 3 is the first step of a tier, 1 the last. [progress] is 0..1 inside the
 * division; at the very top it stays at 1.
 */
data class Rank(
    val tier: RankTier,
    val division: Int,
    val kcalInDivision: Int,
    val kcalPerDivision: Int,
    val progress: Float,
) {
    val kcalToNext: Int get() = kcalPerDivision - kcalInDivision
}

object Ranks {
    /** Provisional (docs/MVP.md): one figure for every step, to adjust with real data. */
    const val KCAL_PER_DIVISION = 1000
    private const val DIVISIONS = 3

    fun forTotal(totalKcal: Double, kcalPerDivision: Int = KCAL_PER_DIVISION): Rank {
        val tiers = RankTier.entries
        val last = tiers.size * DIVISIONS - 1
        val steps = (totalKcal.coerceAtLeast(0.0) / kcalPerDivision).toLong()
        if (steps >= last) {
            return Rank(tiers.last(), 1, kcalPerDivision, kcalPerDivision, 1f)
        }
        val inDivision = (totalKcal - steps * kcalPerDivision).toInt()
        return Rank(
            tier = tiers[(steps / DIVISIONS).toInt()],
            division = DIVISIONS - (steps % DIVISIONS).toInt(),
            kcalInDivision = inDivision,
            kcalPerDivision = kcalPerDivision,
            progress = inDivision.toFloat() / kcalPerDivision,
        )
    }
}
