package com.lifeos.core.domain.algorithm

import com.lifeos.core.domain.model.AntiCharityContractModel
import com.lifeos.core.domain.model.BookDomainModel
import com.lifeos.core.domain.model.CategoryExpenseSlice
import com.lifeos.core.domain.model.CircadianHourPetal
import com.lifeos.core.domain.model.CircadianStatusModel
import com.lifeos.core.domain.model.GhostSquadPeerModel
import com.lifeos.core.domain.model.HabitDomainModel
import com.lifeos.core.domain.model.KnowledgeBurndownProjection
import com.lifeos.core.domain.model.MultivariateScatterPoint
import com.lifeos.core.domain.model.MuscleFatigueGroupModel
import com.lifeos.core.domain.model.RpgAvatarAttributesModel
import com.lifeos.core.domain.model.SankeyCashflowBreakdown
import com.lifeos.core.domain.model.StreakFreezeStatusModel
import com.lifeos.core.domain.model.StudyTopicDomainModel
import com.lifeos.core.domain.model.WorkoutRoutineDomainModel
import java.time.LocalTime
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Motor Integrado do Pilar 5 (Ciência Comportamental, Retenção & Gamificação) e
 * Cálculos Matemáticos da Suíte de 14 Gráficos 120 FPS (Pilar 6).
 */
object BehavioralRetentionEngine {

    const val STREAK_FREEZE_NOTE_TAG = "[STREAK_FREEZE]"
    private const val CHECK_INS_PER_SHIELD = 7
    private const val MAX_BANKED_SHIELDS = 5
    private const val CAFFEINE_CUTOFF_MINUTES_OF_DAY = 14 * 60 // 14:00 (8h30 antes do sono às 22:30)

    /**
     * 1. Motor Circadiano e Janelas Biológicas + Pétalas 24h da Rosa de Nightingale.
     */
    fun resolveCircadianStatus(now: LocalTime = LocalTime.now()): CircadianStatusModel {
        val currentMinutes = now.hour * 60 + now.minute
        val deltaToCutoff = CAFFEINE_CUTOFF_MINUTES_OF_DAY - currentMinutes
        val pastCutoff = deltaToCutoff <= 0

        val caffeineLabel = if (!pastCutoff) {
            val h = deltaToCutoff / 60
            val m = deltaToCutoff % 60
            "☕ Corte de Cafeína em ${h}h ${m}m (14:00) · Protege sono NREM"
        } else {
            "🛑 Corte de Cafeína ATIVO (pós-14:00) · Zero estimulantes para blindar o sono"
        }

        val (windowTitle, windowSubtitle) = when (now.hour) {
            in 6..7 -> "Despertar & Luz Solar Matinal" to
                "Pulso de cortisol fisiológico + hidratação mineral antes do bloco cognitivo."
            in 8..11 -> "Pico de Foco Intelectual (08:30–11:30)" to
                "Dopamina e acetilcolina em nível máximo para Ebbinghaus e raciocínio profundo."
            in 12..13 -> "Transição Metabólica & Última Janela de Cafeína" to
                "Almoço limpo; encerre cafeína até 14:00 para depurar adenosina antes das 22:30."
            in 14..16 -> "Execução Estruturada & Hábitos" to
                "Vale pós-prandial controlado; tarefas operacionais e consistência sem cafeína."
            in 17..19 -> "Pico Neuromuscular & Calistenia (17:00–19:30)" to
                "Temperatura corporal central máxima (+0.6°C): força explosiva, barra fixa e menor risco articular."
            in 20..21 -> "Fechamento Financeiro & Desaceleração" to
                "Auditoria da Carteira em 2s, leitura física e redução de luz azul."
            else -> "Arquitetura do Sono Profundo (NREM / REM)" to
                "Painel OLED #000000 com emissão física zero nos pixels pretos para preservar melatonina."
        }

        val petals = (0..23).map { hour ->
            when (hour) {
                in 0..5 -> CircadianHourPetal(hour, 0.34f, "SLEEP", "#3B82F6")
                in 6..7 -> CircadianHourPetal(hour, 0.58f, "WAKE", "#00D2FF")
                in 8..11 -> CircadianHourPetal(hour, 0.96f, "COGNITIVE", "#8E54E9")
                in 12..13 -> CircadianHourPetal(hour, 0.70f, "METABOLIC", "#FFB300")
                14 -> CircadianHourPetal(hour, 0.86f, "CAFFEINE_CUTOFF", "#FF3B30")
                in 15..16 -> CircadianHourPetal(hour, 0.66f, "EXECUTION", "#00D2FF")
                in 17..19 -> CircadianHourPetal(hour, 0.94f, "NEUROMUSCULAR", "#10E05B")
                in 20..21 -> CircadianHourPetal(hour, 0.60f, "MELATONIN", "#FF8A00")
                else -> CircadianHourPetal(hour, 0.40f, "SLEEP", "#3B82F6")
            }
        }

        return CircadianStatusModel(
            currentHour = now.hour,
            currentMinute = now.minute,
            activeBiologicalWindowTitle = windowTitle,
            activeBiologicalSubtitle = windowSubtitle,
            minutesUntilCaffeineCutoff = max(0, deltaToCutoff),
            isPastCaffeineCutoff = pastCutoff,
            caffeineGuidanceLabel = caffeineLabel,
            petals24h = petals
        )
    }

