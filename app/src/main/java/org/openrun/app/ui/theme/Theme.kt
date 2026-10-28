package org.openrun.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colors of the phases of a session (D36): running is the full green of the brand, walking is yellow, warm-up and
 * cool-down are neutral. Dark against light, so the two can be told apart at a glance, even with red-green color
 * blindness. Always shown with an icon ([org.openrun.app.ui.components.PhaseBadge]) or a label too, never alone.
 */
@Immutable
data class RunColors(
    val run: Color,
    val onRun: Color,
    val runContainer: Color,
    val runBackground: Color,      // the whole run screen while running
    val walk: Color,
    val onWalk: Color,
    val walkContainer: Color,
    val walkBackground: Color,
    val calm: Color,
    val onCalm: Color,
    val calmContainer: Color,
    val calmBackground: Color,
    val ink: Color,                // the dark of the logo: text on yellow, the end of a session
    val gold: Color,
    val goldContainer: Color,
)

private val LightRunColors = RunColors(
    run = Color(0xFF00695C), onRun = Color.White, runContainer = Color(0xFFD3EDE7), runBackground = Color(0xFFE8F3F0),
    walk = Color(0xFFFFC93C), onWalk = Color(0xFF3A2A00), walkContainer = Color(0xFFFFF2CC), walkBackground = Color(0xFFFFF8E6),
    calm = Color(0xFFA9B8B4), onCalm = Color(0xFF3D4D49), calmContainer = Color(0xFFE5ECE9), calmBackground = Color(0xFFF2F6F4),
    ink = Color(0xFF0B2A25),
    gold = Color(0xFFB26A00), goldContainer = Color(0xFFFFDDB0),
)

val LocalRunColors = staticCompositionLocalOf { LightRunColors }

// The accent is provisional (docs/DESIGN.md): dynamic color will replace it.
private val LightScheme = lightColorScheme(
    primary = Color(0xFF00695C), onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFD5), onPrimaryContainer = Color(0xFF00201B),
    secondaryContainer = Color(0xFFDBE8E4), onSecondaryContainer = Color(0xFF14201D),
    background = Color(0xFFF8FAF8), onBackground = Color(0xFF171D1B),
    surface = Color(0xFFF8FAF8), onSurface = Color(0xFF171D1B),
    surfaceVariant = Color(0xFFDBE5E1), onSurfaceVariant = Color(0xFF4A5552),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF2F6F4),
    surfaceContainer = Color(0xFFEEF2F0),
    surfaceContainerHigh = Color(0xFFE6EBE8),
    surfaceContainerHighest = Color(0xFFDFE5E2),
    outline = Color(0xFF74807C), outlineVariant = Color(0xFFC0C9C5),
    error = Color(0xFFBA1A1A), errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
)

@Composable
fun OpenRunTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRunColors provides LightRunColors) {
        MaterialTheme(colorScheme = LightScheme, content = content)
    }
}

object RunTheme {
    val colors: RunColors @Composable get() = LocalRunColors.current
}
