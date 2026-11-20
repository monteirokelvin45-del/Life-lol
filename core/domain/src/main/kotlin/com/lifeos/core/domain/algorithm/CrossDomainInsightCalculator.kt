package com.lifeos.core.domain.algorithm

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Motor de Ciência de Dados Pessoal & Correlações Comportamentais Cruzadas:
 * 1. Cruza consistência física (Treino/Hábitos) com gastos em Alimentação.
 * 2. Projeta a data exata de término de livros com base na velocidade individual.
 */
object CrossDomainInsightCalculator {

    private val shortDatePt = DateTimeFormatter.ofPattern("dd MMM", Locale("pt", "MZ"))

    fun buildWorkoutVsFoodSpendingInsight(
        activeTrainingEpochDays: Set<Long>,
        foodExpensesByEpochDay: Map<Long, Double>
    ): String {
        if (foodExpensesByEpochDay.isEmpty()) {
            return "Registre gastos rápidos (ex: 'Almoço 38 débito') para ativar correlações Treino × Finanças."
        }

        val trainedFoodTotals = foodExpensesByEpochDay
            .filterKeys { it in activeTrainingEpochDays }
            .values
        val skippedFoodTotals = foodExpensesByEpochDay
            .filterKeys { it !in activeTrainingEpochDays }
            .values

        if (trainedFoodTotals.isNotEmpty() && skippedFoodTotals.isNotEmpty()) {
            val avgTrained = trainedFoodTotals.average()
            val avgSkipped = skippedFoodTotals.average()
            if (avgTrained > 0.0 && avgSkipped > avgTrained) {
                val pctHigher = (((avgSkipped - avgTrained) / avgTrained) * 100.0).roundToInt()
                if (pctHigher >= 5) {
                    return "Nos dias sem treino concluído, você gasta $pctHigher% a mais em Alimentação."
                }
            } else if (avgSkipped > 0.0 && avgTrained < avgSkipped) {
                return "Dias de treino ativo reduzem seus gastos impulsivos com comida fora."
            }
        }

        return "Consistência ativa: manter o treino às 18h protege seu orçamento de gastos por fadiga."
    }

    fun buildBookCompletionProjection(
        currentPage: Int,
        totalPages: Int,
        pagesPerHour: Float,
        dailyGoalPages: Int,
        today: LocalDate = LocalDate.now()
    ): String {
        val remainingPages = (totalPages - currentPage).coerceAtLeast(0)
        if (remainingPages == 0) {
            return "Livro concluído! Todas as $totalPages páginas consolidadas."
        }

        val effectiveDailyPages = when {
            dailyGoalPages > 0 -> dailyGoalPages
            pagesPerHour > 0f -> (pagesPerHour * 0.5f).roundToInt().coerceAtLeast(10)
            else -> 20
        }

        val daysNeeded = ceil(remainingPages.toDouble() / effectiveDailyPages.toDouble()).toLong().coerceAtLeast(1L)
        val finishDate = today.plusDays(daysNeeded)
        val weekdayPt = weekdayNamePt(finishDate.dayOfWeek)
        val speedLabel = if (pagesPerHour > 0f) "${pagesPerHour.roundToInt()} páginas/hora" else "$effectiveDailyPages pág/dia"

        return "Velocidade média: $speedLabel. Nesse ritmo, você terminará $weekdayPt (${finishDate.format(shortDatePt)})."
    }

    private fun weekdayNamePt(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
        DayOfWeek.MONDAY -> "na segunda-feira"
        DayOfWeek.TUESDAY -> "na terça-feira"
        DayOfWeek.WEDNESDAY -> "na quarta-feira"
        DayOfWeek.THURSDAY -> "na quinta-feira"
        DayOfWeek.FRIDAY -> "na sexta-feira"
        DayOfWeek.SATURDAY -> "no sábado"
        DayOfWeek.SUNDAY -> "no domingo"
    }
}
