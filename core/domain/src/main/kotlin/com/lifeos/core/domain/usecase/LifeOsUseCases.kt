package com.lifeos.core.domain.usecase

import com.lifeos.core.domain.model.AiExtractedPlanPayload
import com.lifeos.core.domain.repository.LifeOsRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * UseCase central de ingestão atômica do plano extraído pela IA (Pilar 5.2).
 */
class IngestPlanUseCase @Inject constructor(
    private val repository: LifeOsRepository
) {
    suspend operator fun invoke(
        plan: AiExtractedPlanPayload,
        today: LocalDate = LocalDate.now()
    ) {
        repository.ingestExtractedPlan(plan = plan, today = today)
    }
}

class ToggleHabitCheckInUseCase @Inject constructor(
    private val repository: LifeOsRepository
) {
    suspend operator fun invoke(
        habitId: Long,
        date: LocalDate = LocalDate.now(),
        note: String? = null
    ) {
        repository.toggleHabitCheckIn(habitId = habitId, date = date, note = note)
    }
}

class CompleteStudyReviewUseCase @Inject constructor(
    private val repository: LifeOsRepository
) {
    suspend operator fun invoke(
        topicId: Long,
        rememberedWell: Boolean = true,
        today: LocalDate = LocalDate.now()
    ) {
        repository.reviewStudyTopic(
            topicId = topicId,
            rememberedWell = rememberedWell,
            today = today
        )
    }
}

class RecordTransactionUseCase @Inject constructor(
    private val repository: LifeOsRepository
) {
    suspend operator fun invoke(
        amount: Double,
        type: String,
        category: String,
        note: String?,
        currencyCode: String = "MZN",
        paymentMethod: String = "M-Pesa"
    ) {
        require(amount > 0.0) { "O valor financeiro precisa ser maior que zero." }
        repository.recordFinancialTransaction(
            amount = amount,
            type = type,
            category = category,
            note = note,
            currencyCode = currencyCode,
            paymentMethod = paymentMethod
        )
    }
}

class LogReadingSessionUseCase @Inject constructor(
    private val repository: LifeOsRepository
) {
    suspend operator fun invoke(
        bookId: Long,
        newCurrentPage: Int,
        sessionMinutes: Int,
        today: LocalDate = LocalDate.now()
    ) {
        repository.logReadingSession(
            bookId = bookId,
            newCurrentPage = newCurrentPage,
            sessionMinutes = sessionMinutes,
            today = today
        )
    }
}
