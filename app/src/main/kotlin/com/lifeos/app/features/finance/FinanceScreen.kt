@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lifeos.app.features.finance

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.graphics.Brush
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
    LifeOsTheme.PurpleStudy,
    LifeOsTheme.RedExpense,
    LifeOsTheme.GreenHealth,
    LifeOsTheme.AmberFocus,
    LifeOsTheme.CyanFinance
)

@Composable
fun FinanceScreen(viewModel: FinanceViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ov = state.overview
    var showAddTxDialog by remember { mutableStateOf(false) }
    var quickInput by remember { mutableStateOf("") }
    var searchTx by remember { mutableStateOf("") }

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

    val filteredTransactions = remember(ov.recentTransactions, searchTx) {
        if (searchTx.isBlank()) {
            ov.recentTransactions
        } else {
            ov.recentTransactions.filter {
                it.category.contains(searchTx, ignoreCase = true) ||
                    (it.note?.contains(searchTx, ignoreCase = true) == true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LifeOsTheme.PureBlack),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Barra Superior estilo Wallet (Referência 01: Avatar + Search Pill + Ícones)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LifeOsTheme.SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚡", fontSize = 18.sp)
                }

                OutlinedTextField(
                    value = searchTx,
                    onValueChange = { searchTx = it },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = LifeOsTheme.TextSecondary)
                    },
                    placeholder = { Text("Search", color = LifeOsTheme.TextSecondary, fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { showAddTxDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LifeOsTheme.SurfaceElevated)
                ) {
                    Icon(Icons.Rounded.NotificationsNone, contentDescription = null, tint = Color.White)
                }
            }
        }

        // 2. Hero Card Metálico Escuro "Wallet" (Referência 01)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF232429),
                                Color(0xFF111215),
                                Color(0xFF0A0A0C)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFF2A2B30), RoundedCornerShape(28.dp))
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Wallet", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Novo lançamento",
                        tint = Color.White,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { showAddTxDialog = true }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Life OS Vault · SQLCipher", color = LifeOsTheme.TextSecondary, fontSize = 17.sp)

                Spacer(modifier = Modifier.height(42.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = String.format(Locale.US, "%.2f %s", ov.netBalanceMonth, ov.currencyCode),
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Account ** 5087", color = LifeOsTheme.TextSecondary, fontSize = 12.sp)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEB001B))
                            )
                            Box(
                                modifier = Modifier
                                    .offset(x = (-6).dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF79E1B).copy(alpha = 0.9f))
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("**** 3264", color = LifeOsTheme.TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        // 3. Linha Bento Dupla: "Spending" (Pílulas Multicoloridas) + "Top Canais" (Referência 01)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BentoCard(modifier = Modifier.weight(1.1f)) {
                    Text("Spending", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = String.format(Locale.US, "-%.0f %s no mês", ov.totalExpenseMonth, ov.currencyCode),
                        color = LifeOsTheme.TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SegmentedCategoryBar(
                        fractions = if (categorySlicesUi.isNotEmpty()) {
                            categorySlicesUi.map { it.fraction }
                        } else {
                            listOf(0.42f, 0.22f, 0.12f, 0.16f, 0.08f)
                        },
                        colors = categoryPalette
                    )
                }

                BentoCard(modifier = Modifier.weight(0.95f)) {
                    Text("Canais Ativos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Débito · M-Pesa", color = LifeOsTheme.TextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "🍔" to Color(0xFF1F1214),
                            "🏠" to Color(0xFFF2F2F7),
                            "📚" to Color(0xFF141418),
                            "⚡" to Color(0xFFD91E2A)
                        ).forEach { (emoji, bg) ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // 4. Barra Relâmpago de 2 Segundos ("Almoço 38 débito" por texto ou voz)
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

        // 5. Inteligência Cruzada (Treino × Alimentação)
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

        // 6. Card Pixelado Hero "$123 today" (Referência 03 exata!)
        item {
            val prevTotal = ov.burnMatrixPoints.lastOrNull()?.previousMonthCumulative ?: 0.0
            val currTotal = ov.totalExpenseMonth
            val spendingLess = currTotal <= prevTotal || prevTotal == 0.0

            BentoCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = String.format(Locale.US, "%.0f %s", ov.spentToday, ov.currencyCode),
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "today",
                        color = LifeOsTheme.TextSecondary,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format(Locale.US, "Este mês: %.0f %s", currTotal, ov.currencyCode),
                        color = LifeOsTheme.OrangeStreak,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(Locale.US, "Mês ant.: %.0f %s", prevTotal, ov.currencyCode),
                        color = LifeOsTheme.TextMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                SpendingBurnMatrix(
                    currentMonthCumulative = ov.burnMatrixPoints
                        .takeWhile { it.currentMonthCumulative > 0.0 || it.dayOfMonth == 1 }
                        .map { it.currentMonthCumulative },
                    previousMonthCumulative = ov.burnMatrixPoints.map { it.previousMonthCumulative }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (spendingLess) "You're spending less than last month..." else "Ritmo acima do mês anterior — atenção ao teto.",
                    color = LifeOsTheme.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        // 7. Legenda Detalhada de Categorias + Transações Recentes
        item {
            BentoCard {
                Text("Transações & Categorias", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredTransactions.forEach { tx ->
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
