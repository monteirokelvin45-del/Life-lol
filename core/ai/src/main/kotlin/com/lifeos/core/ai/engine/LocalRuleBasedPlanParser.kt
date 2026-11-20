package com.lifeos.core.ai.engine

import com.lifeos.core.domain.model.AiExtractedPlanPayload
import com.lifeos.core.domain.model.DetectedExercisePayload
import com.lifeos.core.domain.model.DetectedFinancePayload
import com.lifeos.core.domain.model.DetectedHabitPayload
import com.lifeos.core.domain.model.DetectedStudyPayload
import com.lifeos.core.domain.model.DetectedWorkoutPayload
import java.util.Locale

/**
 * Motor PLN determinístico local (Offline-First Fallback) que interpreta tanto arquivos
 * estruturados (fichas de treino, editais, notas) quanto frases em linguagem natural por voz:
 * Ex: "Minha meta esse mês é gastar menos de R$ 500 em comida fora, treinar costas na sexta e ler 20 páginas por dia".
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

    private val conversationalSpendLimitRegex = Regex(
        pattern = """gastar\s+(?:menos\s+de\s+|at[ée]\s+)?(?:R\$|MT|MZN|\$)?\s*(\d+(?:[.,]\d{1,2})?)\s*(?:em|com)\s+([A-Za-zÀ-ÿ ]+?)(?:,|;|\.|e\s+treinar|e\s+ler|$)""",
        option = RegexOption.IGNORE_CASE
    )

    private val conversationalWorkoutRegex = Regex(
        pattern = """treinar\s+([A-Za-zÀ-ÿ &]+?)\s+(?:na|no|às|as|toda)\s+(segunda|ter[çc]a|quarta|quinta|sexta|s[áa]bado|domingo)""",
        option = RegexOption.IGNORE_CASE
    )

    private val conversationalReadingRegex = Regex(
        pattern = """ler\s+(\d{1,3})\s+p[áa]ginas?\s+por\s+dia""",
        option = RegexOption.IGNORE_CASE
    )

    private val studyRegex = Regex(
        pattern = """(?:estudo|estudar|mat[ée]ria|revis[ãa]o|disciplina)\s*[:\-–]\s*([^:\-–]+?)\s*[:\-–]\s*(.+)""",
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

        // 0. Extração conversacional de frases corridas / ditado por voz
        conversationalSpendLimitRegex.findAll(rawText).forEach { m ->
            val amount = m.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
            val cat = m.groupValues[2].trim().replaceFirstChar { it.uppercase() }
            if (amount > 0.0) {
                habits += DetectedHabitPayload(
                    name = "Teto de Gasto: $cat (≤ ${amount.toInt()})",
                    category = "FINANCE",
                    hexColor = "#00E5FF",
                    targetDaysMask = 127
                )
            }
        }

        conversationalWorkoutRegex.findAll(rawText).forEach { m ->
            val muscleGroup = m.groupValues[1].trim().replaceFirstChar { it.uppercase() }
            val day = detectWeekdayHeader(m.groupValues[2].lowercase(Locale.ROOT)) ?: 5
            workouts += DetectedWorkoutPayload(
                name = "Treino de $muscleGroup",
                dayOfWeek = day,
                exercises = defaultExercisesForGroup(muscleGroup)
            )
        }

        conversationalReadingRegex.findAll(rawText).forEach { m ->
            val pages = m.groupValues[1].toIntOrNull() ?: 20
            habits += DetectedHabitPayload(
                name = "Leitura Diária ($pages páginas)",
                category = "STUDY",
                hexColor = "#8B5CF6",
                targetDaysMask = 127
            )
        }

        // 1. Extração linha a linha de documentos PDF / DOCX / Markdown
        rawText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                val lower = line.lowercase(Locale.ROOT)

                detectWeekdayHeader(lower)?.let { day ->
                    if (!lower.contains("treinar ")) {
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
                }

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

                studyRegex.find(line)?.let { match ->
                    studies += DetectedStudyPayload(
                        subject = match.groupValues[1].trim(),
                        topic = match.groupValues[2].trim()
                    )
                    return@forEach
                }

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
            detectedHabits = habits.distinctBy { it.name },
            detectedWorkouts = workouts,
            detectedStudies = studies,
            detectedFinances = finances
        )
    }

    private fun defaultExercisesForGroup(group: String): List<DetectedExercisePayload> {
        val lower = group.lowercase(Locale.ROOT)
        return if ("costas" in lower || "bíceps" in lower || "biceps" in lower) {
            listOf(
                DetectedExercisePayload("Barra Fixa Pronada", 4, 10, 90),
                DetectedExercisePayload("Remada Australiana", 4, 12, 75),
                DetectedExercisePayload("Chin-ups Supinados", 3, 10, 90)
            )
        } else {
            listOf(
                DetectedExercisePayload("Flexão Explosiva", 4, 15, 75),
                DetectedExercisePayload("Paralelas (Dips)", 4, 12, 90),
                DetectedExercisePayload("Prancha RKC", 3, 45, 60)
            )
        }
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
            "leitura" in lower || "estud" in lower || "anki" in lower || "página" in lower -> "STUDY" to "#8B5CF6"
            "gasto" in lower || "finan" in lower || "poup" in lower -> "FINANCE" to "#00E5FF"
            "medit" in lower || "sono" in lower || "diário" in lower -> "MIND" to "#FF4800"
            else -> "HEALTH" to "#10E05B"
        }
    }
}
