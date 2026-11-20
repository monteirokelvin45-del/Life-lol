package com.lifeos.app.features.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.core.domain.algorithm.BehavioralRetentionEngine
import com.lifeos.core.domain.algorithm.QuickFinanceNlpParser
import com.lifeos.core.domain.model.AntiCharityContractModel
import com.lifeos.core.domain.model.FinancialOverviewModel
import com.lifeos.core.domain.model.MultivariateScatterPoint
import com.lifeos.core.domain.model.SankeyCashflowBreakdown
import com.lifeos.core.domain.repository.LifeOsRepository
import com.lifeos.core.domain.usecase.RecordTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FinanceUiState(
    val overview: FinancialOverviewModel = FinancialOverviewModel(
        totalIncomeMonth = 0.0,
        totalExpenseMonth = 0.0,
        netBalanceMonth = 0.0,
        spentToday = 0.0,
        currencyCode = "MZN",
        categorySlices = emptyList(),
        burnMatrixPoints = emptyList(),
        recentTransactions = emptyList(),
        behavioralCorrelationInsight = ""
    ),
    val sankeyBreakdown: SankeyCashflowBreakdown = SankeyCashflowBreakdown(65_000.0, 40_000.0, 16_500.0, 8_500.0, 0.0, "MZN"),
    val multivariatePoints: List<MultivariateScatterPoint> = BehavioralRetentionEngine.buildMultivariateScatterDataset(),
    val antiCharityContract: AntiCharityContractModel = AntiCharityContractModel(
        id = 1L,
        weeklyTargetRatio = 0.80f,
        currentWeekRatio = 0.82f,
        stakeAmount = 500.0,
        currencyCode = "MZN",
        institutionName = "Cruz Vermelha & Bolsas STEM",
        isActive = true,
        isProtectedByFreeze = false,
        isAtRisk = false,
        statusSummary = "Meta batida · Caução intacta!"
    ),
    val quickFeedback: String? = null
)

sealed interface FinanceIntent {
    data class SubmitQuickNaturalLanguage(val rawInput: String) : FinanceIntent
    data class AddTransaction(
        val amount: Double,
        val type: String,
        val category: String,
        val note: String?,
        val paymentMethod: String
    ) : FinanceIntent
    data class SaveAntiCharityContract(
        val weeklyTargetRatio: Float,
        val stakeAmount: Double,
        val institutionName: String,
        val isActive: Boolean
    ) : FinanceIntent
    data object DismissQuickFeedback : FinanceIntent
}

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val repository: LifeOsRepository,
    private val recordTransactionUseCase: RecordTransactionUseCase
) : ViewModel() {

    private val feedbackFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FinanceUiState> = combine(
        repository.observeFinancialOverview(),
        repository.observeAntiCharityContract(),
        feedbackFlow
    ) { overview, contract, feedback ->
        val sankey = BehavioralRetentionEngine.calculateSankeyBreakdown(
            totalIncome = overview.totalIncomeMonth,
            totalExpense = overview.totalExpenseMonth,
            slices = overview.categorySlices,
            currencyCode = overview.currencyCode
        )
        FinanceUiState(
            overview = overview,
            sankeyBreakdown = sankey,
            multivariatePoints = BehavioralRetentionEngine.buildMultivariateScatterDataset(),
            antiCharityContract = contract,
            quickFeedback = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FinanceUiState()
    )

    fun onIntent(intent: FinanceIntent) {
        when (intent) {
            is FinanceIntent.SubmitQuickNaturalLanguage -> {
                val parsed = QuickFinanceNlpParser.parse(intent.rawInput)
                if (parsed == null) {
                    feedbackFlow.value = "Informe descrição e valor. Ex: 'Almoço 38 débito'"
                    return
                }
                viewModelScope.launch {
                    recordTransactionUseCase(
                        amount = parsed.amount,
                        type = parsed.type,
                        category = parsed.category,
                        note = parsed.note,
                        paymentMethod = parsed.paymentMethod
                    )
                    feedbackFlow.value = "✓ ${parsed.category}: ${parsed.amount} (${parsed.paymentMethod}) registrado!"
                }
            }
            is FinanceIntent.AddTransaction -> {
                viewModelScope.launch {
                    recordTransactionUseCase(
                        amount = intent.amount,
                        type = intent.type,
                        category = intent.category,
                        note = intent.note,
                        paymentMethod = intent.paymentMethod
                    )
                }
            }
            is FinanceIntent.SaveAntiCharityContract -> {
                viewModelScope.launch {
                    repository.saveAntiCharityContract(
                        weeklyTargetRatio = intent.weeklyTargetRatio,
                        stakeAmount = intent.stakeAmount,
                        currencyCode = uiState.value.overview.currencyCode,
                        institutionName = intent.institutionName,
                        isActive = intent.isActive
                    )
                    feedbackFlow.value = "⚖️ Contrato Anti-Charity atualizado e criptografado!"
                }
            }
            FinanceIntent.DismissQuickFeedback -> {
                feedbackFlow.value = null
            }
        }
    }
}
