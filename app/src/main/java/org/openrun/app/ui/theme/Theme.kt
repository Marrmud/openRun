package org.openrun.app.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.openrun.app.R

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

// The palette of the logo (D31, mockups/index-v2.html): green, mint, ink, yellow for action and reward, pink as a touch.
private val LightScheme = lightColorScheme(
    primary = Color(0xFF00695C), onPrimary = Color.White,
    primaryContainer = Color(0xFFD3EDE7), onPrimaryContainer = Color(0xFF0B4F45),
    secondary = Color(0xFFFFC93C), onSecondary = Color(0xFF0B2A25),
    // tonal buttons, segmented buttons and the navigation pill use this one: the mint of the logo, not the yellow
    secondaryContainer = Color(0xFFD3EDE7), onSecondaryContainer = Color(0xFF0B4F45),
    tertiary = Color(0xFFFF7A9A), onTertiary = Color(0xFF3B0716),
    tertiaryContainer = Color(0xFFFFE3EA), onTertiaryContainer = Color(0xFF3B0716),
    background = Color(0xFFF5F8F6), onBackground = Color(0xFF0F2420),
    surface = Color(0xFFF5F8F6), onSurface = Color(0xFF0F2420),
    surfaceVariant = Color(0xFFE5ECE9), onSurfaceVariant = Color(0xFF4A5A56),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF5F8F6),
    surfaceContainer = Color(0xFFEEF3F1),
    surfaceContainerHigh = Color(0xFFE5ECE9),
    surfaceContainerHighest = Color(0xFFDDE5E2),
    outline = Color(0xFF71807B), outlineVariant = Color(0xFFD2DDD9),
    error = Color(0xFFBA1A1A), errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
)

/** Titles: Fredoka, the round letters of the logo. */
@OptIn(ExperimentalTextApi::class)
val Fredoka = FontFamily(
    listOf(500, 600, 700).map { w -> Font(R.font.fredoka, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w))) },
)

/** Everything else, numbers first: Roboto Flex, with tabular figures so a running clock does not shake. */
@OptIn(ExperimentalTextApi::class)
val RobotoFlex = FontFamily(
    listOf(400, 500, 600, 700, 800).map { w -> Font(R.font.roboto_flex, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w))) },
)

private val Base = Typography()
private fun TextStyle.title(weight: Int = 600) = copy(fontFamily = Fredoka, fontWeight = FontWeight(weight), letterSpacing = (-0.2).sp)
private fun TextStyle.body() = copy(fontFamily = RobotoFlex, fontFeatureSettings = "tnum")

private val AppTypography = Typography(
    displayLarge = Base.displayLarge.title(), displayMedium = Base.displayMedium.title(), displaySmall = Base.displaySmall.title(),
    headlineLarge = Base.headlineLarge.title(), headlineMedium = Base.headlineMedium.title(), headlineSmall = Base.headlineSmall.title(),
    // titles of screens and cards use Fredoka through BrandTitle; title styles carry numbers and buttons, so they stay in Roboto Flex
    titleLarge = Base.titleLarge.body(), titleMedium = Base.titleMedium.body(), titleSmall = Base.titleSmall.body(),
    bodyLarge = Base.bodyLarge.body(), bodyMedium = Base.bodyMedium.body(), bodySmall = Base.bodySmall.body(),
    labelLarge = Base.labelLarge.body(), labelMedium = Base.labelMedium.body(), labelSmall = Base.labelSmall.body(),
)

@Composable
fun OpenRunTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRunColors provides LightRunColors) {
        MaterialTheme(colorScheme = LightScheme, typography = AppTypography) {
            // plain Text without a style gets Roboto Flex too
            CompositionLocalProvider(LocalTextStyle provides AppTypography.bodyLarge, content = content)
        }
    }
}

object RunTheme {
    val colors: RunColors @Composable get() = LocalRunColors.current
}
