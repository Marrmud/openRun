package org.openrun.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openrun.app.ui.theme.Fredoka
import org.openrun.app.ui.theme.RunTheme
import org.openrun.core.RankTier

// The brand pieces of the v2 design (mockups/index-v2.html, D31): the mascot, the word mark, cards, labels, bubbles.

private val Ink = Color(0xFF0B2A25)
private val Yellow = Color(0xFFFFC93C)
private val Pink = Color(0xFFFF7A9A)
private val Mint = Color(0xFFB2DFD8)
private val Green = Color(0xFF00695C)

/** The faces of the mascot. It speaks (tips, empty states, feelings, the end of a session), never during a run. */
enum class Mood { RUN, FRONT, HAPPY, TIRED, MEH, STRONG, COOL, SLEEP }

/**
 * The mascot: the runner ring of the logo with eyes, drawn from the same geometry as the brand sheet (a 108 unit box).
 * [height] is the height of the whole drawing; [legs] stands it up (or makes it run, for [Mood.RUN]).
 */
@Composable
fun Mascot(mood: Mood, height: Dp, modifier: Modifier = Modifier, legs: Boolean = false, speedLines: Color? = Mint) {
    val run = mood == Mood.RUN
    val lines = run && speedLines != null
    val x0 = if (lines) 3f else 18f
    val x1 = if (mood == Mood.SLEEP) 96f else 90f
    val y0 = if (mood == Mood.SLEEP) 2f else 18f
    val y1 = if (legs) 110f else 90f
    val width = height * ((x1 - x0) / (y1 - y0))
    Canvas(modifier.size(width, height)) {
        val k = size.height / (y1 - y0)
        scale(k, k, pivot = Offset.Zero) {
            translate(-x0, -y0) { drawMascot(mood, legs, if (lines) speedLines else null) }
        }
    }
}

