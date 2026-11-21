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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.TrendingUp
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.ui.components.AntiCharityCommitmentBanner
import com.lifeos.ui.components.BentoCard
import com.lifeos.ui.components.EyebrowCapsule
import com.lifeos.ui.components.FluidSankeyCashflowChart
import com.lifeos.ui.components.GlowingIconOrb
import com.lifeos.ui.components.LuxuryStatBadge
import com.lifeos.ui.components.MultivariateCorrelationScatterChart
import com.lifeos.ui.components.SegmentedCategoryBar
import com.lifeos.ui.components.SleekSearchBarPill
import com.lifeos.ui.components.SpendingBurnMatrix
import com.lifeos.ui.theme.LifeOsTheme

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
    var showContractDialog by remember { mutableStateOf(false) }
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
            .background(Color.Transparent),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Barra Superior estilo Wallet (Referência 01: Orbe + Search Pill + Botão Novo)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlowingIconOrb(
                    icon = Icons.Rounded.Lock,
                    tint = LifeOsTheme.CyanFinance,
                    size = 42.dp,
                    iconSize = 18.dp
                )

                SleekSearchBarPill(
                    value = searchTx,
                    onValueChange = { searchTx = it },
                    placeholder = "Buscar categoria ou nota...",
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { showAddTxDialog = true },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(LifeOsTheme.CyanFinance)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Novo lançamento", tint = Color.Black)
                }
            }
        }

        // 2. Cartão Titânio Escovado "Life OS Vault · SQLCipher AES-256" (Referência 01)
        item {
            val cardShape = RoundedCornerShape(30.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF222634),
                                Color(0xFF12141C),
                                Color(0xFF0A0B0F)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                LifeOsTheme.CyanFinance.copy(alpha = 0.48f),
                                Color.White.copy(alpha = 0.14f),
                                LifeOsTheme.BorderSubtle
                            )
                        ),
                        shape = cardShape
                    )
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EyebrowCapsule(
                        text = "SQLCIPHER AES-256 VAULT",
                        accent = LifeOsTheme.CyanFinance
                    )
                    LuxuryStatBadge(
                        label = "+${LifeOsTheme.formatMoney(ov.totalIncomeMonth, ov.currencyCode, 0)}",
                        accent = LifeOsTheme.GreenHealth
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Saldo Líquido Blindado", color = LifeOsTheme.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)

                Spacer(modifier = Modifier.height(26.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = LifeOsTheme.formatMoney(ov.netBalanceMonth, ov.currencyCode, decimals = 2),
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.8).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Hardware Keystore · Conta ** 5087", color = LifeOsTheme.TextSecondary, fontSize = 11.5.sp)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEB001B))
                            )
                            Box(
                                modifier = Modifier
                                    .offset(x = (-7).dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF79E1B).copy(alpha = 0.92f))
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("**** 3264", color = LifeOsTheme.TextSecondary, fontSize = 11.5.sp)
                    }
                }
            }
        }

        // 3. Linha Bento Dupla: "Spending" + "Canais Ativos" com Squircles Vetoriais Escuros
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BentoCard(modifier = Modifier.weight(1.08f)) {
                    Text("Saídas do Mês", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = "-${LifeOsTheme.formatMoney(ov.totalExpenseMonth, ov.currencyCode, 0)}",
                        color = LifeOsTheme.TextSecondary,
                        fontSize = 11.5.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SegmentedCategoryBar(
                        fractions = if (ov.categorySlices.isNotEmpty()) {
                            ov.categorySlices.map { it.fraction }
                        } else {
                            listOf(0.42f, 0.22f, 0.12f, 0.16f, 0.08f)
                        },
                        colors = categoryPalette
                    )
                }

                BentoCard(modifier = Modifier.weight(0.95f)) {
                    Text("Canais Ativos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Débito · M-Pesa", color = LifeOsTheme.TextSecondary, fontSize = 11.5.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            Icons.Rounded.Restaurant to LifeOsTheme.RedExpense,
                            Icons.Rounded.HomeWork to LifeOsTheme.PurpleStudy,
                            Icons.Rounded.MenuBook to LifeOsTheme.GreenHealth,
                            Icons.Rounded.Bolt to LifeOsTheme.CyanFinance
                        ).forEach { (icon, tint) ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(tint.copy(alpha = 0.15f))
                                    .border(1.dp, tint.copy(alpha = 0.32f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // 4. Barra Relâmpago de 2 Segundos ("Almoço 38 débito" por texto ou voz)
        item {
            BentoCard(
                borderColor = LifeOsTheme.CyanFinance.copy(alpha = 0.48f),
                glowAccent = LifeOsTheme.CyanFinance
            ) {
                EyebrowCapsule(
                    text = "LANÇAMENTO NLP EM 2 SEGUNDOS",
                    accent = LifeOsTheme.CyanFinance
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(LifeOsTheme.SurfaceElevated)
                            .border(1.dp, LifeOsTheme.BorderSubtle, CircleShape)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (quickInput.isEmpty()) {
                            Text(
                                text = "Ex: Almoço 38 débito",
                                color = LifeOsTheme.TextMuted,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                        BasicTextField(
                            value = quickInput,
                            onValueChange = { quickInput = it },
                            singleLine = true,
                            cursorBrush = SolidColor(LifeOsTheme.CyanFinance),
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

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
                            .clip(CircleShape)
                            .background(LifeOsTheme.SurfaceElevated)
                            .border(1.dp, LifeOsTheme.BorderSubtle, CircleShape)
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
                            .clip(CircleShape)
                            .background(LifeOsTheme.CyanFinance)
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = "Registrar", tint = Color.Black)
                    }
                }
                state.quickFeedback?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(msg, color = LifeOsTheme.GreenHealth, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 5. Contrato de Compromisso Anti-Charity (Pilar 5.5)
        item {
            val ac = state.antiCharityContract
            AntiCharityCommitmentBanner(
                stakeAmount = ac.stakeAmount,
                currencyCode = ac.currencyCode,
                institutionName = ac.institutionName,
                weeklyTargetRatio = ac.weeklyTargetRatio,
                currentWeekRatio = ac.currentWeekRatio,
                isActive = ac.isActive,
                isAtRisk = ac.isAtRisk,
                statusSummary = ac.statusSummary,
                onConfigureContract = { showContractDialog = true }
            )
        }

        // 5.2. Diagrama de Fluxo de Sankey Fluido (Pilar 6.8)
        item {
            val sk = state.sankeyBreakdown
            BentoCard(glowAccent = LifeOsTheme.CyanFinance) {
                EyebrowCapsule(text = "DIAGRAMA DE SANKEY FLUIDO", accent = LifeOsTheme.CyanFinance)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Ramificação da Renda Líquida", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Spacer(modifier = Modifier.height(12.dp))
                FluidSankeyCashflowChart(
                    netIncome = sk.netIncome,
                    investmentsAmount = sk.investmentsAmount,
                    fixedCostsAmount = sk.fixedEssentialsAmount,
                    leakageAmount = sk.capitalLeakageAmount,
                    currencyCode = sk.currencyCode
                )
            }
        }

        // 5.5. Matriz de Correlação Multivariada da IA (Pilar 6.10)
        item {
            BentoCard(
                borderColor = LifeOsTheme.AmberFocus.copy(alpha = 0.50f),
                glowAccent = LifeOsTheme.AmberFocus
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EyebrowCapsule(
                        text = "CORRELAÇÃO MULTIVARIADA IA",
                        accent = LifeOsTheme.AmberFocus
                    )
                    LuxuryStatBadge(label = "r = -0.94", accent = LifeOsTheme.AmberFocus)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Sono & Treino × Gastos Impulsivos",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                if (ov.behavioralCorrelationInsight.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ov.behavioralCorrelationInsight,
                        color = LifeOsTheme.TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                MultivariateCorrelationScatterChart(
                    xyNormalizedPairs = state.multivariatePoints.map { it.xNormalized to it.yNormalized },
                    positiveFlags = state.multivariatePoints.map { it.isPositiveCluster }
                )
            }
        }

        // 6. Card Pixelado Hero "920 MZN hoje" (Referência 03)
        item {
            val prevTotal = ov.burnMatrixPoints.lastOrNull()?.previousMonthCumulative ?: 0.0
            val currTotal = ov.totalExpenseMonth
            val spendingLess = currTotal <= prevTotal || prevTotal == 0.0

            BentoCard(glowAccent = LifeOsTheme.OrangeStreak) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EyebrowCapsule(text = "ESCADA TÉRMICA DE QUEIMA DE CAIXA", accent = LifeOsTheme.OrangeStreak)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = LifeOsTheme.formatMoney(ov.spentToday, ov.currencyCode, decimals = 0),
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "gasto hoje",
                        color = LifeOsTheme.TextSecondary,
                        fontSize = 13.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Este mês: ${LifeOsTheme.formatMoney(currTotal, ov.currencyCode, 0)}",
                        color = LifeOsTheme.OrangeStreak,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Mês ant.: ${LifeOsTheme.formatMoney(prevTotal, ov.currencyCode, 0)}",
                        color = LifeOsTheme.TextMuted,
                        fontSize = 11.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                SpendingBurnMatrix(
                    currentMonthCumulative = ov.burnMatrixPoints
                        .take(java.time.LocalDate.now().dayOfMonth.coerceAtLeast(1))
                        .map { it.currentMonthCumulative },
                    previousMonthCumulative = ov.burnMatrixPoints.map { it.previousMonthCumulative }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (spendingLess) "Ritmo abaixo do mês anterior · Margem de segurança preservada" else "Ritmo acima do mês anterior · Atenção ao teto variável",
                    color = if (spendingLess) LifeOsTheme.GreenHealth else LifeOsTheme.AmberFocus,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        // 7. Transações Recentes com Orbes de Categoria e Moeda Tabular
        item {
            BentoCard {
                Text("Livro-Razão Criptografado", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    filteredTransactions.forEach { tx ->
                        val isIncome = tx.type == "INCOME"
                        val txIcon = when {
                            isIncome -> Icons.Rounded.TrendingUp
                            tx.category.contains("Moradia", ignoreCase = true) -> Icons.Rounded.HomeWork
                            tx.category.contains("Transp", ignoreCase = true) -> Icons.Rounded.DirectionsCar
                            tx.category.contains("Livro", ignoreCase = true) -> Icons.Rounded.MenuBook
                            else -> Icons.Rounded.Restaurant
                        }
                        val txTint = when {
                            isIncome -> LifeOsTheme.GreenHealth
                            tx.category.contains("Moradia", ignoreCase = true) -> LifeOsTheme.PurpleStudy
                            tx.category.contains("Transp", ignoreCase = true) -> LifeOsTheme.CyanFinance
                            else -> LifeOsTheme.AmberFocus
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(LifeOsTheme.SurfaceElevated)
                                .border(1.dp, LifeOsTheme.BorderSubtle, RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlowingIconOrb(
                                    icon = txIcon,
                                    tint = txTint,
                                    size = 36.dp,
                                    iconSize = 17.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.category, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                    Text(
                                        text = listOfNotNull(tx.note, tx.paymentMethod).joinToString(" · "),
                                        color = LifeOsTheme.TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${if (isIncome) "+" else "-"}${LifeOsTheme.formatMoney(tx.amount, tx.currencyCode, decimals = 0)}",
                                color = if (isIncome) LifeOsTheme.GreenHealth else Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                maxLines = 1,
                                softWrap = false
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

    if (showContractDialog) {
        val ac = state.antiCharityContract
        ConfigureAntiCharityDialog(
            initialStake = ac.stakeAmount,
            initialInstitution = ac.institutionName,
            initialTargetPct = (ac.weeklyTargetRatio * 100).toInt(),
            onDismiss = { showContractDialog = false },
            onSave = { stake, inst, pct ->
                showContractDialog = false
                viewModel.onIntent(
                    FinanceIntent.SaveAntiCharityContract(
                        weeklyTargetRatio = (pct / 100f).coerceIn(0.5f, 1.0f),
                        stakeAmount = stake,
                        institutionName = inst,
                        isActive = true
                    )
                )
            }
        )
    }
}

@Composable
private fun ConfigureAntiCharityDialog(
    initialStake: Double,
    initialInstitution: String,
    initialTargetPct: Int,
    onDismiss: () -> Unit,
    onSave: (Double, String, Int) -> Unit
) {
    var stakeInput by remember { mutableStateOf(initialStake.toInt().toString()) }
    var institutionInput by remember { mutableStateOf(initialInstitution) }
    var targetPctInput by remember { mutableStateOf(initialTargetPct.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LifeOsTheme.SurfaceDark,
        title = { Text("Contrato Anti-Charity", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Vincule uma caução financeira semanal para eliminar a procrastinação por aversão à perda:",
                    color = LifeOsTheme.TextSecondary,
                    fontSize = 12.sp
                )
                OutlinedTextField(value = stakeInput, onValueChange = { stakeInput = it }, label = { Text("Caução Semanal") }, singleLine = true)
                OutlinedTextField(value = institutionInput, onValueChange = { institutionInput = it }, label = { Text("Instituição Destino") }, singleLine = true)
                OutlinedTextField(value = targetPctInput, onValueChange = { targetPctInput = it }, label = { Text("Meta Mínima Semanal (%)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val stake = stakeInput.toDoubleOrNull() ?: initialStake
                    val pct = targetPctInput.toIntOrNull() ?: initialTargetPct
                    onSave(stake, institutionInput, pct)
                },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOsTheme.AmberFocus)
            ) {
                Text("Assinar Contrato", color = Color.Black, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = LifeOsTheme.TextSecondary) }
        }
    )
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
