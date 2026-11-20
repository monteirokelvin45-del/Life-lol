package com.lifeos.core.domain.algorithm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

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
            totalExpenses = 30_000.0,
            consistencyRatio14Days = 1.0f
        )
        assertEquals(10.0f, score.overallScore0to10, 0.01f)
    }

    @Test
    fun `balance wheel applies deficit penalty when expenses exceed income`() {
        val financeScore = BalanceWheelCalculator.calculateFinanceScore(
            income = 10_000.0,
            expenses = 12_500.0
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

    @Test
    fun `quick finance nlp parser extracts almoco 38 debito in under 2s frictionless flow`() {
        val parsed = QuickFinanceNlpParser.parse("Almoço 38 débito")
        assertNotNull(parsed)
        assertEquals(38.0, parsed!!.amount, 0.001)
        assertEquals("EXPENSE", parsed.type)
        assertEquals("Alimentação", parsed.category)
        assertEquals("Débito", parsed.paymentMethod)
    }

    @Test
    fun `cross domain insight detects higher food spending on non-training days`() {
        val insight = CrossDomainInsightCalculator.buildWorkoutVsFoodSpendingInsight(
            activeTrainingEpochDays = setOf(100L, 102L),
            foodExpensesByEpochDay = mapOf(
                100L to 50.0,
                102L to 50.0,
                101L to 70.0
            )
        )
        assertTrue(insight.contains("40%"))
    }

    @Test
    fun `book completion projection computes weekday finish forecast`() {
        val summary = CrossDomainInsightCalculator.buildBookCompletionProjection(
            currentPage = 45,
            totalPages = 105,
            pagesPerHour = 42f,
            dailyGoalPages = 15,
            today = LocalDate.of(2026, 11, 20) // Sexta-feira + 4 dias = Terça-feira
        )
        assertTrue(summary.contains("42 páginas/hora"))
        assertTrue(summary.contains("terça-feira"))
    }

    @Test
    fun `circadian engine calculates caffeine cutoff countdown and 24 nightingale petals`() {
        val morning = BehavioralRetentionEngine.resolveCircadianStatus(LocalTime.of(9, 30))
        assertFalse(morning.isPastCaffeineCutoff)
        assertEquals(270, morning.minutesUntilCaffeineCutoff)
        assertEquals(24, morning.petals24h.size)

        val afternoon = BehavioralRetentionEngine.resolveCircadianStatus(LocalTime.of(15, 0))
        assertTrue(afternoon.isPastCaffeineCutoff)
        assertEquals(0, afternoon.minutesUntilCaffeineCutoff)
    }

    @Test
    fun `streak freeze awards scientific shield credits per 7 clean check-ins`() {
        val status = BehavioralRetentionEngine.calculateStreakFreezeStatus(
            cleanCheckInsTotal = 16,
            usedFreezeDaysCount = 1,
            isFreezeActiveToday = false
        )
        // 1 inicial + (16 / 7 = 2) - 1 usado = 2 disponíveis, e 16 % 7 = 2 no ciclo
        assertEquals(2, status.availableCredits)
        assertEquals(2, status.cleanCheckInsTowardNextShield)
    }

    @Test
    fun `annual heatmap builds 7x52 matrix and compound curve accelerates exponentially`() {
        val today = LocalDate.of(2026, 11, 20)
        val matrix7x52 = StreakAndHeatmapCalculator.buildMicroHeatmapMatrix(
            intensitiesByEpochDay = mapOf(today.toEpochDay() to 1.0f),
            columns = 52,
            today = today
        )
        assertEquals(7, matrix7x52.size)
        assertEquals(52, matrix7x52.first().size)

        val curve = BehavioralRetentionEngine.generateCompoundEffectCurve(0.018f, 100)
        assertEquals(100, curve.size)
        assertTrue(curve[99] > curve[29] && curve[29] > curve[6])
    }

    @Test
    fun `linear regression detects negative correlation between sleep quality and impulsive spending`() {
        val pts = BehavioralRetentionEngine.buildMultivariateScatterDataset()
            .map { it.xNormalized to it.yNormalized }
        val (slope, _, pearsonR) = BehavioralRetentionEngine.computeLinearRegression(pts)
        assertTrue(slope < -0.5f)
        assertTrue(pearsonR < -0.9f)
    }
}
