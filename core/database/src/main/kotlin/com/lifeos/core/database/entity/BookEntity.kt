package com.lifeos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade do domínio de Leitura (controle de páginas, velocidade em páginas/hora,
 * meta diária e projeção de término).
 */
@Entity(
    tableName = "books",
    indices = [
        Index(value = ["status"])
    ]
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "total_pages")
    val totalPages: Int,

    @ColumnInfo(name = "current_page")
    val currentPage: Int = 0,

    @ColumnInfo(name = "pages_per_hour")
    val pagesPerHour: Float = 0f,

    @ColumnInfo(name = "status")
    val status: String = STATUS_READING, // "READING", "COMPLETED"

    @ColumnInfo(name = "author")
    val author: String = "",

    @ColumnInfo(name = "daily_goal_pages")
    val dailyGoalPages: Int = 20,

    @ColumnInfo(name = "pages_read_today")
    val pagesReadToday: Int = 0,

    @ColumnInfo(name = "last_read_epoch_day")
    val lastReadEpochDay: Long? = null
) {
    val progressFraction: Float
        get() = if (totalPages > 0) {
            (currentPage.toFloat() / totalPages.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val estimatedHoursRemaining: Float
        get() = if (pagesPerHour > 0f && currentPage < totalPages) {
            (totalPages - currentPage) / pagesPerHour
        } else {
            0f
        }

    companion object {
        const val STATUS_READING = "READING"
        const val STATUS_COMPLETED = "COMPLETED"
    }
}
