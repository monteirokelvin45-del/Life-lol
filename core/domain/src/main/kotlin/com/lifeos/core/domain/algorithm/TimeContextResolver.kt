package com.lifeos.core.domain.algorithm

import com.lifeos.core.domain.model.TimeOfDayContext
import java.time.LocalTime

/**
 * Resolve o estado mutável da interface principal com base no ciclo circadiano (Pilar 1.2).
 */
object TimeContextResolver {

    fun resolve(time: LocalTime = LocalTime.now()): TimeOfDayContext {
        val hour = time.hour
        return when (hour) {
            in 6..11 -> TimeOfDayContext.MORNING_COGNITIVE
            in 12..16 -> TimeOfDayContext.AFTERNOON_EXECUTION
            in 17..19 -> TimeOfDayContext.LATE_AFTERNOON_PHYSIOLOGY
            else -> TimeOfDayContext.NIGHT_CLOSURE_SLEEP
        }
    }
}
