package com.lifeos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Micro-Heatmap renderizado diretamente em GPU via Canvas (Pilar 8.2 + Referências 05, 09 e 10).
 * Inclui coluna vertical S..D à esquerda e quadrados arredondados multi-intensidade.
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
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 9.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            val rows = 7
            val cols = max(1, activityMatrix.firstOrNull()?.size ?: 20)
            val cellSpacing = 3.dp.toPx()

            val availableWidth = (size.width - (cellSpacing * (cols - 1))).coerceAtLeast(1f)
            val availableHeight = (size.height - (cellSpacing * (rows - 1))).coerceAtLeast(1f)

            val cellWidth = availableWidth / cols
            val cellHeight = availableHeight / rows
            val corner = CornerRadius(3.dp.toPx(), 3.dp.toPx())

            for (col in 0 until cols) {
                for (row in 0 until rows) {
                    val intensity = activityMatrix.getOrNull(row)?.getOrNull(col) ?: 0f
                    val color = if (intensity > 0f) {
                        accentColor.copy(alpha = (0.32f + (intensity * 0.68f)).coerceIn(0f, 1f))
                    } else {
                        accentColor.copy(alpha = 0.09f)
                    }

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(
                            x = col * (cellWidth + cellSpacing),
                            y = row * (cellHeight + cellSpacing)
                        ),
                        size = Size(cellWidth, cellHeight),
                        cornerRadius = corner
                    )
                }
            }
        }
    }
}
