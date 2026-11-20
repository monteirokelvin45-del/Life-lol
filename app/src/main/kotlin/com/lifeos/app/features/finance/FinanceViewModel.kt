package com.lifeos.app.features.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.core.domain.model.FinancialOverviewModel
import com.lifeos.core.domain.repository.LifeOsRepository
import com.lifeos.core.domain.usecase.RecordTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FinanceIntent {
    data class AddTransaction(
        val amount: Double,
        val type: String,
        val category: String,
        val note: String?,
        val paymentMethod: String
    ) : FinanceIntent
}

@HiltViewModel
class FinanceViewModel @Inject constructor(
    repository: LifeOsRepository,
    private val recordTransactionUseCase: RecordTransactionUseCase
) : ViewModel() {

    val uiState: StateFlow<FinancialOverviewModel> = repository.observeFinancialOverview()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FinancialOverviewModel(
                totalIncomeMonth = 0.0,
                totalExpenseMonth = 0.0,
                netBalanceMonth = 0.0,
                spentToday = 0.0,
                currencyCode = "MZN",
                categorySlices = emptyList(),
                burnMatrixPoints = emptyList(),
                recentTransactions = emptyList()
            )
        )

    fun onIntent(intent: FinanceIntent) {
        when (intent) {
            is FinanceIntent.AddTransaction -> {
                viewModelScope.launch {
                    runCatching {
                        recordTransactionUseCase(
                            amount = intent.amount,
                            type = intent.type,
                            category = intent.category,
                            note = intent.note,
                            paymentMethod = intent.paymentMethod
                        )
                    }
                }
            }
        }
    }
}
