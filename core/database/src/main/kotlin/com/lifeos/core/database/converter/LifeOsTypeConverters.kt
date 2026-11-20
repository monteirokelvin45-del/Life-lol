package com.lifeos.core.database.converter

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Conversores determinísticos e null-safe do Room para tipos temporais (`java.time`)
 * e coleções tipadas (`List<String>`, `List<Int>`, `List<Long>`, `List<Float>`).
 *
 * Nomes de métodos distintos evitam colisões de assinatura JVM (Type Erasure) no KAPT.
 */
class LifeOsTypeConverters {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // =========================================================================
    // 1. LocalDate <-> Long (Epoch Day UTC)
    // =========================================================================

    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    // =========================================================================
    // 2. Instant <-> Long (Epoch Millis UTC)
    // =========================================================================

    @TypeConverter
    fun instantToEpochMilli(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun epochMilliToInstant(epochMilli: Long?): Instant? = epochMilli?.let(Instant::ofEpochMilli)

    // =========================================================================
    // 3. LocalDateTime <-> String (ISO-8601)
    // =========================================================================

    @TypeConverter
    fun localDateTimeToIsoString(dateTime: LocalDateTime?): String? =
        dateTime?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    @TypeConverter
    fun isoStringToLocalDateTime(value: String?): LocalDateTime? =
        value?.takeIf { it.isNotBlank() }?.let {
            LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }

    // =========================================================================
    // 4. List<String> <-> JSON String (Imune a vírgulas ou caracteres especiais)
    // =========================================================================

    @TypeConverter
    fun stringListToJson(list: List<String>?): String? =
        list?.let { json.encodeToString(ListSerializer(String.serializer()), it) }

    @TypeConverter
    fun jsonToStringList(raw: String?): List<String>? =
        raw?.let {
            runCatching {
                json.decodeFromString(ListSerializer(String.serializer()), it)
            }.getOrDefault(emptyList())
        }

    // =========================================================================
    // 5. List<Int> <-> JSON String
    // =========================================================================

    @TypeConverter
    fun intListToJson(list: List<Int>?): String? =
        list?.let { json.encodeToString(ListSerializer(Int.serializer()), it) }

    @TypeConverter
    fun jsonToIntList(raw: String?): List<Int>? =
        raw?.let {
            runCatching {
                json.decodeFromString(ListSerializer(Int.serializer()), it)
            }.getOrDefault(emptyList())
        }

    // =========================================================================
    // 6. List<Long> <-> JSON String
    // =========================================================================

    @TypeConverter
    fun longListToJson(list: List<Long>?): String? =
        list?.let { json.encodeToString(ListSerializer(Long.serializer()), it) }

    @TypeConverter
    fun jsonToLongList(raw: String?): List<Long>? =
        raw?.let {
            runCatching {
                json.decodeFromString(ListSerializer(Long.serializer()), it)
            }.getOrDefault(emptyList())
        }

    // =========================================================================
    // 7. List<Float> <-> JSON String (Vetores de intensidade de Heatmaps)
    // =========================================================================

    @TypeConverter
    fun floatListToJson(list: List<Float>?): String? =
        list?.let { json.encodeToString(ListSerializer(Float.serializer()), it) }

    @TypeConverter
    fun jsonToFloatList(raw: String?): List<Float>? =
        raw?.let {
            runCatching {
                json.decodeFromString(ListSerializer(Float.serializer()), it)
            }.getOrDefault(emptyList())
        }
}
