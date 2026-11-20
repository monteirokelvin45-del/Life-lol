package com.lifeos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Rotina semanal de treino e fisiologia (Ex: Calistenia - Costas & Barra Fixa).
 * `dayOfWeek` segue ISO-8601: 1 (Segunda) até 7 (Domingo).
 */
@Entity(
    tableName = "workout_routines",
    indices = [
        Index(value = ["day_of_week"])
    ]
)
data class WorkoutRoutineEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String, // Ex: "Costas e Bíceps"

    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: Int, // 1 (Seg) até 7 (Dom)

    @ColumnInfo(name = "calendar_event_id")
    val calendarEventId: Long? = null,

    @ColumnInfo(name = "estimated_duration_minutes")
    val estimatedDurationMinutes: Int = 45,

    @ColumnInfo(name = "notes")
    val notes: String? = null
)