private fun DrawScope.line(vararg pts: Float, color: Color = Ink, width: Float = 2.2f) {
    val p = Path().apply { moveTo(pts[0], pts[1]); var i = 2; while (i < pts.size) { lineTo(pts[i], pts[i + 1]); i += 2 } }
    drawPath(p, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.curve(x0: Float, y0: Float, cx: Float, cy: Float, x1: Float, y1: Float, close: Boolean = false, fill: Color? = null, width: Float = 2.2f) {
    val p = Path().apply { moveTo(x0, y0); quadraticTo(cx, cy, x1, y1); if (close) close() }
    if (fill != null) drawPath(p, fill, style = Fill)
    drawPath(p, Ink, style = Stroke(if (fill != null) 1.4f else width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.outlined(center: Offset, rx: Float, ry: Float, fill: Color, stroke: Float) {
    val tl = Offset(center.x - rx, center.y - ry)
    drawOval(fill, tl, Size(2 * rx, 2 * ry))
    drawOval(Ink, tl, Size(2 * rx, 2 * ry), style = Stroke(stroke))
}

private fun DrawScope.drawMascot(mood: Mood, legs: Boolean, speedLines: Color?) {
    val run = mood == Mood.RUN
    if (legs) {
        if (run) {
            drawOval(Color(0xFFDCE5E2), Offset(36f, 104.4f), Size(36f, 5.2f))
            line(49f, 82f, 42f, 98f, width = 4f); line(60f, 82f, 67f, 94f, width = 4f)
            outlined(Offset(39.5f, 100f), 6.8f, 3.8f, Pink, 2f)
            rotate(-25f, pivot = Offset(70f, 95.5f)) { outlined(Offset(70f, 95.5f), 6.8f, 3.8f, Pink, 2f) }
        } else {
            drawOval(Color(0xFFDCE5E2), Offset(37f, 102.4f), Size(34f, 5.2f))
            line(48f, 84f, 47f, 98f, width = 4f); line(60f, 84f, 61f, 98f, width = 4f)
            outlined(Offset(44.5f, 100.5f), 6.8f, 3.6f, Pink, 2f)
            outlined(Offset(63.5f, 100.5f), 6.8f, 3.6f, Pink, 2f)
        }
    }
    if (speedLines != null) {
        line(9f, 45f, 16f, 45f, color = speedLines, width = 5f)
        line(5f, 55f, 15f, 55f, color = speedLines, width = 5f)
        line(9f, 65f, 16f, 65f, color = speedLines, width = 5f)
    }
    // the ring: three quarters of a circle, open at the top right, with the sun in the gap
    val ring = Offset(30f, 30f) to Size(48f, 48f)
    drawArc(Ink, 0f, 270f, false, ring.first, ring.second, style = Stroke(18f, cap = StrokeCap.Round))
    drawArc(Color.White, 0f, 270f, false, ring.first, ring.second, style = Stroke(11f, cap = StrokeCap.Round))
    drawCircle(Ink, 8.4f, Offset(70.97f, 37.03f))
    drawCircle(Yellow, 5.1f, Offset(70.97f, 37.03f))

    fun eyes(dx: Float, dy: Float) {
        for (x in listOf(48f, 60f)) {
            drawCircle(Color.White, 4.4f, Offset(x, 52f))
            drawCircle(Ink, 4.4f, Offset(x, 52f), style = Stroke(1.6f))
            drawCircle(Ink, 2.4f, Offset(x + dx, 52f + dy))
        }
    }
    fun smile() = curve(50.6f, 58.4f, 54f, 63.6f, 57.4f, 58.4f, close = true, fill = Pink)
    when (mood) {
        Mood.RUN -> { eyes(1.5f, 0.3f); smile() }
        Mood.FRONT -> { eyes(0f, 0.5f); smile() }
        Mood.HAPPY -> {
            curve(44.6f, 53f, 48f, 48.4f, 51.4f, 53f); curve(56.6f, 53f, 60f, 48.4f, 63.4f, 53f)
            curve(49.4f, 57.2f, 54f, 65.4f, 58.6f, 57.2f, close = true, fill = Pink)
        }
        Mood.TIRED -> {
            line(44.4f, 51.4f, 51.6f, 53.2f); line(56.4f, 53.2f, 63.6f, 51.4f)
            outlined(Offset(54f, 60f), 2.8f, 2.2f, Pink, 1.4f)
            val drop = Path().apply { moveTo(29f, 22f); quadraticTo(25f, 28.5f, 29f, 31f); quadraticTo(33f, 28.5f, 29f, 22f); close() }
            drawPath(drop, Color(0xFF9FDCF0)); drawPath(drop, Ink, style = Stroke(1.3f, join = StrokeJoin.Round))
        }
        Mood.MEH -> { eyes(0f, 0.8f); line(50.5f, 59.6f, 57.5f, 59.6f) }
        Mood.STRONG -> {
            eyes(0.6f, 0.6f); line(44f, 45.6f, 51.2f, 47.8f); line(56.8f, 47.8f, 64f, 45.6f)
            curve(50.6f, 58.4f, 54f, 62.4f, 57.4f, 58.4f, close = true, fill = Pink)
        }
        Mood.COOL -> {
            drawRoundRect(Ink, Offset(42.6f, 48.2f), Size(10.6f, 7f), androidx.compose.ui.geometry.CornerRadius(3.2f))
            drawRoundRect(Ink, Offset(54.8f, 48.2f), Size(10.6f, 7f), androidx.compose.ui.geometry.CornerRadius(3.2f))
            line(53f, 50f, 55f, 50f, width = 1.6f)
            line(44.6f, 50.2f, 47.4f, 50.2f, color = Color.White.copy(alpha = 0.8f), width = 1.1f)
            line(56.8f, 50.2f, 59.6f, 50.2f, color = Color.White.copy(alpha = 0.8f), width = 1.1f)
            curve(50.6f, 59.2f, 54.6f, 61.8f, 57.8f, 58f)
        }
        Mood.SLEEP -> {
            curve(44.6f, 51.4f, 48f, 55f, 51.4f, 51.4f); curve(56.6f, 51.4f, 60f, 55f, 63.4f, 51.4f)
            drawCircle(Ink, 1.7f, Offset(54f, 59.6f))
            line(78f, 15f, 85f, 15f, 78f, 22f, 85f, 22f, color = Green)
            line(88f, 5f, 92.5f, 5f, 88f, 9.5f, 92.5f, 9.5f, color = Green)
        }
    }
}

/** "openRun": the o is the running mascot, "pen" in ink and "Run" in green, Fredoka 600. */
@Composable
fun Wordmark(height: Dp = 26.dp, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Mascot(Mood.RUN, height * 1.15f, legs = true, speedLines = Green, modifier = Modifier.padding(end = 1.dp, bottom = height * 0.16f))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Ink)) { append("pen") }
                withStyle(SpanStyle(color = Green)) { append("Run") }
            },
            fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = (height.value).sp, lineHeight = (height.value).sp,
        )
    }
}

