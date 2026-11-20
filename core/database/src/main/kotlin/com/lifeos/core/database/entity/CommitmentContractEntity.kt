package com.lifeos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade criptografada (SQLCipher AES-256) para Contratos de Compromisso Anti-Charity (Pilar 5.5).
 * Caso o usuário falhe na meta semanal sem Escudo Anti-Falha ativo, a caução financeira é
 * contabilizada compulsoriamente para a instituição configurada.
 */
@Entity(
    tableName = "commitment_contracts",
    indices = [Index(value = ["is_active"])]
)
data class CommitmentContractEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "weekly_target_ratio")
    val weeklyTargetRatio: Float = 0.80f,

    @ColumnInfo(name = "stake_amount")
    val stakeAmount: Double,

    @ColumnInfo(name = "currency_code")
    val currencyCode: String = "MZN",

    @ColumnInfo(name = "institution_name")
    val institutionName: String,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at_epoch_day")
    val createdAtEpochDay: Long = 0L,

    @ColumnInfo(name = "last_audited_week_epoch")
    val lastAuditedWeekEpoch: Long? = null
)
