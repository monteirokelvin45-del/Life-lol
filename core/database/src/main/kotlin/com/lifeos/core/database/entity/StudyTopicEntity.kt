package com.lifeos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tópico de estudo regido pela Curva do Esquecimento de Ebbinghaus.
 * Estágios de retenção: 0=1d, 1=3d, 2=7d, 3=16d, 4=30d.
 */
@Entity(
    tableName = "study_topics",
    indices = [
        Index(value = ["next_review_epoch_day"]),
        Index(value = ["last_reviewed_epoch_day"]),
        Index(value = ["subject"])
    ]
)
data class StudyTopicEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "subject")
    val subject: String, // Ex: "Programação"

    @ColumnInfo(name = "topic")
    val topic: String, // Ex: "Coroutines & Flow"

    @ColumnInfo(name = "stage")
    val stage: Int = 0, // 0=1d, 1=3d, 2=7d, 3=16d, 4=30d

    @ColumnInfo(name = "next_review_epoch_day")
    val nextReviewEpochDay: Long,

    @ColumnInfo(name = "alarm_request_code")
    val alarmRequestCode: Int? = null,

    @ColumnInfo(name = "last_reviewed_epoch_day")
    val lastReviewedEpochDay: Long? = null,

    @ColumnInfo(name = "review_count")
    val reviewCount: Int = 0
)