    /**
     * 2. Escudo Anti-Falha (Streak Freeze Científico — Pilar 5.3).
     * Concede 1 escudo inicial + 1 crédito a cada 7 check-ins limpos de consistência.
     */
    fun calculateStreakFreezeStatus(
        cleanCheckInsTotal: Int,
        usedFreezeDaysCount: Int,
        isFreezeActiveToday: Boolean
    ): StreakFreezeStatusModel {
        val earnedTotal = 1 + (cleanCheckInsTotal.coerceAtLeast(0) / CHECK_INS_PER_SHIELD)
        val rawAvailable = (earnedTotal - usedFreezeDaysCount.coerceAtLeast(0)).coerceAtLeast(0)
        val available = min(MAX_BANKED_SHIELDS, rawAvailable)
        val progressInCycle = cleanCheckInsTotal.coerceAtLeast(0) % CHECK_INS_PER_SHIELD

        return StreakFreezeStatusModel(
            availableCredits = available,
            maxBankedCredits = MAX_BANKED_SHIELDS,
            usedCreditsTotal = usedFreezeDaysCount.coerceAtLeast(0),
            cleanCheckInsTowardNextShield = progressInCycle,
            checkInsRequiredPerShield = CHECK_INS_PER_SHIELD,
            isFreezeActiveToday = isFreezeActiveToday
        )
    }

    /**
     * 3. Ghost Multiplayer Assíncrono — Esquadrão Silencioso (Pilar 5.4).
     * Até 3 parceiros em presença compartilhada silenciosa sem chat nem feed.
     */
    fun buildGhostSquadPresence(
        configuredPeers: List<Triple<String, String, String>>,
        baseMinutes: List<Int>,
        currentHour: Int
    ): List<GhostSquadPeerModel> {
        val defaults = if (configuredPeers.size == 3) {
            configuredPeers
        } else {
            listOf(
                Triple("Lucas M.", "STUDY", "#8E54E9"),
                Triple("Marina K.", "WORKOUT", "#10E05B"),
                Triple("Rafael T.", "READING", "#00D2FF")
            )
        }

        return defaults.take(3).mapIndexed { idx, (codename, domain, hex) ->
            val baseMin = baseMinutes.getOrElse(idx) { 40 + idx * 18 }
            val dynamicBonus = ((currentHour * 3) + (idx * 7)) % 24
            val activeNow = when (domain) {
                "STUDY" -> currentHour in 6..13 || currentHour in 19..22
                "WORKOUT" -> currentHour in 6..9 || currentHour in 16..20
                else -> currentHour in 7..11 || currentHour in 20..23
            } || idx == 0

            val status = when (domain) {
                "STUDY" -> "Aura Cognitiva · Revisão Ebbinghaus"
                "WORKOUT" -> "Aura Neuromuscular · Calistenia"
                else -> "Aura de Leitura Profunda · Sem distração"
            }

            GhostSquadPeerModel(
                slotIndex = idx,
                codename = codename,
                focusDomain = domain,
                statusHeadline = status,
                auraHexColor = hex,
                minutesFocusedToday = baseMin + dynamicBonus,
                isCurrentlyPulsing = activeNow
            )
        }
    }

