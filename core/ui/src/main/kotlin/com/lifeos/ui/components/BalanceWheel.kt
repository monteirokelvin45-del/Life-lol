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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import kotlin.math.cos
import kotlin.math.sin

/**
 * Roda do Equilíbrio da Vida (Pilar 8.3 + Referência 04 "Баланс"):
 * - Setores polares proporcionais em estilo Nightingale Rose sobre trilhas radiais escuras;
 * - Bordas luminosas chanfradas de precisão em cada setor;
 * - Orbe central OLED puro com micro-partículas luminosas multicoloridas e score 0.0–10.0.
 */
@Composable
fun BalanceWheel(
    healthScore: Float,       // 0.0 a 1.0
    studyScore: Float,        // 0.0 a 1.0
    financeScore: Float,      // 0.0 a 1.0
    consistencyScore: Float,  // 0.0 a 1.0
    modifier: Modifier = Modifier.size(260.dp)
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

    // 8 setores visuais inspirados na Referência 04 (4 domínios principais + 4 sub-eixos harmônicos)
    val sectors = listOf(
        Triple(-90f, animHealth, LifeOsTheme.GreenHealth),
        Triple(-38f, (animHealth * 0.72f).coerceIn(0.25f, 1f), LifeOsTheme.RedExpense),
        Triple(14f, animFinance, Color(0xFF2979FF)),
        Triple(66f, animStudy, LifeOsTheme.PurpleStudy),
        Triple(118f, animConsistency, Color(0xFFD97724)),
        Triple(170f, ((animHealth + animConsistency) * 0.5f).coerceIn(0.3f, 1f), LifeOsTheme.CyanFinance),
        Triple(222f, ((animStudy + animFinance) * 0.48f).coerceIn(0.28f, 1f), Color(0xFF94A3B8))
    )

    val sparkleColors = listOf(
        LifeOsTheme.GreenHealth,
        LifeOsTheme.PurpleStudy,
        LifeOsTheme.CyanFinance,
        LifeOsTheme.OrangeStreak,
        LifeOsTheme.AmberFocus
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(236.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f - 8.dp.toPx()
            val innerOrbRadius = maxRadius * 0.39f
            val sectorSweep = (360f / sectors.size) - 5f

            sectors.forEach { (startAngle, fraction, sectorColor) ->
                // Trilha escura completa da fatia (Referência 04)
                drawArc(
                    color = Color(0xFF151619),
                    startAngle = startAngle,
                    sweepAngle = sectorSweep,
                    useCenter = true,
                    topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                    size = Size(maxRadius * 2f, maxRadius * 2f)
                )

                // Fatia polar iluminada proporcional à nota do domínio
                val activeRadius = innerOrbRadius + ((maxRadius - innerOrbRadius) * fraction.coerceIn(0.18f, 1f))
                drawArc(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            sectorColor.copy(alpha = 0.18f),
                            sectorColor.copy(alpha = 0.78f)
                        ),
                        center = center,
                        radius = activeRadius
                    ),
                    startAngle = startAngle,
                    sweepAngle = sectorSweep,
                    useCenter = true,
                    topLeft = Offset(center.x - activeRadius, center.y - activeRadius),
                    size = Size(activeRadius * 2f, activeRadius * 2f)
                )

                // Borda luminosa externa da fatia (efeito chanfro neon da Referência 04)
                drawArc(
                    color = sectorColor,
                    startAngle = startAngle + 1f,
                    sweepAngle = (sectorSweep - 2f).coerceAtLeast(4f),
                    useCenter = false,
                    topLeft = Offset(center.x - activeRadius, center.y - activeRadius),
                    size = Size(activeRadius * 2f, activeRadius * 2f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Orbe central OLED puro
            drawCircle(
                color = Color(0xFF050507),
                radius = innerOrbRadius,
                center = center
            )
            drawCircle(
                color = Color(0xFF22242A),
                radius = innerOrbRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Micro-partículas estelares coloridas dentro do orbe central (fiel à Referência 04)
            for (i in 0 until 28) {
                val angleRad = Math.toRadians((i * 137.5))
                val distRatio = 0.34f + ((i % 7) * 0.085f)
                val px = center.x + (cos(angleRad) * innerOrbRadius * distRatio).toFloat()
                val py = center.y + (sin(angleRad) * innerOrbRadius * distRatio).toFloat()
                val dotRadius = if (i % 3 == 0) 2.1.dp.toPx() else 1.3.dp.toPx()
                drawCircle(
                    color = sparkleColors[i % sparkleColors.size].copy(alpha = 0.78f),
                    radius = dotRadius,
                    center = Offset(px, py)
                )
            }
        }

        // Rótulos orbitais discretos nos 4 pontos cardeais
        Box(modifier = Modifier.size(260.dp)) {
            Text(
                text = "SAÚDE",
                color = LifeOsTheme.TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp)
            )
            Text(
                text = "ESTUDOS",
                color = LifeOsTheme.TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
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
