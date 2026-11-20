package com.lifeos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade criptografada para o Esquadrão Silencioso — Ghost Multiplayer Assíncrono (Pilar 5.4).
 * Armazena até 3 parceiros de foco sem salas de bate-papo ou feeds de distração.
 */
@Entity(
    tableName = "ghost_squad_members",
    indices = [Index(value = ["slot_index"], unique = true)]
)
data class GhostSquadMemberEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "slot_index")
    val slotIndex: Int,

    @ColumnInfo(name = "codename")
    val codename: String,

    @ColumnInfo(name = "focus_domain")
    val focusDomain: String,

    @ColumnInfo(name = "aura_hex_color")
    val auraHexColor: String,

    @ColumnInfo(name = "base_minutes_today")
    val baseMinutesToday: Int = 45
)
