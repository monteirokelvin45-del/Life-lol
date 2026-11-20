package com.lifeos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Entidade de Hábitos recorrentes do Life OS.
 *
 * `targetDaysMask` codifica os dias ativos da semana em 7 bits:
 * - Seg = 1 (1 shl 0)
 * - Ter = 2 (1 shl 1)
 * - Qua = 4 (1 shl 2)
 * - Qui = 8 (1 shl 3)
 * - Sex = 16 (1 shl 4)
 * - Sab = 32 (1 shl 5)
 * - Dom = 64 (1 shl 6)
 * - Todos os dias = 127
 */
@Entity(
    tableName = "habits",
    indices = [
        Index(value = ["is_archived"]),
        Index(value = ["category"])
    ]
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "category")
    val category: String, // "HEALTH", "STUDY", "FINANCE", "MIND"

    @ColumnInfo(name = "hex_color")
    val hexColor: String,

    @ColumnInfo(name = "target_days_mask")
    val targetDaysMask: Int, // Bitmask: 1=Seg, 2=Ter, 4=Qua, 8=Qui, 16=Sex, 32=Sab, 64=Dom

    @ColumnInfo(name = "current_streak")
    val currentStreak: Int = 0,

    @ColumnInfo(name = "longest_streak")
    val longestStreak: Int = 0,

    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "icon_key")
    val iconKey: String = "check",

    @ColumnInfo(name = "daily_goal_value")
    val dailyGoalValue: Float = 1.0f,

    @ColumnInfo(name = "unit_label")
    val unitLabel: String = "check-in",

    @ColumnInfo(name = "created_at_epoch_day")
    val createdAtEpochDay: Long = LocalDate.now().toEpochDay()
) {
    fun isScheduledFor(dayOfWeek: DayOfWeek): Boolean {
        val bit = 1 shl (dayOfWeek.value - 1)
        return (targetDaysMask and bit) != 0
    }

    companion object {
        const val CATEGORY_HEALTH = "HEALTH"
        const val CATEGORY_STUDY = "STUDY"
        const val CATEGORY_FINANCE = "FINANCE"
        const val CATEGORY_MIND = "MIND"
        const val ALL_DAYS_MASK = 0b1111111 // 127
    }
}
