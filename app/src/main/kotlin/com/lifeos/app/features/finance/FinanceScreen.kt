@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lifeos.app.features.finance

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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

@Composable
fun FinanceScreen(
    viewModel: FinanceViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

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
                    Text("Carteira & Fluxo", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Text("Controle diário local-first criptografado", color = LifeOsTheme.TextSecondary, fontSize = 12.sp)
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.CyanFinance)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lançar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 1. Wallet Bento Card (Referência 01)
        item {
            BentoCard {
                Text("SALDO LÍQUIDO DO MÊS", color = LifeOsTheme.TextSecondary, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = String.format(Locale.US, "%,.2f %s", state.netBalanceMonth, state.currencyCode),
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format(Locale.US, "Entradas: +%,.0f", state.totalIncomeMonth),
                        color = LifeOsTheme.GreenHealth,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = String.format(Locale.US, "Saídas: -%,.0f", state.totalExpenseMonth),
                        color = LifeOsTheme.RedExpense,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text("Distribuição por Categoria", color = LifeOsTheme.TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                SegmentedCategoryBar(
                    fractions = state.categorySlices.map { it.fraction }
                )
                if (state.categorySlices.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.categorySlices.take(3).joinToString(" · ") {
                            "${it.category}: ${(it.fraction * 100).toInt()}%"
                        },
                        color = LifeOsTheme.TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 2. Burn Matrix Cumulativa (Referência 03 — `$123 today`)
        item {
            BentoCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = String.format(Locale.US, "%,.0f %s", state.spentToday, state.currencyCode),
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("gasto hoje", color = LifeOsTheme.TextSecondary, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(14.dp))
                SpendingBurnMatrix(
                    currentMonthCumulative = state.burnMatrixPoints.map { it.currentMonthCumulative },
                    previousMonthCumulative = state.burnMatrixPoints.map { it.previousMonthCumulative }
                )
                Spacer(modifier = Modifier.height(8.dp))
                val ratioText = if (state.totalIncomeMonth > 0 && state.totalExpenseMonth <= state.totalIncomeMonth * 0.8) {
                    "Ritmo excelente: despesas abaixo de 80% da receita (Score 10/10)"
                } else {
                    "Atenção ao teto de 80% da receita para preservar o LifeScore"
                }
                Text(ratioText, color = LifeOsTheme.TextSecondary, fontSize = 12.sp)
            }
        }

        // 3. Lançamentos Recentes
        item {
            Text("Movimentações Recentes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        items(state.recentTransactions.size) { idx ->
            val tx = state.recentTransactions[idx]
            val isIncome = tx.type == "INCOME"
            BentoCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tx.category, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            text = buildString {
                                append(tx.paymentMethod)
                                tx.note?.let { append(" · $it") }
                            },
                            color = LifeOsTheme.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = String.format(
                            Locale.US,
                            "%s%,.2f %s",
                            if (isIncome) "+" else "-",
                            tx.amount,
                            tx.currencyCode
                        ),
                        color = if (isIncome) LifeOsTheme.GreenHealth else LifeOsTheme.RedExpense,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { amount, type, category, note, method ->
                showAddDialog = false
                viewModel.onIntent(FinanceIntent.AddTransaction(amount, type, category, note, method))
            }
        )
    }
}

@Composable
private fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (Double, String, String, String?, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }
    var category by remember { mutableStateOf("Alimentação") }
    var note by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("M-Pesa") }

    val methods = listOf("M-Pesa", "e-Mola", "Cartão", "Transferência", "Numerário")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LifeOsTheme.SurfaceDark,
        title = { Text("Novo Lançamento", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = { type = "EXPENSE" },
                        label = { Text("Despesa") }
                    )
                    FilterChip(
                        selected = type == "INCOME",
                        onClick = { type = "INCOME" },
                        label = { Text("Receita") }
                    )
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor (MZN)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoria") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota opcional") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    methods.forEach { method ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(method, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (value > 0.0) {
                        onSave(value, type, category, note.ifBlank { null }, paymentMethod)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.CyanFinance)
            ) {
                Text("Registrar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = LifeOsTheme.TextSecondary)
            }
        }
    )
}
