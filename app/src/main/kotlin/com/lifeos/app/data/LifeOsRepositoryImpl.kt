package com.lifeos.app.data

import com.lifeos.core.database.dao.LifeOsDao
import com.lifeos.core.database.entity.BookEntity
import com.lifeos.core.database.entity.BookQuoteEntity
import com.lifeos.core.database.entity.CommitmentContractEntity
import com.lifeos.core.database.entity.ExerciseEntity
import com.lifeos.core.database.entity.FinancialRecordEntity
import com.lifeos.core.database.entity.GhostSquadMemberEntity
import com.lifeos.core.database.entity.HabitEntity
import com.lifeos.core.database.entity.HabitLogEntity
import com.lifeos.core.database.entity.StudyTopicEntity
import com.lifeos.core.database.entity.WorkoutRoutineEntity
import com.lifeos.core.database.model.HabitWithLogs
import com.lifeos.core.database.model.WorkoutRoutineWithExercises
import com.lifeos.core.domain.algorithm.BalanceWheelCalculator
import com.lifeos.core.domain.algorithm.BehavioralRetentionEngine
import com.lifeos.core.domain.algorithm.CrossDomainInsightCalculator
import com.lifeos.core.domain.algorithm.EbbinghausScheduler
import com.lifeos.core.domain.algorithm.StreakAndHeatmapCalculator
import com.lifeos.core.domain.model.AiExtractedPlanPayload
import com.lifeos.core.domain.model.AntiCharityContractModel
import com.lifeos.core.domain.model.BalanceWheelScore
import com.lifeos.core.domain.model.BookDomainModel
import com.lifeos.core.domain.model.BookQuoteDomainModel
import com.lifeos.core.domain.model.CategoryExpenseSlice
import com.lifeos.core.domain.model.DailyBurnPoint
import com.lifeos.core.domain.model.ExerciseDomainModel
import com.lifeos.core.domain.model.FinancialOverviewModel
import com.lifeos.core.domain.model.FinancialTransactionModel
import com.lifeos.core.domain.model.GhostSquadPeerModel
import com.lifeos.core.domain.model.HabitDomainModel
import com.lifeos.core.domain.model.HabitRecordEntry
import com.lifeos.core.domain.model.StreakFreezeStatusModel
import com.lifeos.core.domain.model.StudyTopicDomainModel
import com.lifeos.core.domain.model.WorkoutRoutineDomainModel
import com.lifeos.core.domain.repository.LifeOsRepository
import com.lifeos.core.system.alarm.SystemAlarmScheduler
import com.lifeos.core.system.calendar.CalendarSyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

