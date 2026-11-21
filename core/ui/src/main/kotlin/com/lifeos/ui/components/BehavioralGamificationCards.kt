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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Shield
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
 * Renderiza auras de foco em tempo real de até 3 parceiros em Zero-GC (drawWithCache),
 * com estética de radar bioluminescente sem salas de chat.
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
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlowingIconOrb(
                    icon = Icons.Rounded.GraphicEq,
                    tint = LifeOsTheme.CyanFinance,
                    size = 38.dp,
                    iconSize = 18.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    EyebrowCapsule(
                        text = "GHOST MULTIPLAYER · ZERO CHAT",
                        accent = LifeOsTheme.CyanFinance
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Esquadrão Silencioso em Foco",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            OutlinedButton(
                onClick = onEditSquad,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Esquadrão", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

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
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    auraColor.copy(alpha = if (active) 0.11f else 0.03f),
                                    LifeOsTheme.SurfaceElevated
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = if (active) auraColor.copy(alpha = 0.42f) else LifeOsTheme.BorderSubtle,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Spacer(
                            modifier = Modifier
                                .size(52.dp)
                                .drawWithCache {
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val maxR = size.minDimension / 2f
                                    val auraBrush = Brush.radialGradient(
                                        colors = listOf(
                                            auraColor.copy(alpha = if (active) 0.60f else 0.18f),
                                            auraColor.copy(alpha = 0.10f),
                                            Color.Transparent
                                        ),
                                        center = center,
                                        radius = maxR
                                    )
                                    val ringStroke = Stroke(width = 1.8.dp.toPx())
                                    val outerHaloStroke = Stroke(width = 1.dp.toPx())
                                    onDrawBehind {
                                        drawCircle(brush = auraBrush, radius = maxR, center = center)
                                        if (active) {
                                            drawCircle(
                                                color = auraColor.copy(alpha = 0.28f),
                                                radius = maxR * 0.86f,
                                                center = center,
                                                style = outerHaloStroke
                                            )
                                        }
                                        drawCircle(
                                            color = auraColor.copy(alpha = if (active) 0.95f else 0.35f),
                                            radius = maxR * 0.56f,
                                            center = center,
                                            style = ringStroke
                                        )
                                    }
                                }
                        )
                        Text(
                            text = codenames[idx].take(2).uppercase(Locale.ROOT),
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = codenames[idx],
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = domainLabels.getOrElse(idx) { "Foco" },
                        color = auraColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (active) "● ${mins}m ativo" else "${mins}m hoje",
                        color = if (active) LifeOsTheme.TextPrimary else LifeOsTheme.TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Pilar 5.3 — Escudo Anti-Falha (Streak Freeze Científico) com cristais de energia.
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
        glowAccent = LifeOsTheme.CyanFinance,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlowingIconOrb(
                    icon = Icons.Rounded.Shield,
                    tint = LifeOsTheme.CyanFinance,
                    size = 42.dp,
                    iconSize = 20.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    EyebrowCapsule(
                        text = "STREAK FREEZE CIENTÍFICO",
                        accent = LifeOsTheme.CyanFinance
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isFreezeActiveToday) {
                            "Escudo Ativo Hoje · Sequência Protegida"
                        } else {
                            "$availableCredits de $maxCredits Cristais de Repouso"
                        },
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (c in 0 until maxCredits.coerceAtLeast(3)) {
                            val charged = c < availableCredits
                            Box(
                                modifier = Modifier
                                    .size(width = 18.dp, height = 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (charged) LifeOsTheme.CyanFinance else LifeOsTheme.SurfaceElevated
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$cleanCheckInsInCycle/$requiredPerShield check-ins",
                            color = LifeOsTheme.TextSecondary,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onActivateFreeze,
                enabled = !isFreezeActiveToday && availableCredits > 0,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LifeOsTheme.CyanFinance,
                    disabledContainerColor = LifeOsTheme.SurfaceElevated
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isFreezeActiveToday) "Ativo ✓" else "Congelar",
                    color = if (isFreezeActiveToday) LifeOsTheme.GreenHealth else Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
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
 * Pilar 5.5 — Contratos de Compromisso Anti-Charity com formatação monetária de luxo.
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
        glowAccent = LifeOsTheme.AmberFocus,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlowingIconOrb(
                    icon = Icons.Rounded.Balance,
                    tint = LifeOsTheme.AmberFocus,
                    size = 40.dp,
                    iconSize = 19.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    EyebrowCapsule(
                        text = "CONTRATO ANTI-CHARITY",
                        accent = LifeOsTheme.AmberFocus
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${LifeOsTheme.formatMoney(stakeAmount, currencyCode, decimals = 0)} → $institutionName",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            OutlinedButton(
                onClick = onConfigureContract,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Caução", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
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
