package com.lifeos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.ui.theme.LifeOsTheme
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pilar 5.4 — Ghost Multiplayer Assíncrono (Esquadrão Silencioso).
 * Renderiza auras de foco em tempo real de até 3 amigos em Zero-GC (drawWithCache),
 * sem salas de conversa ou feeds de distração.
 */
@Composable
fun GhostSquadSilentAuraRow(
    codenames: List<String>,
    domainLabels: List<String>,
    minutesList: List<Int>,
    auraColors: List<Color>,
    pulsingFlags: List<Boolean>,
    onEditSquad: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "👻 ESQUADRÃO SILENCIOSO · GHOST MULTIPLAYER",
                    color = LifeOsTheme.CyanFinance,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Presença compartilhada sem chat ou distrações",
                    color = LifeOsTheme.TextSecondary,
                    fontSize = 12.sp
                )
            }
            OutlinedButton(
                onClick = onEditSquad,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Esquadrão", color = Color.White, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (idx in 0 until minOf(3, codenames.size)) {
                val auraColor = auraColors.getOrElse(idx) { LifeOsTheme.GreenHealth }
                val active = pulsingFlags.getOrElse(idx) { true }
                val mins = minutesList.getOrElse(idx) { 45 }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(LifeOsTheme.SurfaceElevated)
                        .border(
                            width = 1.dp,
                            color = if (active) auraColor.copy(alpha = 0.45f) else LifeOsTheme.BorderSubtle,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Spacer(
                            modifier = Modifier
                                .size(50.dp)
                                .drawWithCache {
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val maxR = size.minDimension / 2f
                                    val auraBrush = Brush.radialGradient(
                                        colors = listOf(
                                            auraColor.copy(alpha = if (active) 0.55f else 0.20f),
                                            auraColor.copy(alpha = 0.08f),
                                            Color.Transparent
                                        ),
                                        center = center,
                                        radius = maxR
                                    )
                                    val ringStroke = Stroke(width = 1.8.dp.toPx())
                                    onDrawBehind {
                                        drawCircle(brush = auraBrush, radius = maxR, center = center)
                                        drawCircle(
                                            color = auraColor.copy(alpha = if (active) 0.9f else 0.35f),
                                            radius = maxR * 0.58f,
                                            center = center,
                                            style = ringStroke
                                        )
                                    }
                                }
                        )
                        Text(
                            text = codenames[idx].take(2).uppercase(Locale.ROOT),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = codenames[idx],
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = domainLabels.getOrElse(idx) { "Foco" },
                        color = auraColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = if (active) "● ${mins}m em foco" else "${mins}m hoje",
                        color = LifeOsTheme.TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * Pilar 5.3 — Escudo Anti-Falha (Streak Freeze Científico).
 */
@Composable
fun StreakFreezeShieldBanner(
    availableCredits: Int,
    maxCredits: Int,
    cleanCheckInsInCycle: Int,
    requiredPerShield: Int,
    isFreezeActiveToday: Boolean,
    onActivateFreeze: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    BentoCard(
        borderColor = if (isFreezeActiveToday) LifeOsTheme.CyanFinance else LifeOsTheme.BorderSubtle,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🛡️ ESCUDO ANTI-FALHA · STREAK FREEZE CIENTÍFICO",
                    color = LifeOsTheme.CyanFinance,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isFreezeActiveToday) {
                        "Escudo Ativo Hoje · Sequência e Ebbinghaus protegidos!"
                    } else {
                        "$availableCredits de $maxCredits créditos de repouso acumulados"
                    },
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Próximo crédito: $cleanCheckInsInCycle/$requiredPerShield check-ins consistentes",
                    color = LifeOsTheme.TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onActivateFreeze,
                enabled = !isFreezeActiveToday && availableCredits > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LifeOsTheme.CyanFinance,
                    disabledContainerColor = LifeOsTheme.SurfaceElevated
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isFreezeActiveToday) "Protegido ✓" else "Usar Escudo",
                    color = if (isFreezeActiveToday) LifeOsTheme.GreenHealth else Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { (cleanCheckInsInCycle.toFloat() / requiredPerShield.coerceAtLeast(1)).coerceIn(0.06f, 1f) },
            color = LifeOsTheme.CyanFinance,
            trackColor = LifeOsTheme.SurfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

/**
 * Pilar 5.5 — Contratos de Compromisso Anti-Charity.
 */
@Composable
fun AntiCharityCommitmentBanner(
    stakeAmount: Double,
    currencyCode: String,
    institutionName: String,
    weeklyTargetRatio: Float,
    currentWeekRatio: Float,
    isActive: Boolean,
    isAtRisk: Boolean,
    statusSummary: String,
    onConfigureContract: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val accent = when {
        !isActive -> LifeOsTheme.TextSecondary
        isAtRisk -> LifeOsTheme.RedExpense
        else -> LifeOsTheme.GreenHealth
    }

    BentoCard(
        borderColor = accent.copy(alpha = 0.55f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "⚖️ CONTRATO DE COMPROMISSO ANTI-CHARITY",
                    color = LifeOsTheme.AmberFocus,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(
                        Locale.US,
                        "Caução: %.0f %s → %s",
                        stakeAmount,
                        currencyCode,
                        institutionName
                    ),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedButton(
                onClick = onConfigureContract,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Caução", color = Color.White, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(statusSummary, color = LifeOsTheme.TextSecondary, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Cumprimento Semanal: ${(currentWeekRatio * 100).roundToInt()}%",
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Cláusula Mínima: ${(weeklyTargetRatio * 100).roundToInt()}%",
                color = LifeOsTheme.TextMuted,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { currentWeekRatio.coerceIn(0.05f, 1f) },
            color = accent,
            trackColor = LifeOsTheme.SurfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}
