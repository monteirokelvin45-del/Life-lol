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
import kotlin.math.roundToInt

/**
 * Barra segmentada multicolorida de despesas por categoria (Referência 01 — Wallet Spending).
 */
@Composable
fun SegmentedCategoryBar(
    fractions: List<Float>,
    colors: List<Color> = listOf(
        LifeOsTheme.PurpleStudy,
        LifeOsTheme.RedExpense,
        LifeOsTheme.GreenHealth,
        LifeOsTheme.AmberFocus,
        LifeOsTheme.CyanFinance
    ),
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(22.dp)
) {
    Canvas(modifier = modifier) {
        if (fractions.isEmpty()) {
            drawRoundRect(
                color = LifeOsTheme.SurfaceElevated,
                size = size,
                cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
            )
            return@Canvas
        }

        val normalized = fractions.map { it.coerceAtLeast(0.04f) }
        val sum = normalized.sum().coerceAtLeast(0.01f)
        val gap = 6.dp.toPx()
        val usableWidth = (size.width - gap * (normalized.size - 1)).coerceAtLeast(10f)
        val radius = CornerRadius(size.height / 2f, size.height / 2f)

        var currentX = 0f
        normalized.forEachIndexed { index, value ->
            val segWidth = usableWidth * (value / sum)
            drawRoundRect(
                color = colors[index % colors.size],
                topLeft = Offset(currentX, 0f),
                size = Size(segWidth, size.height),
                cornerRadius = radius
            )
            currentX += segWidth + gap
        }
    }
}

/**
 * Matriz cumulativa em blocos pixelados comparando o gasto acumulado do mês atual
 * versus o mês anterior (Referência 03 — `$123 today / You're spending less...`).
 */
@Composable
fun SpendingBurnMatrix(
    currentMonthCumulative: List<Double>,
    previousMonthCumulative: List<Double>,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(170.dp)
) {
    Canvas(modifier = modifier) {
        val cols = max(15, max(currentMonthCumulative.size, previousMonthCumulative.size))
        val rows = 16
        val maxVal = max(
            currentMonthCumulative.maxOrNull() ?: 1.0,
            previousMonthCumulative.maxOrNull() ?: 1.0
        ).coerceAtLeast(1.0)

        val gap = 2.dp.toPx()
        val cellWidth = ((size.width - gap * (cols - 1)) / cols).coerceAtLeast(2f)
        val cellHeight = ((size.height - gap * (rows - 1)) / rows).coerceAtLeast(2f)
        val corner = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())

        for (col in 0 until cols) {
            val prevRatio = ((previousMonthCumulative.getOrNull(col) ?: 0.0) / maxVal).coerceIn(0.0, 1.0)
            val currRatio = ((currentMonthCumulative.getOrNull(col) ?: 0.0) / maxVal).coerceIn(0.0, 1.0)

            val prevBlocks = (prevRatio * rows).roundToInt()
            val currBlocks = (currRatio * rows).roundToInt()

            for (blockIndex in 0 until rows) {
                val rowFromTop = (rows - 1) - blockIndex
                val isWithinCurr = col < currentMonthCumulative.size && blockIndex < currBlocks
                val isWithinPrev = blockIndex < prevBlocks

                val blockColor = when {
                    isWithinCurr -> {
                        val verticalGlow = (blockIndex.toFloat() / rows.toFloat()).coerceIn(0f, 1f)
                        LifeOsTheme.OrangeStreak.copy(alpha = 0.35f + (verticalGlow * 0.65f))
                    }
                    isWithinPrev -> LifeOsTheme.SurfaceElevated.copy(alpha = 0.95f)
                    else -> Color.Transparent
                }

                if (blockColor != Color.Transparent) {
                    drawRoundRect(
                        color = blockColor,
                        topLeft = Offset(
                            x = col * (cellWidth + gap),
                            y = rowFromTop * (cellHeight + gap)
                        ),
                        size = Size(cellWidth, cellHeight),
                        cornerRadius = corner
                    )
                }
            }
        }
    }
}
