package com.lifeos.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.lifeos.ui.theme.LifeOsTheme
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Barra segmentada multicolorida de despesas por categoria em Zero-GC (drawWithCache).
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
    Spacer(
        modifier = modifier.drawWithCache {
            val radius = CornerRadius(size.height / 2f, size.height / 2f)
            val normalized = fractions.map { it.coerceAtLeast(0.04f) }
            val sum = normalized.sum().coerceAtLeast(0.01f)
            val gap = 6.dp.toPx()
            val usableWidth = (size.width - gap * max(0, normalized.size - 1)).coerceAtLeast(10f)

            onDrawBehind {
                if (normalized.isEmpty()) {
                    drawRoundRect(
                        color = LifeOsTheme.SurfaceElevated,
                        size = size,
                        cornerRadius = radius
                    )
                    return@onDrawBehind
                }

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
    )
}

/**
 * Gráfico #3 da Suíte 120 FPS — Escada de Queima de Caixa (Pilar 6.3 + Referência 03).
 * Tijolos digitais empilhados em degradê térmico do laranja vivo (#FF8A00) ao vermelho carmesim
 * profundo (#B71C1C), com geometria pré-cacheada em Modifier.drawWithCache (Zero-GC).
 */
@Composable
fun SpendingBurnMatrix(
    currentMonthCumulative: List<Double>,
    previousMonthCumulative: List<Double>,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(170.dp)
) {
    Spacer(
        modifier = modifier.drawWithCache {
            val cols = max(15, max(currentMonthCumulative.size, previousMonthCumulative.size))
            val rows = 16
            val maxVal = max(
                currentMonthCumulative.maxOrNull() ?: 1.0,
                previousMonthCumulative.maxOrNull() ?: 1.0
            ).coerceAtLeast(1.0)

            val gap = 2.dp.toPx()
            val cellWidth = ((size.width - gap * (cols - 1)) / cols).coerceAtLeast(2f)
            val cellHeight = ((size.height - gap * (rows - 1)) / rows).coerceAtLeast(2f)
            val brickSize = Size(cellWidth, cellHeight)
            val corner = CornerRadius(1.8.dp.toPx(), 1.8.dp.toPx())

            val orangeLow = Color(0xFFFF8A00)
            val crimsonHigh = Color(0xFFB71C1C)
            val brickPalette = Array(rows) { blockIdx ->
                val t = (blockIdx.toFloat() / (rows - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
                lerp(orangeLow, crimsonHigh, t)
            }
            val prevBlocksByCol = IntArray(cols) { col ->
                (((previousMonthCumulative.getOrNull(col) ?: 0.0) / maxVal).coerceIn(0.0, 1.0) * rows).roundToInt()
            }
            val currBlocksByCol = IntArray(cols) { col ->
                (((currentMonthCumulative.getOrNull(col) ?: 0.0) / maxVal).coerceIn(0.0, 1.0) * rows).roundToInt()
            }
            val prevShadowColor = LifeOsTheme.SurfaceElevated.copy(alpha = 0.95f)

            onDrawBehind {
                for (col in 0 until cols) {
                    val x = col * (cellWidth + gap)
                    val prevBlocks = prevBlocksByCol[col]
                    val currBlocks = currBlocksByCol[col]
                    val activeCol = col < currentMonthCumulative.size

                    for (blockIndex in 0 until rows) {
                        val rowFromTop = (rows - 1) - blockIndex
                        val brickColor = when {
                            activeCol && blockIndex < currBlocks -> brickPalette[blockIndex]
                            blockIndex < prevBlocks -> prevShadowColor
                            else -> Color.Transparent
                        }

                        if (brickColor != Color.Transparent) {
                            drawRoundRect(
                                color = brickColor,
                                topLeft = Offset(x, rowFromTop * (cellHeight + gap)),
                                size = brickSize,
                                cornerRadius = corner
                            )
                        }
                    }
                }
            }
        }
    )
}
