package com.lifeos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Registro diário de execução de um hábito (alimenta os Micro-Heatmaps em Canvas,
 * cálculo de Streaks, horário médio de check-in e notas diárias).
 */
@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habit_id", "epoch_day"], unique = true),
        Index(value = ["epoch_day"])
    ]
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "habit_id")
    val habitId: Long,

    @ColumnInfo(name = "epoch_day")
    val epochDay: Long, // LocalDate.toEpochDay()

    @ColumnInfo(name = "completed")
    val completed: Boolean,

    @ColumnInfo(name = "value_logged")
    val valueLogged: Float = 1.0f,

    @ColumnInfo(name = "logged_at_millis")
    val loggedAtMillis: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "note")
    val note: String? = null
)
