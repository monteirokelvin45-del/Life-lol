package com.lifeos.app.features.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.core.domain.model.HabitDomainModel
import com.lifeos.core.domain.model.StreakFreezeStatusModel
import com.lifeos.core.domain.repository.LifeOsRepository
import com.lifeos.core.domain.usecase.ToggleHabitCheckInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HabitsUiState(
    val habits: List<HabitDomainModel> = emptyList(),
    val streakFreeze: StreakFreezeStatusModel = StreakFreezeStatusModel(1, 5, 0, 0, 7, false),
    val selectedHabitId: Long? = null
) {
    val selectedHabit: HabitDomainModel?
        get() = habits.find { it.id == selectedHabitId }
}

sealed interface HabitsIntent {
    data class ToggleToday(val habitId: Long, val note: String? = null) : HabitsIntent
    data class SelectHabitDetail(val habitId: Long?) : HabitsIntent
    data object ActivateStreakFreeze : HabitsIntent
    data class CreateHabit(
        val name: String,
        val description: String,
        val category: String,
        val hexColor: String,
        val targetDaysMask: Int
    ) : HabitsIntent
}

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val repository: LifeOsRepository,
    private val toggleHabitCheckInUseCase: ToggleHabitCheckInUseCase
) : ViewModel() {

    private val selectedIdFlow = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<HabitsUiState> = combine(
        repository.observeHabits(),
        repository.observeStreakFreezeStatus(),
        selectedIdFlow
    ) { habits, freeze, selectedId ->
        HabitsUiState(
            habits = habits,
            streakFreeze = freeze,
            selectedHabitId = selectedId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HabitsUiState()
    )

    fun onIntent(intent: HabitsIntent) {
        when (intent) {
            is HabitsIntent.ToggleToday -> {
                viewModelScope.launch {
                    toggleHabitCheckInUseCase(habitId = intent.habitId, note = intent.note)
                }
            }
            is HabitsIntent.SelectHabitDetail -> {
                selectedIdFlow.value = intent.habitId
            }
            HabitsIntent.ActivateStreakFreeze -> {
                viewModelScope.launch {
                    repository.activateStreakFreezeToday()
                }
            }
            is HabitsIntent.CreateHabit -> {
                viewModelScope.launch {
                    repository.createHabit(
                        name = intent.name,
                        description = intent.description,
                        category = intent.category,
                        hexColor = intent.hexColor,
                        targetDaysMask = intent.targetDaysMask
                    )
                }
            }
        }
    }
}
