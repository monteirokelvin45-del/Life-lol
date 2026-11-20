package com.lifeos.core.domain.algorithm

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * Algoritmos de Gamificação Visual (Pilar 1.2 + Pilar 8.2 + Referências 05, 06, 09 e 10):
 * - Cálculo de Streaks respeitando dias alvo (`targetDaysMask`).
 * - Construção da matriz 7x20 do `HabitMicroHeatmap`.
 * - Cálculo do horário médio de check-in (`Avg. Log Time`).
 * - Consistência global de 14 dias para a Roda do Equilíbrio.
 */
object StreakAndHeatmapCalculator {

    private const val HEATMAP_ROWS = 7

    fun isScheduledOnDate(targetDaysMask: Int, date: LocalDate): Boolean {
        val effectiveMask = if (targetDaysMask <= 0) 0b1111111 else targetDaysMask
        val bit = 1 shl (date.dayOfWeek.value - 1)
        return (effectiveMask and bit) != 0
    }

    fun countTargetDaysPerWeek(targetDaysMask: Int): Int {
        val effectiveMask = if (targetDaysMask <= 0) 0b1111111 else targetDaysMask
        return Integer.bitCount(effectiveMask and 0b1111111)
    }

    /**
     * Calcula `(currentStreak, longestStreak)` percorrendo os dias programados no bitmask.
     */
    fun calculateStreaks(
        completedEpochDays: Set<Long>,
        targetDaysMask: Int,
        today: LocalDate = LocalDate.now()
    ): Pair<Int, Int> {
        if (completedEpochDays.isEmpty()) return 0 to 0

        val minDay = completedEpochDays.minOrNull() ?: return 0 to 0
        val startDate = LocalDate.ofEpochDay(minDay)

        var runningStreak = 0
        var longestStreak = 0
        var cursor = startDate

        while (!cursor.isAfter(today)) {
            val epoch = cursor.toEpochDay()
            if (completedEpochDays.contains(epoch)) {
                runningStreak++
                if (runningStreak > longestStreak) {
                    longestStreak = runningStreak
                }
            } else if (isScheduledOnDate(targetDaysMask, cursor) && cursor.isBefore(today)) {
                // Quebra a sequência apenas se era dia obrigatório já encerrado
                runningStreak = 0
            }
            cursor = cursor.plusDays(1)
        }

        return runningStreak to longestStreak
    }

    /**
     * Gera a matriz `7 linhas (Seg..Dom) x columns colunas semanais` com intensidades `0f..1f`
     * alinhada à semana ISO atual para renderização direta no `HabitMicroHeatmap` (Canvas GPU).
     */
    fun buildMicroHeatmapMatrix(
        intensitiesByEpochDay: Map<Long, Float>,
        columns: Int = 20,
        today: LocalDate = LocalDate.now()
    ): List<List<Float>> {
        // Encontra a Segunda-feira da semana atual e recua (columns - 1) semanas
        val currentWeekMonday = today.minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
        val firstColumnMonday = currentWeekMonday.minusWeeks((columns - 1).toLong())

        return List(HEATMAP_ROWS) { row ->
            List(columns) { col ->
                val cellDate = firstColumnMonday
                    .plusWeeks(col.toLong())
                    .plusDays(row.toLong())
                if (cellDate.isAfter(today)) {
                    0f
                } else {
                    (intensitiesByEpochDay[cellDate.toEpochDay()] ?: 0f).coerceIn(0f, 1f)
                }
            }
        }
    }

    /**
     * Calcula o horário médio de registro (ex: "14:50" / "2:50 PM" — Referência 05).
     */
    fun computeAverageLogTime(
        loggedAtMillisList: List<Long>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        if (loggedAtMillisList.isEmpty()) return "--:--"
        val avgMinutesOfDay = loggedAtMillisList
            .map { millis ->
                val localTime = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalTime()
                localTime.hour * 60 + localTime.minute
            }
            .average()
            .toInt()
            .coerceIn(0, 23 * 60 + 59)

        val hour = avgMinutesOfDay / 60
        val minute = avgMinutesOfDay % 60
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    /**
     * Calcula o percentual médio de check-ins realizados vs esperados nos últimos 14 dias (Pilar 6.2).
     */
    fun calculate14DayConsistency(
        habitsMasksById: Map<Long, Int>,
        completedPairs: Set<Pair<Long, Long>>, // (habitId, epochDay)
        today: LocalDate = LocalDate.now()
    ): Float {
        if (habitsMasksById.isEmpty()) return 0f
        var scheduledCount = 0
        var completedCount = 0

        for (offset in 0L until 14L) {
            val day = today.minusDays(offset)
            val epochDay = day.toEpochDay()
            for ((habitId, mask) in habitsMasksById) {
                if (isScheduledOnDate(mask, day)) {
                    scheduledCount++
                    if (completedPairs.contains(habitId to epochDay)) {
                        completedCount++
                    }
                }
            }
        }

        return if (scheduledCount == 0) {
            0f
        } else {
            (completedCount.toFloat() / scheduledCount.toFloat()).coerceIn(0f, 1f)
        }
    }
}