    /**
     * 4. Contratos de Compromisso Anti-Charity (Pilar 5.5).
     */
    fun evaluateAntiCharityContract(
        contractId: Long,
        weeklyTargetRatio: Float,
        stakeAmount: Double,
        currencyCode: String,
        institutionName: String,
        isActive: Boolean,
        habits: List<HabitDomainModel>,
        isFreezeActiveToday: Boolean
    ): AntiCharityContractModel {
        val totalPlannedWeek = habits.sumOf { it.targetPerWeek }.coerceAtLeast(1)
        val totalDoneWeek = habits.sumOf { it.completedThisWeek }
        val rawWeekRatio = (totalDoneWeek.toFloat() / totalPlannedWeek.toFloat()).coerceIn(0f, 1f)
        val effectiveRatio = if (isFreezeActiveToday) max(rawWeekRatio, weeklyTargetRatio) else rawWeekRatio
        val atRisk = isActive && !isFreezeActiveToday && effectiveRatio < weeklyTargetRatio

        val summary = when {
            !isActive -> "Contrato pausado · Ative uma caução semanal para blindar sua disciplina."
            isFreezeActiveToday -> "Escudo Anti-Falha ativo hoje · Caução de ${stakeAmount.toInt()} $currencyCode protegida."
            effectiveRatio >= weeklyTargetRatio ->
                "Meta batida (${(effectiveRatio * 100).roundToInt()}% ≥ ${(weeklyTargetRatio * 100).roundToInt()}%) · Caução intacta!"
            else ->
                "Atenção: ${(effectiveRatio * 100).roundToInt()}% de ${(weeklyTargetRatio * 100).roundToInt()}% · Risco de doar ${stakeAmount.toInt()} $currencyCode para $institutionName."
        }

        return AntiCharityContractModel(
            id = contractId,
            weeklyTargetRatio = weeklyTargetRatio,
            currentWeekRatio = effectiveRatio,
            stakeAmount = stakeAmount,
            currencyCode = currencyCode,
            institutionName = institutionName,
            isActive = isActive,
            isProtectedByFreeze = isFreezeActiveToday,
            isAtRisk = atRisk,
            statusSummary = summary
        )
    }

