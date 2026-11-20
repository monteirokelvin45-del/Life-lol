package com.lifeos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Citações extraídas via OCR de livros físicos pela câmera (Modo Leitura Ativa).
 */
@Entity(
    tableName = "book_quotes",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["book_id"]),
        Index(value = ["captured_at_millis"])
    ]
)
data class BookQuoteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "book_id")
    val bookId: Long,

    @ColumnInfo(name = "quote_text")
    val quoteText: String,

    @ColumnInfo(name = "page_number")
    val pageNumber: Int = 0,

    @ColumnInfo(name = "captured_at_millis")
    val capturedAtMillis: Long = System.currentTimeMillis()
)
