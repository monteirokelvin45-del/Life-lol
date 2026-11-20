package com.lifeos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.ui.theme.LifeOsTheme

/**
 * Curva analítica Bezier suave com área sombreada, grade horizontal sutil, ponto luminoso final
 * e eixo Semanal (Referência 02 — "Analytics / Sleep & Cognitive Quality").
 */
@Composable
fun SmoothMetricSplineChart(
    currentPoints: List<Float>,
    previousPoints: List<Float> = emptyList(),
    lineColor: Color = Color.White,
    xLabels: List<String> = listOf("Sem 1", "Sem 2", "Sem 3", "Sem 4"),
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            if (currentPoints.size < 2) return@Canvas

            val verticalPadding = 14.dp.toPx()
            val chartHeight = (size.height - verticalPadding * 2f).coerceAtLeast(10f)

            // Linhas horizontais sutis de referência (1..5)
            for (row in 0..4) {
                val y = verticalPadding + (chartHeight * (row / 4f))
                drawLine(
                    color = LifeOsTheme.BorderSubtle,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val globalMax = maxOf(
                currentPoints.maxOrNull() ?: 1f,
                previousPoints.maxOrNull() ?: 1f
            ).coerceAtLeast(1f)

            fun mapPoints(values: List<Float>): List<Offset> {
                val stepX = (size.width - 14.dp.toPx()) / (values.size - 1).coerceAtLeast(1)
                return values.mapIndexed { idx, v ->
                    val normalized = (v / globalMax).coerceIn(0f, 1f)
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
                    color = LifeOsTheme.TextMuted.copy(alpha = 0.65f),
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
                        lineColor.copy(alpha = 0.30f),
                        Color.Transparent
                    )
                )
            )

            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
            )

            val endPoint = currOffsets.last()
            drawCircle(
                color = lineColor.copy(alpha = 0.22f),
                radius = 12.dp.toPx(),
                center = endPoint
            )
            drawCircle(
                color = Color.White,
                radius = 7.dp.toPx(),
                center = endPoint
            )
        }

        if (xLabels.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                xLabels.forEach { lbl ->
                    Text(lbl, color = LifeOsTheme.TextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}
