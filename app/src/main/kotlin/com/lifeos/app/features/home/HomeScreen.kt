@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lifeos.app.features.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
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
import androidx.compose.material.icons.rounded.Mic
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
import androidx.compose.runtime.LaunchedEffect
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
import com.lifeos.core.domain.model.AiExtractedPlanPayload
import com.lifeos.core.domain.model.BookDomainModel
import com.lifeos.core.domain.model.TimeOfDayContext
import com.lifeos.ui.components.BalanceWheel
import com.lifeos.ui.components.BentoCard
import com.lifeos.ui.components.HabitMicroHeatmap
import com.lifeos.ui.components.StreakWeekSpotlightBanner
import com.lifeos.ui.theme.LifeOsTheme
import kotlin.math.roundToInt
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    incomingSharedUri: Uri? = null,
    incomingSharedText: String? = null,
    onConsumedSharedPayload: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPortalModal by remember { mutableStateOf(false) }

    LaunchedEffect(incomingSharedUri, incomingSharedText) {
        when {
            incomingSharedUri != null -> {
                viewModel.onIntent(HomeIntent.PreviewDocumentUri(incomingSharedUri))
                onConsumedSharedPayload()
            }
            !incomingSharedText.isNullOrBlank() -> {
                viewModel.onIntent(HomeIntent.PreviewRawText(incomingSharedText))
                onConsumedSharedPayload()
            }
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onIntent(HomeIntent.PreviewDocumentUri(uri))
        }
    }

    val voicePortalLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.onIntent(HomeIntent.PreviewRawText(spoken))
            }
        }
    }

    val topStreak = state.habits.maxOfOrNull { it.currentStreak } ?: 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LifeOsTheme.PureBlack),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Cabeçalho + Portal Mágico de IA (Voz / PDF / DOCX / WhatsApp)
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
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                    )
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                                    putExtra(
                                        RecognizerIntent.EXTRA_PROMPT,
                                        "Diga suas metas de treino, estudo e gastos..."
                                    )
                                }
                                runCatching { voicePortalLauncher.launch(speechIntent) }
                                    .onFailure { showPortalModal = true }
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(LifeOsTheme.SurfaceElevated)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = "Portal por Voz",
                                tint = LifeOsTheme.OrangeStreak,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        OutlinedButton(
                            onClick = { showPortalModal = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = LifeOsTheme.PurpleStudy,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Portal IA", color = Color.White, fontSize = 12.sp)
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
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.UploadFile,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF/DOCX", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        TimeOfDayContext.MORNING_COGNITIVE to "07h Manhã",
                        TimeOfDayContext.AFTERNOON_EXECUTION to "14h Tarde",
                        TimeOfDayContext.LATE_AFTERNOON_PHYSIOLOGY to "18h Treino",
                        TimeOfDayContext.NIGHT_CLOSURE_SLEEP to "22h Fechamento"
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
                        text = "Portal Mágico lendo seu arquivo caótico em <5s...",
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
                        Text(msg, color = LifeOsTheme.GreenHealth, modifier = Modifier.weight(1f), fontSize = 13.sp)
                        TextButton(onClick = { viewModel.onIntent(HomeIntent.DismissMessage) }) {
                            Text("OK", color = Color.White)
                        }
                    }
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

        // 2. Card Contextual da Jornada de 24 Horas
        item {
            when (state.timeContext) {
                TimeOfDayContext.MORNING_COGNITIVE -> MorningPriorityCard(
                    dueCount = state.dueStudies.size,
                    firstTopic = state.dueStudies.firstOrNull()?.let { "${it.subject} (Revisão Ebbinghaus)" },
                    activeBook = state.activeBooks.firstOrNull(),
                    onStartPomodoro25 = { viewModel.onIntent(HomeIntent.StartPomodoroFocus(25)) },
                    onStartPomodoro50 = { viewModel.onIntent(HomeIntent.StartPomodoroFocus(50)) },
                    onCompleteTopReview = {
                        state.dueStudies.firstOrNull()?.let {
                            viewModel.onIntent(HomeIntent.ReviewTopic(it.id))
                        }
                    }
                )
                TimeOfDayContext.AFTERNOON_EXECUTION -> AfternoonExecutionCard(
                    completedToday = state.habits.count { it.completedToday },
                    totalHabits = state.habits.size,
                    correlationInsight = state.financialOverview?.behavioralCorrelationInsight.orEmpty(),
                    onStartPomodoro25 = { viewModel.onIntent(HomeIntent.StartPomodoroFocus(25)) },
                    onStartPomodoro50 = { viewModel.onIntent(HomeIntent.StartPomodoroFocus(50)) }
                )
                TimeOfDayContext.LATE_AFTERNOON_PHYSIOLOGY -> PhysiologyPriorityCard(
                    routineName = state.todayWorkout?.name ?: "Costas & Bíceps",
                    completedExercises = state.todayWorkout?.completedExercisesCount ?: 0,
                    totalExercises = state.todayWorkout?.exercises?.size ?: 0
                )
                TimeOfDayContext.NIGHT_CLOSURE_SLEEP -> NightClosureBannerCard(
                    topStreak = topStreak,
                    overallScore = state.balanceWheel.overallScore0to10,
                    spentToday = state.financialOverview?.spentToday ?: 0.0,
                    currency = state.financialOverview?.currencyCode ?: "MZN"
                )
            }
        }

        // 2.5. Holofote de Streak & Consistência Semanal (Referências 06 & 07)
        item {
            val firstHabit = state.habits.firstOrNull()
            val lastColFlags = (0..6).map { rowIdx ->
                val row = firstHabit?.heatmapMatrix?.getOrNull(rowIdx)
                (row?.lastOrNull() ?: 0f) > 0f
            }
            val pct = if (state.habits.isNotEmpty()) {
                state.habits.map { it.goalProgressPercent }.average().roundToInt()
            } else {
                68
            }
            StreakWeekSpotlightBanner(
                streakDays = topStreak,
                weekCompletedFlags = lastColFlags,
                progressPercent = pct
            )
        }

        // 3. A Roda do Equilíbrio da Vida (Referência 04 "Баланс")
        item {
            BentoCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Balanço · 30 dias",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "✨ Planejamento IA",
                        color = LifeOsTheme.AmberFocus,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

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

                val totalCheckIns = state.habits.sumOf { it.totalCheckIns }
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    DomainScoreRow(
                        iconGlyph = "💼",
                        label = "Carreira & Finanças",
                        subStats = "⏱ 30:30h   ✓ 15   ↻ 25%",
                        score = state.balanceWheel.financeScore0to10,
                        color = LifeOsTheme.CyanFinance
                    )
                    DomainScoreRow(
                        iconGlyph = "➕",
                        label = "Saúde & Corpo",
                        subStats = "⏱ 6:40h   ✓ ${state.todayWorkout?.completedExercisesCount ?: 2}   ↻ 30%",
                        score = state.balanceWheel.bodyScore0to10,
                        color = LifeOsTheme.GreenHealth
                    )
                    DomainScoreRow(
                        iconGlyph = "🧠",
                        label = "Estudos & Leitura",
                        subStats = "⏱ 10:45h   ✓ ${state.dueStudies.size}   ↻ 25%",
                        score = state.balanceWheel.focusScore0to10,
                        color = LifeOsTheme.PurpleStudy
                    )
                    DomainScoreRow(
                        iconGlyph = "🔥",
                        label = "Consistência & Hábitos",
                        subStats = "⏱ 14d   ✓ $totalCheckIns   ↻ $topStreak dias",
                        score = state.balanceWheel.consistencyScore0to10,
                        color = LifeOsTheme.OrangeStreak
                    )
                }
            }
        }

        // 4. Micro-Heatmaps dos Hábitos Ativos
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
                            text = "🔥 ${habit.currentStreak} dias mantidos · ${habit.completedThisWeek}/${habit.targetPerWeek} na semana",
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

    if (showPortalModal) {
        PortalMagicoInputDialog(
            onDismiss = { showPortalModal = false },
            onAnalyze = { rawText ->
                showPortalModal = false
                viewModel.onIntent(HomeIntent.PreviewRawText(rawText))
            }
        )
    }

    state.pendingPlanPreview?.let { preview ->
        ConfirmSistemaPreviewDialog(
            plan = preview,
            onDismiss = { viewModel.onIntent(HomeIntent.CancelPlanPreview) },
            onConfirmSystem = { viewModel.onIntent(HomeIntent.ConfirmExtractedSystem) }
        )
    }
}