/** A white card on the light surface, with a hairline border, as in the mockup. */
@Composable
fun BrandCard(modifier: Modifier = Modifier, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, LineColor), modifier = modifier.fillMaxWidth(),
    ) { Column(Modifier.padding(padding), content = content) }
}

val LineColor = Color(0xFFE3EAE7)

/** The small uppercase label above a group. */
@Composable
fun Label(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text.uppercase(), modifier = modifier, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.7.sp)
}

/** A title in the round letters of the logo. */
@Composable
fun BrandTitle(text: String, modifier: Modifier = Modifier, size: Int = 23, color: Color = RunTheme.colors.ink) {
    Text(text, modifier = modifier, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = size.sp, color = color, letterSpacing = (-0.1).sp)
}

/** The mascot saying something: a tip, an empty state. */
@Composable
fun TipBubble(text: String, modifier: Modifier = Modifier, title: String? = null, mood: Mood = Mood.FRONT, mascotSize: Dp = 46.dp, mint: Boolean = false) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = if (title == null) Alignment.CenterVertically else Alignment.Top) {
        Mascot(mood, mascotSize)
        val bg = if (mint) Color(0xFFE6F4F0) else MaterialTheme.colorScheme.surfaceContainerLowest
        // a speech bubble: its little tail points at the mascot's mouth
        val shape = BubbleShape(tailY = if (title == null) null else mascotSize * 0.55f)
        Surface(shape = shape, color = bg, border = if (mint) null else BorderStroke(1.dp, LineColor), modifier = Modifier.weight(1f)) {
            Column(Modifier.padding(start = 14.dp + BubbleTail, end = 14.dp, top = 12.dp, bottom = 12.dp)) {
                if (title != null) Text(title, fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink)
                Text(text, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
            }
        }
    }
}

/** A thick rounded progress bar, green or yellow (milestones, ranks). */
@Composable
fun BrandProgress(fraction: Float, modifier: Modifier = Modifier, yellow: Boolean = false, track: Color = MaterialTheme.colorScheme.surfaceContainerHigh) {
    Box(modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(track)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(10.dp).clip(RoundedCornerShape(5.dp)).background(if (yellow) Yellow else Green))
    }
}

/** The one big yellow action of a screen (start, begin, finish). */
@Composable
fun GoButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector? = androidx.compose.material.icons.Icons.Rounded.PlayArrow, flash: Boolean = false,
    container: Color = Yellow, content: Color = Ink,
) {
    androidx.compose.material3.Button(
        onClick = onClick, modifier = modifier.flashHalo(flash, container, 34.dp).fillMaxWidth().height(68.dp), shape = RoundedCornerShape(34.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
    ) {
        if (icon != null) androidx.compose.material3.Icon(icon, null)
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = if (icon != null) 8.dp else 0.dp))
    }
}

