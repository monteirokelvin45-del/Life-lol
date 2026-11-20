package com.lifeos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.lifeos.ui.theme.LifeOsTheme

/**
 * Curva analítica Bezier suave com área sombreada e curva comparativa anterior (Referência 02).
 */
@Composable
fun SmoothMetricSplineChart(
    currentPoints: List<Float>,
    previousPoints: List<Float> = emptyList(),
    lineColor: Color = Color.White,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(140.dp)
) {
    Canvas(modifier = modifier) {
        if (currentPoints.size < 2) return@Canvas

        fun mapPoints(values: List<Float>): List<Offset> {
            val maxVal = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
            val stepX = size.width / (values.size - 1).coerceAtLeast(1)
            val verticalPadding = 12.dp.toPx()
            val chartHeight = (size.height - verticalPadding * 2f).coerceAtLeast(10f)
            return values.mapIndexed { idx, v ->
                val normalized = (v / maxVal).coerceIn(0f, 1f)
                Offset(
                    x = idx * stepX,
                    y = verticalPadding + chartHeight * (1f - normalized)
                )
            }
        }

        fun buildBezierPath(offsets: List<Offset>): Path {
            return Path().apply {
                moveTo(offsets.first().x, offsets.first().y)
                for (i in 0 until offsets.lastIndex) {
                    val p0 = offsets[i]
                    val p1 = offsets[i + 1]
                    val controlX = (p0.x + p1.x) / 2f
                    cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                }
            }
        }

        if (previousPoints.size >= 2) {
            val prevOffsets = mapPoints(previousPoints)
            drawPath(
                path = buildBezierPath(prevOffsets),
                color = LifeOsTheme.TextMuted.copy(alpha = 0.55f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        val currOffsets = mapPoints(currentPoints)
        val linePath = buildBezierPath(currOffsets)
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(currOffsets.last().x, size.height)
            lineTo(currOffsets.first().x, size.height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.28f),
                    Color.Transparent
                )
            )
        )

        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        val endPoint = currOffsets.last()
        drawCircle(
            color = lineColor.copy(alpha = 0.25f),
            radius = 10.dp.toPx(),
            center = endPoint
        )
        drawCircle(
            color = lineColor,
            radius = 5.dp.toPx(),
            center = endPoint
        )
    }
}
