@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lifeos.app.features.workout

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.ui.components.AnatomicalMuscleFatigueMap
import com.lifeos.ui.components.BentoCard
import com.lifeos.ui.components.CircularStreakRing
import com.lifeos.ui.components.EyebrowCapsule
import com.lifeos.ui.components.GlowingIconOrb
import com.lifeos.ui.components.LuxuryStatBadge
import com.lifeos.ui.components.MacroDomainDonutRing
import com.lifeos.ui.components.ProgressiveOverloadRegressionChart
import com.lifeos.ui.components.WeeklyGradientBarsChart
import com.lifeos.ui.components.WeeklyTrendDotsChart
import com.lifeos.ui.theme.LifeOsTheme

@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val limeAccent = Color(0xFFB7F34D)

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            viewModel.onIntent(WorkoutIntent.SyncRoutineToCalendar)
        }
    }

    val days = listOf(
        1 to "Seg",
        2 to "Ter",
        3 to "Qua",
        4 to "Qui",
        5 to "Sex",
        6 to "Sáb",
        7 to "Dom"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EyebrowCapsule(
                    text = "18:00 · ENGENHARIA NEUROMUSCULAR",
                    accent = limeAccent
                )
                Text(
                    text = "Calistenia & Fisiologia",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )

                // Seletor Semanal de 7 Cápsulas em Vidro Fumê
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(LifeOsTheme.SurfaceDark)
                        .border(1.dp, LifeOsTheme.BorderSubtle, CircleShape)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    days.forEach { (dayNum, label) ->
                        val active = state.selectedDayOfWeek == dayNum
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(if (active) limeAccent else Color.Transparent)
                                .clickable { viewModel.onIntent(WorkoutIntent.SelectDay(dayNum)) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (active) Color.Black else LifeOsTheme.TextSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        if (state.activeTimer.isRunning) {
            item {
                BentoCard(borderColor = LifeOsTheme.OrangeStreak, glowAccent = LifeOsTheme.OrangeStreak) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            EyebrowCapsule(text = state.activeTimer.label, accent = LifeOsTheme.OrangeStreak)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(state.activeTimer.formattedRemaining, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                        }
                        OutlinedButton(onClick = { viewModel.onIntent(WorkoutIntent.StopRestTimer) }, shape = CircleShape) {
                            Text("Pular Descanso", color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { state.activeTimer.progressFraction },
                        color = LifeOsTheme.OrangeStreak,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }

        state.calendarSyncMessage?.let { msg ->
            item {
                BentoCard(borderColor = LifeOsTheme.GreenHealth, glowAccent = LifeOsTheme.GreenHealth) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(msg, color = LifeOsTheme.GreenHealth, modifier = Modifier.weight(1f), fontSize = 13.sp)
                        TextButton(onClick = { viewModel.onIntent(WorkoutIntent.ClearMessage) }) {
                            Text("OK", color = Color.White)
                        }
                    }
                }
            }
        }

        val routine = state.routine
        if (routine == null) {
            item {
                BentoCard {
                    Text("Recuperação Miofascial & Mobilidade", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Janela dedicada à supercompensação tecidual. Importe um protocolo na aba Hoje para adicionar sessões.",
                        color = LifeOsTheme.TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            item {
                BentoCard(glowAccent = LifeOsTheme.GreenHealth) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(routine.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp)
                            Text(
                                text = "${routine.completedExercisesCount}/${routine.exercises.size} exercícios concluídos · ~${routine.estimatedDurationMinutes} min",
                                color = LifeOsTheme.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                calendarPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_CALENDAR,
                                        Manifest.permission.WRITE_CALENDAR
                                    )
                                )
                            },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = LifeOsTheme.GreenHealth, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Agenda", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { routine.completionFraction.coerceAtLeast(0.06f) },
                        color = LifeOsTheme.GreenHealth,
                        trackColor = LifeOsTheme.SurfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }

            items(routine.exercises.size) { index ->
                val ex = routine.exercises[index]
                val accent = if (ex.isCompletedToday) LifeOsTheme.GreenHealth else LifeOsTheme.AmberFocus

                BentoCard(glowAccent = if (ex.isCompletedToday) LifeOsTheme.GreenHealth else null) {
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
                                icon = if (ex.isCompletedToday) Icons.Rounded.Check else Icons.Rounded.FitnessCenter,
                                tint = accent,
                                size = 40.dp,
                                iconSize = 18.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ex.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
                                val weightBadge = if (ex.lastWeightKg > 0f) {
                                    " · +${ex.lastWeightKg.toInt()} kg"
                                } else {
                                    " · Peso corporal"
                                }
                                Text(
                                    text = "${ex.targetSets}×${ex.targetReps} reps · ${ex.restSeconds}s$weightBadge",
                                    color = LifeOsTheme.TextSecondary,
                                    fontSize = 11.5.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (s in 0 until ex.targetSets.coerceAtMost(6)) {
                                        val done = s < ex.completedSetsToday
                                        Box(
                                            modifier = Modifier
                                                .size(width = 16.dp, height = 5.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (done) LifeOsTheme.GreenHealth else LifeOsTheme.SurfaceElevated
                                                )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${ex.completedSetsToday}/${ex.targetSets}",
                                        color = accent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val nextCount = if (ex.completedSetsToday >= ex.targetSets) 0 else ex.completedSetsToday + 1
                                viewModel.onIntent(
                                    WorkoutIntent.LogExerciseSet(
                                        exerciseId = ex.id,
                                        exerciseName = ex.name,
                                        nextSetsCount = nextCount,
                                        restSeconds = ex.restSeconds
                                    )
                                )
                            },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (ex.isCompletedToday) LifeOsTheme.SurfaceElevated else LifeOsTheme.GreenHealth
                            )
                        ) {
                            Icon(
                                imageVector = if (ex.isCompletedToday) Icons.Rounded.Check else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = if (ex.isCompletedToday) LifeOsTheme.GreenHealth else Color.Black,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (ex.isCompletedToday) "Feito" else "+1 Série",
                                color = if (ex.isCompletedToday) Color.White else Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // Painel Analítico Fisiológico fiel à Referência 08 ("Kalo / Your progress")
        item {
            var selectedPeriod by remember { mutableStateOf("Semana") }
            val periods = listOf("Semana", "Mês", "3 Meses", "Ano")

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Telemetria & Adaptação", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("Biometria muscular, sobrecarga progressiva e gasto calórico ativo.", color = LifeOsTheme.TextSecondary, fontSize = 12.5.sp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(LifeOsTheme.SurfaceDark)
                        .border(1.dp, LifeOsTheme.BorderSubtle, CircleShape)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    periods.forEach { p ->
                        val active = selectedPeriod == p
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(if (active) limeAccent else Color.Transparent)
                                .clickable { selectedPeriod = p }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = p,
                                color = if (active) Color.Black else LifeOsTheme.TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                BentoCard(glowAccent = LifeOsTheme.GreenHealth) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            EyebrowCapsule(text = "SCANNER MIOFASCIAL TERMOGRÁFICO", accent = LifeOsTheme.GreenHealth)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Mapa Anatômico de Fadiga", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        LuxuryStatBadge(label = "120 FPS", accent = LifeOsTheme.AmberFocus)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    val fMap = state.muscleFatigueGroups.associate { it.groupKey to it.recoveryFraction }
                    AnatomicalMuscleFatigueMap(
                        chestRecovery = fMap["CHEST"] ?: 0.90f,
                        backRecovery = fMap["BACK"] ?: 0.42f,
                        shouldersRecovery = fMap["SHOULDERS"] ?: 0.76f,
                        armsRecovery = fMap["ARMS"] ?: 0.54f,
                        coreRecovery = fMap["CORE"] ?: 0.86f,
                        legsRecovery = fMap["LEGS"] ?: 0.96f
                    )
                }

                BentoCard(glowAccent = LifeOsTheme.AmberFocus) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            EyebrowCapsule(text = "SOBRECARGA PROGRESSIVA", accent = LifeOsTheme.AmberFocus)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Repetições × Lastro Extra (kg)", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        }
                        LuxuryStatBadge(label = "Regressão Linear", accent = LifeOsTheme.AmberFocus)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ProgressiveOverloadRegressionChart()
                }

                BentoCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Volume Semanal Ativo", color = LifeOsTheme.TextSecondary, fontSize = 12.sp)
                            Text("1.420 kcal média", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        }
                        LuxuryStatBadge(label = "↑ 8% vs ant.", accent = limeAccent)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    WeeklyGradientBarsChart(
                        dailyValues = listOf(1400f, 1320f, 1450f, 1380f, 1820f, 1410f, 1260f)
                    )
                }

                BentoCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Progressão de Carga Extra", color = LifeOsTheme.TextSecondary, fontSize = 12.sp)
                            Text("+12,0 kg média", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        }
                        LuxuryStatBadge(label = "+2,0 kg", accent = limeAccent)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    WeeklyTrendDotsChart(
                        points = listOf(10f, 10f, 10.5f, 11f, 11.5f, 12f, 12f),
                        lineColor = limeAccent
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BentoCard(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Streak", color = LifeOsTheme.TextSecondary, fontSize = 12.sp)
                                Text("12 dias", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                Text("Consistente", color = LifeOsTheme.TextMuted, fontSize = 11.sp)
                            }
                            CircularStreakRing(progress = 0.78f)
                        }
                    }

                    BentoCard(modifier = Modifier.weight(1f)) {
                        Text("Meta Diária", color = LifeOsTheme.TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("75%", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { 0.75f },
                            color = limeAccent,
                            trackColor = LifeOsTheme.SurfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Ritmo atlético", color = limeAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Text("Síntese Biométrica", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BentoCard(modifier = Modifier.weight(1f), contentPadding = 14.dp) {
                        Text("Séries/Sem", color = LifeOsTheme.TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("56 séries", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text("↑ +6 vs ant.", color = limeAccent, fontSize = 10.sp)
                    }
                    BentoCard(modifier = Modifier.weight(1.3f), contentPadding = 14.dp) {
                        Text("Divisão", color = LifeOsTheme.TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MacroDomainDonutRing()
                            Column {
                                Text("42% Tração", color = limeAccent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text("33% Empurrão", color = LifeOsTheme.GreenHealth, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text("25% Core", color = LifeOsTheme.AmberFocus, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    BentoCard(modifier = Modifier.weight(1f), contentPadding = 14.dp) {
                        Text("Pico", color = LifeOsTheme.TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Sexta", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text("1.820 kcal", color = LifeOsTheme.AmberFocus, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