@Singleton
class LifeOsRepositoryImpl @Inject constructor(
    private val dao: LifeOsDao,
    private val alarmScheduler: SystemAlarmScheduler,
    private val calendarSyncManager: CalendarSyncManager
) : LifeOsRepository {

    private val shortDateFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale("pt", "MZ"))
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    override fun observeHabits(today: LocalDate): Flow<List<HabitDomainModel>> {
        val todayEpoch = today.toEpochDay()
        val weekStartEpoch = today
            .minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
            .toEpochDay()

        return dao.getAllHabitsWithLogs().map { items ->
            items.map { item ->
                mapHabitWithLogs(
                    item = item,
                    today = today,
                    todayEpoch = todayEpoch,
                    weekStartEpoch = weekStartEpoch
                )
            }
        }
    }

    override fun observeTodayWorkout(dayOfWeek: Int): Flow<WorkoutRoutineDomainModel?> {
        return dao.getRoutineWithExercisesForDay(dayOfWeek).map { relation ->
            relation?.let(::mapWorkoutRelation)
        }
    }

    override fun observeAllWorkouts(): Flow<List<WorkoutRoutineDomainModel>> {
        return dao.getAllRoutinesWithExercises().map { list ->
            list.map(::mapWorkoutRelation)
        }
    }

    override fun observeDueStudyTopics(today: LocalDate): Flow<List<StudyTopicDomainModel>> {
        val todayEpoch = today.toEpochDay()
        return dao.getDueStudyReviews(todayEpoch).map { list ->
            list.map { entity -> mapStudyTopic(entity, todayEpoch) }
        }
    }

    override fun observeAllStudyTopics(today: LocalDate): Flow<List<StudyTopicDomainModel>> {
        val todayEpoch = today.toEpochDay()
        return dao.getAllStudyTopics().map { list ->
            list.map { entity -> mapStudyTopic(entity, todayEpoch) }
        }
    }

    override fun observeFinancialOverview(today: LocalDate): Flow<FinancialOverviewModel> {
        val currentMonthStart = today.withDayOfMonth(1)
        val currentMonthEnd = today.withDayOfMonth(today.lengthOfMonth())
        val prevMonth = today.minusMonths(1)
        val prevMonthStart = prevMonth.withDayOfMonth(1)
        val prevMonthEnd = prevMonth.withDayOfMonth(prevMonth.lengthOfMonth())
        val todayEpoch = today.toEpochDay()
        val last30StartEpoch = today.minusDays(30).toEpochDay()

        val financeBundleFlow = combine(
            dao.observeCashflowSummary(currentMonthStart.toEpochDay(), currentMonthEnd.toEpochDay()),
            dao.observeCategoryExpenses(currentMonthStart.toEpochDay(), currentMonthEnd.toEpochDay()),
            dao.observeDailyExpenseBurn(currentMonthStart.toEpochDay(), currentMonthEnd.toEpochDay()),
            dao.observeDailyExpenseBurn(prevMonthStart.toEpochDay(), prevMonthEnd.toEpochDay()),
            dao.getAllTransactions()
        ) { summary, categories, currentDailyBurn, prevDailyBurn, allTransactions ->
            FinanceRawBundle(summary, categories, currentDailyBurn, prevDailyBurn, allTransactions)
        }

        return combine(
            financeBundleFlow,
            dao.getHabitLogsInWindow(last30StartEpoch, todayEpoch)
        ) { fb, logs30d ->
            val totalExp = fb.summary.totalExpense.coerceAtLeast(0.0)
            val slices = fb.categories.map { tuple ->
                CategoryExpenseSlice(
                    category = tuple.category,
                    totalAmount = tuple.totalAmount,
                    fraction = if (totalExp > 0.0) {
                        (tuple.totalAmount / totalExp).toFloat().coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                )
            }

            val currentByDayOfMonth = fb.currentDailyBurn.associate {
                LocalDate.ofEpochDay(it.epochDay).dayOfMonth to it.dailyTotal
            }
            val prevByDayOfMonth = fb.prevDailyBurn.associate {
                LocalDate.ofEpochDay(it.epochDay).dayOfMonth to it.dailyTotal
            }

            val daysCount = max(today.dayOfMonth, 20).coerceAtMost(31)
            var runningCurr = 0.0
            var runningPrev = 0.0
            val burnPoints = (1..daysCount).map { dayNum ->
                if (dayNum <= today.dayOfMonth) {
                    runningCurr += currentByDayOfMonth[dayNum] ?: 0.0
                }
                runningPrev += prevByDayOfMonth[dayNum] ?: 0.0
                DailyBurnPoint(
                    dayOfMonth = dayNum,
                    currentMonthCumulative = if (dayNum <= today.dayOfMonth) runningCurr else 0.0,
                    previousMonthCumulative = runningPrev
                )
            }

            val spentToday = fb.allTransactions
                .filter { it.type == FinancialRecordEntity.TYPE_EXPENSE && it.epochDay == todayEpoch }
                .sumOf { it.amount }

            val activeTrainingDays = logs30d
                .filter { it.completed }
                .map { it.epochDay }
                .toSet()

            val foodExpensesByDay = fb.allTransactions
                .filter {
                    it.type == FinancialRecordEntity.TYPE_EXPENSE &&
                        it.category.contains("Alimenta", ignoreCase = true)
                }
                .groupBy { it.epochDay }
                .mapValues { (_, list) -> list.sumOf { it.amount } }

            val correlationText = CrossDomainInsightCalculator.buildWorkoutVsFoodSpendingInsight(
                activeTrainingEpochDays = activeTrainingDays,
                foodExpensesByEpochDay = foodExpensesByDay
            )

            val activeCurrency = fb.allTransactions.firstOrNull()?.currencyCode ?: "MZN"

            FinancialOverviewModel(
                totalIncomeMonth = fb.summary.totalIncome,
                totalExpenseMonth = fb.summary.totalExpense,
                netBalanceMonth = fb.summary.netBalance,
                spentToday = spentToday,
                currencyCode = activeCurrency,
                categorySlices = slices,
                burnMatrixPoints = burnPoints,
                recentTransactions = fb.allTransactions.take(25).map { tx ->
                    FinancialTransactionModel(
                        id = tx.id,
                        amount = tx.amount,
                        type = tx.type,
                        category = tx.category,
                        timestamp = tx.timestamp,
                        epochDay = tx.epochDay,
                        note = tx.note,
                        currencyCode = tx.currencyCode,
                        paymentMethod = tx.paymentMethod
                    )
                },
                behavioralCorrelationInsight = correlationText
            )
        }
    }

    override fun observeActiveBooks(): Flow<List<BookDomainModel>> {
        val today = LocalDate.now()
        return dao.getActiveBooks().map { books ->
            books.map { b ->
                BookDomainModel(
                    id = b.id,
                    title = b.title,
                    author = b.author,
                    totalPages = b.totalPages,
                    currentPage = b.currentPage,
                    pagesPerHour = b.pagesPerHour,
                    dailyGoalPages = b.dailyGoalPages,
                    pagesReadToday = b.pagesReadToday,
                    progressFraction = b.progressFraction,
                    estimatedHoursRemaining = b.estimatedHoursRemaining,
                    status = b.status,
                    projectedFinishSummary = CrossDomainInsightCalculator.buildBookCompletionProjection(
                        currentPage = b.currentPage,
                        totalPages = b.totalPages,
                        pagesPerHour = b.pagesPerHour,
                        dailyGoalPages = b.dailyGoalPages,
                        today = today
                    )
                )
            }
        }
    }

    override fun observeBookQuotes(): Flow<List<BookQuoteDomainModel>> {
        return combine(
            dao.getAllBookQuotes(),
            dao.getAllBooks()
        ) { quotes, books ->
            val titlesById = books.associate { it.id to it.title }
            quotes.map { q ->
                val dateStr = Instant.ofEpochMilli(q.capturedAtMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(shortDateFormatter)
                BookQuoteDomainModel(
                    id = q.id,
                    bookId = q.bookId,
                    bookTitle = titlesById[q.bookId] ?: "Livro Físico",
                    quoteText = q.quoteText,
                    pageNumber = q.pageNumber,
                    formattedDate = dateStr
                )
            }
        }
    }

    override fun observeBalanceWheel(today: LocalDate): Flow<BalanceWheelScore> {
        val todayEpoch = today.toEpochDay()
        val windowStartEpoch = today.minusDays(13).toEpochDay()
        val monthStartEpoch = today.withDayOfMonth(1).toEpochDay()
        val monthEndEpoch = today.withDayOfMonth(today.lengthOfMonth()).toEpochDay()

        val physiologyAndConsistencyFlow = combine(
            dao.getRoutineWithExercisesForDay(today.dayOfWeek.value),
            dao.getAllHabits(),
            dao.getHabitLogsInWindow(windowStartEpoch, todayEpoch)
        ) { todayRoutine, habits, logs14d ->
            val plannedEx = todayRoutine?.exercises?.size ?: 0
            val completedEx = todayRoutine?.exercises?.count {
                it.completedSetsToday >= it.targetSets && it.targetSets > 0
            } ?: 0

            val masksById = habits.associate { it.id to it.targetDaysMask }
            val completedSet = logs14d
                .filter { it.completed }
                .map { it.habitId to it.epochDay }
                .toSet()

            val consistency = StreakAndHeatmapCalculator.calculate14DayConsistency(
                habitsMasksById = masksById,
                completedPairs = completedSet,
                today = today
            )
            Triple(completedEx, plannedEx, consistency)
        }

        val mindAndFinanceFlow = combine(
            dao.getDueStudyReviews(todayEpoch),
            dao.getStudyTopicsReviewedOnDay(todayEpoch),
            dao.getActiveBooks(),
            dao.observeCashflowSummary(monthStartEpoch, monthEndEpoch)
        ) { dueTopics, reviewedToday, books, cashflow ->
            val pagesReadToday = books.sumOf {
                if (it.lastReadEpochDay == todayEpoch) it.pagesReadToday else 0
            }
            val dailyGoalPages = books.sumOf { it.dailyGoalPages }.coerceAtLeast(20)
            MindAndFinanceSnapshot(
                reviewedCount = reviewedToday.size,
                dueCount = (dueTopics.size + reviewedToday.size).coerceAtLeast(1),
                pagesReadToday = pagesReadToday,
                dailyGoalPages = dailyGoalPages,
                income = cashflow.totalIncome,
                expenses = cashflow.totalExpense
            )
        }

        return combine(physiologyAndConsistencyFlow, mindAndFinanceFlow) { (completedEx, plannedEx, consistency), mf ->
            BalanceWheelCalculator.calculate(
                completedExercisesToday = completedEx,
                plannedExercisesToday = plannedEx,
                reviewedTopicsToday = mf.reviewedCount,
                dueTopicsToday = mf.dueCount,
                pagesReadToday = mf.pagesReadToday,
                dailyGoalPages = mf.dailyGoalPages,
                totalIncome = mf.income,
                totalExpenses = mf.expenses,
                consistencyRatio14Days = consistency
            )
        }
    }

    override fun observeStreakFreezeStatus(today: LocalDate): Flow<StreakFreezeStatusModel> {
        val todayEpoch = today.toEpochDay()
        return combine(
            dao.observeCleanCheckInCount(),
            dao.observeUsedFreezeDaysCount(),
            dao.observeFreezeActiveOnDay(todayEpoch)
        ) { cleanCount, usedDays, activeCount ->
            BehavioralRetentionEngine.calculateStreakFreezeStatus(
                cleanCheckInsTotal = cleanCount,
                usedFreezeDaysCount = usedDays,
                isFreezeActiveToday = activeCount > 0
            )
        }
    }

    override fun observeGhostSquadPeers(): Flow<List<GhostSquadPeerModel>> {
        return dao.observeGhostSquadMembers().map { members ->
            val triples = members.map { Triple(it.codename, it.focusDomain, it.auraHexColor) }
            val mins = members.map { it.baseMinutesToday }
            BehavioralRetentionEngine.buildGhostSquadPresence(
                configuredPeers = triples,
                baseMinutes = mins,
                currentHour = LocalTime.now().hour
            )
        }
    }

    override fun observeAntiCharityContract(today: LocalDate): Flow<AntiCharityContractModel> {
        val todayEpoch = today.toEpochDay()
        return combine(
            dao.observeActiveCommitmentContract(),
            observeHabits(today),
            dao.observeFreezeActiveOnDay(todayEpoch)
        ) { entity, habits, freezeCount ->
            val contract = entity ?: CommitmentContractEntity(
                id = 1L,
                weeklyTargetRatio = 0.80f,
                stakeAmount = 500.0,
                currencyCode = "MZN",
                institutionName = "Cruz Vermelha & Educação",
                isActive = true
            )
            BehavioralRetentionEngine.evaluateAntiCharityContract(
                contractId = contract.id,
                weeklyTargetRatio = contract.weeklyTargetRatio,
                stakeAmount = contract.stakeAmount,
                currencyCode = contract.currencyCode,
                institutionName = contract.institutionName,
                isActive = contract.isActive,
                habits = habits,
                isFreezeActiveToday = freezeCount > 0
            )
        }
    }

    override suspend fun activateStreakFreezeToday(today: LocalDate): Boolean {
        val status = observeStreakFreezeStatus(today).first()
        if (status.isFreezeActiveToday || status.availableCredits <= 0) return false

        val todayEpoch = today.toEpochDay()
        val habitsWithLogs = dao.getAllHabitsWithLogs().first()
        habitsWithLogs.forEach { item ->
            val alreadyDone = item.logs.any { it.epochDay == todayEpoch && it.completed }
            if (!alreadyDone) {
                dao.insertHabitLog(
                    HabitLogEntity(
                        habitId = item.habit.id,
                        epochDay = todayEpoch,
                        completed = true,
                        valueLogged = item.habit.dailyGoalValue,
                        loggedAtMillis = System.currentTimeMillis(),
                        note = BehavioralRetentionEngine.STREAK_FREEZE_NOTE_TAG
                    )
                )
            }
        }

        // Reprograma revisões Ebbinghaus pendentes em +1 dia sem zerar o estágio cognitivo
        val dueTopics = dao.getDueStudyReviews(todayEpoch).first()
        val tomorrowEpoch = today.plusDays(1).toEpochDay()
        dueTopics.forEach { topic ->
            dao.updateStudyTopic(topic.copy(nextReviewEpochDay = tomorrowEpoch))
        }
        return true
    }

    override suspend fun updateGhostSquadPeers(peers: List<Triple<String, String, String>>) {
        val entities = peers.take(3).mapIndexed { index, (name, domain, hex) ->
            GhostSquadMemberEntity(
                slotIndex = index,
                codename = name.trim().ifBlank { "Parceiro ${index + 1}" },
                focusDomain = domain,
                auraHexColor = hex,
                baseMinutesToday = 42 + (index * 16)
            )
        }
        dao.upsertGhostSquadMembers(entities)
    }

    override suspend fun saveAntiCharityContract(
        weeklyTargetRatio: Float,
        stakeAmount: Double,
        currencyCode: String,
        institutionName: String,
        isActive: Boolean
    ) {
        dao.deactivateAllCommitmentContracts()
        dao.upsertCommitmentContract(
            CommitmentContractEntity(
                weeklyTargetRatio = weeklyTargetRatio.coerceIn(0.4f, 1.0f),
                stakeAmount = stakeAmount.coerceAtLeast(10.0),
                currencyCode = currencyCode.ifBlank { "MZN" },
                institutionName = institutionName.trim().ifBlank { "Cruz Vermelha" },
                isActive = isActive,
                createdAtEpochDay = LocalDate.now().toEpochDay()
            )
        )
    }

    override suspend fun createHabit(
        name: String,
        description: String,
        category: String,
        hexColor: String,
        targetDaysMask: Int
    ): Long {
        return dao.insertHabit(
            HabitEntity(
                name = name.trim(),
                description = description.trim(),
                category = category,
                hexColor = hexColor,
                targetDaysMask = targetDaysMask
            )
        )
    }

    override suspend fun toggleHabitCheckIn(
        habitId: Long,
        date: LocalDate,
        note: String?
    ) {
        val epochDay = date.toEpochDay()
        val snapshot = dao.getHabitWithLogsById(habitId).first() ?: return
        val existingToday = snapshot.logs.find { it.epochDay == epochDay && it.completed }

        if (existingToday != null && note == null) {
            dao.deleteHabitLogForDay(habitId = habitId, epochDay = epochDay)
        } else {
            dao.insertHabitLog(
                HabitLogEntity(
                    habitId = habitId,
                    epochDay = epochDay,
                    completed = true,
                    valueLogged = snapshot.habit.dailyGoalValue,
                    loggedAtMillis = System.currentTimeMillis(),
                    note = note ?: existingToday?.note
                )
            )
        }

        val updatedSnapshot = dao.getHabitWithLogsById(habitId).first() ?: return
        val completedDays = updatedSnapshot.logs
            .filter { it.completed }
            .map { it.epochDay }
            .toSet()
        val (currentStreak, longestStreak) = StreakAndHeatmapCalculator.calculateStreaks(
            completedEpochDays = completedDays,
            targetDaysMask = updatedSnapshot.habit.targetDaysMask,
            today = date
        )
        dao.updateHabitStreak(
            habitId = habitId,
            currentStreak = currentStreak,
            longestStreak = max(updatedSnapshot.habit.longestStreak, longestStreak)
        )
    }

    override suspend fun completeExerciseSet(
        exerciseId: Long,
        newCompletedSets: Int,
        date: LocalDate,
        weightKg: Float
    ) {
        dao.updateExerciseProgress(
            exerciseId = exerciseId,
            completedSets = newCompletedSets.coerceAtLeast(0),
            epochDay = date.toEpochDay(),
            weightKg = weightKg
        )
    }

    override suspend fun updateWorkoutCalendarEvent(routineId: Long, calendarEventId: Long) {
        dao.updateWorkoutCalendarEventId(routineId = routineId, calendarEventId = calendarEventId)
    }

    override suspend fun createStudyTopic(
        subject: String,
        topic: String,
        today: LocalDate
    ): Long {
        val nextReview = today.plusDays(1)
        val alarmCode = ((subject.hashCode() * 31) + topic.hashCode()) and 0x7FFFFFFF
        val id = dao.insertStudyTopic(
            StudyTopicEntity(
                subject = subject.trim(),
                topic = topic.trim(),
                stage = 0,
                nextReviewEpochDay = nextReview.toEpochDay(),
                alarmRequestCode = alarmCode
            )
        )
        alarmScheduler.scheduleExact(
            requestCode = alarmCode,
            title = "07:00 · Revisão Ebbinghaus ($subject)",
            message = "Hoje você precisa revisar '$topic' (Revisão de 1 dia — Curva de Ebbinghaus).",
            dateTime = nextReview.atTime(LocalTime.of(7, 0))
        )
        return id
    }

    override suspend fun reviewStudyTopic(
        topicId: Long,
        rememberedWell: Boolean,
        today: LocalDate
    ) {
        val current = dao.getAllStudyTopics().first().find { it.id == topicId } ?: return
        val (nextStage, nextDate) = if (rememberedWell) {
            EbbinghausScheduler.calculateNextReviewDate(current.stage, today)
        } else {
            EbbinghausScheduler.resetToFirstStage(today)
        }
        val alarmCode = current.alarmRequestCode ?: (topicId.toInt() + 10_000)
        val intervalDays = EbbinghausScheduler.STAGE_INTERVALS.getOrElse(nextStage) { 1 }

        dao.updateStudyTopic(
            current.copy(
                stage = nextStage,
                nextReviewEpochDay = nextDate.toEpochDay(),
                lastReviewedEpochDay = today.toEpochDay(),
                reviewCount = current.reviewCount + 1,
                alarmRequestCode = alarmCode
            )
        )

        alarmScheduler.scheduleExact(
            requestCode = alarmCode,
            title = "07:00 · Revisão Ebbinghaus (${current.subject})",
            message = "Hoje você precisa revisar '${current.topic}' (Revisão de $intervalDays dias).",
            dateTime = nextDate.atTime(LocalTime.of(7, 0))
        )
    }

    override suspend fun recordFinancialTransaction(
        amount: Double,
        type: String,
        category: String,
        note: String?,
        currencyCode: String,
        paymentMethod: String
    ) {
        val nowMillis = System.currentTimeMillis()
        dao.insertTransaction(
            FinancialRecordEntity(
                amount = amount,
                type = type,
                category = category.trim(),
                timestamp = nowMillis,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                currencyCode = currencyCode,
                paymentMethod = paymentMethod
            )
        )
    }

    override suspend fun addBook(
        title: String,
        author: String,
        totalPages: Int,
        dailyGoalPages: Int
    ) {
        dao.insertBook(
            BookEntity(
                title = title.trim(),
                author = author.trim(),
                totalPages = totalPages.coerceAtLeast(1),
                dailyGoalPages = dailyGoalPages.coerceAtLeast(5)
            )
        )
    }

    override suspend fun logReadingSession(
        bookId: Long,
        newCurrentPage: Int,
        sessionMinutes: Int,
        today: LocalDate
    ) {
        val current = dao.getBookById(bookId).first() ?: return
        val clampedPage = newCurrentPage.coerceIn(0, current.totalPages)
        val deltaPages = (clampedPage - current.currentPage).coerceAtLeast(0)
        val speedPagesPerHour = if (sessionMinutes > 0 && deltaPages > 0) {
            (deltaPages.toFloat() / (sessionMinutes.toFloat() / 60f))
        } else {
            current.pagesPerHour
        }
        val accumulatedToday = if (current.lastReadEpochDay == today.toEpochDay()) {
            current.pagesReadToday + deltaPages
        } else {
            deltaPages
        }
        val newStatus = if (clampedPage >= current.totalPages) {
            BookEntity.STATUS_COMPLETED
        } else {
            BookEntity.STATUS_READING
        }

        dao.updateBookReadingProgress(
            bookId = bookId,
            currentPage = clampedPage,
            pagesReadToday = accumulatedToday,
            pagesPerHour = speedPagesPerHour,
            status = newStatus,
            epochDay = today.toEpochDay()
        )
    }

    override suspend fun saveBookQuote(
        bookId: Long,
        quoteText: String,
        pageNumber: Int
    ) {
        if (quoteText.isBlank()) return
        dao.insertBookQuote(
            BookQuoteEntity(
                bookId = bookId,
                quoteText = quoteText.trim(),
                pageNumber = pageNumber.coerceAtLeast(0)
            )
        )
    }

    override suspend fun ingestExtractedPlan(
        plan: AiExtractedPlanPayload,
        today: LocalDate
    ) {
        val tomorrow = today.plusDays(1)
        val tomorrowEpoch = tomorrow.toEpochDay()
        val nowMillis = System.currentTimeMillis()

        val habitEntities = plan.detectedHabits.map { h ->
            HabitEntity(
                name = h.name,
                category = h.category,
                hexColor = h.hexColor,
                targetDaysMask = h.targetDaysMask.takeIf { it in 1..127 } ?: 127
            )
        }

        val workoutPairs = plan.detectedWorkouts.map { w ->
            val routine = WorkoutRoutineEntity(
                name = w.name,
                dayOfWeek = w.dayOfWeek.coerceIn(1, 7)
            )
            val exercises = w.exercises.mapIndexed { idx, ex ->
                ExerciseEntity(
                    routineId = 0L,
                    name = ex.name,
                    targetSets = ex.targetSets.coerceAtLeast(1),
                    targetReps = ex.targetReps.coerceAtLeast(1),
                    restSeconds = ex.restSeconds.coerceAtLeast(15),
                    orderIndex = idx
                )
            }
            routine to exercises
        }

        val studyEntities = plan.detectedStudies.mapIndexed { idx, s ->
            StudyTopicEntity(
                subject = s.subject,
                topic = s.topic,
                stage = 0,
                nextReviewEpochDay = tomorrowEpoch,
                alarmRequestCode = ((s.subject.hashCode() * 31) + s.topic.hashCode() + idx) and 0x7FFFFFFF
            )
        }

        val financeEntities = plan.detectedFinances.mapIndexed { idx, f ->
            FinancialRecordEntity(
                amount = f.amount,
                type = f.type,
                category = f.category,
                timestamp = nowMillis + idx,
                epochDay = today.toEpochDay()
            )
        }

        dao.ingestStructuredPlanAtomically(
            habits = habitEntities,
            workoutsWithExercises = workoutPairs,
            studyTopics = studyEntities,
            financialRecords = financeEntities
        )

        // Agenda alarmes exatos de Ebbinghaus às 07:00 e o Fechamento da Roda às 22:00
        studyEntities.forEach { st ->
            st.alarmRequestCode?.let { code ->
                alarmScheduler.scheduleExact(
                    requestCode = code,
                    title = "07:00 · Revisão Ebbinghaus (${st.subject})",
                    message = "Hoje você precisa revisar '${st.topic}' (Revisão de 1 dia).",
                    dateTime = tomorrow.atTime(LocalTime.of(7, 0))
                )
            }
        }

        alarmScheduler.scheduleExact(
            requestCode = NIGHT_BALANCE_WHEEL_ALARM_CODE,
            title = "22:00 · Fechamento da Roda da Vida",
            message = "Hora de fechar a Roda, auditar o fluxo do dia e manter seu Streak aceso!",
            dateTime = today.atTime(LocalTime.of(22, 0)).let {
                if (it.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() <= nowMillis) {
                    it.plusDays(1)
                } else {
                    it
                }
            }
        )

        // Se permissão de calendário já estiver ativa, reserva os treinos automaticamente às 18:00
        if (calendarSyncManager.hasCalendarWritePermission()) {
            workoutPairs.forEach { (routine, exList) ->
                val daysDiff = ((routine.dayOfWeek - today.dayOfWeek.value) + 7) % 7
                val targetDate = today.plusDays(daysDiff.toLong())
                val startMillis = targetDate.atTime(18, 0)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                calendarSyncManager.createEvent(
                    title = "Life OS · ${routine.name}",
                    description = exList.joinToString("\n") { "• ${it.name}: ${it.targetSets}x${it.targetReps}" },
                    startEpochMillis = startMillis,
                    endEpochMillis = startMillis + 50 * 60_000L
                )
            }
        }
    }

    override suspend fun seedInitialShowcaseDataIfEmpty(today: LocalDate) {
        val existingHabits = dao.getAllHabits().first()
        if (existingHabits.isNotEmpty()) return

        val h1 = dao.insertHabit(
            HabitEntity(
                name = "Barra Fixa & Calistenia",
                description = "Força relativa e tração escapular às 18h",
                category = HabitEntity.CATEGORY_HEALTH,
                hexColor = "#10E05B",
                iconKey = "fitness",
                targetDaysMask = 0b0011111, // Seg a Sex
                currentStreak = 33,
                longestStreak = 33,
                createdAtEpochDay = today.minusDays(45).toEpochDay()
            )
        )
        val h2 = dao.insertHabit(
            HabitEntity(
                name = "Hidratação 3L de Água",
                description = "3 litros distribuídos entre 07h e 20h",
                category = HabitEntity.CATEGORY_HEALTH,
                hexColor = "#00E5FF",
                iconKey = "water",
                targetDaysMask = 127,
                currentStreak = 19,
                longestStreak = 24,
                createdAtEpochDay = today.minusDays(40).toEpochDay()
            )
        )
        val h3 = dao.insertHabit(
            HabitEntity(
                name = "Leitura Matinal Ativa",
                description = "15 a 20 páginas com cronômetro antes das redes sociais",
                category = HabitEntity.CATEGORY_STUDY,
                hexColor = "#8B5CF6",
                iconKey = "study",
                targetDaysMask = 127,
                currentStreak = 14,
                longestStreak = 21,
                createdAtEpochDay = today.minusDays(30).toEpochDay()
            )
        )

        for (offset in 180 downTo 1) {
            val day = today.minusDays(offset.toLong())
            val epoch = day.toEpochDay()
            val logMillis = day.atTime(7, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (offset % 4 != 0) {
                dao.insertHabitLog(HabitLogEntity(habitId = h1, epochDay = epoch, completed = true, loggedAtMillis = logMillis + 37_800_000L))
            }
            if (offset % 3 != 0) {
                dao.insertHabitLog(HabitLogEntity(habitId = h2, epochDay = epoch, completed = true, loggedAtMillis = logMillis + 18_000_000L))
            }
            if (offset % 5 != 0) {
                dao.insertHabitLog(HabitLogEntity(habitId = h3, epochDay = epoch, completed = true, loggedAtMillis = logMillis))
            }
        }

        dao.upsertGhostSquadMembers(
            listOf(
                GhostSquadMemberEntity(slotIndex = 0, codename = "Lucas M.", focusDomain = "STUDY", auraHexColor = "#8B5CF6", baseMinutesToday = 48),
                GhostSquadMemberEntity(slotIndex = 1, codename = "Marina K.", focusDomain = "WORKOUT", auraHexColor = "#10E05B", baseMinutesToday = 55),
                GhostSquadMemberEntity(slotIndex = 2, codename = "Rafael T.", focusDomain = "READING", auraHexColor = "#00E5FF", baseMinutesToday = 36)
            )
        )

        dao.upsertCommitmentContract(
            CommitmentContractEntity(
                weeklyTargetRatio = 0.80f,
                stakeAmount = 500.0,
                currencyCode = "MZN",
                institutionName = "Cruz Vermelha & Bolsas STEM",
                isActive = true,
                createdAtEpochDay = today.minusDays(14).toEpochDay()
            )
        )

        val routineId = dao.insertWorkoutRoutine(
            WorkoutRoutineEntity(
                name = "Costas & Bíceps (Calistenia)",
                dayOfWeek = today.dayOfWeek.value,
                estimatedDurationMinutes = 50
            )
        )
        dao.insertExercises(
            listOf(
                ExerciseEntity(routineId = routineId, name = "Barra Fixa Pronada", targetSets = 4, targetReps = 10, restSeconds = 90, orderIndex = 0, completedSetsToday = 2, lastCompletedEpochDay = today.toEpochDay(), lastWeightKg = 10f),
                ExerciseEntity(routineId = routineId, name = "Paralelas (Dips Profundos)", targetSets = 4, targetReps = 12, restSeconds = 90, orderIndex = 1, completedSetsToday = 4, lastCompletedEpochDay = today.toEpochDay(), lastWeightKg = 12f),
                ExerciseEntity(routineId = routineId, name = "Remada Australiana", targetSets = 3, targetReps = 15, restSeconds = 75, orderIndex = 2),
                ExerciseEntity(routineId = routineId, name = "Chin-ups Supinados", targetSets = 3, targetReps = 10, restSeconds = 90, orderIndex = 3)
            )
        )

        dao.insertStudyTopics(
            listOf(
                StudyTopicEntity(subject = "Cálculo Integral", topic = "Integração por Partes & Substituição Trigonométrica", stage = 1, nextReviewEpochDay = today.toEpochDay(), reviewCount = 2),
                StudyTopicEntity(subject = "Arquitetura Android", topic = "Kotlin Coroutines, StateFlow & UDF", stage = 2, nextReviewEpochDay = today.toEpochDay(), reviewCount = 3),
                StudyTopicEntity(subject = "Bancos de Dados", topic = "SQLCipher AES-256 & Índices B-Tree no Room", stage = 3, nextReviewEpochDay = today.plusDays(4).toEpochDay(), reviewCount = 4)
            )
        )

        val bookId = dao.insertBookAndReturnIdFallback(
            BookEntity(
                title = "Hábitos Atômicos",
                author = "James Clear",
                totalPages = 320,
                currentPage = 45,
                pagesPerHour = 42f,
                dailyGoalPages = 15,
                pagesReadToday = 15,
                lastReadEpochDay = today.toEpochDay()
            )
        )
        dao.insertBookQuote(
            BookQuoteEntity(
                bookId = bookId,
                quoteText = "Você não se eleva ao nível das suas metas; você cai ao nível dos seus sistemas.",
                pageNumber = 43
            )
        )

        val baseMillis = System.currentTimeMillis()
        val currMonthStart = today.withDayOfMonth(1)
        val prevMonthStart = today.minusMonths(1).withDayOfMonth(1)
        dao.insertTransactions(
            listOf(
                // Mês anterior (curva de referência cinza na Burn Matrix)
                FinancialRecordEntity(amount = 6_200.0, type = "EXPENSE", category = "Moradia", timestamp = baseMillis - 86_400_000L * 45, epochDay = prevMonthStart.plusDays(2).toEpochDay(), paymentMethod = "Transferência"),
                FinancialRecordEntity(amount = 5_800.0, type = "EXPENSE", category = "Alimentação", timestamp = baseMillis - 86_400_000L * 38, epochDay = prevMonthStart.plusDays(8).toEpochDay(), paymentMethod = "Cartão"),
                FinancialRecordEntity(amount = 7_400.0, type = "EXPENSE", category = "Moradia", timestamp = baseMillis - 86_400_000L * 32, epochDay = prevMonthStart.plusDays(14).toEpochDay(), paymentMethod = "Transferência"),
                FinancialRecordEntity(amount = 8_900.0, type = "EXPENSE", category = "Transporte", timestamp = baseMillis - 86_400_000L * 25, epochDay = prevMonthStart.plusDays(21).toEpochDay(), paymentMethod = "M-Pesa"),

                // Mês atual (escada laranja luminosa crescente na Burn Matrix)
                FinancialRecordEntity(amount = 65_000.0, type = "INCOME", category = "Salário & Engenharia", timestamp = baseMillis - 86_400_000L * 18, epochDay = currMonthStart.plusDays(1).toEpochDay(), paymentMethod = "Transferência"),
                FinancialRecordEntity(amount = 4_200.0, type = "EXPENSE", category = "Transporte", timestamp = baseMillis - 86_400_000L * 17, epochDay = currMonthStart.plusDays(2).toEpochDay(), paymentMethod = "M-Pesa"),
                FinancialRecordEntity(amount = 6_800.0, type = "EXPENSE", category = "Moradia", timestamp = baseMillis - 86_400_000L * 13, epochDay = currMonthStart.plusDays(6).coerceAtMost(today).toEpochDay(), paymentMethod = "Transferência"),
                FinancialRecordEntity(amount = 5_500.0, type = "EXPENSE", category = "Moradia", timestamp = baseMillis - 86_400_000L * 9, epochDay = currMonthStart.plusDays(10).coerceAtMost(today).toEpochDay(), paymentMethod = "Transferência"),
                // Dia sem treino (offset 4 -> 4 % 4 == 0) com gasto maior em comida rápida para evidenciar a correlação real!
                FinancialRecordEntity(amount = 1_650.0, type = "EXPENSE", category = "Alimentação", timestamp = baseMillis - 86_400_000L * 4, epochDay = today.minusDays(4).toEpochDay(), note = "Delivery noturno sem treino", paymentMethod = "Cartão"),
                FinancialRecordEntity(amount = 980.0, type = "EXPENSE", category = "Alimentação", timestamp = baseMillis - 86_400_000L * 2, epochDay = today.minusDays(2).toEpochDay(), note = "Almoço limpo pós-treino", paymentMethod = "M-Pesa"),
                FinancialRecordEntity(amount = 2_850.0, type = "EXPENSE", category = "Livros & Educação", timestamp = baseMillis - 86_400_000L, epochDay = today.minusDays(1).toEpochDay(), paymentMethod = "e-Mola"),
                FinancialRecordEntity(amount = 920.0, type = "EXPENSE", category = "Alimentação", timestamp = baseMillis, epochDay = today.toEpochDay(), note = "Almoço executivo", paymentMethod = "Débito")
            )
        )
    }

    private suspend fun LifeOsDao.insertBookAndReturnIdFallback(book: BookEntity): Long {
        insertBook(book)
        return getAllBooks().first().firstOrNull()?.id ?: 1L
    }

    private fun mapHabitWithLogs(
        item: HabitWithLogs,
        today: LocalDate,
        todayEpoch: Long,
        weekStartEpoch: Long
    ): HabitDomainModel {
        val completedLogs = item.logs.filter { it.completed }
        val completedEpochDays = completedLogs.map { it.epochDay }.toSet()
        val (calcCurrentStreak, calcLongestStreak) = StreakAndHeatmapCalculator.calculateStreaks(
            completedEpochDays = completedEpochDays,
            targetDaysMask = item.habit.targetDaysMask,
            today = today
        )

        val intensities = completedLogs.associate { log ->
            val ratio = if (item.habit.dailyGoalValue > 0f) {
                (log.valueLogged / item.habit.dailyGoalValue).coerceIn(0.35f, 1f)
            } else {
                1f
            }
            log.epochDay to ratio
        }

        val matrix = StreakAndHeatmapCalculator.buildMicroHeatmapMatrix(
            intensitiesByEpochDay = intensities,
            columns = 20,
            today = today
        )
        val annualMatrix52 = StreakAndHeatmapCalculator.buildMicroHeatmapMatrix(
            intensitiesByEpochDay = intensities,
            columns = 52,
            today = today
        )

        val targetPerWeek = StreakAndHeatmapCalculator.countTargetDaysPerWeek(item.habit.targetDaysMask)
        val completedThisWeek = completedLogs.count { it.epochDay in weekStartEpoch..todayEpoch }
        val goalPct = if (targetPerWeek > 0) {
            ((completedThisWeek.toFloat() / targetPerWeek.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        } else {
            0
        }

        val recentRecords = completedLogs
            .sortedByDescending { it.epochDay }
            .take(8)
            .map { log ->
                val dateText = LocalDate.ofEpochDay(log.epochDay).format(shortDateFormatter)
                val timeText = Instant.ofEpochMilli(log.loggedAtMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime()
                    .format(timeFormatter)
                HabitRecordEntry(
                    epochDay = log.epochDay,
                    formattedDate = dateText,
                    formattedTime = timeText,
                    valueLogged = log.valueLogged,
                    note = log.note
                )
            }

        return HabitDomainModel(
            id = item.habit.id,
            name = item.habit.name,
            description = item.habit.description,
            category = item.habit.category,
            hexColor = item.habit.hexColor,
            iconKey = item.habit.iconKey,
            targetDaysMask = item.habit.targetDaysMask,
            currentStreak = max(item.habit.currentStreak, calcCurrentStreak),
            longestStreak = max(item.habit.longestStreak, calcLongestStreak),
            totalCheckIns = completedLogs.size,
            completedToday = completedEpochDays.contains(todayEpoch),
            completedThisWeek = completedThisWeek,
            targetPerWeek = targetPerWeek,
            avgLogTimeFormatted = StreakAndHeatmapCalculator.computeAverageLogTime(
                completedLogs.map { it.loggedAtMillis }
            ),
            goalProgressPercent = goalPct,
            heatmapMatrix = matrix,
            annualHeatmap52Weeks = annualMatrix52,
            recentRecords = recentRecords
        )
    }

    private fun mapWorkoutRelation(relation: WorkoutRoutineWithExercises): WorkoutRoutineDomainModel {
        return WorkoutRoutineDomainModel(
            id = relation.routine.id,
            name = relation.routine.name,
            dayOfWeek = relation.routine.dayOfWeek,
            calendarEventId = relation.routine.calendarEventId,
            estimatedDurationMinutes = relation.routine.estimatedDurationMinutes,
            exercises = relation.sortedExercises.map { ex ->
                ExerciseDomainModel(
                    id = ex.id,
                    routineId = ex.routineId,
                    name = ex.name,
                    targetSets = ex.targetSets,
                    targetReps = ex.targetReps,
                    restSeconds = ex.restSeconds,
                    orderIndex = ex.orderIndex,
                    completedSetsToday = ex.completedSetsToday,
                    lastWeightKg = ex.lastWeightKg
                )
            }
        )
    }

    private fun mapStudyTopic(entity: StudyTopicEntity, todayEpoch: Long): StudyTopicDomainModel {
        val elapsed = if (entity.lastReviewedEpochDay != null) {
            (todayEpoch - entity.lastReviewedEpochDay!!).coerceAtLeast(0L)
        } else {
            1L
        }
        return StudyTopicDomainModel(
            id = entity.id,
            subject = entity.subject,
            topic = entity.topic,
            stage = entity.stage,
            nextReviewEpochDay = entity.nextReviewEpochDay,
            daysUntilReview = entity.nextReviewEpochDay - todayEpoch,
            estimatedRetentionPercent = EbbinghausScheduler.estimateRetentionPercent(
                stage = entity.stage,
                daysElapsedSinceLastReview = elapsed
            ),
            reviewCount = entity.reviewCount,
            reviewedToday = entity.lastReviewedEpochDay == todayEpoch
        )
    }

    private data class FinanceRawBundle(
        val summary: com.lifeos.core.database.model.CashflowSummaryTuple,
        val categories: List<com.lifeos.core.database.model.CategoryExpenseTuple>,
        val currentDailyBurn: List<com.lifeos.core.database.model.DailyExpenseTuple>,
        val prevDailyBurn: List<com.lifeos.core.database.model.DailyExpenseTuple>,
        val allTransactions: List<FinancialRecordEntity>
    )

    private data class MindAndFinanceSnapshot(
        val reviewedCount: Int,
        val dueCount: Int,
        val pagesReadToday: Int,
        val dailyGoalPages: Int,
        val income: Double,
        val expenses: Double
    )

    companion object {
        private const val NIGHT_BALANCE_WHEEL_ALARM_CODE = 99_220
    }
}
