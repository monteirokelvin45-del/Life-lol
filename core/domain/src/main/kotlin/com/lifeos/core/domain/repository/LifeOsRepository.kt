package com.lifeos.core.domain.repository

import com.lifeos.core.domain.model.AiExtractedPlanPayload
import com.lifeos.core.domain.model.AntiCharityContractModel
import com.lifeos.core.domain.model.BalanceWheelScore
import com.lifeos.core.domain.model.BookDomainModel
import com.lifeos.core.domain.model.BookQuoteDomainModel
import com.lifeos.core.domain.model.FinancialOverviewModel
import com.lifeos.core.domain.model.GhostSquadPeerModel
import com.lifeos.core.domain.model.HabitDomainModel
import com.lifeos.core.domain.model.StreakFreezeStatusModel
import com.lifeos.core.domain.model.StudyTopicDomainModel
import com.lifeos.core.domain.model.WorkoutRoutineDomainModel
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface LifeOsRepository {

    fun observeHabits(today: LocalDate = LocalDate.now()): Flow<List<HabitDomainModel>>

    fun observeTodayWorkout(dayOfWeek: Int): Flow<WorkoutRoutineDomainModel?>

    fun observeAllWorkouts(): Flow<List<WorkoutRoutineDomainModel>>

    fun observeDueStudyTopics(today: LocalDate = LocalDate.now()): Flow<List<StudyTopicDomainModel>>

    fun observeAllStudyTopics(today: LocalDate = LocalDate.now()): Flow<List<StudyTopicDomainModel>>

    fun observeFinancialOverview(today: LocalDate = LocalDate.now()): Flow<FinancialOverviewModel>

    fun observeActiveBooks(): Flow<List<BookDomainModel>>

    fun observeBookQuotes(): Flow<List<BookQuoteDomainModel>>

    fun observeBalanceWheel(today: LocalDate = LocalDate.now()): Flow<BalanceWheelScore>

    fun observeStreakFreezeStatus(today: LocalDate = LocalDate.now()): Flow<StreakFreezeStatusModel>

    fun observeGhostSquadPeers(): Flow<List<GhostSquadPeerModel>>

    fun observeAntiCharityContract(today: LocalDate = LocalDate.now()): Flow<AntiCharityContractModel>

    suspend fun activateStreakFreezeToday(today: LocalDate = LocalDate.now()): Boolean

    suspend fun updateGhostSquadPeers(peers: List<Triple<String, String, String>>)

    suspend fun saveAntiCharityContract(
        weeklyTargetRatio: Float,
        stakeAmount: Double,
        currencyCode: String,
        institutionName: String,
        isActive: Boolean
    )

    suspend fun createHabit(
        name: String,
        description: String,
        category: String,
        hexColor: String,
        targetDaysMask: Int
    ): Long

    suspend fun toggleHabitCheckIn(
        habitId: Long,
        date: LocalDate = LocalDate.now(),
        note: String? = null
    )

    suspend fun completeExerciseSet(
        exerciseId: Long,
        newCompletedSets: Int,
        date: LocalDate = LocalDate.now(),
        weightKg: Float = 0f
    )

    suspend fun updateWorkoutCalendarEvent(
        routineId: Long,
        calendarEventId: Long
    )

    suspend fun createStudyTopic(
        subject: String,
        topic: String,
        today: LocalDate = LocalDate.now()
    ): Long

    suspend fun reviewStudyTopic(
        topicId: Long,
        rememberedWell: Boolean,
        today: LocalDate = LocalDate.now()
    )

    suspend fun recordFinancialTransaction(
        amount: Double,
        type: String,
        category: String,
        note: String?,
        currencyCode: String = "MZN",
        paymentMethod: String = "M-Pesa"
    )

    suspend fun addBook(
        title: String,
        author: String,
        totalPages: Int,
        dailyGoalPages: Int
    )

    suspend fun logReadingSession(
        bookId: Long,
        newCurrentPage: Int,
        sessionMinutes: Int,
        today: LocalDate = LocalDate.now()
    )

    suspend fun saveBookQuote(
        bookId: Long,
        quoteText: String,
        pageNumber: Int
    )

    suspend fun ingestExtractedPlan(
        plan: AiExtractedPlanPayload,
        today: LocalDate = LocalDate.now()
    )

    suspend fun seedInitialShowcaseDataIfEmpty(today: LocalDate = LocalDate.now())
}
