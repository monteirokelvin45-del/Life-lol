package com.lifeos.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * Inspirada em cronógrafos espaciais (Ref 04): escala graduada orbital, 8 pétalas polares
 * bioluminescentes por física de mola (Spring) e nebulosa central estelar em Zero-GC.
 */
@Composable
fun BalanceWheel(
    healthScore: Float,       // 0.0 a 1.0
    studyScore: Float,        // 0.0 a 1.0
    financeScore: Float,      // 0.0 a 1.0
    consistencyScore: Float,  // 0.0 a 1.0
    modifier: Modifier = Modifier.size(268.dp)
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
                .size(238.dp)
                .drawWithCache {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val outerDialR = size.minDimension / 2f - 4.dp.toPx()
                    val maxRadius = outerDialR - 8.dp.toPx()
                    val innerOrbRadius = maxRadius * 0.37f

                    val sectors = arrayOf(
                        Triple(-112.5f, animHealth, LifeOsTheme.GreenHealth),
                        Triple(-67.5f, ((animHealth * 0.72f) + (animConsistency * 0.28f)).coerceIn(0.32f, 1f), Color(0xFFB7F34D)),
                        Triple(-22.5f, animFinance, LifeOsTheme.CyanFinance),
                        Triple(22.5f, ((animFinance * 0.68f) + (animStudy * 0.32f)).coerceIn(0.34f, 1f), Color(0xFF3B82F6)),
                        Triple(67.5f, animStudy, LifeOsTheme.PurpleStudy),
                        Triple(112.5f, ((animStudy * 0.65f) + (animConsistency * 0.35f)).coerceIn(0.32f, 1f), Color(0xFFE879F9)),
                        Triple(157.5f, animConsistency, LifeOsTheme.OrangeStreak),
                        Triple(202.5f, ((animHealth + animConsistency) * 0.52f).coerceIn(0.35f, 1f), LifeOsTheme.AmberFocus)
                    )
                    val sectorSweep = (360f / sectors.size) - 4.5f
                    val outerTopLeft = Offset(center.x - maxRadius, center.y - maxRadius)
                    val outerSize = Size(maxRadius * 2f, maxRadius * 2f)
                    val rimStroke = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                    val concentricStroke = Stroke(width = 1.dp.toPx())
                    val orbStroke = Stroke(width = 1.6.dp.toPx())
                    val tickStroke = 1.2.dp.toPx()

                    val activeRadii = FloatArray(sectors.size)
                    val activeBrushes = Array(sectors.size) { idx ->
                        val (_, fraction, color) = sectors[idx]
                        val r = innerOrbRadius + ((maxRadius - innerOrbRadius) * fraction.coerceIn(0.22f, 1f))
                        activeRadii[idx] = r
                        Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = 0.14f),
                                color.copy(alpha = 0.45f),
                                color.copy(alpha = 0.86f)
                            ),
                            center = center,
                            radius = r
                        )
                    }

                    // 48 marcas de cronógrafo ao redor do anel externo
                    val tickStarts = Array(48) { t ->
                        val rad = Math.toRadians(t * 7.5)
                        val r0 = if (t % 6 == 0) outerDialR - 5.dp.toPx() else outerDialR - 2.6.dp.toPx()
                        Offset(
                            center.x + (cos(rad) * r0).toFloat(),
                            center.y + (sin(rad) * r0).toFloat()
                        )
                    }
                    val tickEnds = Array(48) { t ->
                        val rad = Math.toRadians(t * 7.5)
                        Offset(
                            center.x + (cos(rad) * outerDialR).toFloat(),
                            center.y + (sin(rad) * outerDialR).toFloat()
                        )
                    }

                    val coreNebulaBrush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1A1F33),
                            Color(0xFF090A0F),
                            Color(0xFF040406)
                        ),
                        center = center,
                        radius = innerOrbRadius
                    )

                    val sparkleColors = arrayOf(
                        LifeOsTheme.GreenHealth,
                        LifeOsTheme.PurpleStudy,
                        LifeOsTheme.CyanFinance,
                        LifeOsTheme.AmberFocus,
                        Color.White
                    )
                    val starOffsets = Array(30) { i ->
                        val angleRad = Math.toRadians(i * 137.5)
                        val distRatio = 0.30f + ((i % 7) * 0.09f)
                        Offset(
                            x = center.x + (cos(angleRad) * innerOrbRadius * distRatio).toFloat(),
                            y = center.y + (sin(angleRad) * innerOrbRadius * distRatio).toFloat()
                        )
                    }
                    val bigDot = 1.9.dp.toPx()
                    val smallDot = 1.1.dp.toPx()

                    onDrawBehind {
                        // Marcas graduadas de cronógrafo espacial
                        for (t in tickStarts.indices) {
                            drawLine(
                                color = if (t % 6 == 0) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.10f),
                                start = tickStarts[t],
                                end = tickEnds[t],
                                strokeWidth = tickStroke,
                                cap = StrokeCap.Round
                            )
                        }

                        sectors.forEachIndexed { idx, (startAngle, _, sectorColor) ->
                            drawArc(
                                color = Color(0xFF12141B),
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
                                startAngle = startAngle + 1.2f,
                                sweepAngle = (sectorSweep - 2.4f).coerceAtLeast(4f),
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = rimStroke
                            )
                        }

                        // Anéis orbitais concêntricos cósmicos (33%, 66%, 100%)
                        for (ring in 1..3) {
                            val r = innerOrbRadius + ((maxRadius - innerOrbRadius) * (ring / 3f))
                            drawCircle(
                                color = Color.White.copy(alpha = 0.07f),
                                radius = r,
                                center = center,
                                style = concentricStroke
                            )
                        }

                        drawCircle(brush = coreNebulaBrush, radius = innerOrbRadius, center = center)
                        drawCircle(
                            color = LifeOsTheme.BorderSpecular,
                            radius = innerOrbRadius,
                            center = center,
                            style = orbStroke
                        )

                        for (i in starOffsets.indices) {
                            drawCircle(
                                color = sparkleColors[i % sparkleColors.size].copy(alpha = 0.65f),
                                radius = if (i % 4 == 0) bigDot else smallDot,
                                center = starOffsets[i]
                            )
                        }
                    }
                }
        )

        Box(modifier = Modifier.size(268.dp)) {
            OrbitalAxisPill(
                label = "CORPO 30%",
                accent = LifeOsTheme.GreenHealth,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            OrbitalAxisPill(
                label = "FOCO 25%",
                accent = LifeOsTheme.PurpleStudy,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OrbitalAxisPill(label = "STREAK", accent = LifeOsTheme.OrangeStreak)
                OrbitalAxisPill(label = "CAIXA", accent = LifeOsTheme.CyanFinance)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SCORE",
                color = LifeOsTheme.TextSecondary,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.4.sp
            )
            Row {
                Text(
                    text = String.format(Locale.US, "%.1f", overallScore),
                    color = Color.White,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.alignByBaseline()
                )
                Spacer(modifier = Modifier.width(1.dp))
                Text(
                    text = "/10",
                    color = LifeOsTheme.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alignByBaseline()
                )
            }
        }
    }
}

@Composable
private fun OrbitalAxisPill(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF0E1016).copy(alpha = 0.92f))
            .border(1.dp, accent.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = accent,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            softWrap = false
        )
    }
}
