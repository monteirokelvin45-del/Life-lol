package com.lifeos.ui.components

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
 * Desenho exato do decaimento exponencial R(t) = exp(-t / S) com saltos verticais de restauração
 * a 100% nas revisões espaçadas [Dia 1, 3, 7, 16, 30] usando Paths cacheados (Zero-GC).
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
                .height(152.dp)
                .drawWithCache {
                    val padTop = 12.dp.toPx()
                    val padBottom = 10.dp.toPx()
                    val usableH = (size.height - padTop - padBottom).coerceAtLeast(20f)
                    val totalDays = 30f

                    fun mapPt(day: Float, retention0to1: Float): Offset {
                        val x = (day / totalDays).coerceIn(0f, 1f) * size.width
                        val y = padTop + usableH * (1f - retention0to1.coerceIn(0f, 1f))
                        return Offset(x, y)
                    }

                    // Marcos de revisão espaçada [0 -> 1 -> 3 -> 7 -> 16 -> 30] e constantes S crescentes
                    val checkpoints = floatArrayOf(0f, 1f, 3f, 7f, 16f, 30f)
                    val stabilities = floatArrayOf(1.9f, 5.2f, 11.5f, 24.0f, 48.0f)

                    sawtoothPath.reset()
                    val pStart = mapPt(0f, 1f)
                    sawtoothPath.moveTo(pStart.x, pStart.y)

                    for (seg in 0 until checkpoints.lastIndex) {
                        val t0 = checkpoints[seg]
                        val t1 = checkpoints[seg + 1]
                        val s = stabilities[seg]
                        val subSteps = 12
                        for (k in 1..subSteps) {
                            val dt = (t1 - t0) * (k / subSteps.toFloat())
                            val r = exp((-dt / s).toDouble()).toFloat().coerceIn(0.32f, 1f)
                            val pt = mapPt(t0 + dt, r)
                            sawtoothPath.lineTo(pt.x, pt.y)
                        }
                        // Salto vertical de restauração a 100% na revisão espaçada
                        val restoredPt = mapPt(t1, 1f)
                        sawtoothPath.lineTo(restoredPt.x, restoredPt.y)
                    }

                    sawtoothFill.reset()
                    sawtoothFill.addPath(sawtoothPath)
                    sawtoothFill.lineTo(size.width, size.height - padBottom)
                    sawtoothFill.lineTo(0f, size.height - padBottom)
                    sawtoothFill.close()

                    // Curva fantasma sem revisão (esquecimento puro)
                    unreviewedDecayPath.reset()
                    unreviewedDecayPath.moveTo(pStart.x, pStart.y)
                    for (step in 1..30) {
                        val d = step.toFloat()
                        val r = (0.18f + 0.82f * exp((-d / 2.2f).toDouble()).toFloat())
                        val pt = mapPt(d, r)
                        unreviewedDecayPath.lineTo(pt.x, pt.y)
                    }

                    val fillBrush = Brush.verticalGradient(
                        colors = listOf(LifeOsTheme.PurpleStudy.copy(alpha = 0.34f), Color.Transparent)
                    )
                    val mainStroke = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val decayDash = Stroke(
                        width = 1.6.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                    val peakOffsets = Array(checkpoints.size) { i -> mapPt(checkpoints[i], 1f) }
                    val gridPx = 1.dp.toPx()
                    val dotR = 4.2.dp.toPx()

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

                        drawPath(
                            path = unreviewedDecayPath,
                            color = LifeOsTheme.RedExpense.copy(alpha = 0.55f),
                            style = decayDash
                        )
                        drawPath(path = sawtoothFill, brush = fillBrush)
                        drawPath(path = sawtoothPath, color = LifeOsTheme.PurpleStudy, style = mainStroke)

                        peakOffsets.forEachIndexed { idx, pt ->
                            val isReached = idx <= (activeStageIndex + 1)
                            drawCircle(
                                color = if (isReached) LifeOsTheme.GreenHealth else LifeOsTheme.PurpleStudy,
                                radius = dotR,
                                center = pt
                            )
                        }
                    }
                }
        )

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("D0", "1d", "3d", "7d", "16d", "30d (100%)").forEach { lbl ->
                Text(lbl, color = LifeOsTheme.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
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

                    // Regressão linear sobre os pontos normalizados
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
                        // Quadrantes analíticos
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

                        // Reta de correlação multivariada
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
                            drawCircle(color = c.copy(alpha = 0.20f), radius = haloR, center = pt)
                            drawCircle(color = c, radius = dotR, center = pt)
                        }
                    }
                }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("← Menos Sono / Sem Treino", color = LifeOsTheme.RedExpense, fontSize = 10.sp)
            Text("Sono Reparador + Treino →", color = LifeOsTheme.GreenHealth, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Gráfico #11 da Suíte 120 FPS — Mapa Anatômico de Fadiga Muscular (Pilar 6.11 & Pilar 7.3).
 * Silhueta vetorial Anterior e Posterior renderizada em Zero-GC com termografia fisiológica:
 * Vermelho (Fadiga Aguda) -> Âmbar (Recuperação 48h) -> Verde (Supercompensação 100%).
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
    val latsPath = remember { Path() }

    Column(modifier = modifier) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(186.dp)
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

                    val frontCx = size.width * 0.27f
                    val backCx = size.width * 0.73f
                    val topY = 12.dp.toPx()
                    val headR = 11.dp.toPx()
                    val torsoTop = topY + headR * 2f + 6.dp.toPx()
                    val torsoW = 42.dp.toPx()
                    val chestH = 26.dp.toPx()
                    val coreH = 34.dp.toPx()
                    val shoulderR = 9.dp.toPx()
                    val armW = 10.dp.toPx()
                    val armH = 48.dp.toPx()
                    val legW = 15.dp.toPx()
                    val legH = 56.dp.toPx()
                    val pillCorner = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    val silhouetteStroke = Stroke(width = 1.2.dp.toPx())

                    // Trapézio / Grande Dorsal em V na Vista Posterior (Path cacheado)
                    latsPath.reset()
                    latsPath.moveTo(backCx - torsoW * 0.55f, torsoTop)
                    latsPath.lineTo(backCx + torsoW * 0.55f, torsoTop)
                    latsPath.lineTo(backCx + torsoW * 0.34f, torsoTop + chestH + coreH * 0.68f)
                    latsPath.lineTo(backCx - torsoW * 0.34f, torsoTop + chestH + coreH * 0.68f)
                    latsPath.close()

                    onDrawBehind {
                        // ================= VISTA ANTERIOR =================
                        drawCircle(color = Color(0xFF23252C), radius = headR, center = Offset(frontCx, topY + headR))
                        drawCircle(color = LifeOsTheme.TextMuted, radius = headR, center = Offset(frontCx, topY + headR), style = silhouetteStroke)

                        // Deltoides Anterior
                        drawCircle(color = cShoulder, radius = shoulderR, center = Offset(frontCx - torsoW * 0.62f, torsoTop + 6.dp.toPx()))
                        drawCircle(color = cShoulder, radius = shoulderR, center = Offset(frontCx + torsoW * 0.62f, torsoTop + 6.dp.toPx()))

                        // Peitoral Bilateral
                        drawRoundRect(
                            color = cChest,
                            topLeft = Offset(frontCx - torsoW / 2f, torsoTop),
                            size = Size(torsoW / 2f - 1.5.dp.toPx(), chestH),
                            cornerRadius = pillCorner
                        )
                        drawRoundRect(
                            color = cChest,
                            topLeft = Offset(frontCx + 1.5.dp.toPx(), torsoTop),
                            size = Size(torsoW / 2f - 1.5.dp.toPx(), chestH),
                            cornerRadius = pillCorner
                        )

                        // Core / Reto Abdominal
                        drawRoundRect(
                            color = cCore,
                            topLeft = Offset(frontCx - torsoW * 0.38f, torsoTop + chestH + 3.dp.toPx()),
                            size = Size(torsoW * 0.76f, coreH),
                            cornerRadius = pillCorner
                        )

                        // Bíceps / Antebraços
                        drawRoundRect(
                            color = cArms,
                            topLeft = Offset(frontCx - torsoW * 0.62f - armW / 2f, torsoTop + shoulderR + 4.dp.toPx()),
                            size = Size(armW, armH),
                            cornerRadius = pillCorner
                        )
                        drawRoundRect(
                            color = cArms,
                            topLeft = Offset(frontCx + torsoW * 0.62f - armW / 2f, torsoTop + shoulderR + 4.dp.toPx()),
                            size = Size(armW, armH),
                            cornerRadius = pillCorner
                        )

                        // Quadríceps
                        val legsTop = torsoTop + chestH + coreH + 7.dp.toPx()
                        drawRoundRect(
                            color = cLegs,
                            topLeft = Offset(frontCx - legW - 3.dp.toPx(), legsTop),
                            size = Size(legW, legH),
                            cornerRadius = pillCorner
                        )
                        drawRoundRect(
                            color = cLegs,
                            topLeft = Offset(frontCx + 3.dp.toPx(), legsTop),
                            size = Size(legW, legH),
                            cornerRadius = pillCorner
                        )

                        // ================= VISTA POSTERIOR =================
                        drawCircle(color = Color(0xFF23252C), radius = headR, center = Offset(backCx, topY + headR))
                        drawCircle(color = LifeOsTheme.TextMuted, radius = headR, center = Offset(backCx, topY + headR), style = silhouetteStroke)

                        // Deltoides Posterior
                        drawCircle(color = cShoulder, radius = shoulderR, center = Offset(backCx - torsoW * 0.62f, torsoTop + 6.dp.toPx()))
                        drawCircle(color = cShoulder, radius = shoulderR, center = Offset(backCx + torsoW * 0.62f, torsoTop + 6.dp.toPx()))

                        // Dorsal em V (Barra Fixa / Remada)
                        drawPath(path = latsPath, color = cBack)

                        // Lombar / Core Posterior
                        drawRoundRect(
                            color = cCore.copy(alpha = 0.85f),
                            topLeft = Offset(backCx - torsoW * 0.30f, torsoTop + chestH + coreH * 0.72f),
                            size = Size(torsoW * 0.60f, coreH * 0.32f),
                            cornerRadius = pillCorner
                        )

                        // Tríceps
                        drawRoundRect(
                            color = cArms,
                            topLeft = Offset(backCx - torsoW * 0.62f - armW / 2f, torsoTop + shoulderR + 4.dp.toPx()),
                            size = Size(armW, armH),
                            cornerRadius = pillCorner
                        )
                        drawRoundRect(
                            color = cArms,
                            topLeft = Offset(backCx + torsoW * 0.62f - armW / 2f, torsoTop + shoulderR + 4.dp.toPx()),
                            size = Size(armW, armH),
                            cornerRadius = pillCorner
                        )

                        // Isquiotibiais / Posterior de Coxa
                        drawRoundRect(
                            color = cLegs,
                            topLeft = Offset(backCx - legW - 3.dp.toPx(), legsTop),
                            size = Size(legW, legH),
                            cornerRadius = pillCorner
                        )
                        drawRoundRect(
                            color = cLegs,
                            topLeft = Offset(backCx + 3.dp.toPx(), legsTop),
                            size = Size(legW, legH),
                            cornerRadius = pillCorner
                        )
                    }
                }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Text("ANTERIOR (Push / Core)", color = LifeOsTheme.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("POSTERIOR (Pull / Dorsal)", color = LifeOsTheme.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
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
    modifier: Modifier = Modifier.size(210.dp)
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Spacer(
            modifier = Modifier
                .size(192.dp)
                .drawWithCache {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxR = size.minDimension / 2f - 6.dp.toPx()
                    val innerR = maxR * 0.28f
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

                    // Ponteiro da hora atual e linha de Corte de Cafeína (14:00)
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
                                color = c.copy(alpha = if (isNow) 0.95f else 0.56f),
                                startAngle = startAngle,
                                sweepAngle = sweepPerHour,
                                useCenter = true,
                                topLeft = Offset(center.x - r, center.y - r),
                                size = Size(r * 2f, r * 2f)
                            )
                        }

                        // Linha vermelha tracejada do Corte Exato de Cafeína (14:00)
                        drawLine(
                            color = LifeOsTheme.RedExpense,
                            start = center,
                            end = cutoffEnd,
                            strokeWidth = 1.8.dp.toPx(),
                            pathEffect = dash
                        )

                        // Ponteiro branco da Hora Atual
                        drawLine(
                            color = Color.White,
                            start = center,
                            end = nowEnd,
                            strokeWidth = handStroke,
                            cap = StrokeCap.Round
                        )

                        drawCircle(color = Color(0xFF060608), radius = innerR, center = center)
                        drawCircle(color = LifeOsTheme.BorderSubtle, radius = innerR, center = center, style = ringStroke)
                    }
                }
        )

        Box(modifier = Modifier.size(210.dp)) {
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
            Text("BIO", color = LifeOsTheme.AmberFocus, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Gráfico #13 da Suíte 120 FPS — Burndown de Conhecimento com "Cone de Incerteza" (Pilar 6.13 & Pilar 7.3).
 * Exibe a queima real de páginas/matérias e o cone sombreado matemático (otimista p10, realista p50 e
 * conservador p90) usando Paths cacheados (Zero-GC).
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
                        drawPath(path = optimisticPath, color = LifeOsTheme.GreenHealth.copy(alpha = 0.75f), style = boundStroke)
                        drawPath(path = conservativePath, color = LifeOsTheme.AmberFocus.copy(alpha = 0.75f), style = boundStroke)
                        drawPath(path = realisticPath, color = LifeOsTheme.CyanFinance, style = dashStroke)
                        drawPath(path = historyPath, color = Color.White, style = solidStroke)

                        drawCircle(color = LifeOsTheme.CyanFinance, radius = 5.dp.toPx(), center = pivotPoint)
                    }
                }
        )

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Início", color = LifeOsTheme.TextMuted, fontSize = 10.sp)
            Text("● Hoje", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("Cone Otimista / Conservador →", color = LifeOsTheme.CyanFinance, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
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
    normalizedAttributes6: List<Float>, // [Força, Intelecto, Finanças, Resiliência, Foco, Cultura]
    attributeScores0to100: List<Int>,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val gridWebPath = remember { Path() }
    val avatarPolyPath = remember { Path() }
    val labels = listOf("FORÇA", "INTELECTO", "FINANÇAS", "RESILIÊNCIA", "FOCO", "CULTURA")

    Box(modifier = modifier.height(226.dp), contentAlignment = Alignment.Center) {
        Spacer(
            modifier = Modifier
                .size(182.dp)
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

                    // Teia hexagonal concêntrica (33%, 66%, 100%) + raios axiais em um único Path cacheado
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
                        val frac = normalizedAttributes6.getOrElse(v) { 0.65f }.coerceIn(0.15f, 1f)
                        hexVertex(v, frac)
                    }

                    avatarPolyPath.reset()
                    activePts.forEachIndexed { v, pt ->
                        if (v == 0) avatarPolyPath.moveTo(pt.x, pt.y) else avatarPolyPath.lineTo(pt.x, pt.y)
                    }
                    avatarPolyPath.close()

                    val polyFill = Brush.radialGradient(
                        colors = listOf(
                            LifeOsTheme.PurpleStudy.copy(alpha = 0.45f),
                            LifeOsTheme.GreenHealth.copy(alpha = 0.28f)
                        ),
                        center = center,
                        radius = maxR
                    )
                    val webStroke = Stroke(width = 1.dp.toPx())
                    val polyStroke = Stroke(width = 2.5.dp.toPx(), join = StrokeJoin.Round)
                    val dotR = 4.2.dp.toPx()

                    onDrawBehind {
                        drawPath(path = gridWebPath, color = LifeOsTheme.BorderSubtle, style = webStroke)
                        drawPath(path = avatarPolyPath, brush = polyFill)
                        drawPath(path = avatarPolyPath, color = LifeOsTheme.GreenHealth, style = polyStroke)

                        activePts.forEach { pt ->
                            drawCircle(color = Color.White, radius = dotR, center = pt)
                        }
                    }
                }
        )

        // Rótulos nos 6 vértices do Hexágono RPG
        Box(modifier = Modifier.fillMaxWidth().height(226.dp)) {
            val s = attributeScores0to100
            Text(
                text = "${labels[0]} ${s.getOrElse(0) { 80 }}",
                color = LifeOsTheme.GreenHealth,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            Text(
                text = "${labels[3]} ${s.getOrElse(3) { 85 }}",
                color = LifeOsTheme.OrangeStreak,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp),
                verticalArrangement = Arrangement.spacedBy(76.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text("${labels[1]} ${s.getOrElse(1) { 84 }}", color = LifeOsTheme.PurpleStudy, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("${labels[2]} ${s.getOrElse(2) { 78 }}", color = LifeOsTheme.CyanFinance, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp),
                verticalArrangement = Arrangement.spacedBy(76.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text("${labels[5]} ${s.getOrElse(5) { 76 }}", color = LifeOsTheme.AmberFocus, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("${labels[4]} ${s.getOrElse(4) { 82 }}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