/** The segmented control of the mockup: a light track, the chosen one on a white pill. */
@Composable
fun <T> PillTabs(
    options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier,
    track: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    Row(
        modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(track).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { o ->
            val on = o == selected
            // no ripple: the white pill moving is the feedback
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (on) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent,
                shadowElevation = if (on) 1.dp else 0.dp,
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(o) },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        label(o), fontSize = 14.sp, maxLines = 1, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        color = if (on) Ink else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- rank badges

private class RankLook(val base: Color, val dark: Color, val light: Color, val shape: String, val mood: Mood, val wings: Int = 0, val laurel: Boolean = false)

private val rankLooks = mapOf(
    RankTier.FER to RankLook(Color(0xFF8F9BA1), Color(0xFF4F5B62), Color(0xFFDDE3E6), "circle", Mood.TIRED),
    RankTier.BRONZE to RankLook(Color(0xFFC98149), Color(0xFF7A4520), Color(0xFFF3CBA6), "circle2", Mood.MEH),
    RankTier.ARGENT to RankLook(Color(0xFFAAB6C0), Color(0xFF5F6C78), Color(0xFFF1F4F6), "shield", Mood.FRONT),
    RankTier.OR to RankLook(Color(0xFFF2B62A), Color(0xFF9A6800), Color(0xFFFFE79C), "shield", Mood.RUN, laurel = true),
    RankTier.PLATINE to RankLook(Color(0xFF38BDB2), Color(0xFF11706B), Color(0xFFB8F2EB), "hex", Mood.STRONG, wings = 1),
    RankTier.DIAMANT to RankLook(Color(0xFF6B93FF), Color(0xFF2847B8), Color(0xFFD6E2FF), "diamond", Mood.COOL, wings = 2),
)

private fun svg(d: String) = PathParser().parsePathString(d).toPath()

private val ShieldPath = svg("M32 2 L59 11 V31 C59 47.5 48 57.5 32 64 C16 57.5 5 47.5 5 31 V11 Z")
private val HexPath = svg("M32 1 L60 17 L60 49 L32 65 L4 49 L4 17 Z")
private val DiamondPath = svg("M32 -1 L63 33 L32 67 L1 33 Z")
private val DiamondFacets = svg("M32 -1 L20 20 L32 33 L44 20 Z M1 33 L20 20 M63 33 L44 20")
private val Wing1 = svg("M60 22 C70 17 77 21 76 26 C72 25.5 68 26.5 63 29 C69 29.5 73 33 71.5 37 C67 35.5 63 36 60 38 Z")
private val Wing2 = svg("M59 18 C71 10 81 15 80.5 21 C76 20.5 71 21.5 65 24.5 C73 24 79 28 77.5 33 C72 31.5 67 32 62 34.5 C68 35.5 72 39.5 70 43.5 C65.5 41.5 61 42 58 43 Z")

/**
 * The badge of a rank tier (brand sheet, mockup v2): its own shape and metal, wings and laurels for the high tiers, and
 * the mascot inside, struggling in Fer and cool in Diamant. [size] is its height; it is a little wider than tall.
 */
@Composable
fun RankBadge(tier: RankTier, size: Dp = 40.dp, dimmed: Boolean = false) {
    val r = rankLooks.getValue(tier)
    Canvas(Modifier.size(size * (96f / 84f), size).alpha(if (dimmed) 0.4f else 1f)) {
        val k = this.size.height / 84f
        scale(k, k, pivot = Offset.Zero) {
            translate(16f, 12f) { drawRankBadge(r) }
        }
    }
}

private fun DrawScope.drawRankBadge(r: RankLook) {
    fun metal(top: Offset, bottom: Offset) = Brush.linearGradient(0f to r.light, 0.55f to r.base, 1f to r.base, start = top, end = bottom)
    fun inner(block: DrawScope.() -> Unit) = scale(0.84f, 0.84f, pivot = Offset(32f, 33f)) { block() }

    if (r.wings > 0) {
        val wing = if (r.wings == 1) Wing1 else Wing2
        for (mirror in listOf(false, true)) {
            scale(if (mirror) -1f else 1f, 1f, pivot = Offset(32f, 0f)) {
                drawPath(wing, r.light)
                drawPath(wing, r.dark, style = Stroke(1.6f, join = StrokeJoin.Round))
            }
        }
    }
    if (r.laurel) {
        for (side in listOf(1, -1)) for (k in 0 until 4) {
            val a = (if (side > 0) 62f else 118f) - side * k * 17f
            val ar = Math.toRadians(a.toDouble())
            val c = Offset(32f + 31f * kotlin.math.cos(ar).toFloat(), 33f + 31f * kotlin.math.sin(ar).toFloat())
            rotate(a + 90f * side, pivot = c) {
                drawOval(Green, Offset(c.x - 5.2f, c.y - 2.4f), Size(10.4f, 4.8f))
                drawOval(Ink, Offset(c.x - 5.2f, c.y - 2.4f), Size(10.4f, 4.8f), style = Stroke(1f))
            }
        }
    }
    when (r.shape) {
        "circle", "circle2" -> {
            drawCircle(r.dark, 29f, Offset(32f, 33f))
            drawCircle(metal(Offset(7.5f, 8.5f), Offset(46.7f, 57.5f)), 24.5f, Offset(32f, 33f))
            if (r.shape == "circle2") for (i in 0 until 12) {
                val a = i * Math.PI / 6
                drawCircle(r.light, 1.1f, Offset(32f + 26.8f * kotlin.math.cos(a).toFloat(), 33f + 26.8f * kotlin.math.sin(a).toFloat()))
            }
        }
        "shield" -> { drawPath(ShieldPath, r.dark); inner { drawPath(ShieldPath, metal(Offset(5f, 2f), Offset(48f, 64f))) } }
        "hex" -> { drawPath(HexPath, r.dark); inner { drawPath(HexPath, metal(Offset(4f, 1f), Offset(48f, 65f))) } }
        "diamond" -> {
            drawPath(DiamondPath, r.dark)
            inner {
                drawPath(DiamondPath, metal(Offset(1f, -1f), Offset(51f, 67f)))
                drawPath(DiamondFacets, Color(0x22FFFFFF))
                drawPath(DiamondFacets, Color(0x55FFFFFF), style = Stroke(1f))
            }
        }
    }
    // shine
    rotate(-18f, pivot = Offset(25f, 13f)) { drawOval(Color.White.copy(alpha = 0.28f), Offset(14f, 9f), Size(22f, 8f)) }
    // the mascot inside, on a white disc
    val ms = when (r.shape) { "diamond" -> 34f; "circle", "circle2" -> 42f; else -> 39f }
    val sc = ms / 72f
    val cy = 33.5f
    drawCircle(Color.White.copy(alpha = 0.92f), 17f * sc, Offset(32f, cy))
    translate(32f - 54f * sc, cy - 54f * sc) {
        scale(sc, sc, pivot = Offset.Zero) { drawMascot(r.mood, legs = false, speedLines = null) }
    }
}

private val BubbleTail = 7.dp

/** A rounded box with a small tail on its left side, at [tailY] from the top (or in the middle). */
private class BubbleShape(private val tailY: Dp?) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(size: Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
        with(density) {
            val tail = BubbleTail.toPx()
            val r = 18.dp.toPx()
            val y = (tailY?.toPx() ?: (size.height / 2)).coerceIn(r, size.height - r)
            val body = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(tail, 0f, size.width, size.height, androidx.compose.ui.geometry.CornerRadius(r))) }
            val point = Path().apply { moveTo(tail + 1f, y - tail); lineTo(0f, y); lineTo(tail + 1f, y + tail); close() }
            return androidx.compose.ui.graphics.Outline.Generic(Path.combine(androidx.compose.ui.graphics.PathOperation.Union, body, point))
        }
    }
}

/**
 * A screen's highlight, asked for by the navigation: on the first visit of the launch, and each time its tab is tapped
 * while already open. Never when simply switching tabs. [play] is true once per request.
 */
class Highlight(val id: Int, private val claim: (Int) -> Boolean) {
    @Composable
    fun play(): Boolean = remember(id) { id > 0 && claim(id) }

    companion object { val None = Highlight(0) { false } }
}

/** Two soft halos around the element when [play]: "here it is". */
@Composable
fun Modifier.flashHalo(play: Boolean, color: Color, corner: Dp): Modifier {
    val t = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(play) {
        if (!play) return@LaunchedEffect
        repeat(2) {
            t.snapTo(0f)
            t.animateTo(1f, androidx.compose.animation.core.tween(650, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
        }
        t.snapTo(0f)
    }
    return this.drawBehind {
        val v = t.value
        if (v > 0f) {
            val spread = 14.dp.toPx() * v
            drawRoundRect(
                color.copy(alpha = 0.8f * (1f - v)), Offset(-spread, -spread), Size(size.width + 2 * spread, size.height + 2 * spread),
                androidx.compose.ui.geometry.CornerRadius(corner.toPx() + spread),
            )
        }
    }
}
