package org.openrun.app.ui.run

import java.util.Locale

/** 18:42, or 1:02:05 past the hour. */
fun clock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Seconds per kilometre as 5:52, or dashes when it is not known yet. */
fun paceText(secPerKm: Double?): String {
    if (secPerKm == null || secPerKm.isNaN() || secPerKm > 3599) return "--:--"
    val total = secPerKm.toInt()
    return "%d:%02d".format(total / 60, total % 60)
}

fun kmText(meters: Double): String = String.format(Locale.getDefault(), "%.2f", meters / 1000)

fun kmText1(meters: Double): String = String.format(Locale.getDefault(), "%.1f", meters / 1000)
