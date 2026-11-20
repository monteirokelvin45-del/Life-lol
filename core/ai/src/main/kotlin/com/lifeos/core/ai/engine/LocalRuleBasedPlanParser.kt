package com.lifeos.core.ai.engine

import com.lifeos.core.domain.model.AiExtractedPlanPayload
import com.lifeos.core.domain.model.DetectedExercisePayload
import com.lifeos.core.domain.model.DetectedFinancePayload
import com.lifeos.core.domain.model.DetectedHabitPayload
import com.lifeos.core.domain.model.DetectedStudyPayload
import com.lifeos.core.domain.model.DetectedWorkoutPayload
import java.util.Locale

/**
 * Motor PLN determinístico local (Offline-First Fallback) que interpreta planos caóticos
 * em Português/Inglês quando o aparelho está sem rede ou sem chave de API remota configurada.
 */
object LocalRuleBasedPlanParser {

    private val exerciseRegex = Regex(
        pattern = """^[-*•]?\s*([A-Za-zÀ-ÿ0-9 &/()+-]+?)\s*[:\-–]?\s*(\d{1,2})\s*[xX×]\s*(\d{1,3})(?:\s*\(?(\d{2,3})\s*s\)?)?""",
        option = RegexOption.IGNORE_CASE
    )

    private val financeRegex = Regex(
        pattern = """(receita|entrada|sal[áa]rio|despesa|gasto|saida|sa[íi]da)\s*[:\-–]?\s*([A-Za-zÀ-ÿ ]+?)\s*[:\-–]?\s*(?:R\$|MT|MZN|\$)?\s*(\d+(?:[.,]\d{1,2})?)""",
        option = RegexOption.IGNORE_CASE
    )

    private val studyRegex = Regex(
        pattern = """(?:estudo|mat[ée]ria|revis[ãa]o|disciplina)\s*[:\-–]\s*([^:\-–]+?)\s*[:\-–]\s*(.+)""",
        option = RegexOption.IGNORE_CASE
    )

    private val habitRegex = Regex(
        pattern = """(?:h[áa]bito|rotina|meta di[áa]ria)\s*[:\-–]\s*(.+)""",
        option = RegexOption.IGNORE_CASE
    )

    fun parse(rawText: String): AiExtractedPlanPayload {
        val habits = mutableListOf<DetectedHabitPayload>()
        val exercisesBuffer = mutableListOf<DetectedExercisePayload>()
        val workouts = mutableListOf<DetectedWorkoutPayload>()
        val studies = mutableListOf<DetectedStudyPayload>()
        val finances = mutableListOf<DetectedFinancePayload>()

        var currentWorkoutName = "Calistenia & Força Funcional"
        var currentDayOfWeek = 1

        rawText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                val lower = line.lowercase(Locale.ROOT)

                // Detecta cabeçalho de dia de treino
                detectWeekdayHeader(lower)?.let { day ->
                    if (exercisesBuffer.isNotEmpty()) {
                        workouts += DetectedWorkoutPayload(
                            name = currentWorkoutName,
                            dayOfWeek = currentDayOfWeek,
                            exercises = exercisesBuffer.toList()
                        )
                        exercisesBuffer.clear()
                    }
                    currentDayOfWeek = day
                    currentWorkoutName = line.removePrefix("#").trim()
                }

                // 1. Exercícios no padrão "Barra Fixa 4x10 90s"
                exerciseRegex.find(line)?.let { match ->
                    val name = match.groupValues[1].trim()
                    val sets = match.groupValues[2].toIntOrNull() ?: 4
                    val reps = match.groupValues[3].toIntOrNull() ?: 10
                    val rest = match.groupValues[4].toIntOrNull() ?: 90
                    exercisesBuffer += DetectedExercisePayload(
                        name = name,
                        targetSets = sets,
                        targetReps = reps,
                        restSeconds = rest
                    )
                    return@forEach
                }

                // 2. Finanças no padrão "Despesa: Alimentação 850"
                financeRegex.find(line)?.let { match ->
                    val kind = match.groupValues[1].lowercase(Locale.ROOT)
                    val category = match.groupValues[2].trim().ifBlank { "Geral" }
                    val amount = match.groupValues[3].replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (amount > 0.0) {
                        val type = if (kind.startsWith("receita") || kind.startsWith("entrada") || kind.startsWith("sal")) {
                            "INCOME"
                        } else {
                            "EXPENSE"
                        }
                        finances += DetectedFinancePayload(
                            amount = amount,
                            type = type,
                            category = category
                        )
                    }
                    return@forEach
                }

                // 3. Estudos no padrão "Estudo: Programação - Coroutines & Flow"
                studyRegex.find(line)?.let { match ->
                    studies += DetectedStudyPayload(
                        subject = match.groupValues[1].trim(),
                        topic = match.groupValues[2].trim()
                    )
                    return@forEach
                }

                // 4. Hábitos explícitos no padrão "Hábito: Leitura 30 min"
                habitRegex.find(line)?.let { match ->
                    val habitTitle = match.groupValues[1].trim()
                    val (category, color) = inferHabitCategoryAndColor(habitTitle)
                    habits += DetectedHabitPayload(
                        name = habitTitle,
                        category = category,
                        hexColor = color,
                        targetDaysMask = 127
                    )
                    return@forEach
                }
            }

        if (exercisesBuffer.isNotEmpty()) {
            workouts += DetectedWorkoutPayload(
                name = currentWorkoutName,
                dayOfWeek = currentDayOfWeek,
                exercises = exercisesBuffer.toList()
            )
        }

        return AiExtractedPlanPayload(
            detectedHabits = habits,
            detectedWorkouts = workouts,
            detectedStudies = studies,
            detectedFinances = finances
        )
    }

    private fun detectWeekdayHeader(lowerLine: String): Int? = when {
        "segunda" in lowerLine -> 1
        "terça" in lowerLine || "terca" in lowerLine -> 2
        "quarta" in lowerLine -> 3
        "quinta" in lowerLine -> 4
        "sexta" in lowerLine -> 5
        "sábado" in lowerLine || "sabado" in lowerLine -> 6
        "domingo" in lowerLine -> 7
        else -> null
    }

    private fun inferHabitCategoryAndColor(title: String): Pair<String, String> {
        val lower = title.lowercase(Locale.ROOT)
        return when {
            "leitura" in lower || "estud" in lower || "anki" in lower -> "STUDY" to "#8B5CF6"
            "gasto" in lower || "finan" in lower || "poup" in lower -> "FINANCE" to "#00E5FF"
            "medit" in lower || "sono" in lower || "diário" in lower -> "MIND" to "#FF4800"
            else -> "HEALTH" to "#10E05B"
        }
    }
}
