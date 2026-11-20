package com.lifeos.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Contexto temporal adaptativo da Home (Pilar 1.2).
 */
enum class TimeOfDayContext(
    val badgeLabel: String,
    val headline: String,
    val subtitle: String
) {
    MORNING_COGNITIVE(
        badgeLabel = "06h – 12h · FOCO COGNITIVO",
        headline = "Manhã de Alta Retenção",
        subtitle = "Prioridade máxima: revisões de Ebbinghaus e leitura profunda."
    ),
    AFTERNOON_EXECUTION(
        badgeLabel = "12h – 17h · EXECUÇÃO",
        headline = "Ritmo & Consistência",
        subtitle = "Mantenha a matriz de hábitos ativa sem acumular pendências."
    ),
    LATE_AFTERNOON_PHYSIOLOGY(
        badgeLabel = "17h – 20h · FISIOLOGIA",
        headline = "Janela de Calistenia & Corpo",
        subtitle = "Temperatura corporal ideal para força, barra fixa e hidratação."
    ),
    NIGHT_CLOSURE_SLEEP(
        badgeLabel = "20h – 06h · FECHAMENTO & SONO",
        headline = "Fechamento Diário & Higiene do Sono",
        subtitle = "Audite o fluxo de caixa do dia e desacelere o córtex pré-frontal."
    )
}

data class HabitDomainModel(
    val id: Long,
    val name: String,
    val description: String,
    val category: String,
    val hexColor: String,
    val iconKey: String,
    val targetDaysMask: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCheckIns: Int,
    val completedToday: Boolean,
    val completedThisWeek: Int,
    val targetPerWeek: Int,
    val avgLogTimeFormatted: String,
    val goalProgressPercent: Int,
    val heatmapMatrix: List<List<Float>>, // 7 linhas (Seg..Dom) x 20 colunas semanais
    val annualHeatmap52Weeks: List<List<Float>> = emptyList(), // 7 linhas x 52 semanas (Pilar 6.1)
    val recentRecords: List<HabitRecordEntry>
)

data class HabitRecordEntry(
    val epochDay: Long,
    val formattedDate: String,
    val formattedTime: String,
    val valueLogged: Float,
    val note: String?
)

data class ExerciseDomainModel(
    val id: Long,
    val routineId: Long,
    val name: String,
    val targetSets: Int,
    val targetReps: Int,
    val restSeconds: Int,
    val orderIndex: Int,
    val completedSetsToday: Int,
    val lastWeightKg: Float = 0f
) {
    val isCompletedToday: Boolean
        get() = completedSetsToday >= targetSets && targetSets > 0
}

data class WorkoutRoutineDomainModel(
    val id: Long,
    val name: String,
    val dayOfWeek: Int,
    val calendarEventId: Long?,
    val estimatedDurationMinutes: Int,
    val exercises: List<ExerciseDomainModel>
) {
    val completedExercisesCount: Int
        get() = exercises.count { it.isCompletedToday }

    val completionFraction: Float
        get() = if (exercises.isEmpty()) 0f else completedExercisesCount.toFloat() / exercises.size.toFloat()
}

data class StudyTopicDomainModel(
    val id: Long,
    val subject: String,
    val topic: String,
    val stage: Int,
    val nextReviewEpochDay: Long,
    val daysUntilReview: Long,
    val estimatedRetentionPercent: Int,
    val reviewCount: Int,
    val reviewedToday: Boolean
)

data class FinancialTransactionModel(
    val id: Long,
    val amount: Double,
    val type: String, // "INCOME" | "EXPENSE"
    val category: String,
    val timestamp: Long,
    val epochDay: Long,
    val note: String?,
    val currencyCode: String,
    val paymentMethod: String
)

data class CategoryExpenseSlice(
    val category: String,
    val totalAmount: Double,
    val fraction: Float
)

data class DailyBurnPoint(
    val dayOfMonth: Int,
    val currentMonthCumulative: Double,
    val previousMonthCumulative: Double
)

