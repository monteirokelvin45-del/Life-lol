package com.lifeos.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * Roda do Equilíbrio da Vida (Pilar 8.3 + Referência 04):
 * Desenha os 4 setores polares proporcionais em estilo Nightingale Rose + arcos luminosos
 * externos e núcleo escuro com o LifeScore ponderado (0.0 a 10.0).
 */
@Composable
fun BalanceWheel(
    healthScore: Float,       // 0.0 a 1.0
    studyScore: Float,        // 0.0 a 1.0
    financeScore: Float,      // 0.0 a 1.0
    consistencyScore: Float,  // 0.0 a 1.0
    modifier: Modifier = Modifier.size(240.dp)
) {
    val spec = tween<Float>(durationMillis = 900, easing = FastOutSlowInEasing)
    val animHealth by animateFloatAsState(healthScore.coerceIn(0f, 1f), spec, label = "health")
    val animStudy by animateFloatAsState(studyScore.coerceIn(0f, 1f), spec, label = "study")
    val animFinance by animateFloatAsState(financeScore.coerceIn(0f, 1f), spec, label = "finance")
    val animConsistency by animateFloatAsState(consistencyScore.coerceIn(0f, 1f), spec, label = "consistency")

    val overallScore = (
        (animHealth * 0.30f) +
            (animStudy * 0.25f) +
            (animFinance * 0.25f) +
            (animConsistency * 0.20f)
        ) * 10f

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(224.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f - 10.dp.toPx()
            val innerOrbRadius = maxRadius * 0.38f
            val strokeWidth = 10.dp.toPx()
            val gapDegrees = 8f
            val fullSectorSweep = 90f - gapDegrees

            // Trilhas base de fundo dos 4 quadrantes
            val quadrants = listOf(
                Triple(180f, animHealth, LifeOsTheme.GreenHealth),
                Triple(270f, animStudy, LifeOsTheme.PurpleStudy),
                Triple(0f, animFinance, LifeOsTheme.CyanFinance),
                Triple(90f, animConsistency, LifeOsTheme.OrangeStreak)
            )

            quadrants.forEach { (startAngle, fraction, domainColor) ->
                // Fundo sutil do setor polar
                drawArc(
                    color = LifeOsTheme.SurfaceElevated,
                    startAngle = startAngle + gapDegrees / 2f,
                    sweepAngle = fullSectorSweep,
                    useCenter = true,
                    topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                    size = Size(maxRadius * 2f, maxRadius * 2f)
                )

                // Setor polar proporcional (Referência 04)
                val activeRadius = innerOrbRadius + ((maxRadius - innerOrbRadius) * fraction.coerceAtLeast(0.08f))
                drawArc(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            domainColor.copy(alpha = 0.12f),
                            domainColor.copy(alpha = 0.52f)
                        ),
                        center = center,
                        radius = activeRadius
                    ),
                    startAngle = startAngle + gapDegrees / 2f,
                    sweepAngle = fullSectorSweep,
                    useCenter = true,
                    topLeft = Offset(center.x - activeRadius, center.y - activeRadius),
                    size = Size(activeRadius * 2f, activeRadius * 2f)
                )

                // Arco externo luminoso de precisão (Pilar 8.3)
                drawArc(
                    color = domainColor,
                    startAngle = startAngle + gapDegrees / 2f,
                    sweepAngle = (fullSectorSweep * fraction).coerceAtLeast(4f),
                    useCenter = false,
                    topLeft = Offset(center.x - activeRadius, center.y - activeRadius),
                    size = Size(activeRadius * 2f, activeRadius * 2f),
                    style = Stroke(width = strokeWidth * 0.55f, cap = StrokeCap.Round)
                )
            }

            // Orbe central OLED puro
            drawCircle(
                color = LifeOsTheme.PureBlack,
                radius = innerOrbRadius,
                center = center
            )
            drawCircle(
                color = LifeOsTheme.BorderSubtle,
                radius = innerOrbRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format(Locale.US, "%.1f", overallScore),
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "/10",
                    color = LifeOsTheme.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "LIFE SCORE",
                color = LifeOsTheme.TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.1.sp
            )
        }
    }
}
