package com.lifeos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lifeos.ui.theme.LifeOsTheme
import kotlin.math.max

/**
 * Micro-Heatmap renderizado diretamente em GPU via Canvas (Pilar 8.2 + Referências 05, 09 e 10).
 * Matriz temporal: 7 linhas (Seg..Dom) x 18 a 24 colunas semanais.
 */
@Composable
fun HabitMicroHeatmap(
    activityMatrix: List<List<Float>>,
    accentColor: Color = LifeOsTheme.GreenHealth,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(84.dp)
) {
    Canvas(modifier = modifier) {
        val rows = 7
        val cols = max(1, activityMatrix.firstOrNull()?.size ?: 20)
        val cellSpacing = 3.dp.toPx()

        val availableWidth = (size.width - (cellSpacing * (cols - 1))).coerceAtLeast(1f)
        val availableHeight = (size.height - (cellSpacing * (rows - 1))).coerceAtLeast(1f)

        val cellWidth = availableWidth / cols
        val cellHeight = availableHeight / rows
        val corner = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())

        for (col in 0 until cols) {
            for (row in 0 until rows) {
                val intensity = activityMatrix.getOrNull(row)?.getOrNull(col) ?: 0f
                val color = if (intensity > 0f) {
                    accentColor.copy(alpha = (0.28f + (intensity * 0.72f)).coerceIn(0f, 1f))
                } else {
                    LifeOsTheme.SurfaceElevated
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