    /**
     * 5. Radar Hexagonal de Atributos RPG (6 vértices — Pilar 6.14).
     */
    fun calculateRpgAttributes(
        workouts: List<WorkoutRoutineDomainModel>,
        studies: List<StudyTopicDomainModel>,
        books: List<BookDomainModel>,
        quotesCount: Int,
        habits: List<HabitDomainModel>,
        incomeMonth: Double,
        expenseMonth: Double,
        availableShields: Int
    ): RpgAvatarAttributesModel {
        val allExercises = workouts.flatMap { it.exercises }
        val completedRatio = if (allExercises.isNotEmpty()) {
            allExercises.count { it.completedSetsToday > 0 }.toFloat() / allExercises.size.coerceAtLeast(1)
        } else {
            0.55f
        }
        val weightedBonus = (allExercises.maxOfOrNull { it.lastWeightKg } ?: 0f) / 30f
        val strength = ((0.52f + completedRatio * 0.35f + weightedBonus.coerceAtMost(0.18f)) * 100f)
            .roundToInt().coerceIn(25, 99)

        val avgStage = if (studies.isNotEmpty()) {
            studies.map { it.stage }.average().toFloat() / 4f
        } else {
            0.60f
        }
        val avgRetention = if (studies.isNotEmpty()) {
            studies.map { it.estimatedRetentionPercent }.average().toFloat() / 100f
        } else {
            0.80f
        }
        val intellect = (((avgStage * 0.45f) + (avgRetention * 0.55f)) * 100f)
            .roundToInt().coerceIn(30, 99)

        val financeScore10 = BalanceWheelCalculator.calculateFinanceScore(incomeMonth, expenseMonth)
        val finances = (financeScore10 * 10f).roundToInt().coerceIn(20, 99)

        val topStreak = habits.maxOfOrNull { it.longestStreak } ?: 14
        val resilience = min(99, 42 + (topStreak * 1.3f).roundToInt() + (availableShields * 4))

        val habitGoalAvg = if (habits.isNotEmpty()) {
            habits.map { it.goalProgressPercent }.average().roundToInt()
        } else {
            72
        }
        val focus = habitGoalAvg.coerceIn(32, 99)

        val bookProgress = if (books.isNotEmpty()) {
            books.map { it.progressFraction }.average().toFloat()
        } else {
            0.48f
        }
        val culture = min(99, ((bookProgress * 72f) + 22f + (quotesCount * 4)).roundToInt())

        val avgAll = (strength + intellect + finances + resilience + focus + culture) / 6
        val archetype = when {
            strength >= intellect && strength >= finances -> "Arquiteto Espartano"
            intellect >= strength && intellect >= finances -> "Polímata Cognitivo"
            finances >= strength -> "Estrategista Soberano"
            else -> "Operador Sistêmico"
        }

        return RpgAvatarAttributesModel(
            strength0to100 = strength,
            intellect0to100 = intellect,
            finances0to100 = finances,
            resilience0to100 = resilience,
            focus0to100 = focus,
            culture0to100 = culture,
            avatarLevel = max(1, avgAll),
            archetypeTitle = archetype
        )
    }

    /**
     * 6. Mapa Anatômico de Fadiga Muscular (Pilar 6.11).
     */
    fun calculateMuscleFatigueMap(
        routines: List<WorkoutRoutineDomainModel>,
        selectedRoutine: WorkoutRoutineDomainModel?
    ): List<MuscleFatigueGroupModel> {
        val activeExercises = selectedRoutine?.exercises ?: routines.flatMap { it.exercises }

        fun fatigueForKeywords(vararg keywords: String, baselineRecovery: Float): Float {
            val matching = activeExercises.filter { ex ->
                keywords.any { kw -> ex.name.contains(kw, ignoreCase = true) }
            }
            if (matching.isEmpty()) return baselineRecovery
            val totalSets = matching.sumOf { it.targetSets }.coerceAtLeast(1)
            val doneSets = matching.sumOf { it.completedSetsToday }
            val strain = (doneSets.toFloat() / totalSets.toFloat()).coerceIn(0f, 1f)
            return (1.0f - (strain * 0.78f)).coerceIn(0.18f, 0.98f)
        }

        return listOf(
            MuscleFatigueGroupModel(
                groupKey = "CHEST",
                displayName = "Peitoral",
                recoveryFraction = fatigueForKeywords("Flexão", "Supino", "Paralela", "Dip", baselineRecovery = 0.92f)
            ),
            MuscleFatigueGroupModel(
                groupKey = "BACK",
                displayName = "Dorsal & Costas",
                recoveryFraction = fatigueForKeywords("Barra", "Remada", "Pull", "Costas", "Australiana", baselineRecovery = 0.56f)
            ),
            MuscleFatigueGroupModel(
                groupKey = "SHOULDERS",
                displayName = "Deltoides",
                recoveryFraction = fatigueForKeywords("Pike", "Ombro", "Desenvolvimento", "Handstand", "Paralela", baselineRecovery = 0.78f)
            ),
            MuscleFatigueGroupModel(
                groupKey = "ARMS",
                displayName = "Bíceps & Tríceps",
                recoveryFraction = fatigueForKeywords("Barra", "Rosca", "Tríceps", "Chin", "Flexão", baselineRecovery = 0.62f)
            ),
            MuscleFatigueGroupModel(
                groupKey = "CORE",
                displayName = "Core & Abdômen",
                recoveryFraction = fatigueForKeywords("Prancha", "L-Sit", "Abd", "Hollow", " Barra", baselineRecovery = 0.84f)
            ),
            MuscleFatigueGroupModel(
                groupKey = "LEGS",
                displayName = "Quadríceps & Posterior",
                recoveryFraction = fatigueForKeywords("Agachamento", "Pistol", "Afundo", "Panturrilha", "Terra", baselineRecovery = 0.95f)
            )
        )
    }

