package org.openrun.app.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.openrun.app.R
import org.openrun.core.Programs

@StringRes
fun programNameRes(id: String): Int = when (id) {
    Programs.PRE_C25K -> R.string.program_pre_c25k
    Programs.C25K -> R.string.program_c25k
    Programs.BRIDGE_10K -> R.string.program_bridge_10k
    Programs.C210K -> R.string.program_c210k
    else -> R.string.app_name
}

@StringRes
fun programBlurbRes(id: String): Int = when (id) {
    Programs.PRE_C25K -> R.string.program_pre_c25k_blurb
    Programs.C25K -> R.string.program_c25k_blurb
    Programs.BRIDGE_10K -> R.string.program_bridge_10k_blurb
    Programs.C210K -> R.string.program_c210k_blurb
    else -> R.string.app_name
}

/** A tip is a key of the core (`c25k.w2`); its text lives in strings.xml as `tip_c25k_w2`. */
@Composable
fun tipText(key: String): String {
    val context = LocalContext.current
    val id = context.resources.getIdentifier("tip_" + key.replace('.', '_'), "string", context.packageName)
    return if (id != 0) context.getString(id) else ""
}
