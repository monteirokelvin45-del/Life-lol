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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.ui.theme.LifeOsTheme
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Gráfico #2 da Suíte 120 FPS — Roda do Equilíbrio Cósmico (Pilar 5.1 & Pilar 6.2).
 * - Preenchimento elástico por física de mola (Spring) nos 4 eixos ponderados:
 *   Corpo (30%), Foco (25%), Finanças (25%) e Consistência (20%).
 * - Arcos circulares concêntricos + setores radiais + orbe central estelar em Zero-GC (drawWithCache).
 */
@Composable
fun BalanceWheel(
    healthScore: Float,       // 0.0 a 1.0
    studyScore: Float,        // 0.0 a 1.0
    financeScore: Float,      // 0.0 a 1.0
    consistencyScore: Float,  // 0.0 a 1.0
    modifier: Modifier = Modifier.size(260.dp)
) {
    val elasticSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    val animHealth by animateFloatAsState(healthScore.coerceIn(0f, 1f), elasticSpec, label = "health")
    val animStudy by animateFloatAsState(studyScore.coerceIn(0f, 1f), elasticSpec, label = "study")
    val animFinance by animateFloatAsState(financeScore.coerceIn(0f, 1f), elasticSpec, label = "finance")
    val animConsistency by animateFloatAsState(consistencyScore.coerceIn(0f, 1f), elasticSpec, label = "consistency")

    val overallScore = (
        (animHealth * 0.30f) +
            (animStudy * 0.25f) +
            (animFinance * 0.25f) +
            (animConsistency * 0.20f)
        ) * 10f

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Spacer(
            modifier = Modifier
                .size(236.dp)
                .drawWithCache {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = size.minDimension / 2f - 8.dp.toPx()
                    val innerOrbRadius = maxRadius * 0.39f

                    val sectors = arrayOf(
                        Triple(-90f, animHealth, LifeOsTheme.GreenHealth),
                        Triple(-38f, (animHealth * 0.78f).coerceIn(0.25f, 1f), LifeOsTheme.RedExpense),
                        Triple(14f, animFinance, Color(0xFF2979FF)),
                        Triple(66f, animStudy, LifeOsTheme.PurpleStudy),
                        Triple(118f, animConsistency, Color(0xFFD97724)),
                        Triple(170f, ((animHealth + animConsistency) * 0.5f).coerceIn(0.3f, 1f), LifeOsTheme.CyanFinance),
                        Triple(222f, ((animStudy + animFinance) * 0.48f).coerceIn(0.28f, 1f), Color(0xFF94A3B8))
                    )
                    val sectorSweep = (360f / sectors.size) - 5f
                    val outerTopLeft = Offset(center.x - maxRadius, center.y - maxRadius)
                    val outerSize = Size(maxRadius * 2f, maxRadius * 2f)
                    val rimStroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    val concentricStroke = Stroke(width = 1.dp.toPx())
                    val orbStroke = Stroke(width = 1.5.dp.toPx())

                    val activeRadii = FloatArray(sectors.size)
                    val activeBrushes = Array(sectors.size) { idx ->
                        val (_, fraction, color) = sectors[idx]
                        val r = innerOrbRadius + ((maxRadius - innerOrbRadius) * fraction.coerceIn(0.18f, 1f))
                        activeRadii[idx] = r
                        Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.18f), color.copy(alpha = 0.80f)),
                            center = center,
                            radius = r
                        )
                    }

                    val sparkleColors = arrayOf(
                        LifeOsTheme.GreenHealth,
                        LifeOsTheme.PurpleStudy,
                        LifeOsTheme.CyanFinance,
                        LifeOsTheme.OrangeStreak,
                        LifeOsTheme.AmberFocus
                    )
                    val starOffsets = Array(28) { i ->
                        val angleRad = Math.toRadians(i * 137.5)
                        val distRatio = 0.34f + ((i % 7) * 0.085f)
                        Offset(
                            x = center.x + (cos(angleRad) * innerOrbRadius * distRatio).toFloat(),
                            y = center.y + (sin(angleRad) * innerOrbRadius * distRatio).toFloat()
                        )
                    }
                    val bigDot = 2.1.dp.toPx()
                    val smallDot = 1.3.dp.toPx()

                    onDrawBehind {
                        sectors.forEachIndexed { idx, (startAngle, _, sectorColor) ->
                            drawArc(
                                color = Color(0xFF151619),
                                startAngle = startAngle,
                                sweepAngle = sectorSweep,
                                useCenter = true,
                                topLeft = outerTopLeft,
                                size = outerSize
                            )

                            val activeR = activeRadii[idx]
                            val topLeft = Offset(center.x - activeR, center.y - activeR)
                            val arcSize = Size(activeR * 2f, activeR * 2f)

                            drawArc(
                                brush = activeBrushes[idx],
                                startAngle = startAngle,
                                sweepAngle = sectorSweep,
                                useCenter = true,
                                topLeft = topLeft,
                                size = arcSize
                            )

                            drawArc(
                                color = sectorColor,
                                startAngle = startAngle + 1f,
                                sweepAngle = (sectorSweep - 2f).coerceAtLeast(4f),
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = rimStroke
                            )
                        }

                        // Anéis orbitais concêntricos cósmicos (25%, 50%, 75%)
                        for (ring in 1..3) {
                            val r = innerOrbRadius + ((maxRadius - innerOrbRadius) * (ring / 4f))
                            drawCircle(
                                color = Color.White.copy(alpha = 0.06f),
                                radius = r,
                                center = center,
                                style = concentricStroke
                            )
                        }

                        drawCircle(color = Color(0xFF050507), radius = innerOrbRadius, center = center)
                        drawCircle(
                            color = Color(0xFF22242A),
                            radius = innerOrbRadius,
                            center = center,
                            style = orbStroke
                        )

                        for (i in starOffsets.indices) {
                            drawCircle(
                                color = sparkleColors[i % sparkleColors.size].copy(alpha = 0.78f),
                                radius = if (i % 3 == 0) bigDot else smallDot,
                                center = starOffsets[i]
                            )
                        }
                    }
                }
        )

        Box(modifier = Modifier.size(260.dp)) {
            Text(
                text = "CORPO 30%",
                color = LifeOsTheme.TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp)
            )
            Text(
                text = "FOCO 25%",
                color = LifeOsTheme.TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("STREAK", color = LifeOsTheme.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text("CAIXA", color = LifeOsTheme.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row {
                Text(
                    text = String.format(Locale.US, "%.1f", overallScore),
                    color = Color.White,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.alignByBaseline()
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "/10",
                    color = LifeOsTheme.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alignByBaseline()
                )
            }
        }
    }
}
