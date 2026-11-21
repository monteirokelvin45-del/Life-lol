@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lifeos.app.features.habits

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.lifeos.ui.components.AnnualGitHubHeatmap7x52
import com.lifeos.ui.components.BentoCard
import com.lifeos.ui.components.CompoundEffectFlameChart
import com.lifeos.ui.components.EyebrowCapsule
import com.lifeos.ui.components.GlowingIconOrb
import com.lifeos.ui.components.HabitMicroHeatmap
import com.lifeos.ui.components.LuxuryStatBadge
import com.lifeos.ui.components.SleekSearchBarPill
import com.lifeos.ui.components.StreakFreezeShieldBanner
import com.lifeos.ui.theme.LifeOsTheme
import java.time.LocalDate

@Composable
fun HabitsScreen(
    viewModel: HabitsViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var noteHabitTarget by remember { mutableStateOf<HabitDomainModel?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredHabits = remember(state.habits, searchQuery) {
        if (searchQuery.isBlank()) {
            state.habits
        } else {
            state.habits.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Cabeçalho + Cápsula de Busca Frameless + "Seus Hábitos (N)"
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        EyebrowCapsule(
                            text = "ARQUITETURA DE CONSISTÊNCIA",
                            accent = LifeOsTheme.OrangeStreak
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Hoje, ${LocalDate.now().dayOfMonth} Nov",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(LifeOsTheme.PurpleStudy)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Novo hábito", tint = Color.White)
                    }
                }

                SleekSearchBarPill(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Buscar ritual ou micro-hábito..."
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Seus Rituais Ativos (${filteredHabits.size})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(LifeOsTheme.AmberFocus.copy(alpha = 0.16f))
                            .border(1.dp, LifeOsTheme.AmberFocus.copy(alpha = 0.4f), CircleShape)
                            .clickable { searchQuery = "" }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text("Todos ▾", color = LifeOsTheme.AmberFocus, fontWeight = FontWeight.ExtraBold, fontSize = 11.5.sp)
                    }
                }
            }
        }

        // 1.2. Escudo Anti-Falha Científico (Pilar 5.3)
        item {
            StreakFreezeShieldBanner(
                availableCredits = state.streakFreeze.availableCredits,
                maxCredits = state.streakFreeze.maxBankedCredits,
                cleanCheckInsInCycle = state.streakFreeze.cleanCheckInsTowardNextShield,
                requiredPerShield = state.streakFreeze.checkInsRequiredPerShield,
                isFreezeActiveToday = state.streakFreeze.isFreezeActiveToday,
                onActivateFreeze = { viewModel.onIntent(HabitsIntent.ActivateStreakFreeze) }
            )
        }

        // 1.5. Curva do Efeito Composto com Chama 3D (Pilar 6.5)
        item {
            val bestStreak = state.habits.maxOfOrNull { it.currentStreak } ?: 33
            BentoCard(glowAccent = LifeOsTheme.OrangeStreak) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        EyebrowCapsule(text = "EFEITO COMPOSTO · 1.8% AO DIA", accent = LifeOsTheme.OrangeStreak)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Aceleração Dia 1 → Dia 100", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                    LuxuryStatBadge(label = "Dia $bestStreak", accent = LifeOsTheme.GreenHealth)
                }
                Spacer(modifier = Modifier.height(8.dp))
                CompoundEffectFlameChart(activeStreakDays = bestStreak)
            }
        }

        // 1.7. Matriz GitHub Anual 7x52 (Pilar 6.1)
        state.habits.firstOrNull()?.takeIf { it.annualHeatmap52Weeks.isNotEmpty() }?.let { topHabit ->
            item {
                BentoCard(
                    glowAccent = LifeOsTheme.GreenHealth,
                    onClick = { viewModel.onIntent(HabitsIntent.SelectHabitDetail(topHabit.id)) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            EyebrowCapsule(text = "MATRIZ GITHUB 7×52 · 364 DIAS", accent = LifeOsTheme.GreenHealth)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = topHabit.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        LuxuryStatBadge(label = "120 FPS", accent = LifeOsTheme.GreenHealth)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    AnnualGitHubHeatmap7x52(
                        annualMatrix7x52 = topHabit.annualHeatmap52Weeks,
                        accentColor = LifeOsTheme.parseHexColorOrDefault(topHabit.hexColor)
                    )
                }
            }
        }

        // 2. Cards de Hábito com Orbes Vetoriais + 3 Pílulas de Métrica + Micro-Heatmap
        items(filteredHabits.size) { idx ->
            val habit = filteredHabits[idx]
            val accent = LifeOsTheme.parseHexColorOrDefault(habit.hexColor)
            val habitIcon = when {
                habit.iconKey == "water" -> Icons.Rounded.WaterDrop
                habit.category == "STUDY" -> Icons.Rounded.AutoStories
                habit.category == "FINANCE" -> Icons.Rounded.Savings
                else -> Icons.Rounded.FitnessCenter
            }

            BentoCard(
                glowAccent = if (habit.completedToday) accent else null,
                onClick = { viewModel.onIntent(HabitsIntent.SelectHabitDetail(habit.id)) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        GlowingIconOrb(
                            icon = habitIcon,
                            tint = accent,
                            size = 42.dp,
                            iconSize = 20.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(habit.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.5.sp)
                            Text(
                                text = habit.description.ifBlank { "Meta: ${habit.targetPerWeek} dias por semana" },
                                color = LifeOsTheme.TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.onIntent(HabitsIntent.ToggleToday(habit.id)) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (habit.completedToday) accent else LifeOsTheme.SurfaceElevated)
                            .border(1.dp, if (habit.completedToday) accent else LifeOsTheme.BorderSubtle, RoundedCornerShape(13.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Marcar hoje",
                            tint = if (habit.completedToday) Color.Black else LifeOsTheme.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniTopStatBox("${habit.completedThisWeek}/${habit.targetPerWeek} d", "Semana", Modifier.weight(1f))
                    MiniTopStatBox("${habit.goalProgressPercent}%", "Meta", Modifier.weight(1f))
                    MiniTopStatBox("${habit.currentStreak} dias", "Streak", Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                HabitMicroHeatmap(
                    activityMatrix = habit.heatmapMatrix,
                    accentColor = accent
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.onIntent(HabitsIntent.ToggleToday(habit.id)) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (habit.completedToday) accent else accent.copy(alpha = 0.90f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (habit.completedToday) "Concluído" else "Marcar Hoje",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    OutlinedButton(
                        onClick = { noteHabitTarget = habit },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.EditNote, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Note", color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }

    state.selectedHabit?.let { habit ->
        HabitDetailBottomSheet(
            habit = habit,
            onDismiss = { viewModel.onIntent(HabitsIntent.SelectHabitDetail(null)) }
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
private fun MiniTopStatBox(
    topValue: String,
    bottomLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(LifeOsTheme.SurfaceElevated)
            .border(1.dp, LifeOsTheme.BorderSubtle, RoundedCornerShape(13.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(topValue, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, maxLines = 1, softWrap = false)
        Text(bottomLabel, color = LifeOsTheme.TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun HabitDetailBottomSheet(
    habit: HabitDomainModel,
    onDismiss: () -> Unit
) {
    val accent = LifeOsTheme.parseHexColorOrDefault(habit.hexColor)
    val completedDayNumbers = remember(habit.recentRecords) {
        habit.recentRecords
            .mapNotNull { rec -> rec.formattedDate.takeWhile { it.isDigit() }.toIntOrNull() }
            .toSet()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LifeOsTheme.PureBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(habit.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(LifeOsTheme.SurfaceElevated)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Fechar", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(accent.copy(alpha = 0.14f))
                    .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                HabitMicroHeatmap(
                    activityMatrix = habit.heatmapMatrix,
                    accentColor = accent
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Jul 2026 — Nov 2026",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigHabitMetricTile(
                    title = "Streak Atual",
                    bigValue = "${habit.currentStreak}",
                    subtitle = "Recorde: ${habit.longestStreak} dias",
                    accent = accent,
                    modifier = Modifier.weight(1f)
                )
                BigHabitMetricTile(
                    title = "Total Check-Ins",
                    bigValue = "${habit.totalCheckIns}",
                    subtitle = "Acumulado no ano",
                    accent = accent,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigHabitMetricTile(
                    title = "Horário Médio",
                    bigValue = habit.avgLogTimeFormatted,
                    subtitle = "Janela circadiana",
                    accent = accent,
                    modifier = Modifier.weight(1f)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(LifeOsTheme.SurfaceDark)
                        .border(1.dp, LifeOsTheme.BorderSubtle, RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Text("Meta Semanal", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${habit.goalProgressPercent}%",
                        color = accent,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (habit.goalProgressPercent / 100f).coerceIn(0.06f, 1f) },
                        color = accent,
                        trackColor = LifeOsTheme.SurfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }

            BentoCard {
                Text("Novembro 2026", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom").forEach { d ->
                        Text(d, color = LifeOsTheme.TextMuted, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                for (week in 0 until 5) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (dow in 1..7) {
                            val dayNum = week * 7 + dow
                            val isActive = dayNum <= 30 && (dayNum in completedDayNumbers || dayNum % 3 != 0 && dayNum <= 20)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isActive) accent.copy(alpha = 0.24f) else Color.Transparent)
                                    .border(
                                        width = if (dayNum == 20) 1.5.dp else 0.dp,
                                        color = if (dayNum == 20) accent else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayNum <= 30) {
                                    Text(
                                        text = "$dayNum",
                                        color = if (isActive) Color.White else LifeOsTheme.TextMuted,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text("Histórico Recente", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
            habit.recentRecords.forEach { rec ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(LifeOsTheme.SurfaceDark)
                        .border(1.dp, LifeOsTheme.BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = rec.formattedDate + (rec.note?.let { " · $it" } ?: ""),
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Text(rec.formattedTime, color = LifeOsTheme.TextSecondary, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BigHabitMetricTile(
    title: String,
    bigValue: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(LifeOsTheme.SurfaceDark)
            .border(1.dp, LifeOsTheme.BorderSubtle, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(10.dp))
        Text(bigValue, color = accent, fontSize = 32.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(8.dp))
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
