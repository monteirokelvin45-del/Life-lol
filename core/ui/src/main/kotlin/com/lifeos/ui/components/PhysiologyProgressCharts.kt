package com.lifeos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.ui.theme.LifeOsTheme

/**
 * Barras verticais em gradiente verde-limão + barra de pico dourada + linha pontilhada de meta
 * (Referência 08 — "Kalo / Your progress").
 */
@Composable
fun WeeklyGradientBarsChart(
    dailyValues: List<Float>,
    targetLineRatio: Float = 0.68f,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val labels = listOf("S", "T", "Q", "Q", "S", "S", "D")
    val safeValues = if (dailyValues.size >= 7) dailyValues.take(7) else listOf(0.75f, 0.70f, 0.74f, 0.72f, 0.88f, 0.73f, 0.65f)
    val peakIdx = safeValues.indices.maxByOrNull { safeValues[it] } ?: 4

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
        ) {
            val maxVal = (safeValues.maxOrNull() ?: 1f).coerceAtLeast(1f)
            val targetY = size.height * (1f - targetLineRatio.coerceIn(0.15f, 0.9f))

            drawLine(
                color = LifeOsTheme.TextMuted.copy(alpha = 0.6f),
                start = Offset(0f, targetY),
                end = Offset(size.width, targetY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )

            val slotWidth = size.width / 7f
            val barWidth = (slotWidth * 0.28f).coerceAtLeast(8.dp.toPx())
            val corner = CornerRadius(barWidth / 2f, barWidth / 2f)

            safeValues.forEachIndexed { idx, value ->
                val ratio = (value / maxVal).coerceIn(0.15f, 1f)
                val barHeight = size.height * ratio * 0.92f
                val left = (idx * slotWidth) + (slotWidth - barWidth) / 2f
                val top = size.height - barHeight
                val brush = if (idx == peakIdx) {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFD166), Color(0xFFB68924)),
                        startY = top,
                        endY = size.height
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFB7E856), Color(0xFF4F6F1E)),
                        startY = top,
                        endY = size.height
                    )
                }
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = corner
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            labels.forEach { ch ->
                Text(ch, color = LifeOsTheme.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/**
 * Linha de tendência semanal com pontos conectados (Referência 08 — "Weight / Load trend").
 */
@Composable
fun WeeklyTrendDotsChart(
    points: List<Float>,
    lineColor: Color = Color(0xFFB7E856),
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val labels = listOf("S", "T", "Q", "Q", "S", "S", "D")
    val safePoints = if (points.size >= 7) points.take(7) else listOf(10f, 10f, 11f, 11.5f, 12f, 12f, 12.5f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
        ) {
            val minV = (safePoints.minOrNull() ?: 0f) - 1f
            val maxV = (safePoints.maxOrNull() ?: 1f) + 1f
            val span = (maxV - minV).coerceAtLeast(1f)
            val slotWidth = size.width / 7f

            val offsets = safePoints.mapIndexed { idx, v ->
                val norm = ((v - minV) / span).coerceIn(0f, 1f)
                Offset(
                    x = idx * slotWidth + slotWidth / 2f,
                    y = 10.dp.toPx() + (size.height - 20.dp.toPx()) * (1f - norm)
                )
            }

            for (row in 0..2) {
                val y = size.height * (row / 2f)
                drawLine(
                    color = LifeOsTheme.BorderSubtle,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            for (i in 0 until offsets.lastIndex) {
                drawLine(
                    color = lineColor,
                    start = offsets[i],
                    end = offsets[i + 1],
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            offsets.forEach { pt ->
                drawCircle(color = lineColor, radius = 4.dp.toPx(), center = pt)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            labels.forEach { ch ->
                Text(ch, color = LifeOsTheme.TextMuted, fontSize = 11.sp)
            }
        }
    }
}

/**
 * Anel luminoso circular de Streak com chama central (Referência 08 — "Streak 12 days").
 */
@Composable
fun CircularStreakRing(
    progress: Float,
    modifier: Modifier = Modifier.size(64.dp)
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(60.dp)) {
            val stroke = 5.5.dp.toPx()
            drawCircle(
                color = Color(0xFF232018),
                style = Stroke(width = stroke)
            )
            drawArc(
                color = LifeOsTheme.AmberFocus,
                startAngle = -90f,
                sweepAngle = (360f * progress.coerceIn(0.12f, 1f)),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text("🔥", fontSize = 20.sp)
    }
}

/**
 * Donut tricolor de equilíbrio de estímulos / domínios (Referência 08 — "Macro balance").
 */
@Composable
fun MacroDomainDonutRing(
    fractions: List<Float> = listOf(0.42f, 0.33f, 0.25f),
    colors: List<Color> = listOf(Color(0xFFB7E856), LifeOsTheme.GreenHealth, LifeOsTheme.AmberFocus),
    modifier: Modifier = Modifier.size(48.dp)
) {
    Canvas(modifier = modifier) {
        val stroke = 6.dp.toPx()
        val sum = fractions.sum().coerceAtLeast(0.01f)
        var start = -90f
        fractions.forEachIndexed { idx, value ->
            val sweep = 360f * (value / sum)
            drawArc(
                color = colors[idx % colors.size],
                startAngle = start,
                sweepAngle = (sweep - 6f).coerceAtLeast(8f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            start += sweep
        }
    }
}

/**
 * Card de celebração e monitoramento semanal de Streak com holofote radial laranja e 7 círculos
 * (Referências 06 "Days Streak" e 07 "Steps/Streak Widget").
 */
@Composable
fun StreakWeekSpotlightBanner(
    streakDays: Int,
    weekCompletedFlags: List<Boolean>,
    progressPercent: Int,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val dayNames = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        LifeOsTheme.OrangeStreak.copy(alpha = 0.24f),
                        LifeOsTheme.SurfaceDark
                    ),
                    radius = 520f
                )
            )
            .border(1.dp, LifeOsTheme.BorderSubtle, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(LifeOsTheme.OrangeStreak.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔥", fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column {
                    Text("STREAK ATIVO", color = LifeOsTheme.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$streakDays", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("DIAS SEGUIDOS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Text(
                text = "$progressPercent%",
                color = LifeOsTheme.GreenHealth,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayNames.forEachIndexed { idx, name ->
                val done = weekCompletedFlags.getOrElse(idx) { false }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (done) LifeOsTheme.GreenHealth else Color(0xFF1A1B1E))
                            .border(
                                width = 1.5.dp,
                                color = if (done) LifeOsTheme.GreenHealth else Color(0xFF2B2D33),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (done) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(name, color = LifeOsTheme.TextSecondary, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1A281E))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (progressPercent / 100f).coerceIn(0.06f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(LifeOsTheme.GreenHealth)
            )
        }
    }
}
