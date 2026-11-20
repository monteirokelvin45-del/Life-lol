package com.lifeos.app.features.workout

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.core.domain.model.WorkoutRoutineDomainModel
import com.lifeos.core.domain.repository.LifeOsRepository
import com.lifeos.core.system.calendar.CalendarSyncManager
import com.lifeos.core.system.service.ActiveExecutionService
import com.lifeos.core.system.service.ActiveTimerSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class WorkoutUiState(
    val selectedDayOfWeek: Int = LocalDate.now().dayOfWeek.value,
    val routine: WorkoutRoutineDomainModel? = null,
    val activeTimer: ActiveTimerSnapshot = ActiveTimerSnapshot(),
    val calendarSyncMessage: String? = null
)

sealed interface WorkoutIntent {
    data class SelectDay(val dayOfWeek: Int) : WorkoutIntent
    data class LogExerciseSet(
        val exerciseId: Long,
        val exerciseName: String,
        val nextSetsCount: Int,
        val restSeconds: Int
    ) : WorkoutIntent
    data object SyncRoutineToCalendar : WorkoutIntent
    data object StopRestTimer : WorkoutIntent
    data object ClearMessage : WorkoutIntent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkoutViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: LifeOsRepository,
    private val calendarSyncManager: CalendarSyncManager
) : ViewModel() {

    private val selectedDayFlow = MutableStateFlow(LocalDate.now().dayOfWeek.value)
    private val calendarMessageFlow = MutableStateFlow<String?>(null)

    private val routineFlow = selectedDayFlow.flatMapLatest { day ->
        repository.observeTodayWorkout(day)
    }

    val uiState: StateFlow<WorkoutUiState> = combine(
        selectedDayFlow,
        routineFlow,
        ActiveExecutionService.timerState,
        calendarMessageFlow
    ) { day, routine, timer, msg ->
        WorkoutUiState(
            selectedDayOfWeek = day,
            routine = routine,
            activeTimer = timer,
            calendarSyncMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WorkoutUiState()
    )

    fun onIntent(intent: WorkoutIntent) {
        when (intent) {
            is WorkoutIntent.SelectDay -> {
                selectedDayFlow.value = intent.dayOfWeek.coerceIn(1, 7)
            }
            is WorkoutIntent.LogExerciseSet -> {
                viewModelScope.launch {
                    repository.completeExerciseSet(
                        exerciseId = intent.exerciseId,
                        newCompletedSets = intent.nextSetsCount
                    )
                    ActiveExecutionService.startTimer(
                        context = appContext,
                        label = "Descanso · ${intent.exerciseName}",
                        durationSeconds = intent.restSeconds
                    )
                }
            }
            WorkoutIntent.SyncRoutineToCalendar -> {
                syncCurrentRoutineToGoogleCalendar()
            }
            WorkoutIntent.StopRestTimer -> {
                ActiveExecutionService.stopTimer(appContext)
            }
            WorkoutIntent.ClearMessage -> {
                calendarMessageFlow.value = null
            }
        }
    }

    private fun syncCurrentRoutineToGoogleCalendar() {
        val currentRoutine = uiState.value.routine ?: return
        viewModelScope.launch {
            val today = LocalDate.now()
            val startMillis = today.atTime(17, 30)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            val endMillis = startMillis + (currentRoutine.estimatedDurationMinutes * 60_000L)
            val exerciseSummary = currentRoutine.exercises.joinToString("\n") {
                "• ${it.name}: ${it.targetSets}x${it.targetReps} (${it.restSeconds}s)"
            }

            val eventId = calendarSyncManager.createEvent(
                title = "Life OS · ${currentRoutine.name}",
                description = exerciseSummary,
                startEpochMillis = startMillis,
                endEpochMillis = endMillis
            )

            if (eventId != null) {
                repository.updateWorkoutCalendarEvent(currentRoutine.id, eventId)
                calendarMessageFlow.value = "Sincronizado com o Google Calendar (ID #$eventId)."
            } else {
                calendarMessageFlow.value = "Permissão de Calendário pendente no sistema Android."
            }
        }
    }
}
