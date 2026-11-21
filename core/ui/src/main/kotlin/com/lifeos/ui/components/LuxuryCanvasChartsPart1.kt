package com.lifeos.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.ui.theme.LifeOsTheme
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Gráfico #5 da Suíte 120 FPS — Curva do Efeito Composto com Chama 3D (Pilar 6.5 & Pilar 7.3).
 * Exibe a aceleração exponencial nos marcos Dia 1, Dia 7, Dia 30 e Dia 100 usando Paths
 * cacheados e reaproveitados (Zero Garbage Collection).
 */
@Composable
fun CompoundEffectFlameChart(
    dailyRate: Float = 0.018f,
    activeStreakDays: Int = 33,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val curvePath = remember { Path() }
    val areaPath = remember { Path() }
    val outerFlamePath = remember { Path() }
    val midFlamePath = remember { Path() }
    val coreFlamePath = remember { Path() }

    Column(modifier = modifier) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .drawWithCache {
                    val padTop = 30.dp.toPx()
                    val padBottom = 14.dp.toPx()
                    val padRight = 22.dp.toPx()
                    val usableW = (size.width - padRight).coerceAtLeast(20f)
                    val usableH = (size.height - padTop - padBottom).coerceAtLeast(20f)

                    val totalDays = 100
                    val minVal = 1.0f
                    val maxVal = (1.0 + dailyRate.toDouble()).pow(totalDays.toDouble()).toFloat().coerceAtLeast(2f)

                    fun pointForDay(day: Int): Offset {
                        val x = ((day - 1).toFloat() / (totalDays - 1).toFloat()) * usableW
                        val rawY = (1.0 + dailyRate.toDouble()).pow(day.toDouble()).toFloat()
                        val normY = ((rawY - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                        val y = padTop + usableH * (1f - normY)
                        return Offset(x, y)
                    }

                    curvePath.reset()
                    val startPt = pointForDay(1)
                    curvePath.moveTo(startPt.x, startPt.y)
                    for (d in 2..totalDays) {
                        val pt = pointForDay(d)
                        curvePath.lineTo(pt.x, pt.y)
                    }

                    val tipPt = pointForDay(totalDays)
                    areaPath.reset()
                    areaPath.addPath(curvePath)
                    areaPath.lineTo(tipPt.x, size.height - padBottom)
                    areaPath.lineTo(startPt.x, size.height - padBottom)
                    areaPath.close()

                    // Construção vetorial da Chama 3D multicamada no Dia 100
                    val fx = tipPt.x
                    val fy = tipPt.y
                    val fw = 13.5.dp.toPx()
                    val fh = 25.dp.toPx()

                    outerFlamePath.reset()
                    outerFlamePath.moveTo(fx, fy - fh)
                    outerFlamePath.cubicTo(fx + fw * 1.25f, fy - fh * 0.45f, fx + fw, fy + fh * 0.25f, fx, fy + fh * 0.35f)
                    outerFlamePath.cubicTo(fx - fw, fy + fh * 0.25f, fx - fw * 1.25f, fy - fh * 0.45f, fx, fy - fh)
                    outerFlamePath.close()

                    midFlamePath.reset()
                    midFlamePath.moveTo(fx, fy - fh * 0.72f)
                    midFlamePath.cubicTo(fx + fw * 0.82f, fy - fh * 0.28f, fx + fw * 0.68f, fy + fh * 0.20f, fx, fy + fh * 0.28f)
                    midFlamePath.cubicTo(fx - fw * 0.68f, fy + fh * 0.20f, fx - fw * 0.82f, fy - fh * 0.28f, fx, fy - fh * 0.72f)
                    midFlamePath.close()

                    coreFlamePath.reset()
                    coreFlamePath.moveTo(fx, fy - fh * 0.40f)
                    coreFlamePath.cubicTo(fx + fw * 0.42f, fy - fh * 0.08f, fx + fw * 0.36f, fy + fh * 0.16f, fx, fy + fh * 0.22f)
                    coreFlamePath.cubicTo(fx - fw * 0.36f, fy + fh * 0.16f, fx - fw * 0.42f, fy - fh * 0.08f, fx, fy - fh * 0.40f)
                    coreFlamePath.close()

                    val milestones = intArrayOf(1, 7, 30, 100)
                    val milestoneOffsets = Array(milestones.size) { i -> pointForDay(milestones[i]) }
                    val userDayPt = pointForDay(activeStreakDays.coerceIn(1, 100))

                    val areaBrush = Brush.verticalGradient(
                        colors = listOf(
                            LifeOsTheme.OrangeStreak.copy(alpha = 0.38f),
                            LifeOsTheme.AmberFocus.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                    val lineBrush = Brush.horizontalGradient(
                        colors = listOf(LifeOsTheme.AmberFocus, LifeOsTheme.OrangeStreak, Color(0xFFFF2A4B))
                    )
                    val outerFlameBrush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFF2A4B), LifeOsTheme.OrangeStreak)
                    )
                    val midFlameBrush = Brush.verticalGradient(
                        colors = listOf(LifeOsTheme.AmberFocus, LifeOsTheme.OrangeStreak)
                    )
                    val strokeCurve = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                    val dashGuide = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                    val oneDp = 1.dp.toPx()
                    val nodeR = 4.5.dp.toPx()
                    val userHaloR = 10.dp.toPx()

                    onDrawBehind {
                        for (row in 0..3) {
                            val y = padTop + usableH * (row / 3f)
                            drawLine(
                                color = LifeOsTheme.BorderSubtle,
                                start = Offset(0f, y),
                                end = Offset(usableW, y),
                                strokeWidth = oneDp
                            )
                        }

                        drawPath(path = areaPath, brush = areaBrush)
                        drawPath(path = curvePath, brush = lineBrush, style = strokeCurve)

                        milestoneOffsets.forEach { pt ->
                            drawLine(
                                color = LifeOsTheme.TextMuted.copy(alpha = 0.45f),
                                start = pt,
                                end = Offset(pt.x, size.height - padBottom),
                                strokeWidth = oneDp,
                                pathEffect = dashGuide
                            )
                            drawCircle(color = LifeOsTheme.AmberFocus, radius = nodeR, center = pt)
                        }

                        // Posição atual do usuário na curva exponencial
                        drawCircle(color = LifeOsTheme.GreenHealth.copy(alpha = 0.28f), radius = userHaloR, center = userDayPt)
                        drawCircle(color = LifeOsTheme.GreenHealth, radius = nodeR, center = userDayPt)

                        // Chama 3D no ápice do Dia 100
                        drawCircle(
                            color = LifeOsTheme.OrangeStreak.copy(alpha = 0.24f),
                            radius = 22.dp.toPx(),
                            center = Offset(fx, fy - 4.dp.toPx())
                        )
                        drawPath(path = outerFlamePath, brush = outerFlameBrush)
                        drawPath(path = midFlamePath, brush = midFlameBrush)
                        drawPath(path = coreFlamePath, color = Color(0xFFFFF3B0))
                    }
                }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Dia 1 · 1.0x", color = LifeOsTheme.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text("Dia 7", color = LifeOsTheme.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text("Dia 30 · 1.7x", color = LifeOsTheme.AmberFocus, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("◆ Dia 100 · 6.0x", color = LifeOsTheme.OrangeStreak, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

/**
 * Gráfico #6 da Suíte 120 FPS — Anéis Concêntricos de Foco e Macros estilo Apple Fitness (Pilar 6.6).
 * Renderização elástica a 120 FPS com trilhas, arcos luminosos e pontas esféricas em Zero-GC.
 */
@Composable
fun ConcentricFocusMacroRings(
    outerFocusProgress: Float,    // Bloco Pomodoro / Estudo
    middleMacroProgress: Float,   // Proteína / Treino Fisiológico
    innerReadingProgress: Float,  // Leitura / Hidratação
    modifier: Modifier = Modifier.size(152.dp)
) {
    val spec = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    val animOuter by animateFloatAsState(outerFocusProgress.coerceIn(0.08f, 1f), spec, label = "outerRing")
    val animMid by animateFloatAsState(middleMacroProgress.coerceIn(0.08f, 1f), spec, label = "midRing")
    val animInner by animateFloatAsState(innerReadingProgress.coerceIn(0.08f, 1f), spec, label = "innerRing")

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Spacer(
            modifier = Modifier
                .size(148.dp)
                .drawWithCache {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val strokeW = 12.dp.toPx()
                    val gap = 5.dp.toPx()
                    val ringStroke = Stroke(width = strokeW, cap = StrokeCap.Round)

                    val rOuter = (size.minDimension / 2f) - strokeW / 2f - 2.dp.toPx()
                    val rMid = rOuter - strokeW - gap
                    val rInner = rMid - strokeW - gap

                    val rings = arrayOf(
                        Triple(rOuter, animOuter, Color(0xFFFF2A6D)),
                        Triple(rMid, animMid, Color(0xFFA8FF00)),
                        Triple(rInner, animInner, LifeOsTheme.CyanFinance)
                    )

                    onDrawBehind {
                        rings.forEach { (radius, fraction, ringColor) ->
                            val topLeft = Offset(center.x - radius, center.y - radius)
                            val arcSize = Size(radius * 2f, radius * 2f)

                            drawArc(
                                color = ringColor.copy(alpha = 0.14f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = ringStroke
                            )

                            val sweep = 360f * fraction
                            drawArc(
                                color = ringColor,
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = ringStroke
                            )

                            val endRad = Math.toRadians((-90f + sweep).toDouble())
                            val tipCenter = Offset(
                                x = center.x + (cos(endRad) * radius).toFloat(),
                                y = center.y + (sin(endRad) * radius).toFloat()
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.90f),
                                radius = strokeW * 0.25f,
                                center = tipCenter
                            )
                        }
                    }
                }
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val avgPct = (((animOuter + animMid + animInner) / 3f) * 100f).toInt()
            Text("$avgPct%", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text("RINGS", color = LifeOsTheme.TextMuted, fontWeight = FontWeight.ExtraBold, fontSize = 8.sp, letterSpacing = 1.sp)
        }
    }
}

/**
 * Gráfico #7 da Suíte 120 FPS — Gráfico de Sobrecarga Progressiva (Pilar 6.7).
 * Barras emparelhadas (Volume de Repetições × Carga Extra em kg) + linha matemática de
 * regressão linear (mínimos quadrados) renderizada em Zero-GC.
 */
@Composable
fun ProgressiveOverloadRegressionChart(
    monthlyRepsTotals: List<Float> = listOf(140f, 158f, 172f, 188f, 205f, 224f),
    monthlyLoadKg: List<Float> = listOf(8f, 10f, 12.5f, 15f, 17.5f, 20f),
    monthLabels: List<String> = listOf("Jun", "Jul", "Ago", "Set", "Out", "Nov"),
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Column(modifier = modifier) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .drawWithCache {
                    val count = minOf(monthlyRepsTotals.size, monthlyLoadKg.size).coerceAtLeast(2)
                    val maxReps = (monthlyRepsTotals.maxOrNull() ?: 1f).coerceAtLeast(1f)
                    val maxLoad = (monthlyLoadKg.maxOrNull() ?: 1f).coerceAtLeast(1f)
                    val padTop = 12.dp.toPx()
                    val padBottom = 8.dp.toPx()
                    val usableH = (size.height - padTop - padBottom).coerceAtLeast(20f)
                    val slotW = size.width / count.toFloat()
                    val barW = (slotW * 0.24f).coerceAtLeast(6.dp.toPx())
                    val gap = 3.dp.toPx()
                    val corner = CornerRadius(barW / 2f, barW / 2f)

                    val compositeNorm = FloatArray(count) { i ->
                        val rNorm = (monthlyRepsTotals[i] / maxReps).coerceIn(0f, 1f)
                        val lNorm = (monthlyLoadKg[i] / maxLoad).coerceIn(0f, 1f)
                        (rNorm * 0.5f) + (lNorm * 0.5f)
                    }
                    val meanX = (count - 1) / 2f
                    val meanY = compositeNorm.average().toFloat()
                    var num = 0f
                    var den = 0f
                    for (i in 0 until count) {
                        val dx = i - meanX
                        num += dx * (compositeNorm[i] - meanY)
                        den += dx * dx
                    }
                    val slope = if (den > 0f) num / den else 0f
                    val intercept = meanY - slope * meanX

                    val regStartX = slotW / 2f
                    val regEndX = (count - 1) * slotW + slotW / 2f
                    val regStartY = padTop + usableH * (1f - intercept.coerceIn(0.05f, 0.98f))
                    val regEndY = padTop + usableH * (1f - (slope * (count - 1) + intercept).coerceIn(0.05f, 0.98f))

                    val repsBrush = Brush.verticalGradient(
                        colors = listOf(LifeOsTheme.GreenHealth, LifeOsTheme.GreenHealth.copy(alpha = 0.28f))
                    )
                    val loadBrush = Brush.verticalGradient(
                        colors = listOf(LifeOsTheme.CyanFinance, LifeOsTheme.CyanFinance.copy(alpha = 0.28f))
                    )
                    val gridPx = 1.dp.toPx()
                    val regStrokePx = 2.6.dp.toPx()
                    val nodeR = 4.dp.toPx()

                    onDrawBehind {
                        for (r in 0..3) {
                            val y = padTop + usableH * (r / 3f)
                            drawLine(
                                color = LifeOsTheme.BorderSubtle,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = gridPx
                            )
                        }

                        for (i in 0 until count) {
                            val centerSlot = i * slotW + slotW / 2f
                            val repsH = usableH * (monthlyRepsTotals[i] / maxReps).coerceIn(0.12f, 1f)
                            val loadH = usableH * (monthlyLoadKg[i] / maxLoad).coerceIn(0.12f, 1f)

                            drawRoundRect(
                                brush = repsBrush,
                                topLeft = Offset(centerSlot - barW - gap / 2f, padTop + usableH - repsH),
                                size = Size(barW, repsH),
                                cornerRadius = corner
                            )
                            drawRoundRect(
                                brush = loadBrush,
                                topLeft = Offset(centerSlot + gap / 2f, padTop + usableH - loadH),
                                size = Size(barW, loadH),
                                cornerRadius = corner
                            )
                        }

                        drawLine(
                            color = LifeOsTheme.AmberFocus,
                            start = Offset(regStartX, regStartY),
                            end = Offset(regEndX, regEndY),
                            strokeWidth = regStrokePx,
                            cap = StrokeCap.Round
                        )
                        drawCircle(color = LifeOsTheme.AmberFocus, radius = nodeR, center = Offset(regStartX, regStartY))
                        drawCircle(color = LifeOsTheme.AmberFocus, radius = nodeR, center = Offset(regEndX, regEndY))
                    }
                }
        )

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            monthLabels.forEach { m ->
                Text(m, color = LifeOsTheme.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Gráfico #8 da Suíte 120 FPS — Diagrama de Fluxo de Sankey Fluido (Pilar 6.8 & Pilar 7.3).
 * Ramificação luminosa em curvas cúbicas de Bézier cacheadas (Zero-GC) com numerais tabulares formatados.
 */
@Composable
fun FluidSankeyCashflowChart(
    netIncome: Double,
    investmentsAmount: Double,
    fixedCostsAmount: Double,
    leakageAmount: Double,
    currencyCode: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val investRibbon = remember { Path() }
    val fixedRibbon = remember { Path() }
    val leakRibbon = remember { Path() }

    val totalOut = (investmentsAmount + fixedCostsAmount + leakageAmount).coerceAtLeast(1.0)
    val invFrac = (investmentsAmount / totalOut).toFloat().coerceIn(0.12f, 0.76f)
    val fixFrac = (fixedCostsAmount / totalOut).toFloat().coerceIn(0.12f, 0.76f)
    val leakFrac = (leakageAmount / totalOut).toFloat().coerceIn(0.08f, 0.76f)
    val normSum = (invFrac + fixFrac + leakFrac).coerceAtLeast(0.01f)

    val fInv = invFrac / normSum
    val fFix = fixFrac / normSum
    val fLeak = leakFrac / normSum

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.padding(end = 10.dp)) {
                Text("RENDA", color = LifeOsTheme.TextMuted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
                Text(
                    text = LifeOsTheme.formatMoney(netIncome, "", decimals = 0).trim(),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    softWrap = false
                )
                Text(currencyCode, color = LifeOsTheme.CyanFinance, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .height(152.dp)
                    .drawWithCache {
                        val leftBarW = 8.dp.toPx()
                        val rightBarW = 8.dp.toPx()
                        val startX = leftBarW + 2.dp.toPx()
                        val endX = size.width - rightBarW - 2.dp.toPx()

                        val sourceHeight = size.height * 0.64f
                        val sourceTop = (size.height - sourceHeight) / 2f

                        val hInv = sourceHeight * fInv
                        val hFix = sourceHeight * fFix
                        val hLeak = sourceHeight * fLeak

                        val targetGap = 15.dp.toPx()
                        val totalTargetSpan = hInv + hFix + hLeak + targetGap * 2f
                        val targetStartTop = ((size.height - totalTargetSpan) / 2f).coerceAtLeast(4f)

                        val tgtInvTop = targetStartTop
                        val tgtFixTop = tgtInvTop + hInv + targetGap
                        val tgtLeakTop = tgtFixTop + hFix + targetGap

                        val srcInvTop = sourceTop
                        val srcFixTop = srcInvTop + hInv
                        val srcLeakTop = srcFixTop + hFix

                        fun buildRibbon(path: Path, sTop: Float, tTop: Float, thickness: Float) {
                            val midX = (startX + endX) / 2f
                            path.reset()
                            path.moveTo(startX, sTop)
                            path.cubicTo(midX, sTop, midX, tTop, endX, tTop)
                            path.lineTo(endX, tTop + thickness)
                            path.cubicTo(midX, tTop + thickness, midX, sTop + thickness, startX, sTop + thickness)
                            path.close()
                        }

                        buildRibbon(investRibbon, srcInvTop, tgtInvTop, hInv)
                        buildRibbon(fixedRibbon, srcFixTop, tgtFixTop, hFix)
                        buildRibbon(leakRibbon, srcLeakTop, tgtLeakTop, hLeak)

                        val brushInv = Brush.horizontalGradient(
                            colors = listOf(LifeOsTheme.CyanFinance.copy(alpha = 0.48f), LifeOsTheme.GreenHealth.copy(alpha = 0.82f))
                        )
                        val brushFix = Brush.horizontalGradient(
                            colors = listOf(LifeOsTheme.CyanFinance.copy(alpha = 0.42f), LifeOsTheme.PurpleStudy.copy(alpha = 0.78f))
                        )
                        val brushLeak = Brush.horizontalGradient(
                            colors = listOf(LifeOsTheme.AmberFocus.copy(alpha = 0.42f), LifeOsTheme.RedExpense.copy(alpha = 0.82f))
                        )
                        val barCorner = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        val ribbonRim = Stroke(width = 0.8.dp.toPx())

                        onDrawBehind {
                            drawRoundRect(
                                color = LifeOsTheme.CyanFinance,
                                topLeft = Offset(0f, sourceTop),
                                size = Size(leftBarW, sourceHeight),
                                cornerRadius = barCorner
                            )

                            drawPath(path = investRibbon, brush = brushInv)
                            drawPath(path = investRibbon, color = LifeOsTheme.GreenHealth.copy(alpha = 0.45f), style = ribbonRim)

                            drawPath(path = fixedRibbon, brush = brushFix)
                            drawPath(path = fixedRibbon, color = LifeOsTheme.PurpleStudy.copy(alpha = 0.45f), style = ribbonRim)

                            drawPath(path = leakRibbon, brush = brushLeak)
                            drawPath(path = leakRibbon, color = LifeOsTheme.RedExpense.copy(alpha = 0.45f), style = ribbonRim)

                            drawRoundRect(
                                color = LifeOsTheme.GreenHealth,
                                topLeft = Offset(size.width - rightBarW, tgtInvTop),
                                size = Size(rightBarW, hInv),
                                cornerRadius = barCorner
                            )
                            drawRoundRect(
                                color = LifeOsTheme.PurpleStudy,
                                topLeft = Offset(size.width - rightBarW, tgtFixTop),
                                size = Size(rightBarW, hFix),
                                cornerRadius = barCorner
                            )
                            drawRoundRect(
                                color = LifeOsTheme.RedExpense,
                                topLeft = Offset(size.width - rightBarW, tgtLeakTop),
                                size = Size(rightBarW, hLeak),
                                cornerRadius = barCorner
                            )
                        }
                    }
            )

            Column(
                modifier = Modifier.padding(start = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text("Investimentos", color = LifeOsTheme.GreenHealth, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(LifeOsTheme.formatMoney(investmentsAmount, currencyCode, decimals = 0), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
                Column {
                    Text("Custos Fixos", color = LifeOsTheme.PurpleStudy, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(LifeOsTheme.formatMoney(fixedCostsAmount, currencyCode, decimals = 0), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
                Column {
                    Text("Vazamentos", color = LifeOsTheme.RedExpense, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(LifeOsTheme.formatMoney(leakageAmount, currencyCode, decimals = 0), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}
