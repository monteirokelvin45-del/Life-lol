package com.lifeos.core.ai.engine

import com.lifeos.core.domain.algorithm.QuickFinanceNlpParser
import com.lifeos.core.domain.model.AiExtractedPlanPayload
import com.lifeos.core.domain.model.DetectedExercisePayload
import com.lifeos.core.domain.model.DetectedFinancePayload
import com.lifeos.core.domain.model.DetectedHabitPayload
import com.lifeos.core.domain.model.DetectedStudyPayload
import com.lifeos.core.domain.model.DetectedWorkoutPayload
import java.util.Locale

/**
 * Motor Semântico Local V2 (Offline-First, <5ms, Custo Zero):
 * Interpreta documentos caóticos (editais PDF, fichas DOCX, notas, extratos e ditados por voz)
 * sem exigir chave de nuvem nem internet.
 */
object LocalRuleBasedPlanParser {

    private val exerciseRegex = Regex(
        pattern = """^[-*•\d.)]?\s*([A-Za-zÀ-ÿ0-9 &/()+-]+?)\s*[:\-–]?\s*(\d{1,2})\s*(?:[xX×]|s[ée]ries?\s*(?:de)?)\s*(\d{1,3})(?:\s*reps?)?(?:\s*\(?(\d{2,3})\s*s\)?)?""",
        option = RegexOption.IGNORE_CASE
    )

    private val financeRegex = Regex(
        pattern = """(receita|entrada|sal[áa]rio|despesa|gasto|saida|sa[íi]da)\s*[:\-–]?\s*([A-Za-zÀ-ÿ ]+?)\s*[:\-–]?\s*(?:R\$|MT|MZN|\$)?\s*(\d+(?:[.,]\d{1,2})?)""",
        option = RegexOption.IGNORE_CASE
    )

    private val conversationalSpendLimitRegex = Regex(
        pattern = """gastar\s+(?:menos\s+de\s+|at[ée]\s+)?(?:R\$|MT|MZN|\$)?\s*(\d+(?:[.,]\d{1,2})?)\s*(?:em|com)\s+([A-Za-zÀ-ÿ ]+?)(?:,|;|\.|e\s+treinar|e\s+ler|e\s+estudar|$)""",
        option = RegexOption.IGNORE_CASE
    )

    private val conversationalWorkoutRegex = Regex(
        pattern = """treinar\s+([A-Za-zÀ-ÿ &]+?)\s+(?:na|no|às|as|toda)\s+(segunda|ter[çc]a|quarta|quinta|sexta|s[áa]bado|domingo)""",
        option = RegexOption.IGNORE_CASE
    )

    private val conversationalReadingRegex = Regex(
        pattern = """ler\s+(\d{1,3})\s+p[áa]ginas?\s+(?:por\s+dia|todo\s+dia|diariamente)""",
        option = RegexOption.IGNORE_CASE
    )

    private val conversationalStudyRegex = Regex(
        pattern = """(?:estudar|revisar)\s+([A-Za-zÀ-ÿ0-9 ]{3,45}?)(?:,|;|\.|todo\s+dia|por\s+dia|e\s+treinar|e\s+ler|e\s+gastar|$)""",
        option = RegexOption.IGNORE_CASE
    )

    private val conversationalDailyHabitRegex = Regex(
        pattern = """(beber\s+[^,.;]+|meditar\s*[^,.;]*|correr\s*[^,.;]*|acordar\s+[^,.;]+|caminhar\s*[^,.;]*)\s*(?:todo\s+dia|por\s+dia|diariamente)?""",
        option = RegexOption.IGNORE_CASE
    )

    private val studyRegex = Regex(
        pattern = """(?:estudo|estudar|mat[ée]ria|revis[ãa]o|disciplina|edital|aula)\s*[:\-–]\s*([^:\-–]+?)(?:\s*[:\-–]\s*(.+))?$""",
        option = RegexOption.IGNORE_CASE
    )