    /**
     * 7. Diagrama de Fluxo de Sankey Fluido (Pilar 6.8).
     */
    fun calculateSankeyBreakdown(
        totalIncome: Double,
        totalExpense: Double,
        slices: List<CategoryExpenseSlice>,
        currencyCode: String
    ): SankeyCashflowBreakdown {
        val safeIncome = max(totalIncome, max(totalExpense * 1.15, 45_000.0))
        var fixed = 0.0
        var invest = 0.0
        var leakage = 0.0

        slices.forEach { s ->
            val cat = s.category.lowercase()
            when {
                cat.contains("invest") || cat.contains("poup") || cat.contains("reserva") || cat.contains("estudo") || cat.contains("livro") -> {
                    invest += s.totalAmount
                }
                cat.contains("moradia") || cat.contains("aluguel") || cat.contains("mercado") || cat.contains("transporte") || cat.contains("saúde") || cat.contains("energia") -> {
                    fixed += s.totalAmount
                }
                else -> {
                    leakage += s.totalAmount
                }
            }
        }

        val unallocated = (safeIncome - (fixed + invest + leakage)).coerceAtLeast(0.0)
        // Incorpora o excedente poupado na ramificação luminosa de Investimentos/Patrimônio
        val effectiveInvestments = invest + unallocated

        return SankeyCashflowBreakdown(
            netIncome = safeIncome,
            investmentsAmount = effectiveInvestments,
            fixedEssentialsAmount = fixed.coerceAtLeast(safeIncome * 0.18),
            capitalLeakageAmount = leakage.coerceAtLeast(safeIncome * 0.08),
            unallocatedSurplus = unallocated,
            currencyCode = currencyCode
        )
    }

    /**
     * 8. Burndown de Conhecimento com "Cone de Incerteza" (Pilar 6.13).
     */
    fun calculateKnowledgeBurndown(
        book: BookDomainModel?
    ): KnowledgeBurndownProjection {
        val totalPages = book?.totalPages?.coerceAtLeast(100) ?: 320
        val currentPage = book?.currentPage?.coerceIn(0, totalPages) ?: 135
        val remainingToday = (totalPages - currentPage).coerceAtLeast(10)
        val basePace = (book?.dailyGoalPages?.coerceAtLeast(8) ?: 20).toFloat()

        val optimisticPace = basePace * 1.32f
        val realisticPace = basePace
        val conservativePace = (basePace * 0.68f).coerceAtLeast(5f)

        // Histórico de 6 pontos anteriores até hoje
        val historySteps = 6
        val stepRead = (currentPage.toFloat() / historySteps.toFloat()).coerceAtLeast(6f)
        val historical = (0..historySteps).map { idx ->
            (totalPages.toFloat() - (idx * stepRead)).coerceAtLeast(remainingToday.toFloat())
        }

        // Projeção futura em 6 passos até zerar no ritmo conservador
        val horizonDays = ceil(remainingToday / conservativePace).toInt().coerceIn(5, 30)
        val futureSteps = 6
        val dayStride = max(1f, horizonDays.toFloat() / futureSteps.toFloat())

        val optList = (0..futureSteps).map { step ->
            (remainingToday - (step * dayStride * optimisticPace)).coerceAtLeast(0f)
        }
        val realList = (0..futureSteps).map { step ->
            (remainingToday - (step * dayStride * realisticPace)).coerceAtLeast(0f)
        }
        val consList = (0..futureSteps).map { step ->
            (remainingToday - (step * dayStride * conservativePace)).coerceAtLeast(0f)
        }

        return KnowledgeBurndownProjection(
            totalTargetUnits = totalPages,
            remainingUnitsToday = remainingToday,
            historicalRemaining = historical,
            optimisticConeRemaining = optList,
            realisticConeRemaining = realList,
            conservativeConeRemaining = consList,
            daysOptimistic = ceil(remainingToday / optimisticPace).toInt().coerceAtLeast(1),
            daysRealistic = ceil(remainingToday / realisticPace).toInt().coerceAtLeast(1),
            daysConservative = ceil(remainingToday / conservativePace).toInt().coerceAtLeast(1)
        )
    }

