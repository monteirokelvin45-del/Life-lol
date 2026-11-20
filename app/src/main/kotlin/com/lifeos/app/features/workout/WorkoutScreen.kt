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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.ui.components.BentoCard
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
    }
}
