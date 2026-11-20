package com.lifeos.core.domain.algorithm

import java.time.LocalDate
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Motor matemático da Curva do Esquecimento de Hermann Ebbinghaus (Pilar 6.1).
 *
 * Retenção mnemônica: R = e^(-t / S), onde `t` é o tempo decorrido em dias desde a última
 * consolidação sináptica e `S` é a estabilidade/força da memória no estágio atual.
 */
object EbbinghausScheduler {

    val STAGE_INTERVALS: List<Int> = listOf(1, 3, 7, 16, 30)

    /**
     * Avança o tópico para o próximo estágio de retenção após revisão bem-sucedida.
     */
    fun calculateNextReviewDate(
        currentStage: Int,
        fromDate: LocalDate = LocalDate.now()
    ): Pair<Int, LocalDate> {
        val nextStage = (currentStage + 1).coerceAtMost(STAGE_INTERVALS.size - 1)
        val daysToAdd = STAGE_INTERVALS[nextStage]
        return Pair(nextStage, fromDate.plusDays(daysToAdd.toLong()))
    }

    /**
     * Caso o usuário erre a recuperação ativa, retorna ao estágio 0 (1 dia).
     */
    fun resetToFirstStage(fromDate: LocalDate = LocalDate.now()): Pair<Int, LocalDate> {
        return Pair(0, fromDate.plusDays(STAGE_INTERVALS[0].toLong()))
    }

    /**
     * Estima o percentual de retenção mnemônica (0..100%) via R = e^(-t / S).
     */
    fun estimateRetentionPercent(
        stage: Int,
        daysElapsedSinceLastReview: Long
    ): Int {
        if (daysElapsedSinceLastReview <= 0L) return 100
        val clampedStage = stage.coerceIn(0, STAGE_INTERVALS.lastIndex)
        // A força S escala para que no dia exato da revisão programada R ≈ 78% (ponto ótimo de reforço)
        val memoryStrengthS = STAGE_INTERVALS[clampedStage].toDouble() / 0.25
        val retention = exp(-daysElapsedSinceLastReview.toDouble() / memoryStrengthS)
        return (retention * 100.0).roundToInt().coerceIn(5, 100)
    }
}
