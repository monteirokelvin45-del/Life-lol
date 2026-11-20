package com.lifeos.app.features.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.lifeos.core.domain.model.TimeOfDayContext
import com.lifeos.ui.components.BalanceWheel
import com.lifeos.ui.components.BentoCard
import com.lifeos.ui.components.HabitMicroHeatmap
import com.lifeos.ui.theme.LifeOsTheme
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPasteModal by remember { mutableStateOf(false) }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onIntent(HomeIntent.IngestDocumentUri(uri))
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LifeOsTheme.PureBlack),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Cabeçalho Contextual Baseado em Tempo (Pilar 1.2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.timeContext.badgeLabel,
                        color = LifeOsTheme.GreenHealth,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showPasteModal = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = LifeOsTheme.PurpleStudy,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Colar Plano", color = Color.White, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                documentPickerLauncher.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "text/*",
                                        "image/*"
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.GreenHealth),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.UploadFile,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF/DOCX/MD", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text(
                    text = state.timeContext.headline,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = state.timeContext.subtitle,
                    color = LifeOsTheme.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Chips de mutação temporal (permite auditar os 4 modos contextuais)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        TimeOfDayContext.MORNING_COGNITIVE to "Manhã (Foco)",
                        TimeOfDayContext.AFTERNOON_EXECUTION to "Tarde (Ritmo)",
                        TimeOfDayContext.LATE_AFTERNOON_PHYSIOLOGY to "17h–20h (Treino)",
                        TimeOfDayContext.NIGHT_CLOSURE_SLEEP to "Noite (Finanças)"
                    )
                    modes.forEach { (ctx, label) ->
                        FilterChip(
                            selected = state.timeContext == ctx,
                            onClick = { viewModel.onIntent(HomeIntent.SelectTimeContext(ctx)) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LifeOsTheme.SurfaceElevated,
                                selectedLabelColor = Color.White,
                                containerColor = LifeOsTheme.SurfaceDark,
                                labelColor = LifeOsTheme.TextSecondary
                            )
                        )
                    }
                }
            }
        }

        if (state.isIngesting) {
            item {
                BentoCard {
                    Text(
                        text = "Extraindo documento e persistindo transação atômica...",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = LifeOsTheme.GreenHealth
                    )
                }
            }
        }

        state.statusMessage?.let { msg ->
            item {
                BentoCard(borderColor = LifeOsTheme.GreenHealth) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(msg, color = LifeOsTheme.GreenHealth, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.onIntent(HomeIntent.DismissMessage) }) {
                            Text("OK", color = Color.White)
                        }
                    }
                }
            }
        }

        // Banner do Cronômetro Ativo em Primeiro Plano (Pilar 3)
        if (state.activeTimer.isRunning) {
            item {
                BentoCard(borderColor = LifeOsTheme.OrangeStreak) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = state.activeTimer.label,
                                color = LifeOsTheme.OrangeStreak,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = state.activeTimer.formattedRemaining,
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        OutlinedButton(onClick = { viewModel.onIntent(HomeIntent.StopActiveTimer) }) {
                            Text("Encerrar", color = Color.White)
                        }
                    }
                }
            }
        }

        // 2. Roda do Equilíbrio da Vida Polar (Pilar 6.2 + Pilar 8.3 + Referência 04)
        item {
            BentoCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Equilíbrio da Vida",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Últimos 14 dias",
                        color = LifeOsTheme.TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BalanceWheel(
                        healthScore = state.balanceWheel.healthFraction,
                        studyScore = state.balanceWheel.studyFraction,
                        financeScore = state.balanceWheel.financeFraction,
                        consistencyScore = state.balanceWheel.consistencyFraction
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DomainScoreRow("Corpo & Fisiologia (30%)", state.balanceWheel.bodyScore0to10, LifeOsTheme.GreenHealth)
                    DomainScoreRow("Foco & Ebbinghaus (25%)", state.balanceWheel.focusScore0to10, LifeOsTheme.PurpleStudy)
                    DomainScoreRow("Fluxo Financeiro (25%)", state.balanceWheel.financeScore0to10, LifeOsTheme.CyanFinance)
                    DomainScoreRow("Consistência Hábitos (20%)", state.balanceWheel.consistencyScore0to10, LifeOsTheme.OrangeStreak)
                }
            }
        }

        // 3. Card Prioritário Mutante pelo Horário do Dia
        item {
            when (state.timeContext) {
                TimeOfDayContext.MORNING_COGNITIVE -> MorningPriorityCard(
                    dueCount = state.dueStudies.size,
                    firstTopic = state.dueStudies.firstOrNull()?.let { "${it.subject}: ${it.topic}" },
                    activeBookTitle = state.activeBooks.firstOrNull()?.title,
                    onStartPomodoro = { viewModel.onIntent(HomeIntent.StartPomodoroFocus(25)) },
                    onCompleteTopReview = {
                        state.dueStudies.firstOrNull()?.let {
                            viewModel.onIntent(HomeIntent.ReviewTopic(it.id))
                        }
                    }
                )
                TimeOfDayContext.LATE_AFTERNOON_PHYSIOLOGY -> PhysiologyPriorityCard(
                    routineName = state.todayWorkout?.name ?: "Calistenia Regenerativa",
                    completedExercises = state.todayWorkout?.completedExercisesCount ?: 0,
                    totalExercises = state.todayWorkout?.exercises?.size ?: 0
                )
                TimeOfDayContext.NIGHT_CLOSURE_SLEEP -> NightFinancePriorityCard(
                    spentToday = state.financialOverview?.spentToday ?: 0.0,
                    netBalance = state.financialOverview?.netBalanceMonth ?: 0.0,
                    currency = state.financialOverview?.currencyCode ?: "MZN"
                )
                TimeOfDayContext.AFTERNOON_EXECUTION -> AfternoonExecutionCard(
                    completedToday = state.habits.count { it.completedToday },
                    totalHabits = state.habits.size
                )
            }
        }

        // 4. Micro-Heatmaps Rápidos dos Hábitos Principais (Referência 09)
        items(state.habits.size) { index ->
            val habit = state.habits[index]
            val accent = LifeOsTheme.parseHexColorOrDefault(habit.hexColor)
            BentoCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = habit.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "🔥 ${habit.currentStreak} dias · ${habit.completedThisWeek}/${habit.targetPerWeek} nesta semana",
                            color = LifeOsTheme.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(
                        onClick = { viewModel.onIntent(HomeIntent.ToggleHabit(habit.id)) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (habit.completedToday) accent else LifeOsTheme.SurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Check-in",
                            tint = if (habit.completedToday) Color.Black else LifeOsTheme.TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                HabitMicroHeatmap(
                    activityMatrix = habit.heatmapMatrix,
                    accentColor = accent
                )
            }
        }
    }

    if (showPasteModal) {
        PasteChaosPlanDialog(
            onDismiss = { showPasteModal = false },
            onConfirm = { rawText ->
                showPasteModal = false
                viewModel.onIntent(HomeIntent.IngestRawText(rawText))
            }
        )
    }
}

@Composable
private fun DomainScoreRow(label: String, score: Float, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LifeOsTheme.SurfaceElevated)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, color = Color.White, fontSize = 13.sp)
        }
        Text(
            text = String.format(Locale.US, "%.1f / 10", score),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun MorningPriorityCard(
    dueCount: Int,
    firstTopic: String?,
    activeBookTitle: String?,
    onStartPomodoro: () -> Unit,
    onCompleteTopReview: () -> Unit
) {
    BentoCard(borderColor = LifeOsTheme.PurpleStudy.copy(alpha = 0.45f)) {
        Text("FOCO DA MANHÃ · EBBINGHAUS", color = LifeOsTheme.PurpleStudy, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (dueCount > 0) "$dueCount revisões críticas para hoje" else "Curva de memória em dia!",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        if (firstTopic != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Próximo: $firstTopic", color = LifeOsTheme.TextSecondary, fontSize = 13.sp)
        }
        if (activeBookTitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Leitura ativa: $activeBookTitle", color = LifeOsTheme.GreenHealth, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onStartPomodoro,
                colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.PurpleStudy)
            ) {
                Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pomodoro 25m")
            }
            if (dueCount > 0) {
                OutlinedButton(onClick = onCompleteTopReview) {
                    Text("Concluir 1ª Revisão", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun PhysiologyPriorityCard(
    routineName: String,
    completedExercises: Int,
    totalExercises: Int
) {
    BentoCard(borderColor = LifeOsTheme.GreenHealth.copy(alpha = 0.45f)) {
        Text("JANELA FISIOLÓGICA · 17H–20H", color = LifeOsTheme.GreenHealth, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(6.dp))
        Text(routineName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$completedExercises de $totalExercises exercícios concluídos hoje",
            color = LifeOsTheme.TextSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun NightFinancePriorityCard(
    spentToday: Double,
    netBalance: Double,
    currency: String
) {
    BentoCard(borderColor = LifeOsTheme.CyanFinance.copy(alpha = 0.45f)) {
        Text("FECHAMENTO NOTURNO · 20H–23H", color = LifeOsTheme.CyanFinance, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = String.format(Locale.US, "Gasto hoje: %.2f %s", spentToday, currency),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = String.format(Locale.US, "Saldo líquido do mês: %.2f %s", netBalance, currency),
            color = LifeOsTheme.TextSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun AfternoonExecutionCard(
    completedToday: Int,
    totalHabits: Int
) {
    BentoCard {
        Text("EXECUÇÃO DA TARDE", color = LifeOsTheme.OrangeStreak, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "$completedToday de $totalHabits hábitos marcados hoje",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun PasteChaosPlanDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember {
        mutableStateOf(
            """
            # Segunda - Calistenia Superior
            - Barra Fixa 4x10 (90s)
            - Paralelas 4x12 (90s)
            Hábito: Hidratação 3L de água
            Estudo: Kotlin Android - Jetpack Compose Canvas 120fps
            Despesa: Alimentação - 650
            """.trimIndent()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LifeOsTheme.SurfaceDark,
        title = { Text("Ingestão Fricção Zero (IA)", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Cole anotações caóticas de treino, matérias de estudo, hábitos ou gastos:",
                    color = LifeOsTheme.TextSecondary,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.GreenHealth)
            ) {
                Text("Extrair & Salvar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = LifeOsTheme.TextSecondary)
            }
        }
    )
}
