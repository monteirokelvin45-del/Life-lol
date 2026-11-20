package com.lifeos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.ZoneId

/**
 * Registro financeiro (Fluxo de Caixa, Burn Matrix diária e Roda do Equilíbrio).
 * `type` assume estritamente "INCOME" ou "EXPENSE".
 */
@Entity(
    tableName = "financial_records",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["type", "epoch_day"]),
        Index(value = ["category"])
    ]
)
data class FinancialRecordEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "type")
    val type: String, // "INCOME" ou "EXPENSE"

    @ColumnInfo(name = "category")
    val category: String, // "Moradia", "Alimentação", etc.

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "epoch_day")
    val epochDay: Long = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toEpochDay(),

    @ColumnInfo(name = "currency_code")
    val currencyCode: String = "MZN",

    @ColumnInfo(name = "payment_method")
    val paymentMethod: String = "WALLET"
) {
    companion object {
        const val TYPE_INCOME = "INCOME"
        const val TYPE_EXPENSE = "EXPENSE"
    }
}
