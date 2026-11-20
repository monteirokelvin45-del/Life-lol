@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lifeos.app.features.habits

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.lifeos.core.domain.model.HabitDomainModel
import com.lifeos.ui.components.BentoCard
import com.lifeos.ui.components.HabitMicroHeatmap
import com.lifeos.ui.theme.LifeOsTheme

@Composable
fun HabitsScreen(
    viewModel: HabitsViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var noteHabitTarget by remember { mutableStateOf<HabitDomainModel?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LifeOsTheme.PureBlack),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Hábitos & Consistência", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "${state.habits.size} rituais monitorados em Canvas 120 FPS",
                        color = LifeOsTheme.TextSecondary,
                        fontSize = 13.sp
                    )
                }
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.GreenHealth)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Novo", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(state.habits.size) { idx ->
            val habit = state.habits[idx]
            val accent = LifeOsTheme.parseHexColorOrDefault(habit.hexColor)

            BentoCard(
                onClick = { viewModel.onIntent(HabitsIntent.SelectHabitDetail(habit.id)) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(habit.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        if (habit.description.isNotBlank()) {
                            Text(habit.description, color = LifeOsTheme.TextSecondary, fontSize = 12.sp)
                        }
                    }
                    IconButton(
                        onClick = { viewModel.onIntent(HabitsIntent.ToggleToday(habit.id)) },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (habit.completedToday) accent else LifeOsTheme.SurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Marcar hoje",
                            tint = if (habit.completedToday) Color.Black else LifeOsTheme.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                HabitMicroHeatmap(
                    activityMatrix = habit.heatmapMatrix,
                    accentColor = accent
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBadge("🔥 ${habit.currentStreak}", accent)
                        StatBadge("${habit.completedThisWeek}/${habit.targetPerWeek} semana", LifeOsTheme.TextSecondary)
                    }
                    OutlinedButton(
                        onClick = { noteHabitTarget = habit },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Rounded.EditNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nota", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Modal de Detalhe com 4 Bento Tiles (Referência 05: Streak, Total Check-Ins, Avg. Log Time, Goal Progress)
    state.selectedHabit?.let { habit ->
        val accent = LifeOsTheme.parseHexColorOrDefault(habit.hexColor)
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(HabitsIntent.SelectHabitDetail(null)) },
            containerColor = LifeOsTheme.SurfaceDark,
            title = { Text(habit.name, color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricTile("Streak", "${habit.currentStreak}", "Recorde: ${habit.longestStreak}", accent, Modifier.weight(1f))
                        MetricTile("Check-Ins", "${habit.totalCheckIns}", "Acumulado", accent, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricTile("Horário Médio", habit.avgLogTimeFormatted, "Consistência", accent, Modifier.weight(1f))
                        MetricTile("Meta Semanal", "${habit.goalProgressPercent}%", "${habit.completedThisWeek}/${habit.targetPerWeek}", accent, Modifier.weight(1f))
                    }
                    LinearProgressIndicator(
                        progress = { habit.goalProgressPercent / 100f },
                        color = accent,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (habit.recentRecords.isNotEmpty()) {
                        Text("Registros Recentes", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        habit.recentRecords.take(4).forEach { rec ->
                            Text(
                                text = "${rec.formattedDate} · ${rec.formattedTime}" + (rec.note?.let { " — $it" } ?: ""),
                                color = LifeOsTheme.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(HabitsIntent.SelectHabitDetail(null)) }) {
                    Text("Fechar", color = Color.White)
                }
            }
        )
    }

    noteHabitTarget?.let { target ->
        var noteText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { noteHabitTarget = null },
            containerColor = LifeOsTheme.SurfaceDark,
            title = { Text("Check-in com Nota · ${target.name}", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("Ex: Treino intenso em jejum, ótima energia") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onIntent(HabitsIntent.ToggleToday(target.id, noteText.ifBlank { null }))
                        noteHabitTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.GreenHealth)
                ) {
                    Text("Salvar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteHabitTarget = null }) {
                    Text("Cancelar", color = LifeOsTheme.TextSecondary)
                }
            }
        )
    }

    if (showCreateDialog) {
        CreateHabitDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, cat, hex ->
                showCreateDialog = false
                viewModel.onIntent(HabitsIntent.CreateHabit(name, desc, cat, hex, 127))
            }
        )
    }
}

@Composable
private fun StatBadge(label: String, color: Color) {
    Text(
        text = label,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(LifeOsTheme.SurfaceElevated)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
private fun MetricTile(
    title: String,
    bigValue: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(LifeOsTheme.SurfaceElevated)
            .padding(12.dp)
    ) {
        Text(title, color = LifeOsTheme.TextSecondary, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(bigValue, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text(subtitle, color = LifeOsTheme.TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun CreateHabitDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("HEALTH") }

    val palette = mapOf(
        "HEALTH" to "#10E05B",
        "STUDY" to "#8B5CF6",
        "FINANCE" to "#00E5FF",
        "MIND" to "#FF4800"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LifeOsTheme.SurfaceDark,
        title = { Text("Novo Hábito", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do hábito") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição curta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    palette.keys.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name, description, selectedCategory, palette[selectedCategory] ?: "#10E05B")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.GreenHealth)
            ) {
                Text("Criar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = LifeOsTheme.TextSecondary)
            }
        }
    )
}
