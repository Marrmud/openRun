package org.openrun.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colors of the phases of a session. Always shown with a label or an icon too, never alone. */
@Immutable
data class RunColors(
    val run: Color,
    val runContainer: Color,
    val walk: Color,
    val walkContainer: Color,
    val calm: Color,
    val calmContainer: Color,
    val gold: Color,
    val goldContainer: Color,
)

private val LightRunColors = RunColors(
    run = Color(0xFFD4440C), runContainer = Color(0xFFFFDBD0),
    walk = Color(0xFF1565C0), walkContainer = Color(0xFFD6E4FF),
    calm = Color(0xFF3F8A5F), calmContainer = Color(0xFFCDEED9),
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
