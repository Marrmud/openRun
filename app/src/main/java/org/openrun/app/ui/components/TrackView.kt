package org.openrun.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.openrun.app.R
import org.openrun.app.ui.theme.RunTheme
import org.openrun.core.TrackPoint
import org.openrun.core.TrackShape

/** The route drawn on its own, no map behind it (the map comes later at this same place). */
@Composable
fun TrackView(points: List<TrackPoint>, modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 180.dp) {
    val shape = remember(points) { TrackShape.project(points) }
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, border = androidx.compose.foundation.BorderStroke(1.dp, LineColor), modifier = modifier.fillMaxWidth().height(height)) {
        if (shape == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_track), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val color = RunTheme.colors.run
            val start = RunTheme.colors.walk
            Canvas(Modifier.fillMaxSize().padding(20.dp)) {
                // fit the route in the box without changing its proportions
                val scale = minOf(size.width / shape.widthFraction.coerceAtLeast(0.001f), size.height / shape.heightFraction.coerceAtLeast(0.001f))
                val dx = (size.width - shape.widthFraction * scale) / 2
                val dy = (size.height - shape.heightFraction * scale) / 2
                fun at(p: Pair<Float, Float>) = Offset(dx + p.first * scale, dy + p.second * scale)
                val path = Path().apply {
                    shape.points.forEachIndexed { i, p -> val o = at(p); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
                }
                drawPath(path, color, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawCircle(Color.White, 8.dp.toPx(), at(shape.points.first()))
                drawCircle(start, 6.dp.toPx(), at(shape.points.first()))
                drawCircle(color, 6.dp.toPx(), at(shape.points.last()))
            }
        }
    }
}