    private val habitRegex = Regex(
        pattern = """(?:h[áa]bito|rotina|meta(?:\s+di[áa]ria)?)\s*[:\-–]\s*(.+)""",
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
        var inStudySection = false

        // 1. Extração semântica conversacional (frases corridas / WhatsApp / voz)
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

        conversationalStudyRegex.findAll(rawText).forEach { m ->
            val subj = m.groupValues[1].trim().replaceFirstChar { it.uppercase() }
            if (subj.length >= 3) {
                studies += DetectedStudyPayload(
                    subject = subj,
                    topic = "Revisão Ebbinghaus — Fundamentos & Exercícios"
                )
            }
        }

        conversationalDailyHabitRegex.findAll(rawText).forEach { m ->
            val title = m.groupValues[1].trim().replaceFirstChar { it.uppercase() }
            val (cat, hex) = inferHabitCategoryAndColor(title)
            habits += DetectedHabitPayload(
                name = title,
                category = cat,
                hexColor = hex,
                targetDaysMask = 127
            )
        }

        // 2. Extração estruturada linha a linha (PDFs, editais, fichas, recibos)
        rawText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                val lower = line.lowercase(Locale.ROOT)

                if (lower.contains("edital") || lower.contains("conteúdo programático") || lower.contains("disciplinas")) {
                    inStudySection = true
                }

                detectWeekdayHeader(lower)?.let { day ->
                    if (!lower.contains("treinar ")) {
                        inStudySection = false
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
                    val s = match.groupValues[1].trim()
                    val t = match.groupValues[2].trim().ifBlank { "Revisão de Fixação (1d)" }
                    studies += DetectedStudyPayload(subject = s, topic = t)
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

                // Tópicos numerados/marcadores dentro de bloco de Edital/Disciplina
                if (inStudySection && (line.startsWith("-") || line.startsWith("•") || line.first().isDigit())) {
                    val cleanTopic = line.replace(Regex("""^[-•*\d.)]+\s*"""), "").trim()
                    if (cleanTopic.length in 4..90) {
                        studies += DetectedStudyPayload(
                            subject = "Edital Importado",
                            topic = cleanTopic
                        )
                        return@forEach
                    }
                }

                // Linhas curtas de despesa estilo extrato/recibo ("Uber 38,50", "Almoço 42 débito")
                if (line.length <= 42 && (lower.contains("r$") || lower.contains("mzn") || lower.contains("débito") || lower.contains("pix") || lower.contains("mpesa"))) {
                    QuickFinanceNlpParser.parse(line)?.let { quickTx ->
                        finances += DetectedFinancePayload(
                            amount = quickTx.amount,
                            type = quickTx.type,
                            category = quickTx.category
                        )
                    }
                }
            }

        if (exercisesBuffer.isNotEmpty()) {
            workouts += DetectedWorkoutPayload(
                name = currentWorkoutName,
                dayOfWeek = currentDayOfWeek,
                exercises = exercisesBuffer.toList()
            )
        }

        // Fallback inteligente caso o utilizador cole um objetivo livre curto sem palavras-chave rígidas
        if (habits.isEmpty() && workouts.isEmpty() && studies.isEmpty() && finances.isEmpty()) {
            val firstSentence = rawText.lineSequence().firstOrNull { it.isNotBlank() }?.take(60)?.trim().orEmpty()
            if (firstSentence.isNotEmpty()) {
                val (cat, hex) = inferHabitCategoryAndColor(firstSentence)
                habits += DetectedHabitPayload(
                    name = firstSentence,
                    category = cat,
                    hexColor = hex,
                    targetDaysMask = 127
                )
            }
        }

        return AiExtractedPlanPayload(
            detectedHabits = habits.distinctBy { it.name.lowercase(Locale.ROOT) },
            detectedWorkouts = workouts.distinctBy { "${it.dayOfWeek}-${it.name.lowercase(Locale.ROOT)}" },
            detectedStudies = studies.distinctBy { "${it.subject}-${it.topic}".lowercase(Locale.ROOT) },
            detectedFinances = finances
        )
    }

    private fun defaultExercisesForGroup(group: String): List<DetectedExercisePayload> {
        val lower = group.lowercase(Locale.ROOT)
        return when {
            "costas" in lower || "bíceps" in lower || "biceps" in lower -> listOf(
                DetectedExercisePayload("Barra Fixa Pronada", 4, 10, 90),
                DetectedExercisePayload("Remada Australiana", 4, 12, 75),
                DetectedExercisePayload("Chin-ups Supinados", 3, 10, 90)
            )
            "perna" in lower || "agacha" in lower || "inferior" in lower -> listOf(
                DetectedExercisePayload("Agachamento Pistol Assistido", 4, 8, 90),
                DetectedExercisePayload("Afundo Búlgaro", 4, 12, 75),
                DetectedExercisePayload("Panturrilha Unilateral", 4, 20, 45)
            )
            else -> listOf(
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
            "gasto" in lower || "finan" in lower || "poup" in lower || "econom" in lower -> "FINANCE" to "#00E5FF"
            "medit" in lower || "sono" in lower || "diário" in lower || "acordar" in lower -> "MIND" to "#FF4800"
            else -> "HEALTH" to "#10E05B"
        }
    }
}
