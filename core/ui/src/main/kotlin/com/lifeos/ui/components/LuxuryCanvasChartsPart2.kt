package com.lifeos.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.ui.theme.LifeOsTheme
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Gráfico #9 da Suíte 120 FPS — Curva Dente-de-Serra de Ebbinghaus (Pilar 6.9 & Pilar 7.3).
 * Escala perceptual calibrada para que cada revisão espaçada [D0, 1d, 3d, 7d, 16d, 30d] exiba
 * nitidamente a queda exponencial R(t) = exp(-t / S) e o salto vertical a 100% em Zero-GC.
 */
@Composable
fun EbbinghausSawtoothCurveChart(
    activeStageIndex: Int = 2,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val sawtoothPath = remember { Path() }
    val sawtoothFill = remember { Path() }
    val unreviewedDecayPath = remember { Path() }

    Column(modifier = modifier) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(158.dp)
                .drawWithCache {
                    val padTop = 12.dp.toPx()
                    val padBottom = 10.dp.toPx()
                    val padHoriz = 8.dp.toPx()
                    val usableW = (size.width - padHoriz * 2f).coerceAtLeast(20f)
                    val usableH = (size.height - padTop - padBottom).coerceAtLeast(20f)

                    // Posições perceptuais equiespaçadas para D0, 1d, 3d, 7d, 16d e 30d
                    val checkpointsDays = floatArrayOf(0f, 1f, 3f, 7f, 16f, 30f)
                    val stageFractions = floatArrayOf(0.00f, 0.17f, 0.35f, 0.55f, 0.77f, 1.00f)
                    val stabilities = floatArrayOf(1.95f, 5.1f, 11.2f, 23.5f, 46.0f)

                    fun mapStagePt(stagePos0to1: Float, retention0to1: Float): Offset {
                        val x = padHoriz + stagePos0to1.coerceIn(0f, 1f) * usableW
                        val y = padTop + usableH * (1f - retention0to1.coerceIn(0f, 1f))
                        return Offset(x, y)
                    }

                    sawtoothPath.reset()
                    val pStart = mapStagePt(0f, 1f)
                    sawtoothPath.moveTo(pStart.x, pStart.y)

                    for (seg in 0 until checkpointsDays.lastIndex) {
                        val t0 = checkpointsDays[seg]
                        val t1 = checkpointsDays[seg + 1]
                        val x0 = stageFractions[seg]
                        val x1 = stageFractions[seg + 1]
                        val s = stabilities[seg]
                        val subSteps = 14
                        for (k in 1..subSteps) {
                            val u = k / subSteps.toFloat()
                            val dt = (t1 - t0) * u
                            val r = exp((-dt / s).toDouble()).toFloat().coerceIn(0.38f, 1f)
                            val pt = mapStagePt(x0 + (x1 - x0) * u, r)
                            sawtoothPath.lineTo(pt.x, pt.y)
                        }
                        // Salto vertical de restauração mnemônica a 100%
                        val restoredPt = mapStagePt(x1, 1f)
                        sawtoothPath.lineTo(restoredPt.x, restoredPt.y)
                    }

                    val endPt = mapStagePt(1f, 1f)
                    sawtoothFill.reset()
                    sawtoothFill.addPath(sawtoothPath)
                    sawtoothFill.lineTo(endPt.x, size.height - padBottom)
                    sawtoothFill.lineTo(pStart.x, size.height - padBottom)
                    sawtoothFill.close()

                    // Curva fantasma sem revisão (decaimento puro até ~20%)
                    unreviewedDecayPath.reset()
                    unreviewedDecayPath.moveTo(pStart.x, pStart.y)
                    for (step in 1..40) {
                        val u = step / 40f
                        val virtualDay = u * 30f
                        val r = (0.20f + 0.80f * exp((-virtualDay / 3.4f).toDouble()).toFloat())
                        val pt = mapStagePt(u, r)
                        unreviewedDecayPath.lineTo(pt.x, pt.y)
                    }

                    val fillBrush = Brush.verticalGradient(
                        colors = listOf(
                            LifeOsTheme.PurpleStudy.copy(alpha = 0.38f),
                            LifeOsTheme.CyanFinance.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                    val lineBrush = Brush.horizontalGradient(
                        colors = listOf(LifeOsTheme.PurpleStudy, Color(0xFFB388FF), LifeOsTheme.GreenHealth)
                    )
                    val mainStroke = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val decayDash = Stroke(
                        width = 1.6.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                    val guideDash = PathEffect.dashPathEffect(floatArrayOf(4f, 5f), 0f)
                    val peakOffsets = Array(stageFractions.size) { i -> mapStagePt(stageFractions[i], 1f) }
                    val gridPx = 1.dp.toPx()
                    val dotR = 4.5.dp.toPx()
                    val haloR = 8.5.dp.toPx()

                    onDrawBehind {
                        for (r in 0..3) {
                            val y = padTop + usableH * (r / 3f)
                            drawLine(
                                color = LifeOsTheme.BorderSubtle,
                                start = Offset(padHoriz, y),
                                end = Offset(size.width - padHoriz, y),
                                strokeWidth = gridPx
                            )
                        }

                        peakOffsets.forEach { pt ->
                            drawLine(
                                color = Color.White.copy(alpha = 0.08f),
                                start = pt,
                                end = Offset(pt.x, size.height - padBottom),
                                strokeWidth = gridPx,
                                pathEffect = guideDash
                            )
                        }

                        drawPath(
                            path = unreviewedDecayPath,
                            color = LifeOsTheme.RedExpense.copy(alpha = 0.58f),
                            style = decayDash
                        )
                        drawPath(path = sawtoothFill, brush = fillBrush)
                        drawPath(path = sawtoothPath, brush = lineBrush, style = mainStroke)

                        peakOffsets.forEachIndexed { idx, pt ->
                            val isReached = idx <= (activeStageIndex + 1)
                            val nodeColor = if (isReached) LifeOsTheme.GreenHealth else LifeOsTheme.PurpleStudy
                            drawCircle(color = nodeColor.copy(alpha = 0.24f), radius = haloR, center = pt)
                            drawCircle(color = nodeColor, radius = dotR, center = pt)
                        }
                    }
                }
        )

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("D0", "+1d", "+3d", "+7d", "+16d", "+30d · 100%").forEachIndexed { idx, lbl ->
                val reached = idx <= (activeStageIndex + 1)
                Text(
                    text = lbl,
                    color = if (reached) LifeOsTheme.GreenHealth else LifeOsTheme.TextMuted,
                    fontSize = 10.sp,
                    fontWeight = if (reached) FontWeight.Bold else FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Gráfico #10 da Suíte 120 FPS — Matriz de Correlação Multivariada da IA (Pilar 6.10 & Pilar 7.3).
 * Dispersão analítica cruzando variáveis comportamentais (ex: Sono/Treino vs. Gasto Impulsivo)
 * com reta de tendência linear pré-calculada em Zero-GC.
 */
@Composable
fun MultivariateCorrelationScatterChart(
    xyNormalizedPairs: List<Pair<Float, Float>>,
    positiveFlags: List<Boolean>,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Column(modifier = modifier) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .drawWithCache {
                    val pad = 14.dp.toPx()
                    val w = (size.width - pad * 2f).coerceAtLeast(20f)
                    val h = (size.height - pad * 2f).coerceAtLeast(20f)

                    val pts = Array(xyNormalizedPairs.size) { i ->
                        val (nx, ny) = xyNormalizedPairs[i]
                        Offset(
                            x = pad + nx.coerceIn(0f, 1f) * w,
                            y = pad + (1f - ny.coerceIn(0f, 1f)) * h
                        )
                    }

                    val n = xyNormalizedPairs.size.coerceAtLeast(1).toFloat()
                    val meanX = xyNormalizedPairs.sumOf { it.first.toDouble() }.toFloat() / n
                    val meanY = xyNormalizedPairs.sumOf { it.second.toDouble() }.toFloat() / n
                    var num = 0f
                    var den = 0f
                    xyNormalizedPairs.forEach { (x, y) ->
                        val dx = x - meanX
                        num += dx * (y - meanY)
                        den += dx * dx
                    }
                    val slope = if (den > 0.0001f) num / den else -0.7f
                    val intercept = meanY - slope * meanX

                    val yAt0 = (intercept).coerceIn(0.05f, 0.95f)
                    val yAt1 = (slope * 1f + intercept).coerceIn(0.05f, 0.95f)
                    val trendStart = Offset(pad, pad + (1f - yAt0) * h)
                    val trendEnd = Offset(pad + w, pad + (1f - yAt1) * h)

                    val dash = PathEffect.dashPathEffect(floatArrayOf(7f, 7f), 0f)
                    val gridPx = 1.dp.toPx()
                    val trendStroke = 2.2.dp.toPx()
                    val haloR = 8.5.dp.toPx()
                    val dotR = 4.2.dp.toPx()

                    onDrawBehind {
                        drawLine(
                            color = LifeOsTheme.BorderSubtle,
                            start = Offset(size.width / 2f, pad),
                            end = Offset(size.width / 2f, size.height - pad),
                            strokeWidth = gridPx
                        )
                        drawLine(
                            color = LifeOsTheme.BorderSubtle,
                            start = Offset(pad, size.height / 2f),
                            end = Offset(size.width - pad, size.height / 2f),
                            strokeWidth = gridPx
                        )

                        drawLine(
                            color = LifeOsTheme.AmberFocus.copy(alpha = 0.85f),
                            start = trendStart,
                            end = trendEnd,
                            strokeWidth = trendStroke,
                            pathEffect = dash
                        )

                        pts.forEachIndexed { idx, pt ->
                            val c = if (positiveFlags.getOrElse(idx) { true }) {
                                LifeOsTheme.GreenHealth
                            } else {
                                LifeOsTheme.RedExpense
                            }
                            drawCircle(color = c.copy(alpha = 0.22f), radius = haloR, center = pt)
                            drawCircle(color = c, radius = dotR, center = pt)
                        }
                    }
                }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("← Privação de Sono", color = LifeOsTheme.RedExpense, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text("Sono + Treino Consistente →", color = LifeOsTheme.GreenHealth, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Gráfico #11 da Suíte 120 FPS — Mapa Anatômico Esculpido de Fadiga Muscular (Pilar 6.11 & Pilar 7.3).
 * Silhueta anatômica orgânica Anterior e Posterior esculpida em curvas de Bézier cacheadas (Zero-GC)
 * com termografia miofascial: Vermelho (Fadiga Aguda) -> Âmbar (Síntese 48h) -> Verde (Supercompensado).
 */
@Composable
fun AnatomicalMuscleFatigueMap(
    chestRecovery: Float,
    backRecovery: Float,
    shouldersRecovery: Float,
    armsRecovery: Float,
    coreRecovery: Float,
    legsRecovery: Float,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    // Paths anatômicos esculpidos uma única vez em RAM e reutilizados em drawWithCache (Zero-GC)
    val pecLeftPath = remember { Path() }
    val pecRightPath = remember { Path() }
    val trapsFrontPath = remember { Path() }
    val trapsBackPath = remember { Path() }
    val latsBackPath = remember { Path() }
    val deltPath = remember { Path() }
    val armsFrontPath = remember { Path() }
    val armsBackPath = remember { Path() }
    val quadCalvesFrontPath = remember { Path() }
    val glutesHamsBackPath = remember { Path() }

    Column(modifier = modifier) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(212.dp)
                .drawWithCache {
                    fun recoveryColor(fraction: Float): Color {
                        val f = fraction.coerceIn(0f, 1f)
                        return if (f < 0.5f) {
                            lerp(LifeOsTheme.RedExpense, LifeOsTheme.AmberFocus, f * 2f)
                        } else {
                            lerp(LifeOsTheme.AmberFocus, LifeOsTheme.GreenHealth, (f - 0.5f) * 2f)
                        }
                    }

                    val cChest = recoveryColor(chestRecovery)
                    val cBack = recoveryColor(backRecovery)
                    val cShoulder = recoveryColor(shouldersRecovery)
                    val cArms = recoveryColor(armsRecovery)
                    val cCore = recoveryColor(coreRecovery)
                    val cLegs = recoveryColor(legsRecovery)

                    val fCx = size.width * 0.26f
                    val bCx = size.width * 0.74f
                    val u = size.height / 212f

                    // Cabeça e referências verticais proporcionais
                    val headCy = 22f * u
                    val headRx = 11f * u
                    val neckBottom = 38f * u
                    val chestBottom = 68f * u
                    val waistY = 104f * u
                    val crotchY = 114f * u
                    val kneeY = 156f * u
                    val ankleY = 196f * u

                    // 1. Trapézio Anterior
                    trapsFrontPath.reset()
                    trapsFrontPath.moveTo(fCx - 7f * u, neckBottom - 4f * u)
                    trapsFrontPath.lineTo(fCx - 24f * u, neckBottom + 4f * u)
                    trapsFrontPath.lineTo(fCx + 24f * u, neckBottom + 4f * u)
                    trapsFrontPath.lineTo(fCx + 7f * u, neckBottom - 4f * u)
                    trapsFrontPath.close()

                    // 2. Peitoral Maior Esculpido (Esquerdo & Direito)
                    pecLeftPath.reset()
                    pecLeftPath.moveTo(fCx - 2f * u, neckBottom + 4f * u)
                    pecLeftPath.lineTo(fCx - 23f * u, neckBottom + 5f * u)
                    pecLeftPath.cubicTo(fCx - 26f * u, chestBottom - 8f * u, fCx - 21f * u, chestBottom, fCx - 10f * u, chestBottom)
                    pecLeftPath.cubicTo(fCx - 4f * u, chestBottom, fCx - 2f * u, chestBottom - 4f * u, fCx - 2f * u, chestBottom - 7f * u)
                    pecLeftPath.close()

                    pecRightPath.reset()
                    pecRightPath.moveTo(fCx + 2f * u, neckBottom + 4f * u)
                    pecRightPath.lineTo(fCx + 23f * u, neckBottom + 5f * u)
                    pecRightPath.cubicTo(fCx + 26f * u, chestBottom - 8f * u, fCx + 21f * u, chestBottom, fCx + 10f * u, chestBottom)
                    pecRightPath.cubicTo(fCx + 4f * u, chestBottom, fCx + 2f * u, chestBottom - 4f * u, fCx + 2f * u, chestBottom - 7f * u)
                    pecRightPath.close()

                    // 3. Cápsulas de Deltoides (Anterior + Posterior)
                    deltPath.reset()
                    fun addDeltCap(cx: Float, sign: Float) {
                        deltPath.moveTo(cx + sign * 22f * u, neckBottom + 3f * u)
                        deltPath.cubicTo(
                            cx + sign * 35f * u, neckBottom + 5f * u,
                            cx + sign * 36f * u, neckBottom + 22f * u,
                            cx + sign * 26f * u, neckBottom + 26f * u
                        )
                        deltPath.cubicTo(
                            cx + sign * 22f * u, neckBottom + 20f * u,
                            cx + sign * 21f * u, neckBottom + 10f * u,
                            cx + sign * 22f * u, neckBottom + 3f * u
                        )
                        deltPath.close()
                    }
                    addDeltCap(fCx, -1f)
                    addDeltCap(fCx, 1f)
                    addDeltCap(bCx, -1f)
                    addDeltCap(bCx, 1f)

                    // 4. Bíceps + Antebraços Orgânicos (Anterior) e Tríceps + Antebraços (Posterior)
                    fun buildArmsForCenter(target: Path, cx: Float) {
                        target.reset()
                        for (sign in floatArrayOf(-1f, 1f)) {
                            // Bíceps / Tríceps fusiforme
                            target.moveTo(cx + sign * 26f * u, neckBottom + 26f * u)
                            target.cubicTo(
                                cx + sign * 36f * u, neckBottom + 34f * u,
                                cx + sign * 37f * u, neckBottom + 48f * u,
                                cx + sign * 31f * u, neckBottom + 56f * u
                            )
                            // Antebraço afunilando até o punho
                            target.cubicTo(
                                cx + sign * 36f * u, neckBottom + 64f * u,
                                cx + sign * 34f * u, neckBottom + 78f * u,
                                cx + sign * 31f * u, neckBottom + 86f * u
                            )
                            target.lineTo(cx + sign * 25f * u, neckBottom + 86f * u)
                            target.cubicTo(
                                cx + sign * 25f * u, neckBottom + 74f * u,
                                cx + sign * 23f * u, neckBottom + 62f * u,
                                cx + sign * 23f * u, neckBottom + 54f * u
                            )
                            target.cubicTo(
                                cx + sign * 21f * u, neckBottom + 44f * u,
                                cx + sign * 21f * u, neckBottom + 33f * u,
                                cx + sign * 26f * u, neckBottom + 26f * u
                            )
                            target.close()
                        }
                    }
                    buildArmsForCenter(armsFrontPath, fCx)
                    buildArmsForCenter(armsBackPath, bCx)

                    // 5. Quadríceps em Gota + Panturrilhas (Anterior)
                    quadCalvesFrontPath.reset()
                    for (sign in floatArrayOf(-1f, 1f)) {
                        // Quadríceps (Vasto Lateral externo curvo + Vasto Medial em gota)
                        quadCalvesFrontPath.moveTo(fCx + sign * 18f * u, waistY + 4f * u)
                        quadCalvesFrontPath.cubicTo(
                            fCx + sign * 26f * u, waistY + 20f * u,
                            fCx + sign * 24f * u, kneeY - 12f * u,
                            fCx + sign * 16f * u, kneeY
                        )
                        quadCalvesFrontPath.lineTo(fCx + sign * 6f * u, kneeY)
                        quadCalvesFrontPath.cubicTo(
                            fCx + sign * 3f * u, kneeY - 14f * u,
                            fCx + sign * 3f * u, crotchY + 8f * u,
                            fCx + sign * 4f * u, crotchY
                        )
                        quadCalvesFrontPath.lineTo(fCx + sign * 18f * u, waistY + 4f * u)
                        quadCalvesFrontPath.close()

                        // Gastrocnêmio / Tibial Anterior
                        quadCalvesFrontPath.moveTo(fCx + sign * 16f * u, kneeY + 3f * u)
                        quadCalvesFrontPath.cubicTo(
                            fCx + sign * 21f * u, kneeY + 15f * u,
                            fCx + sign * 18f * u, ankleY - 12f * u,
                            fCx + sign * 13f * u, ankleY
                        )
                        quadCalvesFrontPath.lineTo(fCx + sign * 7f * u, ankleY)
                        quadCalvesFrontPath.cubicTo(
                            fCx + sign * 5f * u, ankleY - 14f * u,
                            fCx + sign * 4f * u, kneeY + 14f * u,
                            fCx + sign * 7f * u, kneeY + 3f * u
                        )
                        quadCalvesFrontPath.close()
                    }

                    // 6. Trapézio Diamante + Grande Dorsal em V (Posterior)
                    trapsBackPath.reset()
                    trapsBackPath.moveTo(bCx, neckBottom - 5f * u)
                    trapsBackPath.lineTo(bCx - 22f * u, neckBottom + 5f * u)
                    trapsBackPath.lineTo(bCx, chestBottom + 4f * u)
                    trapsBackPath.lineTo(bCx + 22f * u, neckBottom + 5f * u)
                    trapsBackPath.close()

                    latsBackPath.reset()
                    latsBackPath.moveTo(bCx - 23f * u, neckBottom + 7f * u)
                    latsBackPath.lineTo(bCx - 2f * u, chestBottom + 4f * u)
                    latsBackPath.lineTo(bCx - 2f * u, waistY - 4f * u)
                    latsBackPath.lineTo(bCx - 14f * u, waistY - 4f * u)
                    latsBackPath.cubicTo(
                        bCx - 20f * u, chestBottom + 16f * u,
                        bCx - 26f * u, chestBottom - 2f * u,
                        bCx - 23f * u, neckBottom + 7f * u
                    )
                    latsBackPath.close()

                    latsBackPath.moveTo(bCx + 23f * u, neckBottom + 7f * u)
                    latsBackPath.lineTo(bCx + 2f * u, chestBottom + 4f * u)
                    latsBackPath.lineTo(bCx + 2f * u, waistY - 4f * u)
                    latsBackPath.lineTo(bCx + 14f * u, waistY - 4f * u)
                    latsBackPath.cubicTo(
                        bCx + 20f * u, chestBottom + 16f * u,
                        bCx + 26f * u, chestBottom - 2f * u,
                        bCx + 23f * u, neckBottom + 7f * u
                    )
                    latsBackPath.close()

                    // 7. Glúteos + Isquiotibiais + Panturrilhas em Diamante (Posterior)
                    glutesHamsBackPath.reset()
                    for (sign in floatArrayOf(-1f, 1f)) {
                        // Glúteo + Isquiotibiais
                        glutesHamsBackPath.moveTo(bCx + sign * 15f * u, waistY + 2f * u)
                        glutesHamsBackPath.cubicTo(
                            bCx + sign * 25f * u, waistY + 14f * u,
                            bCx + sign * 24f * u, kneeY - 14f * u,
                            bCx + sign * 16f * u, kneeY
                        )
                        glutesHamsBackPath.lineTo(bCx + sign * 6f * u, kneeY)
                        glutesHamsBackPath.cubicTo(
                            bCx + sign * 3f * u, kneeY - 14f * u,
                            bCx + sign * 2f * u, crotchY + 4f * u,
                            bCx + sign * 2f * u, waistY + 2f * u
                        )
                        glutesHamsBackPath.close()

                        // Gastrocnêmio Posterior
                        glutesHamsBackPath.moveTo(bCx + sign * 16f * u, kneeY + 3f * u)
                        glutesHamsBackPath.cubicTo(
                            bCx + sign * 22f * u, kneeY + 15f * u,
                            bCx + sign * 18f * u, ankleY - 12f * u,
                            bCx + sign * 13f * u, ankleY
                        )
                        glutesHamsBackPath.lineTo(bCx + sign * 7f * u, ankleY)
                        glutesHamsBackPath.cubicTo(
                            bCx + sign * 4f * u, ankleY - 14f * u,
                            bCx + sign * 4f * u, kneeY + 14f * u,
                            bCx + sign * 7f * u, kneeY + 3f * u
                        )
                        glutesHamsBackPath.close()
                    }

                    val outlineStroke = Stroke(width = 1.1.dp.toPx(), join = StrokeJoin.Round)
                    val abCorner = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    val gridDash = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
                    val podiumFront = Brush.radialGradient(
                        colors = listOf(cChest.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(fCx, size.height * 0.52f),
                        radius = 86.dp.toPx()
                    )
                    val podiumBack = Brush.radialGradient(
                        colors = listOf(cBack.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(bCx, size.height * 0.52f),
                        radius = 86.dp.toPx()
                    )

                    onDrawBehind {
                        // Auras de scanner biométrico de fundo
                        drawCircle(brush = podiumFront, radius = 86.dp.toPx(), center = Offset(fCx, size.height * 0.52f))
                        drawCircle(brush = podiumBack, radius = 86.dp.toPx(), center = Offset(bCx, size.height * 0.52f))

                        // Divisor central de telemetria
                        drawLine(
                            color = LifeOsTheme.BorderSubtle,
                            start = Offset(size.width / 2f, 10f * u),
                            end = Offset(size.width / 2f, ankleY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = gridDash
                        )

                        // Cabeça + Pescoço (Anterior e Posterior)
                        for (cx in floatArrayOf(fCx, bCx)) {
                            drawRoundRect(
                                color = Color(0xFF1F2330),
                                topLeft = Offset(cx - 5.5f * u, headCy + headRx - 2f * u),
                                size = Size(11f * u, 9f * u),
                                cornerRadius = abCorner
                            )
                            drawCircle(color = Color(0xFF1B1F2B), radius = headRx, center = Offset(cx, headCy))
                            drawCircle(color = LifeOsTheme.BorderSpecular, radius = headRx, center = Offset(cx, headCy), style = outlineStroke)
                        }

                        // Trapézios e Deltoides
                        drawPath(path = trapsFrontPath, color = cShoulder.copy(alpha = 0.72f))
                        drawPath(path = deltPath, color = cShoulder.copy(alpha = 0.88f))
                        drawPath(path = deltPath, color = Color.White.copy(alpha = 0.22f), style = outlineStroke)

                        // Peitorais Esculpidos
                        drawPath(path = pecLeftPath, color = cChest.copy(alpha = 0.88f))
                        drawPath(path = pecRightPath, color = cChest.copy(alpha = 0.88f))
                        drawPath(path = pecLeftPath, color = Color.White.copy(alpha = 0.24f), style = outlineStroke)
                        drawPath(path = pecRightPath, color = Color.White.copy(alpha = 0.24f), style = outlineStroke)

                        // Reto Abdominal Segmentado (6-pack anatômico)
                        val abW = 10.5f * u
                        val abH = 9.2f * u
                        for (row in 0..2) {
                            val ay = chestBottom + 3f * u + row * (abH + 2.4f * u)
                            drawRoundRect(
                                color = cCore.copy(alpha = 0.86f),
                                topLeft = Offset(fCx - abW - 1.4f * u, ay),
                                size = Size(abW, abH),
                                cornerRadius = abCorner
                            )
                            drawRoundRect(
                                color = cCore.copy(alpha = 0.86f),
                                topLeft = Offset(fCx + 1.4f * u, ay),
                                size = Size(abW, abH),
                                cornerRadius = abCorner
                            )
                        }

                        // Dorsal em V + Trapézio Diamante + Lombar (Posterior)
                        drawPath(path = latsBackPath, color = cBack.copy(alpha = 0.88f))
                        drawPath(path = latsBackPath, color = Color.White.copy(alpha = 0.22f), style = outlineStroke)
                        drawPath(path = trapsBackPath, color = cBack.copy(alpha = 0.96f))
                        drawPath(path = trapsBackPath, color = Color.White.copy(alpha = 0.26f), style = outlineStroke)

                        drawRoundRect(
                            color = cCore.copy(alpha = 0.78f),
                            topLeft = Offset(bCx - 12f * u, waistY - 4f * u),
                            size = Size(24f * u, 9f * u),
                            cornerRadius = abCorner
                        )

                        // Braços e Pernas (Anterior + Posterior)
                        drawPath(path = armsFrontPath, color = cArms.copy(alpha = 0.86f))
                        drawPath(path = armsFrontPath, color = Color.White.copy(alpha = 0.20f), style = outlineStroke)
                        drawPath(path = armsBackPath, color = cArms.copy(alpha = 0.86f))
                        drawPath(path = armsBackPath, color = Color.White.copy(alpha = 0.20f), style = outlineStroke)

                        drawPath(path = quadCalvesFrontPath, color = cLegs.copy(alpha = 0.86f))
                        drawPath(path = quadCalvesFrontPath, color = Color.White.copy(alpha = 0.20f), style = outlineStroke)
                        drawPath(path = glutesHamsBackPath, color = cLegs.copy(alpha = 0.86f))
                        drawPath(path = glutesHamsBackPath, color = Color.White.copy(alpha = 0.20f), style = outlineStroke)
                    }
                }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Text("ANTERIOR · PUSH & CORE", color = LifeOsTheme.TextPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
            Text("POSTERIOR · PULL & CADEIA", color = LifeOsTheme.TextPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RecoveryScalePill("Fadiga Aguda", LifeOsTheme.RedExpense)
            RecoveryScalePill("Síntese 48h", LifeOsTheme.AmberFocus)
            RecoveryScalePill("100% Pronto", LifeOsTheme.GreenHealth)
        }
    }
}

@Composable
private fun RecoveryScalePill(label: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.28f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            color = color,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Gráfico #12 da Suíte 120 FPS — Chrono Wheel de 24 Horas / Rosa de Nightingale (Pilar 6.12 & Pilar 7.3).
 * Mostrador circular de relógio de 24h em pétalas polares coloridas com marcador de Caffeine Cutoff (14:00)
 * e ponteiro da hora biológica atual em Zero-GC.
 */
@Composable
fun ChronoNightingaleWheel24h(
    hourlyIntensities24: List<Float>,
    hourlyHexColors24: List<String>,
    currentHour24: Int,
    caffeineCutoffHour: Int = 14,
    modifier: Modifier = Modifier.size(214.dp)
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Spacer(
            modifier = Modifier
                .size(194.dp)
                .drawWithCache {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxR = size.minDimension / 2f - 6.dp.toPx()
                    val innerR = maxR * 0.30f
                    val sweepPerHour = (360f / 24f) - 2.2f

                    val petalColors = Array(24) { h ->
                        LifeOsTheme.parseHexColorOrDefault(
                            hourlyHexColors24.getOrElse(h) { "#8E54E9" }
                        )
                    }
                    val petalRadii = FloatArray(24) { h ->
                        val frac = hourlyIntensities24.getOrElse(h) { 0.5f }.coerceIn(0.22f, 1f)
                        innerR + (maxR - innerR) * frac
                    }

                    val nowAngleRad = Math.toRadians((-90f + currentHour24 * 15f + 7.5f).toDouble())
                    val nowEnd = Offset(
                        x = center.x + (cos(nowAngleRad) * (maxR + 4.dp.toPx())).toFloat(),
                        y = center.y + (sin(nowAngleRad) * (maxR + 4.dp.toPx())).toFloat()
                    )

                    val cutoffAngleRad = Math.toRadians((-90f + caffeineCutoffHour * 15f).toDouble())
                    val cutoffEnd = Offset(
                        x = center.x + (cos(cutoffAngleRad) * (maxR + 4.dp.toPx())).toFloat(),
                        y = center.y + (sin(cutoffAngleRad) * (maxR + 4.dp.toPx())).toFloat()
                    )

                    val dash = PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f)
                    val ringStroke = Stroke(width = 1.dp.toPx())
                    val handStroke = 2.4.dp.toPx()

                    onDrawBehind {
                        drawCircle(
                            color = LifeOsTheme.BorderSubtle,
                            radius = maxR,
                            center = center,
                            style = ringStroke
                        )

                        for (h in 0 until 24) {
                            val startAngle = -90f + (h * 15f)
                            val r = petalRadii[h]
                            val c = petalColors[h]
                            val isNow = h == currentHour24
                            drawArc(
                                color = c.copy(alpha = if (isNow) 0.96f else 0.58f),
                                startAngle = startAngle,
                                sweepAngle = sweepPerHour,
                                useCenter = true,
                                topLeft = Offset(center.x - r, center.y - r),
                                size = Size(r * 2f, r * 2f)
                            )
                        }

                        drawLine(
                            color = LifeOsTheme.RedExpense,
                            start = center,
                            end = cutoffEnd,
                            strokeWidth = 1.8.dp.toPx(),
                            pathEffect = dash
                        )

                        drawLine(
                            color = Color.White,
                            start = center,
                            end = nowEnd,
                            strokeWidth = handStroke,
                            cap = StrokeCap.Round
                        )

                        drawCircle(color = Color(0xFF07080C), radius = innerR, center = center)
                        drawCircle(color = LifeOsTheme.BorderSpecular, radius = innerR, center = center, style = ringStroke)
                    }
                }
        )

        Box(modifier = Modifier.size(214.dp)) {
            Text("00h", color = LifeOsTheme.TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopCenter))
            Text("12h", color = LifeOsTheme.TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter))
            Text("18h", color = LifeOsTheme.GreenHealth, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterStart))
            Text("06h", color = LifeOsTheme.CyanFinance, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterEnd))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%02dh", currentHour24),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
            Text("BIO", color = LifeOsTheme.AmberFocus, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        }
    }
}

/**
 * Gráfico #13 da Suíte 120 FPS — Burndown de Conhecimento com "Cone de Incerteza" (Pilar 6.13 & Pilar 7.3).
 */
@Composable
fun KnowledgeBurndownConeChart(
    historicalRemaining: List<Float>,
    optimisticCone: List<Float>,
    realisticCone: List<Float>,
    conservativeCone: List<Float>,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val historyPath = remember { Path() }
    val conePolygonPath = remember { Path() }
    val realisticPath = remember { Path() }
    val optimisticPath = remember { Path() }
    val conservativePath = remember { Path() }

    Column(modifier = modifier) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp)
                .drawWithCache {
                    val histCount = historicalRemaining.size.coerceAtLeast(2)
                    val futCount = realisticCone.size.coerceAtLeast(2)
                    val totalSegments = (histCount - 1) + (futCount - 1)
                    val maxUnits = maxOf(
                        historicalRemaining.maxOrNull() ?: 100f,
                        conservativeCone.maxOrNull() ?: 100f
                    ).coerceAtLeast(10f)

                    val padTop = 10.dp.toPx()
                    val padBottom = 10.dp.toPx()
                    val usableH = (size.height - padTop - padBottom).coerceAtLeast(20f)

                    fun ptAt(segIndex: Int, value: Float): Offset {
                        val x = (segIndex.toFloat() / totalSegments.coerceAtLeast(1).toFloat()) * size.width
                        val y = padTop + usableH * (1f - (value / maxUnits).coerceIn(0f, 1f))
                        return Offset(x, y)
                    }

                    historyPath.reset()
                    historicalRemaining.forEachIndexed { i, v ->
                        val p = ptAt(i, v)
                        if (i == 0) historyPath.moveTo(p.x, p.y) else historyPath.lineTo(p.x, p.y)
                    }

                    val pivotSeg = histCount - 1
                    val pivotPoint = ptAt(pivotSeg, historicalRemaining.lastOrNull() ?: 50f)

                    optimisticPath.reset()
                    realisticPath.reset()
                    conservativePath.reset()
                    conePolygonPath.reset()

                    optimisticCone.forEachIndexed { j, v ->
                        val p = ptAt(pivotSeg + j, v)
                        if (j == 0) {
                            optimisticPath.moveTo(p.x, p.y)
                            conePolygonPath.moveTo(p.x, p.y)
                        } else {
                            optimisticPath.lineTo(p.x, p.y)
                            conePolygonPath.lineTo(p.x, p.y)
                        }
                    }

                    for (j in conservativeCone.indices.reversed()) {
                        val p = ptAt(pivotSeg + j, conservativeCone[j])
                        conePolygonPath.lineTo(p.x, p.y)
                    }
                    conePolygonPath.close()

                    realisticCone.forEachIndexed { j, v ->
                        val p = ptAt(pivotSeg + j, v)
                        if (j == 0) realisticPath.moveTo(p.x, p.y) else realisticPath.lineTo(p.x, p.y)
                    }
                    conservativeCone.forEachIndexed { j, v ->
                        val p = ptAt(pivotSeg + j, v)
                        if (j == 0) conservativePath.moveTo(p.x, p.y) else conservativePath.lineTo(p.x, p.y)
                    }

                    val coneBrush = Brush.horizontalGradient(
                        colors = listOf(
                            LifeOsTheme.CyanFinance.copy(alpha = 0.28f),
                            LifeOsTheme.PurpleStudy.copy(alpha = 0.14f)
                        )
                    )
                    val solidStroke = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                    val dashStroke = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
                    )
                    val boundStroke = Stroke(width = 1.2.dp.toPx())
                    val gridPx = 1.dp.toPx()

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

                        drawPath(path = conePolygonPath, brush = coneBrush)
                        drawPath(path = optimisticPath, color = LifeOsTheme.GreenHealth.copy(alpha = 0.78f), style = boundStroke)
                        drawPath(path = conservativePath, color = LifeOsTheme.AmberFocus.copy(alpha = 0.78f), style = boundStroke)
                        drawPath(path = realisticPath, color = LifeOsTheme.CyanFinance, style = dashStroke)
                        drawPath(path = historyPath, color = Color.White, style = solidStroke)

                        drawCircle(color = LifeOsTheme.CyanFinance.copy(alpha = 0.25f), radius = 9.dp.toPx(), center = pivotPoint)
                        drawCircle(color = LifeOsTheme.CyanFinance, radius = 4.8.dp.toPx(), center = pivotPoint)
                    }
                }
        )

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Início Edital", color = LifeOsTheme.TextMuted, fontSize = 10.sp)
            Text("● Hoje", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("Cone p10 · p50 · p90 →", color = LifeOsTheme.CyanFinance, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Gráfico #14 da Suíte 120 FPS — Radar Hexagonal de Atributos RPG (Pilar 6.14 & Pilar 7.3).
 * Teia de 6 vértices (Força, Intelecto, Finanças, Resiliência, Foco e Cultura) com Paths
 * cacheados em Zero-GC.
 */
@Composable
fun HexagonalRpgRadarChart(
    normalizedAttributes6: List<Float>,
    attributeScores0to100: List<Int>,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val gridWebPath = remember { Path() }
    val avatarPolyPath = remember { Path() }
    val labels = listOf("FORÇA", "INTELECTO", "FINANÇAS", "RESILIÊNCIA", "FOCO", "CULTURA")

    Box(modifier = modifier.height(232.dp), contentAlignment = Alignment.Center) {
        Spacer(
            modifier = Modifier
                .size(184.dp)
                .drawWithCache {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxR = size.minDimension / 2f - 8.dp.toPx()

                    fun hexVertex(vertexIdx: Int, radiusFraction: Float): Offset {
                        val angleRad = Math.toRadians((-90.0 + vertexIdx * 60.0))
                        val r = maxR * radiusFraction
                        return Offset(
                            x = center.x + (cos(angleRad) * r).toFloat(),
                            y = center.y + (sin(angleRad) * r).toFloat()
                        )
                    }

                    gridWebPath.reset()
                    for (ring in 1..3) {
                        val frac = ring / 3f
                        for (v in 0 until 6) {
                            val pt = hexVertex(v, frac)
                            if (v == 0) gridWebPath.moveTo(pt.x, pt.y) else gridWebPath.lineTo(pt.x, pt.y)
                        }
                        gridWebPath.close()
                    }
                    for (v in 0 until 6) {
                        val outer = hexVertex(v, 1f)
                        gridWebPath.moveTo(center.x, center.y)
                        gridWebPath.lineTo(outer.x, outer.y)
                    }

                    val activePts = Array(6) { v ->
                        val frac = normalizedAttributes6.getOrElse(v) { 0.65f }.coerceIn(0.18f, 1f)
                        hexVertex(v, frac)
                    }

                    avatarPolyPath.reset()
                    activePts.forEachIndexed { v, pt ->
                        if (v == 0) avatarPolyPath.moveTo(pt.x, pt.y) else avatarPolyPath.lineTo(pt.x, pt.y)
                    }
                    avatarPolyPath.close()

                    val polyFill = Brush.radialGradient(
                        colors = listOf(
                            LifeOsTheme.PurpleStudy.copy(alpha = 0.48f),
                            LifeOsTheme.GreenHealth.copy(alpha = 0.28f)
                        ),
                        center = center,
                        radius = maxR
                    )
                    val webStroke = Stroke(width = 1.dp.toPx())
                    val polyStroke = Stroke(width = 2.6.dp.toPx(), join = StrokeJoin.Round)
                    val dotR = 4.2.dp.toPx()

                    onDrawBehind {
                        drawPath(path = gridWebPath, color = LifeOsTheme.BorderSpecular, style = webStroke)
                        drawPath(path = avatarPolyPath, brush = polyFill)
                        drawPath(path = avatarPolyPath, color = LifeOsTheme.GreenHealth, style = polyStroke)

                        activePts.forEach { pt ->
                            drawCircle(color = LifeOsTheme.GreenHealth.copy(alpha = 0.30f), radius = 7.5.dp.toPx(), center = pt)
                            drawCircle(color = Color.White, radius = dotR, center = pt)
                        }
                    }
                }
        )

        Box(modifier = Modifier.fillMaxWidth().height(232.dp)) {
            val s = attributeScores0to100
            LuxuryStatBadge(
                label = "${labels[0]} ${s.getOrElse(0) { 80 }}",
                accent = LifeOsTheme.GreenHealth,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            LuxuryStatBadge(
                label = "${labels[3]} ${s.getOrElse(3) { 85 }}",
                accent = LifeOsTheme.OrangeStreak,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp),
                verticalArrangement = Arrangement.spacedBy(78.dp),
                horizontalAlignment = Alignment.End
            ) {
                LuxuryStatBadge("${labels[1]} ${s.getOrElse(1) { 84 }}", LifeOsTheme.PurpleStudy)
                LuxuryStatBadge("${labels[2]} ${s.getOrElse(2) { 78 }}", LifeOsTheme.CyanFinance)
            }
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 2.dp),
                verticalArrangement = Arrangement.spacedBy(78.dp),
                horizontalAlignment = Alignment.Start
            ) {
                LuxuryStatBadge("${labels[5]} ${s.getOrElse(5) { 76 }}", LifeOsTheme.AmberFocus)
                LuxuryStatBadge("${labels[4]} ${s.getOrElse(4) { 82 }}", Color.White)
            }
        }
    }
}
