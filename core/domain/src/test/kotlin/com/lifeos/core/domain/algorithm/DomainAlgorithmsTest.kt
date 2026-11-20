package com.lifeos.core.domain.algorithm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DomainAlgorithmsTest {

    @Test
    fun `ebbinghaus progresses through 1 3 7 16 30 day stages`() {
        val base = LocalDate.of(2026, 11, 20)

        val (s1, d1) = EbbinghausScheduler.calculateNextReviewDate(0, base)
        assertEquals(1, s1)
        assertEquals(base.plusDays(3), d1)

        val (s2, d2) = EbbinghausScheduler.calculateNextReviewDate(s1, d1)
        assertEquals(2, s2)
        assertEquals(d1.plusDays(7), d2)

        val (s3, d3) = EbbinghausScheduler.calculateNextReviewDate(s2, d2)
        assertEquals(3, s3)
        assertEquals(d2.plusDays(16), d3)

        val (s4, d4) = EbbinghausScheduler.calculateNextReviewDate(s3, d3)
        assertEquals(4, s4)
        assertEquals(d3.plusDays(30), d4)
    }

    @Test
    fun `balance wheel returns 10 when all dimensions hit optimal targets`() {
        val score = BalanceWheelCalculator.calculate(
            completedExercisesToday = 4,
            plannedExercisesToday = 4,
            reviewedTopicsToday = 3,
            dueTopicsToday = 3,
            pagesReadToday = 25,
            dailyGoalPages = 20,
            totalIncome = 50_000.0,
            totalExpenses = 30_000.0, // <= 80% income
            consistencyRatio14Days = 1.0f
        )
        assertEquals(10.0f, score.overallScore0to10, 0.01f)
    }

    @Test
    fun `balance wheel applies deficit penalty when expenses exceed income`() {
        val financeScore = BalanceWheelCalculator.calculateFinanceScore(
            income = 10_000.0,
            expenses = 12_500.0 // 25% deficit -> 10 - (0.25 * 20) = 5.0
        )
        assertEquals(5.0f, financeScore, 0.01f)
    }

    @Test
    fun `heatmap matrix builds 7 rows by 20 columns`() {
        val today = LocalDate.of(2026, 11, 20)
        val matrix = StreakAndHeatmapCalculator.buildMicroHeatmapMatrix(
            intensitiesByEpochDay = mapOf(today.toEpochDay() to 1.0f),
            columns = 20,
            today = today
        )
        assertEquals(7, matrix.size)
        assertEquals(20, matrix.first().size)
        assertTrue(matrix.flatten().any { it == 1.0f })
    }
}
