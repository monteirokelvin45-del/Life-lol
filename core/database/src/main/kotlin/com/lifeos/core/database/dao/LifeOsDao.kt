package com.lifeos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.lifeos.core.database.entity.BookEntity
import com.lifeos.core.database.entity.BookQuoteEntity
import com.lifeos.core.database.entity.ExerciseEntity
import com.lifeos.core.database.entity.FinancialRecordEntity
import com.lifeos.core.database.entity.HabitEntity
import com.lifeos.core.database.entity.HabitLogEntity
import com.lifeos.core.database.entity.StudyTopicEntity
import com.lifeos.core.database.entity.WorkoutRoutineEntity
import com.lifeos.core.database.model.CashflowSummaryTuple
import com.lifeos.core.database.model.CategoryExpenseTuple
import com.lifeos.core.database.model.DailyExpenseTuple
import com.lifeos.core.database.model.HabitWithLogs
import com.lifeos.core.database.model.WorkoutRoutineWithExercises
import kotlinx.coroutines.flow.Flow

/**
 * Interface DAO unificada do Life OS com consultas reativas (`Flow<List<T>>`)
 * e operações assíncronas (`suspend`) para os 5 domínios centrais + Roda do Equilíbrio.
 */
@Dao
interface LifeOsDao {

    // =========================================================================
    // 1. DOMÍNIO HÁBITOS & MICRO-HEATMAPS
    // =========================================================================

    @Query("SELECT * FROM habits WHERE is_archived = 0 ORDER BY id ASC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Transaction
    @Query("SELECT * FROM habits WHERE is_archived = 0 ORDER BY id ASC")
    fun getAllHabitsWithLogs(): Flow<List<HabitWithLogs>>

    @Transaction
    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    fun getHabitWithLogsById(habitId: Long): Flow<HabitWithLogs?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<HabitEntity>): List<Long>

    /**
     * Atualiza ou insere o hábito sem acionar DELETE em cascata sobre `habit_logs`.
     */
    @Upsert
    suspend fun upsertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query(
        """
        UPDATE habits
        SET current_streak = :currentStreak,
            longest_streak = CASE
                WHEN :longestStreak > longest_streak THEN :longestStreak
                ELSE longest_streak
            END
        WHERE id = :habitId
        """
    )
    suspend fun updateHabitStreak(
        habitId: Long,
        currentStreak: Int,
        longestStreak: Int
    )

