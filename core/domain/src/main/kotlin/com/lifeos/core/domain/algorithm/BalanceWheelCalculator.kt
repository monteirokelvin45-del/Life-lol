package com.lifeos.core.domain.algorithm

import com.lifeos.core.domain.model.BalanceWheelScore
import kotlin.math.max
import kotlin.math.min

/**
 * Algoritmo da Roda do Equilíbrio da Vida — Balance Wheel Index (Pilar 6.2 + Referência 04).
 *
 * LifeScore = (Corpo * 0.30) + (Foco * 0.25) + (Finanças * 0.25) + (Consistência * 0.20)
 */
object BalanceWheelCalculator {

    private const val WEIGHT_BODY = 0.30f
    private const val WEIGHT_FOCUS = 0.25f
    private const val WEIGHT_FINANCE = 0.25f
    private const val WEIGHT_CONSISTENCY = 0.20f

    fun calculate(
        completedExercisesToday: Int,
        plannedExercisesToday: Int,
        reviewedTopicsToday: Int,
        dueTopicsToday: Int,
        pagesReadToday: Int,
        dailyGoalPages: Int,
        totalIncome: Double,
        totalExpenses: Double,
        consistencyRatio14Days: Float // 0.0f .. 1.0f
    ): BalanceWheelScore {
        // 1. Score Corpo (0.0 a 10.0)
        val bodyScore = if (plannedExercisesToday > 0) {
            (completedExercisesToday.toFloat() / plannedExercisesToday.toFloat())
                .coerceIn(0f, 1f) * 10f
        } else {
            // Dia de descanso regenerativo programado preserva equilíbrio fisiológico base
            8.5f
        }

        // 2. Score Foco (0.0 a 10.0)
        val studyRatio = when {
            dueTopicsToday > 0 -> (reviewedTopicsToday.toFloat() / dueTopicsToday.toFloat()).coerceIn(0f, 1f)
            reviewedTopicsToday > 0 -> 1.0f
            else -> 0.5f
        }
        val readingRatio = if (dailyGoalPages > 0) {
            min(pagesReadToday.toFloat() / dailyGoalPages.toFloat(), 1.0f).coerceAtLeast(0f)
        } else {
            0.5f
        }
        val focusScore = ((studyRatio + readingRatio) / 2.0f) * 10.0f

        // 3. Score Finanças (0.0 a 10.0) — Regras estritas do Pilar 6.2
        val financeScore = calculateFinanceScore(
            income = totalIncome,
            expenses = totalExpenses
        )

        // 4. Score Consistência nos últimos 14 dias (0.0 a 10.0)
        val consistencyScore = consistencyRatio14Days.coerceIn(0f, 1f) * 10.0f

        // Ponderação Unificada
        val overall = (bodyScore * WEIGHT_BODY) +
            (focusScore * WEIGHT_FOCUS) +
            (financeScore * WEIGHT_FINANCE) +
            (consistencyScore * WEIGHT_CONSISTENCY)

        return BalanceWheelScore(
            bodyScore0to10 = bodyScore.coerceIn(0f, 10f),
            focusScore0to10 = focusScore.coerceIn(0f, 10f),
            financeScore0to10 = financeScore.coerceIn(0f, 10f),
            consistencyScore0to10 = consistencyScore.coerceIn(0f, 10f),
            overallScore0to10 = overall.coerceIn(0f, 10f)
        )
    }

    fun calculateFinanceScore(income: Double, expenses: Double): Float {
        if (income <= 0.0) {
            return if (expenses <= 0.0) 7.5f else 0.0f
        }
        return when {
            // Despesas <= 80% das Receitas -> 10.0 máximo
            expenses <= 0.80 * income -> 10.0f

            // Zona de atenção entre 80% e 100% da receita (interpola 10.0 -> 8.0)
            expenses < income -> {
                val ratioOver80 = ((expenses - 0.80 * income) / (0.20 * income)).toFloat()
                (10.0f - (ratioOver80 * 2.0f)).coerceIn(0f, 10f)
            }

            // Despesas >= Receitas -> max(0, 10.0 - (((Despesas - Receitas) / Receitas) * 20))
            else -> {
                val deficitPenalty = (((expenses - income) / income) * 20.0).toFloat()
                max(0.0f, 10.0f - deficitPenalty)
            }
        }
    }
}