data class FinancialOverviewModel(
    val totalIncomeMonth: Double,
    val totalExpenseMonth: Double,
    val netBalanceMonth: Double,
    val spentToday: Double,
    val currencyCode: String,
    val categorySlices: List<CategoryExpenseSlice>,
    val burnMatrixPoints: List<DailyBurnPoint>,
    val recentTransactions: List<FinancialTransactionModel>,
    val behavioralCorrelationInsight: String = ""
)

data class BookDomainModel(
    val id: Long,
    val title: String,
    val author: String,
    val totalPages: Int,
    val currentPage: Int,
    val pagesPerHour: Float,
    val dailyGoalPages: Int,
    val pagesReadToday: Int,
    val progressFraction: Float,
    val estimatedHoursRemaining: Float,
    val status: String,
    val projectedFinishSummary: String = ""
) {
    val morningTargetRangeLabel: String
        get() {
            val nextTargetPage = (currentPage + dailyGoalPages).coerceAtMost(totalPages)
            return "Meta matinal: $dailyGoalPages páginas (da pág. $currentPage até $nextTargetPage)"
        }
}

data class BookQuoteDomainModel(
    val id: Long,
    val bookId: Long,
    val bookTitle: String,
    val quoteText: String,
    val pageNumber: Int,
    val formattedDate: String
)

/**
 * Resultado consolidado da Roda do Equilíbrio da Vida (Pilar 6.2 + Referência 04).
 */
data class BalanceWheelScore(
    val bodyScore0to10: Float,
    val focusScore0to10: Float,
    val financeScore0to10: Float,
    val consistencyScore0to10: Float,
    val overallScore0to10: Float
) {
    val healthFraction: Float get() = (bodyScore0to10 / 10f).coerceIn(0f, 1f)
    val studyFraction: Float get() = (focusScore0to10 / 10f).coerceIn(0f, 1f)
    val financeFraction: Float get() = (financeScore0to10 / 10f).coerceIn(0f, 1f)
    val consistencyFraction: Float get() = (consistencyScore0to10 / 10f).coerceIn(0f, 1f)
}

// =============================================================================
// PILAR 5 & PILAR 6: CIÊNCIA COMPORTAMENTAL, RETENÇÃO & 14 GRÁFICOS 120 FPS
// =============================================================================

data class CircadianHourPetal(
    val hour24: Int,
    val intensityFraction: Float, // 0.15f .. 1.0f
    val phaseKey: String,         // "SLEEP", "COGNITIVE", "CAFFEINE_CUTOFF", "EXECUTION", "NEUROMUSCULAR", "MELATONIN"
    val hexColor: String
)

data class CircadianStatusModel(
    val currentHour: Int,
    val currentMinute: Int,
    val activeBiologicalWindowTitle: String,
    val activeBiologicalSubtitle: String,
    val intellectualPeakWindow: String = "08:30 – 11:30",
    val caffeineCutoffTime: String = "14:00",
    val minutesUntilCaffeineCutoff: Int,
    val isPastCaffeineCutoff: Boolean,
    val caffeineGuidanceLabel: String,
    val neuromuscularPeakWindow: String = "17:00 – 19:30",
    val sleepGateTime: String = "22:30",
    val petals24h: List<CircadianHourPetal>
)

data class StreakFreezeStatusModel(
    val availableCredits: Int,
    val maxBankedCredits: Int = 5,
    val usedCreditsTotal: Int,
    val cleanCheckInsTowardNextShield: Int,
    val checkInsRequiredPerShield: Int = 7,
    val isFreezeActiveToday: Boolean
)

data class GhostSquadPeerModel(
    val slotIndex: Int,
    val codename: String,
    val focusDomain: String,
    val statusHeadline: String,
    val auraHexColor: String,
    val minutesFocusedToday: Int,
    val isCurrentlyPulsing: Boolean
)

data class AntiCharityContractModel(
    val id: Long,
    val weeklyTargetRatio: Float,
    val currentWeekRatio: Float,
    val stakeAmount: Double,
    val currencyCode: String,
    val institutionName: String,
    val isActive: Boolean,
    val isProtectedByFreeze: Boolean,
    val isAtRisk: Boolean,
    val statusSummary: String
)

