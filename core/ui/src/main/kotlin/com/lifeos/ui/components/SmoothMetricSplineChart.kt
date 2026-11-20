package com.lifeos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.ui.theme.LifeOsTheme
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Gráfico #4 da Suíte 120 FPS — Curva Spline de Bézier com Haptic Scrubber (Pilar 6.4 & Pilar 7.3).
 * - Paths reutilizados em memória (remember { Path() } + reset() em drawWithCache -> Zero GC).
 * - Resposta vibratória milimétrica (HapticFeedbackType.TextHandleMove) ao arrastar o dedo sobre a curva.
 */
@Composable
fun SmoothMetricSplineChart(
    currentPoints: List<Float>,
    previousPoints: List<Float> = emptyList(),
    lineColor: Color = Color.White,
    xLabels: List<String> = listOf("Sem 1", "Sem 2", "Sem 3", "Sem 4"),
    unitSuffix: String = "%",
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val haptics = LocalHapticFeedback.current
    var scrubbedIndex by remember { mutableIntStateOf(-1) }
    var chartWidthPx by remember { mutableIntStateOf(1) }

    // Objetos Path alocados uma única vez em RAM e reaproveitados sem GC por quadro
    val cachedLinePath = remember { Path() }
    val cachedFillPath = remember { Path() }
    val cachedPrevPath = remember { Path() }

    Column(modifier = modifier) {
        if (scrubbedIndex in currentPoints.indices) {
            val valAtFinger = currentPoints[scrubbedIndex]
            val lbl = xLabels.getOrElse(scrubbedIndex) { "Ponto ${scrubbedIndex + 1}" }
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LifeOsTheme.SurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "⚡ Scrubber %s: %.1f%s", lbl, valAtFinger, unitSuffix),
                    color = lineColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .onSizeChanged { chartWidthPx = it.width.coerceAtLeast(1) }
                .pointerInput(currentPoints.size, chartWidthPx) {
                    if (currentPoints.size < 2) return@pointerInput
                    detectTapGestures { offset ->
                        val idx = ((offset.x / chartWidthPx) * (currentPoints.size - 1))
                            .roundToInt()
                            .coerceIn(0, currentPoints.lastIndex)
                        if (idx != scrubbedIndex) {
                            scrubbedIndex = idx
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                }
                .pointerInput(currentPoints.size, chartWidthPx) {
                    if (currentPoints.size < 2) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = { scrubbedIndex = -1 },
                        onDragCancel = { scrubbedIndex = -1 }
                    ) { change, _ ->
                        change.consume()
                        val idx = ((change.position.x / chartWidthPx) * (currentPoints.size - 1))
                            .roundToInt()
                            .coerceIn(0, currentPoints.lastIndex)
                        if (idx != scrubbedIndex) {
                            scrubbedIndex = idx
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                }
                .drawWithCache {
                    if (currentPoints.size < 2) {
                        return@drawWithCache onDrawBehind {}
                    }

                    val verticalPadding = 14.dp.toPx()
                    val chartHeight = (size.height - verticalPadding * 2f).coerceAtLeast(10f)
                    val globalMax = maxOf(
                        currentPoints.maxOrNull() ?: 1f,
                        previousPoints.maxOrNull() ?: 1f
                    ).coerceAtLeast(1f)

                    fun computeOffsets(values: List<Float>): Array<Offset> {
                        val stepX = (size.width - 14.dp.toPx()) / (values.size - 1).coerceAtLeast(1)
                        return Array(values.size) { idx ->
                            val normalized = (values[idx] / globalMax).coerceIn(0f, 1f)
                            Offset(
                                x = idx * stepX,
                                y = verticalPadding + chartHeight * (1f - normalized)
                            )
                        }
                    }

                    fun populateCubicBezier(target: Path, pts: Array<Offset>) {
                        target.reset()
                        target.moveTo(pts[0].x, pts[0].y)
                        for (i in 0 until pts.lastIndex) {
                            val p0 = pts[i]
                            val p1 = pts[i + 1]
                            val cx = (p0.x + p1.x) / 2f
                            target.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                    }

                    val hasPrev = previousPoints.size >= 2
                    if (hasPrev) {
                        populateCubicBezier(cachedPrevPath, computeOffsets(previousPoints))
                    }

                    val currOffsets = computeOffsets(currentPoints)
                    populateCubicBezier(cachedLinePath, currOffsets)

                    cachedFillPath.reset()
                    cachedFillPath.addPath(cachedLinePath)
                    cachedFillPath.lineTo(currOffsets.last().x, size.height)
                    cachedFillPath.lineTo(currOffsets.first().x, size.height)
                    cachedFillPath.close()

                    val areaBrush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.32f), Color.Transparent)
                    )
                    val mainStroke = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                    val prevStroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    val scrubberDash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    val gridStrokePx = 1.dp.toPx()
                    val haloRadius = 12.dp.toPx()
                    val dotRadius = 6.5.dp.toPx()
                    val activeIdx = if (scrubbedIndex in currOffsets.indices) scrubbedIndex else currOffsets.lastIndex
                    val activePoint = currOffsets[activeIdx]

                    onDrawBehind {
                        for (row in 0..4) {
                            val y = verticalPadding + (chartHeight * (row / 4f))
                            drawLine(
                                color = LifeOsTheme.BorderSubtle,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = gridStrokePx
                            )
                        }

                        if (hasPrev) {
                            drawPath(
                                path = cachedPrevPath,
                                color = LifeOsTheme.TextMuted.copy(alpha = 0.65f),
                                style = prevStroke
                            )
                        }

                        drawPath(path = cachedFillPath, brush = areaBrush)
                        drawPath(path = cachedLinePath, color = lineColor, style = mainStroke)

                        if (scrubbedIndex in currOffsets.indices) {
                            drawLine(
                                color = lineColor.copy(alpha = 0.7f),
                                start = Offset(activePoint.x, 0f),
                                end = Offset(activePoint.x, size.height),
                                strokeWidth = gridStrokePx * 1.4f,
                                pathEffect = scrubberDash
                            )
                        }

                        drawCircle(
                            color = lineColor.copy(alpha = 0.24f),
                            radius = haloRadius,
                            center = activePoint
                        )
                        drawCircle(
                            color = Color.White,
                            radius = dotRadius,
                            center = activePoint
                        )
                    }
                }
        )

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