    @Query("UPDATE habits SET is_archived = 1 WHERE id = :habitId")
    suspend fun archiveHabit(habitId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitLog(log: HabitLogEntity)

    @Query(
        """
        SELECT * FROM habit_logs
        WHERE habit_id = :habitId AND epoch_day >= :startEpochDay
        ORDER BY epoch_day ASC
        """
    )
    fun getLogsForHabit(habitId: Long, startEpochDay: Long): Flow<List<HabitLogEntity>>

    @Query(
        """
        SELECT * FROM habit_logs
        WHERE epoch_day BETWEEN :startEpochDay AND :endEpochDay
        ORDER BY epoch_day ASC
        """
    )
    fun getHabitLogsInWindow(
        startEpochDay: Long,
        endEpochDay: Long
    ): Flow<List<HabitLogEntity>>

    @Query("DELETE FROM habit_logs WHERE habit_id = :habitId AND epoch_day = :epochDay")
    suspend fun deleteHabitLogForDay(habitId: Long, epochDay: Long)

    // =========================================================================
    // 2. DOMÍNIO TREINO & FISIOLOGIA
    // =========================================================================

    @Query("SELECT * FROM workout_routines WHERE day_of_week = :dayOfWeek LIMIT 1")
    fun getRoutineForDay(dayOfWeek: Int): Flow<WorkoutRoutineEntity?>

    @Transaction
    @Query("SELECT * FROM workout_routines WHERE day_of_week = :dayOfWeek LIMIT 1")
    fun getRoutineWithExercisesForDay(dayOfWeek: Int): Flow<WorkoutRoutineWithExercises?>

    @Transaction
    @Query("SELECT * FROM workout_routines ORDER BY day_of_week ASC")
    fun getAllRoutinesWithExercises(): Flow<List<WorkoutRoutineWithExercises>>

    @Query("SELECT * FROM exercises WHERE routine_id = :routineId ORDER BY order_index ASC")
    fun getExercisesForRoutine(routineId: Long): Flow<List<ExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutRoutine(routine: WorkoutRoutineEntity): Long

    @Upsert
    suspend fun upsertWorkoutRoutine(routine: WorkoutRoutineEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Query("UPDATE workout_routines SET calendar_event_id = :calendarEventId WHERE id = :routineId")
    suspend fun updateWorkoutCalendarEventId(routineId: Long, calendarEventId: Long)

    @Query(
        """
        UPDATE exercises
        SET completed_sets_today = :completedSets,
            last_completed_epoch_day = :epochDay,
            last_weight_kg = CASE WHEN :weightKg > 0 THEN :weightKg ELSE last_weight_kg END
        WHERE id = :exerciseId
        """
    )
    suspend fun updateExerciseProgress(
        exerciseId: Long,
        completedSets: Int,
        epochDay: Long,
        weightKg: Float = 0f
    )

    @Query("DELETE FROM exercises WHERE routine_id = :routineId")
    suspend fun deleteExercisesForRoutine(routineId: Long)

    // =========================================================================
    // 3. DOMÍNIO ESTUDOS & CURVA DE EBBINGHAUS
    // =========================================================================

    @Query(
        """
        SELECT * FROM study_topics
        WHERE next_review_epoch_day <= :currentEpochDay
        ORDER BY next_review_epoch_day ASC, stage ASC
        """
    )
    fun getDueStudyReviews(currentEpochDay: Long): Flow<List<StudyTopicEntity>>

    @Query("SELECT * FROM study_topics ORDER BY next_review_epoch_day ASC")
    fun getAllStudyTopics(): Flow<List<StudyTopicEntity>>

    @Query("SELECT * FROM study_topics WHERE last_reviewed_epoch_day = :epochDay")
    fun getStudyTopicsReviewedOnDay(epochDay: Long): Flow<List<StudyTopicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyTopic(topic: StudyTopicEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyTopics(topics: List<StudyTopicEntity>): List<Long>

    @Update
    suspend fun updateStudyTopic(topic: StudyTopicEntity)

    @Query("DELETE FROM study_topics WHERE id = :topicId")
    suspend fun deleteStudyTopicById(topicId: Long)

    // =========================================================================
    // 4. DOMÍNIO FINANÇAS & FLUXO DE CAIXA
    // =========================================================================

    @Query("SELECT * FROM financial_records ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<FinancialRecordEntity>>

    @Query(
        """
        SELECT * FROM financial_records
        WHERE timestamp BETWEEN :startTimestamp AND :endTimestamp
        ORDER BY timestamp DESC
        """
    )
    fun getTransactionsBetween(
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<List<FinancialRecordEntity>>

    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0.0 END), 0.0) AS total_income,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0.0 END), 0.0) AS total_expense
        FROM financial_records
        WHERE epoch_day BETWEEN :startEpochDay AND :endEpochDay
        """
    )
    fun observeCashflowSummary(
        startEpochDay: Long,
        endEpochDay: Long
    ): Flow<CashflowSummaryTuple>

    @Query(
        """
        SELECT
            category AS category,
            COALESCE(SUM(amount), 0.0) AS total_amount
        FROM financial_records
        WHERE type = 'EXPENSE' AND epoch_day BETWEEN :startEpochDay AND :endEpochDay
        GROUP BY category
        ORDER BY total_amount DESC
        """
    )
    fun observeCategoryExpenses(
        startEpochDay: Long,
        endEpochDay: Long
    ): Flow<List<CategoryExpenseTuple>>

    @Query(
        """
        SELECT
            epoch_day AS epoch_day,
            COALESCE(SUM(amount), 0.0) AS daily_total
        FROM financial_records
        WHERE type = 'EXPENSE' AND epoch_day BETWEEN :startEpochDay AND :endEpochDay
        GROUP BY epoch_day
        ORDER BY epoch_day ASC
        """
    )
    fun observeDailyExpenseBurn(
        startEpochDay: Long,
        endEpochDay: Long
    ): Flow<List<DailyExpenseTuple>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: FinancialRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<FinancialRecordEntity>)

    @Query("DELETE FROM financial_records WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: Long)

    // =========================================================================
    // 5. DOMÍNIO LEITURA
    // =========================================================================

    @Query("SELECT * FROM books WHERE status = 'READING' ORDER BY id DESC")
    fun getActiveBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY id DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    fun getBookById(bookId: Long): Flow<BookEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query(
        """
        UPDATE books
        SET current_page = :currentPage,
            pages_read_today = :pagesReadToday,
            pages_per_hour = :pagesPerHour,
            status = :status,
            last_read_epoch_day = :epochDay
        WHERE id = :bookId
        """
    )
    suspend fun updateBookReadingProgress(
        bookId: Long,
        currentPage: Int,
        pagesReadToday: Int,
        pagesPerHour: Float,
        status: String,
        epochDay: Long
    )

    @Query("SELECT * FROM book_quotes ORDER BY captured_at_millis DESC")
    fun getAllBookQuotes(): Flow<List<BookQuoteEntity>>

    @Query("SELECT * FROM book_quotes WHERE book_id = :bookId ORDER BY captured_at_millis DESC")
    fun getQuotesForBook(bookId: Long): Flow<List<BookQuoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookQuote(quote: BookQuoteEntity): Long

    // =========================================================================
    // 6. TRANSAÇÃO ATÔMICA DE INGESTÃO DA IA (PILAR 5.2)
    // =========================================================================

    /**
     * Persiste atomicamente todo o plano extraído pelo motor de IA (Hábitos, Rotinas + Exercícios,
     * Tópicos de Estudo e Registros Financeiros). Caso qualquer inserção falhe, o SQLite faz
     * rollback completo, garantindo integridade referencial absoluta.
     */
    @Transaction
    suspend fun ingestStructuredPlanAtomically(
        habits: List<HabitEntity>,
        workoutsWithExercises: List<Pair<WorkoutRoutineEntity, List<ExerciseEntity>>>,
        studyTopics: List<StudyTopicEntity>,
        financialRecords: List<FinancialRecordEntity>
    ) {
        if (habits.isNotEmpty()) {
            insertHabits(habits)
        }

        for ((routine, exercises) in workoutsWithExercises) {
            val generatedRoutineId = insertWorkoutRoutine(routine)
            if (exercises.isNotEmpty()) {
                val boundExercises = exercises.mapIndexed { index, exercise ->
                    exercise.copy(
                        routineId = generatedRoutineId,
                        orderIndex = if (exercise.orderIndex >= 0) exercise.orderIndex else index
                    )
                }
                insertExercises(boundExercises)
            }
        }

        if (studyTopics.isNotEmpty()) {
            insertStudyTopics(studyTopics)
        }

        if (financialRecords.isNotEmpty()) {
            insertTransactions(financialRecords)
        }
    }
}
