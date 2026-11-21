package com.lifeos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.ui.theme.LifeOsTheme
import kotlin.math.max

/**
 * Gráfico #1 da Suíte 120 FPS — Micro-Heatmap (7x20 compacto e 7x52 GitHub Anual).
 * Performance Zero Garbage Collection (Pilar 7.3): geometria de células pré-calculada em
 * Modifier.drawWithCache sem alocações por quadro em onDrawBehind.
 */
@Composable
fun HabitMicroHeatmap(
    activityMatrix: List<List<Float>>,
    accentColor: Color = LifeOsTheme.GreenHealth,
    showWeekdayLabels: Boolean = true,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(92.dp)
) {
    val dayInitials = listOf("S", "T", "Q", "Q", "S", "S", "D")

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showWeekdayLabels) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                dayInitials.forEach { ch ->
                    Text(
                        text = ch,
                        color = LifeOsTheme.TextMuted,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 9.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Spacer(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .drawWithCache {
                    val rows = 7
                    val cols = max(1, activityMatrix.firstOrNull()?.size ?: 20)
                    val cellSpacing = 3.dp.toPx()

                    val availableWidth = (size.width - (cellSpacing * (cols - 1))).coerceAtLeast(1f)
                    val availableHeight = (size.height - (cellSpacing * (rows - 1))).coerceAtLeast(1f)
                    val cellSize = Size(availableWidth / cols, availableHeight / rows)
                    val corner = CornerRadius(3.2.dp.toPx(), 3.2.dp.toPx())
                    val emptyColor = Color(0xFF161922)

                    onDrawBehind {
                        for (col in 0 until cols) {
                            val x = col * (cellSize.width + cellSpacing)
                            for (row in 0 until rows) {
                                val intensity = activityMatrix.getOrNull(row)?.getOrNull(col) ?: 0f
                                val color = if (intensity > 0f) {
                                    accentColor.copy(alpha = (0.28f + (intensity * 0.72f)).coerceIn(0f, 1f))
                                } else {
                                    emptyColor
                                }
                                drawRoundRect(
                                    color = color,
                                    topLeft = Offset(x, row * (cellSize.height + cellSpacing)),
                                    size = cellSize,
                                    cornerRadius = corner
                                )
                            }
                        }
                    }
                }
        )
    }
}

/**
 * Matriz Anual Completa 7x52 estilo GitHub (364 dias — Pilar 6.1) com régua de meses,
 * legenda de densidade e rolagem horizontal fluida a 120 FPS.
 */
@Composable
fun AnnualGitHubHeatmap7x52(
    annualMatrix7x52: List<List<Float>>,
    accentColor: Color = LifeOsTheme.GreenHealth,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val scrollState = rememberScrollState(initial = Int.MAX_VALUE)
    val months = listOf("Dez", "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov")

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            Column(modifier = Modifier.width(652.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, bottom = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    months.forEach { m ->
                        Text(
                            text = m,
                            color = LifeOsTheme.TextMuted,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                HabitMicroHeatmap(
                    activityMatrix = annualMatrix7x52,
                    accentColor = accentColor,
                    showWeekdayLabels = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Menos", color = LifeOsTheme.TextMuted, fontSize = 9.5.sp)
            Spacer(modifier = Modifier.width(6.dp))
            listOf(0f, 0.28f, 0.55f, 0.80f, 1.0f).forEach { lvl ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 1.5.dp)
                        .size(9.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(
                            if (lvl == 0f) Color(0xFF161922) else accentColor.copy(alpha = lvl)
                        )
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text("Mais", color = LifeOsTheme.TextSecondary, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
