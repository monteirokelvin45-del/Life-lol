package com.lifeos.core.database.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class LifeOsTypeConvertersTest {

    private val converters = LifeOsTypeConverters()

    @Test
    fun `localDate round-trip preserves exact epoch day`() {
        val date = LocalDate.of(2026, 11, 20)
        val encoded = converters.localDateToEpochDay(date)
        val decoded = converters.epochDayToLocalDate(encoded)
        assertEquals(date, decoded)
        assertNull(converters.localDateToEpochDay(null))
        assertNull(converters.epochDayToLocalDate(null))
    }

    @Test
    fun `instant round-trip preserves exact epoch millis`() {
        val instant = Instant.ofEpochMilli(1795180800123L)
        val encoded = converters.instantToEpochMilli(instant)
        val decoded = converters.epochMilliToInstant(encoded)
        assertEquals(instant, decoded)
    }

    @Test
    fun `localDateTime round-trip preserves ISO-8601 precision`() {
        val dt = LocalDateTime.of(2026, 11, 20, 6, 30, 15)
        val iso = converters.localDateTimeToIsoString(dt)
        assertEquals(dt, converters.isoStringToLocalDateTime(iso))
    }

    @Test
    fun `stringList round-trip safely handles commas quotes and unicode`() {
        val tags = listOf("Barra Fixa, 4x10", "Coroutines & \"Flow\"", "🔥 32 DIAS")
        val json = converters.stringListToJson(tags)
        assertEquals(tags, converters.jsonToStringList(json))
    }

    @Test
    fun `numeric lists round-trip accurately`() {
        val stages = listOf(1, 3, 7, 16, 30)
        assertEquals(stages, converters.jsonToIntList(converters.intListToJson(stages)))

        val intensities = listOf(0f, 0.25f, 0.5f, 1.0f)
        assertEquals(intensities, converters.jsonToFloatList(converters.floatListToJson(intensities)))
    }
}
