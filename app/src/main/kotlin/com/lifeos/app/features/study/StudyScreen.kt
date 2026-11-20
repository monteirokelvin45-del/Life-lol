package com.lifeos.app.features.study

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.core.domain.algorithm.EbbinghausScheduler
import com.lifeos.core.domain.model.BookDomainModel
import com.lifeos.ui.components.BentoCard
import com.lifeos.ui.components.SmoothMetricSplineChart
import com.lifeos.ui.theme.LifeOsTheme
import java.util.Locale

@Composable
fun StudyScreen(
    viewModel: StudyViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddTopicModal by remember { mutableStateOf(false) }
    var readingBookTarget by remember { mutableStateOf<BookDomainModel?>(null) }

    val retentionCurve = remember(state.allTopics) {
        if (state.allTopics.size >= 2) {
            state.allTopics.map { it.estimatedRetentionPercent.toFloat() }
        } else {
            listOf(100f, 82f, 95f, 88f, 97f)
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
                Column {
                    Text("Estudos & Leitura", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "Curva de Ebbinghaus (1d · 3d · 7d · 16d · 30d)",
                        color = LifeOsTheme.TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = { showAddTopicModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.PurpleStudy)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tópico")
                }
            }
        }

        // Curva Analítica de Retenção Sináptica (Referência 02)
        item {
            BentoCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Retenção Mnemônica Estimada", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        text = "R = e^(-t/S)",
                        color = LifeOsTheme.PurpleStudy,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                SmoothMetricSplineChart(
                    currentPoints = retentionCurve,
                    previousPoints = listOf(100f, 65f, 45f, 32f, 20f),
                    lineColor = LifeOsTheme.PurpleStudy
                )
            }
        }

        // Fila de Revisão Crítica de Hoje
        item {
            Text(
                text = "Revisões Vencidas (${state.dueTopics.size})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        if (state.dueTopics.isEmpty()) {
            item {
                BentoCard {
                    Text("Zero decaimento pendente hoje!", color = LifeOsTheme.GreenHealth, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Todos os tópicos estão consolidados com alarmes exatos programados.",
                        color = LifeOsTheme.TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(state.dueTopics.size) { idx ->
                val item = state.dueTopics[idx]
                val intervalDays = EbbinghausScheduler.STAGE_INTERVALS.getOrElse(item.stage) { 1 }
                BentoCard(borderColor = LifeOsTheme.PurpleStudy.copy(alpha = 0.45f)) {
                    Text(item.subject.uppercase(Locale.ROOT), color = LifeOsTheme.PurpleStudy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(item.topic, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Estágio ${item.stage + 1}/5 (${intervalDays}d) · Retenção estimada: ${item.estimatedRetentionPercent}%",
                        color = LifeOsTheme.TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.onIntent(StudyIntent.ReviewTopic(item.id, rememberedWell = true)) },
                            colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.GreenHealth)
                        ) {
                            Text("Lembrei Bem → Avançar", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { viewModel.onIntent(StudyIntent.ReviewTopic(item.id, rememberedWell = false)) }
                        ) {
                            Text("Revisar em 1d", color = Color.White)
                        }
                    }
                }
            }
        }

        // Domínio Leitura
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Leitura Ativa & Projeção", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        items(state.activeBooks.size) { idx ->
            val book = state.activeBooks[idx]
            BentoCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(book.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        if (book.author.isNotBlank()) {
                            Text(book.author, color = LifeOsTheme.TextSecondary, fontSize = 12.sp)
                        }
                    }
                    OutlinedButton(onClick = { readingBookTarget = book }) {
                        Text("+ Páginas", color = Color.White, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { book.progressFraction },
                    color = LifeOsTheme.CyanFinance,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pág. ${book.currentPage}/${book.totalPages} (${(book.progressFraction * 100).toInt()}%)",
                        color = LifeOsTheme.TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = String.format(
                            Locale.US,
                            "%.0f pág/h · ~%.1fh restantes",
                            book.pagesPerHour,
                            book.estimatedHoursRemaining
                        ),
                        color = LifeOsTheme.CyanFinance,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showAddTopicModal) {
        var subject by remember { mutableStateOf("") }
        var topic by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddTopicModal = false },
            containerColor = LifeOsTheme.SurfaceDark,
            title = { Text("Novo Tópico Ebbinghaus", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Disciplina (ex: Kotlin)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        label = { Text("Tópico específico") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subject.isNotBlank() && topic.isNotBlank()) {
                            viewModel.onIntent(StudyIntent.AddTopic(subject, topic))
                            showAddTopicModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.PurpleStudy)
                ) {
                    Text("Agendar 1d")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTopicModal = false }) {
                    Text("Cancelar", color = LifeOsTheme.TextSecondary)
                }
            }
        )
    }

    readingBookTarget?.let { target ->
        var pageInput by remember { mutableStateOf((target.currentPage + 15).coerceAtMost(target.totalPages).toString()) }
        var minutesInput by remember { mutableStateOf("30") }
        AlertDialog(
            onDismissRequest = { readingBookTarget = null },
            containerColor = LifeOsTheme.SurfaceDark,
            title = { Text("Registrar Leitura · ${target.title}", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { pageInput = it },
                        label = { Text("Página atual alcançada") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = minutesInput,
                        onValueChange = { minutesInput = it },
                        label = { Text("Duração da sessão (minutos)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = pageInput.toIntOrNull() ?: target.currentPage
                        val m = minutesInput.toIntOrNull() ?: 30
                        viewModel.onIntent(StudyIntent.LogReading(target.id, p, m))
                        readingBookTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.CyanFinance)
                ) {
                    Text("Atualizar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { readingBookTarget = null }) {
                    Text("Cancelar", color = LifeOsTheme.TextSecondary)
                }
            }
        )
    }
}
