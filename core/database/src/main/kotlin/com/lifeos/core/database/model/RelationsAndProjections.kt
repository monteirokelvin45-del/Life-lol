package com.lifeos.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Relation
import com.lifeos.core.database.entity.ExerciseEntity
import com.lifeos.core.database.entity.HabitEntity
import com.lifeos.core.database.entity.HabitLogEntity
import com.lifeos.core.database.entity.WorkoutRoutineEntity

/**
 * Projeção relacional 1:N unindo um Hábito com todo o seu histórico de check-ins.
 */
data class HabitWithLogs(
    @Embedded
    val habit: HabitEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "habit_id"
    )
    val logs: List<HabitLogEntity>
)

/**
 * Projeção relacional 1:N unindo a Rotina do dia com seus Exercícios prescritos.
 */
data class WorkoutRoutineWithExercises(
    @Embedded
    val routine: WorkoutRoutineEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "routine_id"
    )
    val exercises: List<ExerciseEntity>
) {
    val sortedExercises: List<ExerciseEntity>
        get() = exercises.sortedBy { it.orderIndex }
}

/**
 * Tupla agregada de receitas vs despesas para cálculo direto do Score Finanças (Pilar 6.2)
 * e saldo consolidado da carteira (Referência 01).
 */
data class CashflowSummaryTuple(
    @ColumnInfo(name = "total_income")
    val totalIncome: Double,

    @ColumnInfo(name = "total_expense")
    val totalExpense: Double
) {
    val netBalance: Double
        get() = totalIncome - totalExpense
}

/**
 * Tupla agregada por categoria financeira (barra segmentada da Referência 01).
 */
data class CategoryExpenseTuple(
    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "total_amount")
    val totalAmount: Double
)

/**
 * Tupla de queima financeira diária (Burn Matrix em blocos da Referência 03).
 */
data class DailyExpenseTuple(
    @ColumnInfo(name = "epoch_day")
    val epochDay: Long,

    @ColumnInfo(name = "daily_total")
    val dailyTotal: Double
)
