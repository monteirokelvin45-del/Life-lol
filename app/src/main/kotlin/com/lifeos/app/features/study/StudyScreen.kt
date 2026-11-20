@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lifeos.app.features.study

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.core.domain.algorithm.EbbinghausScheduler
import com.lifeos.core.domain.model.BookDomainModel
import com.lifeos.ui.components.BentoCard
import com.lifeos.ui.components.EbbinghausSawtoothCurveChart
import com.lifeos.ui.components.KnowledgeBurndownConeChart
import com.lifeos.ui.components.SmoothMetricSplineChart
import com.lifeos.ui.theme.LifeOsTheme
import java.util.Locale

@Composable
fun StudyScreen(viewModel: StudyViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddTopicDialog by remember { mutableStateOf(false) }
    var stoppingBook by remember { mutableStateOf<BookDomainModel?>(null) }

    val cameraOcrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.onIntent(StudyIntent.ScanBookPageBitmap(bitmap))
        }
    }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "07:00 · REVISÃO ESPAÇADA & LEITURA ATIVA",
                        color = LifeOsTheme.PurpleStudy,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "Estudos & Biblioteca",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showAddTopicDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.PurpleStudy),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tópico", maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 0.5. Analytics · Curva Suave de Retenção & Qualidade Cognitiva (Referência 02)
        item {
            val avgRetention = if (state.allTopics.isNotEmpty()) {
                state.allTopics.map { it.estimatedRetentionPercent }.average().toInt()
            } else {
                86
            }
            BentoCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Curva Spline · Haptic Scrubber", color = LifeOsTheme.TextSecondary, fontSize = 11.sp)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$avgRetention%", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Arraste para tátil ↑", color = LifeOsTheme.GreenHealth, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("—● Atual", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("— Anterior", color = LifeOsTheme.TextMuted, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                SmoothMetricSplineChart(
                    currentPoints = listOf(62f, 88f, 74f, 48f, avgRetention.toFloat()),
                    previousPoints = listOf(50f, 54f, 68f, 73f, 78f),
                    lineColor = Color.White
                )
            }
        }

        // 0.7. Curva Dente-de-Serra de Ebbinghaus (Pilar 6.9)
        item {
            val highestStage = state.allTopics.maxOfOrNull { it.stage } ?: 2
            BentoCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("CURVA DENTE-DE-SERRA DE EBBINGHAUS", color = LifeOsTheme.PurpleStudy, style = MaterialTheme.typography.labelSmall)
                        Text("Saltos de Restauração a 100%", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                    Text("-- Sem revisão", color = LifeOsTheme.RedExpense, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                EbbinghausSawtoothCurveChart(activeStageIndex = highestStage)
            }
        }

        // 0.9. Burndown de Conhecimento com Cone de Incerteza (Pilar 6.13)
        item {
            val bd = state.burndownProjection
            BentoCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("BURNDOWN · CONE DE INCERTEZA", color = LifeOsTheme.CyanFinance, style = MaterialTheme.typography.labelSmall)
                        Text("${bd.remainingUnitsToday} de ${bd.totalTargetUnits} pág. restantes", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                    Text(
                        text = "${bd.daysOptimistic}d – ${bd.daysConservative}d",
                        color = LifeOsTheme.GreenHealth,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                KnowledgeBurndownConeChart(
                    historicalRemaining = bd.historicalRemaining,
                    optimisticCone = bd.optimisticConeRemaining,
                    realisticCone = bd.realisticConeRemaining,
                    conservativeCone = bd.conservativeConeRemaining
                )
            }
        }

        // 1. Fila de Revisões Ebbinghaus [1, 3, 7, 16, 30 dias]
        item {
            BentoCard(borderColor = LifeOsTheme.PurpleStudy.copy(alpha = 0.5f)) {
                Text(
                    text = "Pendentes Hoje · Curva [1, 3, 7, 16, 30 dias]",
                    color = LifeOsTheme.PurpleStudy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (state.dueTopics.isEmpty()) {
                    Text(
                        text = "Zero pendências! Próximos alarmes exatos programados às 07:00.",
                        color = LifeOsTheme.TextSecondary,
                        fontSize = 13.sp
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.dueTopics.forEach { topic ->
                            val interval = EbbinghausScheduler.STAGE_INTERVALS.getOrElse(topic.stage) { 1 }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(LifeOsTheme.SurfaceElevated)
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(topic.subject, color = LifeOsTheme.PurpleStudy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Estágio ${topic.stage + 1}/5 (${interval}d) · Retenção ${topic.estimatedRetentionPercent}%",
                                        color = LifeOsTheme.TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(topic.topic, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.onIntent(StudyIntent.ReviewTopic(topic.id, rememberedWell = true)) },
                                        colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.GreenHealth)
                                    ) {
                                        Text("Dominei (+Avançar)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.onIntent(StudyIntent.ReviewTopic(topic.id, rememberedWell = false)) }
                                    ) {
                                        Text("Esqueci (Reset 1d)", color = LifeOsTheme.RedExpense, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Modo Leitura Ativa (Cronômetro Play/Stop + OCR Câmera do Livro Físico)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Modo Leitura Ativa",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedButton(
                    onClick = { runCatching { cameraOcrLauncher.launch(null) } },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = "Escanear Frase OCR",
                        tint = LifeOsTheme.GreenHealth,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Escanear Frase (OCR)", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        items(state.activeBooks.size) { index ->
            val book = state.activeBooks[index]
            val isTimerActiveForThisBook = state.readingTimer.activeBookId == book.id
            val mins = state.readingTimer.elapsedSeconds / 60
            val secs = state.readingTimer.elapsedSeconds % 60

            BentoCard(
                borderColor = if (isTimerActiveForThisBook) LifeOsTheme.GreenHealth else LifeOsTheme.BorderSubtle
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(book.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(book.author, color = LifeOsTheme.TextSecondary, fontSize = 12.sp)
                    }
                    Text(
                        text = "${book.currentPage}/${book.totalPages} pág",
                        color = LifeOsTheme.GreenHealth,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(book.morningTargetRangeLabel, color = Color.White, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { book.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = LifeOsTheme.GreenHealth,
                    trackColor = LifeOsTheme.SurfaceElevated
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = book.projectedFinishSummary,
                    color = LifeOsTheme.TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isTimerActiveForThisBook) {
                        Text(
                            text = String.format(Locale.US, "⏱ Lendo agora: %02d:%02d", mins, secs),
                            color = LifeOsTheme.GreenHealth,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Button(
                            onClick = { stoppingBook = book },
                            colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.OrangeStreak)
                        ) {
                            Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Parar & Informar Pág.", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.onIntent(StudyIntent.StartReadingTimer(book.id)) },
                            colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.GreenHealth)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play Leitura Ativa", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = { stoppingBook = book }) {
                            Text("+ Pág. Manual", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 3. Diário de Frases Escaneadas dos Livros Físicos (OCR)
        item {
            BentoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.FormatQuote, contentDescription = null, tint = LifeOsTheme.PurpleStudy)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Diário de Frases Capturadas (OCR)", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (state.quotes.isEmpty()) {
                    Text(
                        text = "Aponte a câmera para uma página do livro físico para extrair e guardar trechos marcantes.",
                        color = LifeOsTheme.TextSecondary,
                        fontSize = 12.sp
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.quotes.forEach { q ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LifeOsTheme.SurfaceElevated)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "“${q.quoteText}”",
                                    color = Color.White,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${q.bookTitle} · Pág. ${q.pageNumber} · ${q.formattedDate}",
                                    color = LifeOsTheme.PurpleStudy,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    stoppingBook?.let { book ->
        StopReadingPageDialog(
            book = book,
            wasStopwatchRunning = state.readingTimer.activeBookId == book.id,
            onDismiss = { stoppingBook = null },
            onConfirm = { newPage, manualMinutes ->
                if (state.readingTimer.activeBookId == book.id) {
                    viewModel.onIntent(StudyIntent.FinishReadingTimer(book.id, newPage))
                } else {
                    viewModel.onIntent(StudyIntent.LogReading(book.id, newPage, manualMinutes))
                }
                stoppingBook = null
            }
        )
    }

    state.scannedQuoteDraft?.let { draft ->
        val defaultBook = state.activeBooks.firstOrNull()
        SaveOcrQuoteDialog(
            initialText = draft,
            defaultPage = defaultBook?.currentPage ?: 1,
            onDismiss = { viewModel.onIntent(StudyIntent.DismissScannedDraft) },
            onSave = { finalQuote, pageNum ->
                viewModel.onIntent(
                    StudyIntent.SaveScannedQuote(
                        bookId = defaultBook?.id ?: 1L,
                        quoteText = finalQuote,
                        pageNumber = pageNum
                    )
                )
            }
        )
    }

    if (showAddTopicDialog) {
        AddStudyTopicDialog(
            onDismiss = { showAddTopicDialog = false },
            onConfirm = { subject, topic ->
                viewModel.onIntent(StudyIntent.AddTopic(subject, topic))
                showAddTopicDialog = false
            }
        )
    }
}

@Composable
private fun StopReadingPageDialog(
    book: BookDomainModel,
    wasStopwatchRunning: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var pageInput by remember { mutableStateOf((book.currentPage + book.dailyGoalPages).coerceAtMost(book.totalPages).toString()) }
    var minutesInput by remember { mutableStateOf("25") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LifeOsTheme.SurfaceDark,
        title = { Text("Em qual página você parou?", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { pageInput = it },
                    label = { Text("Página atual (de ${book.totalPages})") }
                )
                if (!wasStopwatchRunning) {
                    OutlinedTextField(
                        value = minutesInput,
                        onValueChange = { minutesInput = it },
                        label = { Text("Duração da leitura (minutos)") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = pageInput.toIntOrNull() ?: book.currentPage
                    val m = minutesInput.toIntOrNull() ?: 25
                    onConfirm(p, m)
                },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.GreenHealth)
            ) {
                Text("Calcular Ritmo & Salvar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = LifeOsTheme.TextSecondary) }
        }
    )
}

@Composable
private fun SaveOcrQuoteDialog(
    initialText: String,
    defaultPage: Int,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var quoteText by remember { mutableStateOf(initialText) }
    var pageText by remember { mutableStateOf(defaultPage.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LifeOsTheme.SurfaceDark,
        title = { Text("Frase Extraída via OCR", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = quoteText,
                    onValueChange = { quoteText = it },
                    label = { Text("Trecho do livro") },
                    minLines = 3
                )
                OutlinedTextField(
                    value = pageText,
                    onValueChange = { pageText = it },
                    label = { Text("Número da página") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(quoteText, pageText.toIntOrNull() ?: defaultPage) },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.PurpleStudy)
            ) {
                Text("Guardar no Diário", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Descartar", color = LifeOsTheme.TextSecondary) }
        }
    )
}

@Composable
private fun AddStudyTopicDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LifeOsTheme.SurfaceDark,
        title = { Text("Agendar Tópico Ebbinghaus", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Disciplina (Ex: Cálculo Integral)") }
                )
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Tópico (Ex: Teorema Fundamental)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subject.isNotBlank() && topic.isNotBlank()) {
                        onConfirm(subject, topic)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.PurpleStudy)
            ) {
                Text("Agendar Alarme 07:00")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = LifeOsTheme.TextSecondary) }
        }
    )
}
