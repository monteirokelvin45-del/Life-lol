@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lifeos.app.features.workout

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
            .background(LifeOsTheme.PureBlack),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Calistenia & Fisiologia", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                days.forEach { (dayNum, label) ->
                    FilterChip(
                        selected = state.selectedDayOfWeek == dayNum,
                        onClick = { viewModel.onIntent(WorkoutIntent.SelectDay(dayNum)) },
                        label = { Text(label) }
                    )
                }
            }
        }

        if (state.activeTimer.isRunning) {
            item {
                BentoCard(borderColor = LifeOsTheme.OrangeStreak) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(state.activeTimer.label, color = LifeOsTheme.OrangeStreak, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(state.activeTimer.formattedRemaining, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                        }
                        OutlinedButton(onClick = { viewModel.onIntent(WorkoutIntent.StopRestTimer) }) {
                            Text("Pular Descanso", color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { state.activeTimer.progressFraction },
                        color = LifeOsTheme.OrangeStreak,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        state.calendarSyncMessage?.let { msg ->
            item {
                BentoCard(borderColor = LifeOsTheme.GreenHealth) {
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
                    Text("Descanso Regenerativo & Mobilidade", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Nenhuma rotina pesada agendada para este dia. Importe um PDF/MD na Home para adicionar treinos.",
                        color = LifeOsTheme.TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            item {
                BentoCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(routine.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(
                                text = "${routine.completedExercisesCount}/${routine.exercises.size} exercícios concluídos · ~${routine.estimatedDurationMinutes} min",
                                color = LifeOsTheme.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                calendarPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_CALENDAR,
                                        Manifest.permission.WRITE_CALENDAR
                                    )
                                )
                            }
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = LifeOsTheme.GreenHealth, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Agenda", color = Color.White, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { routine.completionFraction },
                        color = LifeOsTheme.GreenHealth,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            items(routine.exercises.size) { index ->
                val ex = routine.exercises[index]
                BentoCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ex.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            val weightBadge = if (ex.lastWeightKg > 0f) {
                                " · Carga anterior: +${ex.lastWeightKg.toInt()} kg"
                            } else {
                                " · Peso corporal"
                            }
                            Text(
                                text = "${ex.targetSets} séries × ${ex.targetReps} reps · Descanso ${ex.restSeconds}s$weightBadge",
                                color = LifeOsTheme.TextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Progresso hoje: ${ex.completedSetsToday}/${ex.targetSets} séries",
                                color = if (ex.isCompletedToday) LifeOsTheme.GreenHealth else LifeOsTheme.OrangeStreak,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (ex.isCompletedToday) LifeOsTheme.SurfaceElevated else LifeOsTheme.GreenHealth
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = if (ex.isCompletedToday) Color.White else Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (ex.isCompletedToday) "Reiniciar" else "+1 Série",
                                color = if (ex.isCompletedToday) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold
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
            val limeAccent = Color(0xFFB7E856)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Seu Progresso Fisiológico", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("Pequenos passos, grandes adaptações neuromusculares.", color = LifeOsTheme.TextSecondary, fontSize = 12.sp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(LifeOsTheme.SurfaceDark)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    periods.forEach { p ->
                        val active = selectedPeriod == p
                        TextButton(
                            onClick = { selectedPeriod = p },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (active) limeAccent else Color.Transparent)
                        ) {
                            Text(
                                text = p,
                                color = if (active) Color.Black else LifeOsTheme.TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                BentoCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("MAPA ANATÔMICO DE FADIGA MUSCULAR", color = LifeOsTheme.GreenHealth, style = MaterialTheme.typography.labelSmall)
                            Text("Recuperação Fisiológica Termográfica", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        }
                        Text("120 FPS", color = LifeOsTheme.AmberFocus, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val fMap = state.muscleFatigueGroups.associate { it.groupKey to it.recoveryFraction }
                    AnatomicalMuscleFatigueMap(
                        chestRecovery = fMap["CHEST"] ?: 0.90f,
                        backRecovery = fMap["BACK"] ?: 0.52f,
                        shouldersRecovery = fMap["SHOULDERS"] ?: 0.78f,
                        armsRecovery = fMap["ARMS"] ?: 0.60f,
                        coreRecovery = fMap["CORE"] ?: 0.84f,
                        legsRecovery = fMap["LEGS"] ?: 0.95f
                    )
                }

                BentoCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("SOBRECARGA PROGRESSIVA & REGRESSÃO", color = LifeOsTheme.AmberFocus, style = MaterialTheme.typography.labelSmall)
                            Text("Repetições × Carga Extra (kg)", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        }
                        Text("— Regressão Linear", color = LifeOsTheme.AmberFocus, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                        Column {
                            Text("Volume Semanal Ativo", color = LifeOsTheme.TextSecondary, fontSize = 12.sp)
                            Text("1.420 kcal média", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        }
                        Text(
                            text = "↑ 8% vs sem. ant.",
                            color = limeAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(limeAccent.copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
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
                        Column {
                            Text("Progressão de Carga Extra", color = LifeOsTheme.TextSecondary, fontSize = 12.sp)
                            Text("+12.0 kg média", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        }
                        Text(
                            text = "+2.0 kg evolução",
                            color = limeAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                                Text("Mantenha!", color = LifeOsTheme.TextMuted, fontSize = 11.sp)
                            }
                            CircularStreakRing(progress = 0.78f)
                        }
                    }

                    BentoCard(modifier = Modifier.weight(1f)) {
                        Text("Meta Diária", color = LifeOsTheme.TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("68%", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { 0.68f },
                            color = limeAccent,
                            trackColor = LifeOsTheme.SurfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("No ritmo!", color = limeAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Text("Insights", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BentoCard(modifier = Modifier.weight(1f)) {
                        Text("Séries/Sem", color = LifeOsTheme.TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("56 séries", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text("↑ +6 vs ant.", color = limeAccent, fontSize = 10.sp)
                    }
                    BentoCard(modifier = Modifier.weight(1.25f)) {
                        Text("Divisão", color = LifeOsTheme.TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MacroDomainDonutRing()
                            Column {
                                Text("42% Tração", color = limeAccent, fontSize = 10.sp)
                                Text("33% Empurrão", color = LifeOsTheme.GreenHealth, fontSize = 10.sp)
                                Text("25% Core", color = LifeOsTheme.AmberFocus, fontSize = 10.sp)
                            }
                        }
                    }
                    BentoCard(modifier = Modifier.weight(1f)) {
                        Text("Melhor Dia", color = LifeOsTheme.TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Sexta ⭐", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text("1.820 kcal", color = LifeOsTheme.AmberFocus, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