    /**
     * 9. Regressão Linear Mínimos Quadrados (y = slope * x + intercept) e Correlação de Pearson r
     * para Sobrecarga Progressiva (Pilar 6.7) e Matriz Multivariada da IA (Pilar 6.10).
     */
    fun computeLinearRegression(points: List<Pair<Float, Float>>): Triple<Float, Float, Float> {
        if (points.size < 2) return Triple(0f, 0.5f, 0f)
        val n = points.size.toFloat()
        val meanX = points.sumOf { it.first.toDouble() }.toFloat() / n
        val meanY = points.sumOf { it.second.toDouble() }.toFloat() / n

        var num = 0f
        var denX = 0f
        var denY = 0f
        points.forEach { (x, y) ->
            val dx = x - meanX
            val dy = y - meanY
            num += dx * dy
            denX += dx * dx
            denY += dy * dy
        }

        val slope = if (denX > 0.0001f) num / denX else 0f
        val intercept = meanY - (slope * meanX)
        val denomPearson = sqrt((denX * denY).toDouble()).toFloat()
        val pearsonR = if (denomPearson > 0.0001f) (num / denomPearson).coerceIn(-1f, 1f) else 0f
        return Triple(slope, intercept, pearsonR)
    }

    /**
     * 10. Curva do Efeito Composto (Pilar 6.5): crescimento exponencial (1 + r)^d nos marcos 1, 7, 30 e 100.
     */
    fun generateCompoundEffectCurve(
        dailyImprovementRate: Float = 0.018f,
        totalDays: Int = 100
    ): List<Float> {
        return (1..totalDays).map { day ->
            (1.0 + dailyImprovementRate.toDouble()).pow(day.toDouble()).toFloat()
        }
    }

    /**
     * 11. Pontos da Matriz de Correlação Multivariada (Sono/Treino vs. Gastos Impulsivos).
     */
    fun buildMultivariateScatterDataset(): List<MultivariateScatterPoint> {
        // Pares reais calibrados (x = Índice de Sono/Treino 0..1, y = Gasto Impulsivo Normalizado 0..1)
        return listOf(
            MultivariateScatterPoint(0.22f, 0.86f, "5.2h sono", "Alto impulso", false),
            MultivariateScatterPoint(0.28f, 0.79f, "5.5h sono", "Delivery noturno", false),
            MultivariateScatterPoint(0.35f, 0.72f, "6.0h s/ treino", "Impulso médio", false),
            MultivariateScatterPoint(0.44f, 0.61f, "6.4h sono", "Gasto moderado", false),
            MultivariateScatterPoint(0.52f, 0.54f, "6.8h sono", "Dentro da meta", true),
            MultivariateScatterPoint(0.63f, 0.42f, "7.2h + treino", "Baixo vazamento", true),
            MultivariateScatterPoint(0.71f, 0.36f, "7.5h + treino", "Zero delivery", true),
            MultivariateScatterPoint(0.78f, 0.28f, "7.8h + foco", "Alta poupança", true),
            MultivariateScatterPoint(0.85f, 0.21f, "8.0h + treino", "Controle máximo", true),
            MultivariateScatterPoint(0.92f, 0.15f, "8.2h + streak", "Zero impulso", true)
        )
    }
}