data class RpgAvatarAttributesModel(
    val strength0to100: Int,
    val intellect0to100: Int,
    val finances0to100: Int,
    val resilience0to100: Int,
    val focus0to100: Int,
    val culture0to100: Int,
    val avatarLevel: Int,
    val archetypeTitle: String
) {
    fun asNormalizedList(): List<Float> = listOf(
        (strength0to100 / 100f).coerceIn(0.12f, 1f),
        (intellect0to100 / 100f).coerceIn(0.12f, 1f),
        (finances0to100 / 100f).coerceIn(0.12f, 1f),
        (resilience0to100 / 100f).coerceIn(0.12f, 1f),
        (focus0to100 / 100f).coerceIn(0.12f, 1f),
        (culture0to100 / 100f).coerceIn(0.12f, 1f)
    )
}

data class MuscleFatigueGroupModel(
    val groupKey: String,
    val displayName: String,
    val recoveryFraction: Float // 0.0f = fadiga aguda (vermelho), 0.5f = recuperação (âmbar), 1.0f = 100% pronto (verde)
)

data class SankeyCashflowBreakdown(
    val netIncome: Double,
    val investmentsAmount: Double,
    val fixedEssentialsAmount: Double,
    val capitalLeakageAmount: Double,
    val unallocatedSurplus: Double,
    val currencyCode: String
)

data class MultivariateScatterPoint(
    val xNormalized: Float, // Ex: Qualidade do Sono / Consistência Física (0..1)
    val yNormalized: Float, // Ex: Gasto Impulsivo ou Horas de Estudo (0..1)
    val rawXLabel: String,
    val rawYLabel: String,
    val isPositiveCluster: Boolean
)

data class KnowledgeBurndownProjection(
    val totalTargetUnits: Int,
    val remainingUnitsToday: Int,
    val historicalRemaining: List<Float>,
    val optimisticConeRemaining: List<Float>,
    val realisticConeRemaining: List<Float>,
    val conservativeConeRemaining: List<Float>,
    val daysOptimistic: Int,
    val daysRealistic: Int,
    val daysConservative: Int
)

// =============================================================================
// JSON SCHEMA ESTRITO DE INGESTÃO DOCUMENTAL (PILAR 5.1)
// =============================================================================

@Serializable
data class AiExtractedPlanPayload(
    @SerialName("detected_habits")
    val detectedHabits: List<DetectedHabitPayload> = emptyList(),

    @SerialName("detected_workouts")
    val detectedWorkouts: List<DetectedWorkoutPayload> = emptyList(),

    @SerialName("detected_studies")
    val detectedStudies: List<DetectedStudyPayload> = emptyList(),

    @SerialName("detected_finances")
    val detectedFinances: List<DetectedFinancePayload> = emptyList()
)

@Serializable
data class DetectedHabitPayload(
    @SerialName("name") val name: String,
    @SerialName("category") val category: String,
    @SerialName("hex_color") val hexColor: String,
    @SerialName("target_days_mask") val targetDaysMask: Int
)

@Serializable
data class DetectedWorkoutPayload(
    @SerialName("name") val name: String,
    @SerialName("day_of_week") val dayOfWeek: Int,
    @SerialName("exercises") val exercises: List<DetectedExercisePayload>
)

@Serializable
data class DetectedExercisePayload(
    @SerialName("name") val name: String,
    @SerialName("target_sets") val targetSets: Int,
    @SerialName("target_reps") val targetReps: Int,
    @SerialName("rest_seconds") val restSeconds: Int
)

@Serializable
data class DetectedStudyPayload(
    @SerialName("subject") val subject: String,
    @SerialName("topic") val topic: String
)

@Serializable
data class DetectedFinancePayload(
    @SerialName("amount") val amount: Double,
    @SerialName("type") val type: String,
    @SerialName("category") val category: String
)