@Composable
private fun DomainScoreRow(
    iconGlyph: String,
    label: String,
    subStats: String,
    score: Float,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LifeOsTheme.SurfaceElevated)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(iconGlyph, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subStats, color = LifeOsTheme.TextSecondary, fontSize = 11.sp)
            }
        }
        Text(
            text = "${(score * 10f).roundToInt()}%  ›",
            color = LifeOsTheme.GreenHealth,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun MorningPriorityCard(
    dueCount: Int,
    firstTopic: String?,
    activeBook: BookDomainModel?,
    onStartPomodoro25: () -> Unit,
    onStartPomodoro50: () -> Unit,
    onCompleteTopReview: () -> Unit
) {
    BentoCard(borderColor = LifeOsTheme.PurpleStudy.copy(alpha = 0.55f)) {
        Text("🌅 07:00 · DESPERTAR & FOCO INTELECTUAL", color = LifeOsTheme.PurpleStudy, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (firstTopic != null) "Hoje você precisa revisar $firstTopic" else "Curva de Ebbinghaus 100% em dia!",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )
        activeBook?.let { book ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Livro atual: '${book.title}'. ${book.morningTargetRangeLabel}",
                color = LifeOsTheme.GreenHealth,
                fontSize = 13.sp
            )
            Text(
                text = book.projectedFinishSummary,
                color = LifeOsTheme.TextSecondary,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onStartPomodoro25,
                colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.PurpleStudy)
            ) {
                Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("25m", fontSize = 12.sp)
            }
            OutlinedButton(onClick = onStartPomodoro50) {
                Text("50m Foco", color = Color.White, fontSize = 12.sp)
            }
            if (dueCount > 0) {
                OutlinedButton(onClick = onCompleteTopReview) {
                    Text("✓ Revisado", color = LifeOsTheme.GreenHealth, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AfternoonExecutionCard(
    completedToday: Int,
    totalHabits: Int,
    correlationInsight: String,
    onStartPomodoro25: () -> Unit,
    onStartPomodoro50: () -> Unit
) {
    BentoCard(borderColor = LifeOsTheme.CyanFinance.copy(alpha = 0.45f)) {
        Text("☀️ 14:00 · GESTÃO DE GASTOS & PRODUTIVIDADE", color = LifeOsTheme.CyanFinance, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "$completedToday de $totalHabits rituais concluídos hoje",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        if (correlationInsight.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "💡 $correlationInsight",
                color = LifeOsTheme.AmberFocus,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onStartPomodoro25) {
                Text("Pomodoro 25m", color = Color.White, fontSize = 12.sp)
            }
            OutlinedButton(onClick = onStartPomodoro50) {
                Text("Pomodoro 50m", color = Color.White, fontSize = 12.sp)
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
    BentoCard(borderColor = LifeOsTheme.GreenHealth.copy(alpha = 0.6f)) {
        Text("🏋️ 18:00 · MODO FISIOLOGIA & TREINO", color = LifeOsTheme.GreenHealth, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(6.dp))
        Text("TREINO DE HOJE: ${routineName.uppercase(Locale.ROOT)}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$completedExercises de $totalExercises exercícios concluídos · Cronômetro de 90s com vibração dupla ativo na aba Treino",
            color = LifeOsTheme.TextSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun NightClosureBannerCard(
    topStreak: Int,
    overallScore: Float,
    spentToday: Double,
    currency: String
) {
    BentoCard(borderColor = LifeOsTheme.OrangeStreak.copy(alpha = 0.7f)) {
        Text("🌙 22:00 · FECHAMENTO DO DIA & RODA DA VIDA", color = LifeOsTheme.OrangeStreak, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = String.format(Locale.US, "Score do Dia: %.1f / 10 · 🔥 Streak de %d Dias Mantido!", overallScore, topStreak),
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = String.format(Locale.US, "Saídas hoje: %.2f %s · Micro-Heatmaps acesos para preservar sua sequência.", spentToday, currency),
            color = LifeOsTheme.TextSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun PortalMagicoInputDialog(
    onDismiss: () -> Unit,
    onAnalyze: (String) -> Unit
) {
    var text by remember {
        mutableStateOf(
            """
            Minha meta esse mês é gastar menos de R$ 500 em comida fora, treinar costas na sexta e ler 20 páginas por dia.
            Estudo: Cálculo Integral - Revisão de Integrais Improprias
            Despesa: Alimentação - 38
            """.trimIndent()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LifeOsTheme.SurfaceDark,
        title = { Text("✨ Portal Mágico de IA", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Cole qualquer ficha caótica, edital, lista de contas ou frase falada. A IA montará o sistema inteiro em segundos:",
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
                onClick = { onAnalyze(text) },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.PurpleStudy)
            ) {
                Text("Analisar Documento →", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = LifeOsTheme.TextSecondary)
            }
        }
    )
}

@Composable
private fun ConfirmSistemaPreviewDialog(
    plan: AiExtractedPlanPayload,
    onDismiss: () -> Unit,
    onConfirmSystem: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LifeOsTheme.SurfaceDark,
        title = { Text("Sistema Estruturado pela IA", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Revise os módulos extraídos antes de configurar alarmes, treinos e orçamento:",
                    color = LifeOsTheme.TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = "⚡ Hábitos detectados: ${plan.detectedHabits.size}",
                    color = LifeOsTheme.GreenHealth,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                plan.detectedHabits.take(3).forEach { h ->
                    Text("  • ${h.name}", color = Color.White, fontSize = 12.sp)
                }

                Text(
                    text = "🏋️ Rotinas de Treino: ${plan.detectedWorkouts.size}",
                    color = LifeOsTheme.OrangeStreak,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                plan.detectedWorkouts.take(2).forEach { w ->
                    Text("  • ${w.name} (${w.exercises.size} exercícios)", color = Color.White, fontSize = 12.sp)
                }

                Text(
                    text = "🧠 Estudos Ebbinghaus (Alarme 07:00): ${plan.detectedStudies.size}",
                    color = LifeOsTheme.PurpleStudy,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                plan.detectedStudies.take(3).forEach { s ->
                    Text("  • ${s.subject}: ${s.topic}", color = Color.White, fontSize = 12.sp)
                }

                Text(
                    text = "💰 Registros Financeiros: ${plan.detectedFinances.size}",
                    color = LifeOsTheme.CyanFinance,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmSystem,
                colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.GreenHealth)
            ) {
                Text("Confirmar Sistema", color = Color.Black, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Descartar", color = LifeOsTheme.TextSecondary)
            }
        }
    )
}
