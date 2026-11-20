@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lifeos.app.features.finance

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.lifeos.ui.components.BentoCard
import com.lifeos.ui.components.SegmentedCategoryBar
import com.lifeos.ui.components.SpendingBurnMatrix
import com.lifeos.ui.theme.LifeOsTheme
import java.util.Locale

private data class CategoryLegendRow(
    val label: String,
    val amountFormatted: String,
    val fraction: Float,
    val color: Color
)

private val categoryPalette = listOf(
    LifeOsTheme.CyanFinance,
    LifeOsTheme.PurpleStudy,
    LifeOsTheme.OrangeStreak,
    LifeOsTheme.GreenHealth,
    LifeOsTheme.AmberFocus
)

@Composable
fun FinanceScreen(viewModel: FinanceViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ov = state.overview
    var showAddTxDialog by remember { mutableStateOf(false) }
    var quickInput by remember { mutableStateOf("") }

    val voiceExpenseLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.onIntent(FinanceIntent.SubmitQuickNaturalLanguage(spoken))
            }
        }
    }

    val categorySlicesUi = ov.categorySlices.mapIndexed { idx, s ->
        CategoryLegendRow(
            label = s.category,
            amountFormatted = String.format(Locale.US, "%.0f %s", s.totalAmount, ov.currencyCode),
            fraction = s.fraction,
            color = categoryPalette[idx % categoryPalette.size]
        )
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
                    Text(
                        text = "14:00 · GESTÃO ATRITO ZERO (SQLCIPHER AES-256)",
                        color = LifeOsTheme.CyanFinance,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "Finanças & Caixa",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
                Button(
                    onClick = { showAddTxDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.CyanFinance)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manual", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 1. Barra Relâmpago de 2 Segundos ("Almoço 38 débito" por texto ou voz)
        item {
            BentoCard(borderColor = LifeOsTheme.CyanFinance.copy(alpha = 0.55f)) {
                Text(
                    text = "⚡ Lançamento em 2 Segundos (Texto ou Voz)",
                    color = LifeOsTheme.CyanFinance,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quickInput,
                        onValueChange = { quickInput = it },
                        placeholder = { Text("Ex: Almoço 38 débito", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale: Almoço 38 débito...")
                            }
                            runCatching { voiceExpenseLauncher.launch(intent) }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LifeOsTheme.SurfaceElevated)
                    ) {
                        Icon(Icons.Rounded.Mic, contentDescription = "Falar gasto", tint = LifeOsTheme.OrangeStreak)
                    }
                    IconButton(
                        onClick = {
                            if (quickInput.isNotBlank()) {
                                viewModel.onIntent(FinanceIntent.SubmitQuickNaturalLanguage(quickInput))
                                quickInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LifeOsTheme.CyanFinance)
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = "Registrar", tint = Color.Black)
                    }
                }
                state.quickFeedback?.let { msg ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(msg, color = LifeOsTheme.GreenHealth, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 2. Card de Cruzamento Comportamental (Treino × Gastos em Alimentação)
        if (ov.behavioralCorrelationInsight.isNotBlank()) {
            item {
                BentoCard(borderColor = LifeOsTheme.AmberFocus.copy(alpha = 0.55f)) {
                    Text(
                        text = "🧠 INTELIGÊNCIA CRUZADA (TREINO × FINANÇAS)",
                        color = LifeOsTheme.AmberFocus,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ov.behavioralCorrelationInsight,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 3. Resumo + Barra Segmentada Horizontal (Referência 01 & 08)
        item {
            BentoCard {
                Text("SALDO LÍQUIDO MENSAL", color = LifeOsTheme.TextSecondary, style = MaterialTheme.typography.labelSmall)
                Text(
                    text = String.format(Locale.US, "%.2f %s", ov.netBalanceMonth, ov.currencyCode),
                    color = if (ov.netBalanceMonth >= 0) LifeOsTheme.GreenHealth else LifeOsTheme.RedExpense,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Receitas", color = LifeOsTheme.TextSecondary, fontSize = 12.sp)
                        Text(
                            text = String.format(Locale.US, "+%.2f %s", ov.totalIncomeMonth, ov.currencyCode),
                            color = LifeOsTheme.GreenHealth,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Saídas", color = LifeOsTheme.TextSecondary, fontSize = 12.sp)
                        Text(
                            text = String.format(Locale.US, "-%.2f %s", ov.totalExpenseMonth, ov.currencyCode),
                            color = LifeOsTheme.RedExpense,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (categorySlicesUi.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SegmentedCategoryBar(
                        fractions = categorySlicesUi.map { it.fraction },
                        colors = categoryPalette
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        categorySlicesUi.forEach { slice ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(slice.color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(slice.label, color = Color.White, fontSize = 13.sp)
                                }
                                Text(slice.amountFormatted, color = LifeOsTheme.TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // 4. Matriz Step-Chart (Referência 03 & 10)
        item {
            BentoCard {
                Text("Ritmo de Gastos: Este Mês vs Mês Anterior", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Linha degrau sólida = acumulado atual · Blocos de fundo = mês anterior",
                    color = LifeOsTheme.TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                SpendingBurnMatrix(
                    currentMonthCumulative = ov.burnMatrixPoints
                        .takeWhile { it.currentMonthCumulative > 0.0 || it.dayOfMonth == 1 }
                        .map { it.currentMonthCumulative },
                    previousMonthCumulative = ov.burnMatrixPoints.map { it.previousMonthCumulative }
                )
            }
        }

        // 5. Livro-Razão Recente
        item {
            BentoCard {
                Text("Transações Recentes", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ov.recentTransactions.forEach { tx ->
                        val isIncome = tx.type == "INCOME"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(LifeOsTheme.SurfaceElevated)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tx.category, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    text = listOfNotNull(tx.note, tx.paymentMethod).joinToString(" · "),
                                    color = LifeOsTheme.TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%s%.2f %s",
                                    if (isIncome) "+" else "-",
                                    tx.amount,
                                    tx.currencyCode
                                ),
                                color = if (isIncome) LifeOsTheme.GreenHealth else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddTxDialog) {
        AddTransactionDialog(
            onDismiss = { showAddTxDialog = false },
            onConfirm = { amount, type, cat, note, method ->
                viewModel.onIntent(FinanceIntent.AddTransaction(amount, type, cat, note, method))
                showAddTxDialog = false
            }
        )
    }
}

@Composable
private fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String, String?, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }
    var category by remember { mutableStateOf("Alimentação") }
    var note by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Débito") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LifeOsTheme.SurfaceDark,
        title = { Text("Registrar Fluxo Financeiro", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isExpense,
                        onClick = { isExpense = true },
                        label = { Text("Saída (Despesa)") }
                    )
                    FilterChip(
                        selected = !isExpense,
                        onClick = { isExpense = false },
                        label = { Text("Entrada (Receita)") }
                    )
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor") }
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoria (Ex: Alimentação, Moradia)") }
                )
                OutlinedTextField(
                    value = method,
                    onValueChange = { method = it },
                    label = { Text("Método (Débito, M-Pesa, Cartão)") }
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Observação") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (v > 0.0) {
                        onConfirm(v, if (isExpense) "EXPENSE" else "INCOME", category, note, method)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.CyanFinance)
            ) {
                Text("Salvar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = LifeOsTheme.TextSecondary) }
        }
    )
}
