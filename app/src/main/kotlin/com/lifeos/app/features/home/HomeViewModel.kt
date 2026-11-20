package com.lifeos.app.features.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.core.ai.engine.StructuredPlanAiEngine
import com.lifeos.core.ai.extractor.DocumentTextExtractor
import com.lifeos.core.domain.algorithm.TimeContextResolver
import com.lifeos.core.domain.model.BalanceWheelScore
import com.lifeos.core.domain.model.BookDomainModel
import com.lifeos.core.domain.model.FinancialOverviewModel
import com.lifeos.core.domain.model.HabitDomainModel
import com.lifeos.core.domain.model.StudyTopicDomainModel
import com.lifeos.core.domain.model.TimeOfDayContext
import com.lifeos.core.domain.model.WorkoutRoutineDomainModel
import com.lifeos.core.domain.repository.LifeOsRepository
import com.lifeos.core.domain.usecase.CompleteStudyReviewUseCase
import com.lifeos.core.domain.usecase.IngestPlanUseCase
import com.lifeos.core.domain.usecase.ToggleHabitCheckInUseCase
import com.lifeos.core.system.service.ActiveExecutionService
import com.lifeos.core.system.service.ActiveTimerSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val timeContext: TimeOfDayContext = TimeContextResolver.resolve(),
    val balanceWheel: BalanceWheelScore = BalanceWheelScore(0f, 0f, 0f, 0f, 0f),
    val habits: List<HabitDomainModel> = emptyList(),
    val todayWorkout: WorkoutRoutineDomainModel? = null,
    val dueStudies: List<StudyTopicDomainModel> = emptyList(),
    val activeBooks: List<BookDomainModel> = emptyList(),
    val financialOverview: FinancialOverviewModel? = null,
    val activeTimer: ActiveTimerSnapshot = ActiveTimerSnapshot(),
    val isIngesting: Boolean = false,
    val statusMessage: String? = null
)

sealed interface HomeIntent {
    data class SelectTimeContext(val context: TimeOfDayContext) : HomeIntent
    data class ToggleHabit(val habitId: Long) : HomeIntent
    data class ReviewTopic(val topicId: Long) : HomeIntent
    data class IngestDocumentUri(val uri: Uri) : HomeIntent
    data class IngestRawText(val text: String) : HomeIntent
    data class StartPomodoroFocus(val minutes: Int = 25) : HomeIntent
    data object StopActiveTimer : HomeIntent
    data object DismissMessage : HomeIntent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: LifeOsRepository,
    private val toggleHabitUseCase: ToggleHabitCheckInUseCase,
    private val completeStudyReviewUseCase: CompleteStudyReviewUseCase,
    private val ingestPlanUseCase: IngestPlanUseCase,
    private val documentTextExtractor: DocumentTextExtractor,
    private val aiEngine: StructuredPlanAiEngine
) : ViewModel() {

    private val manualTimeContext = MutableStateFlow<TimeOfDayContext?>(null)
    private val isIngestingFlow = MutableStateFlow(false)
    private val messageFlow = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.seedInitialShowcaseDataIfEmpty(LocalDate.now())
        }
    }

    private val coreDomainBundle = combine(
        repository.observeBalanceWheel(),
        repository.observeHabits(),
        repository.observeTodayWorkout(LocalDate.now().dayOfWeek.value),
        repository.observeDueStudyTopics(),
        repository.observeActiveBooks()
    ) { wheel, habits, workout, studies, books ->
        CoreBundle(wheel, habits, workout, studies, books)
    }

    private val auxStateBundle = combine(
        repository.observeFinancialOverview(),
        ActiveExecutionService.timerState,
        manualTimeContext,
        isIngestingFlow,
        messageFlow
    ) { finance, timer, overrideCtx, ingesting, msg ->
        AuxBundle(
            finance = finance,
            timer = timer,
            timeContext = overrideCtx ?: TimeContextResolver.resolve(),
            isIngesting = ingesting,
            message = msg
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(coreDomainBundle, auxStateBundle) { core, aux ->
        HomeUiState(
            timeContext = aux.timeContext,
            balanceWheel = core.wheel,
            habits = core.habits,
            todayWorkout = core.workout,
            dueStudies = core.studies,
            activeBooks = core.books,
            financialOverview = aux.finance,
            activeTimer = aux.timer,
            isIngesting = aux.isIngesting,
            statusMessage = aux.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SelectTimeContext -> {
                manualTimeContext.value = intent.context
            }
            is HomeIntent.ToggleHabit -> {
                viewModelScope.launch { toggleHabitUseCase(intent.habitId) }
            }
            is HomeIntent.ReviewTopic -> {
                viewModelScope.launch { completeStudyReviewUseCase(intent.topicId, rememberedWell = true) }
            }
            is HomeIntent.IngestDocumentUri -> {
                ingestFromUri(intent.uri)
            }
            is HomeIntent.IngestRawText -> {
                ingestFromText(intent.text)
            }
            is HomeIntent.StartPomodoroFocus -> {
                ActiveExecutionService.startTimer(
                    context = appContext,
                    label = "Pomodoro Cognitivo (${intent.minutes}m)",
                    durationSeconds = intent.minutes * 60
                )
            }
            HomeIntent.StopActiveTimer -> {
                ActiveExecutionService.stopTimer(appContext)
            }
            HomeIntent.DismissMessage -> {
                messageFlow.value = null
            }
        }
    }

    private fun ingestFromUri(uri: Uri) {
        viewModelScope.launch {
            isIngestingFlow.value = true
            runCatching {
                val rawText = documentTextExtractor.extractTextFromUri(uri)
                val plan = aiEngine.extractStructuredPlan(rawText)
                ingestPlanUseCase(plan)
                val totalItems = plan.detectedHabits.size +
                    plan.detectedWorkouts.size +
                    plan.detectedStudies.size +
                    plan.detectedFinances.size
                messageFlow.value = "Plano ingerido com sucesso ($totalItems blocos detectados)."
            }.onFailure { err ->
                messageFlow.value = "Falha na leitura do arquivo: ${err.localizedMessage ?: "formato inválido"}"
            }
            isIngestingFlow.value = false
        }
    }

    private fun ingestFromText(rawText: String) {
        viewModelScope.launch {
            isIngestingFlow.value = true
            runCatching {
                val plan = aiEngine.extractStructuredPlan(rawText)
                ingestPlanUseCase(plan)
                val totalItems = plan.detectedHabits.size +
                    plan.detectedWorkouts.size +
                    plan.detectedStudies.size +
                    plan.detectedFinances.size
                messageFlow.value = "Rotina hiper-automatizada criada ($totalItems itens persistidos)."
            }.onFailure { err ->
                messageFlow.value = "Erro ao processar texto: ${err.localizedMessage}"
            }
            isIngestingFlow.value = false
        }
    }

    private data class CoreBundle(
        val wheel: BalanceWheelScore,
        val habits: List<HabitDomainModel>,
        val workout: WorkoutRoutineDomainModel?,
        val studies: List<StudyTopicDomainModel>,
        val books: List<BookDomainModel>
    )

    private data class AuxBundle(
        val finance: FinancialOverviewModel,
        val timer: ActiveTimerSnapshot,
        val timeContext: TimeOfDayContext,
        val isIngesting: Boolean,
        val message: String?
    )
}
