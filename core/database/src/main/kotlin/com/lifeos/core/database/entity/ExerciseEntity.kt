package com.lifeos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Exercício vinculado a uma rotina semanal, contendo prescrição de séries,
 * repetições, descanso em segundos (para o Foreground Service Timer) e progresso do dia.
 */
@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutRoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["routine_id"]),
        Index(value = ["routine_id", "order_index"])
    ]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "routine_id")
    val routineId: Long,

    @ColumnInfo(name = "name")
    val name: String, // Ex: "Barra Fixa"

    @ColumnInfo(name = "target_sets")
    val targetSets: Int, // 4

    @ColumnInfo(name = "target_reps")
    val targetReps: Int, // 10

    @ColumnInfo(name = "rest_seconds")
    val restSeconds: Int = 90,

    @ColumnInfo(name = "order_index")
    val orderIndex: Int,

    @ColumnInfo(name = "completed_sets_today")
    val completedSetsToday: Int = 0,

    @ColumnInfo(name = "last_completed_epoch_day")
    val lastCompletedEpochDay: Long? = null,

    @ColumnInfo(name = "last_weight_kg")
    val lastWeightKg: Float = 0f
)
