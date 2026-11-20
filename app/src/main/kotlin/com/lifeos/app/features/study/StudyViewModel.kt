package com.lifeos.app.features.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.core.domain.model.BookDomainModel
import com.lifeos.core.domain.model.StudyTopicDomainModel
import com.lifeos.core.domain.repository.LifeOsRepository
import com.lifeos.core.domain.usecase.CompleteStudyReviewUseCase
import com.lifeos.core.domain.usecase.LogReadingSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudyUiState(
    val dueTopics: List<StudyTopicDomainModel> = emptyList(),
    val allTopics: List<StudyTopicDomainModel> = emptyList(),
    val activeBooks: List<BookDomainModel> = emptyList()
)

sealed interface StudyIntent {
    data class ReviewTopic(val topicId: Long, val rememberedWell: Boolean) : StudyIntent
    data class AddTopic(val subject: String, val topic: String) : StudyIntent
    data class AddBook(
        val title: String,
        val author: String,
        val totalPages: Int,
        val dailyGoalPages: Int
    ) : StudyIntent
    data class LogReading(
        val bookId: Long,
        val newCurrentPage: Int,
        val sessionMinutes: Int
    ) : StudyIntent
}

@HiltViewModel
class StudyViewModel @Inject constructor(
    private val repository: LifeOsRepository,
    private val completeStudyReviewUseCase: CompleteStudyReviewUseCase,
    private val logReadingSessionUseCase: LogReadingSessionUseCase
) : ViewModel() {

    val uiState: StateFlow<StudyUiState> = combine(
        repository.observeDueStudyTopics(),
        repository.observeAllStudyTopics(),
        repository.observeActiveBooks()
    ) { due, all, books ->
        StudyUiState(
            dueTopics = due,
            allTopics = all,
            activeBooks = books
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StudyUiState()
    )

    fun onIntent(intent: StudyIntent) {
        when (intent) {
            is StudyIntent.ReviewTopic -> {
                viewModelScope.launch {
                    completeStudyReviewUseCase(
                        topicId = intent.topicId,
                        rememberedWell = intent.rememberedWell
                    )
                }
            }
            is StudyIntent.AddTopic -> {
                viewModelScope.launch {
                    repository.createStudyTopic(
                        subject = intent.subject,
                        topic = intent.topic
                    )
                }
            }
            is StudyIntent.AddBook -> {
                viewModelScope.launch {
                    repository.addBook(
                        title = intent.title,
                        author = intent.author,
                        totalPages = intent.totalPages,
                        dailyGoalPages = intent.dailyGoalPages
                    )
                }
            }
            is StudyIntent.LogReading -> {
                viewModelScope.launch {
                    logReadingSessionUseCase(
                        bookId = intent.bookId,
                        newCurrentPage = intent.newCurrentPage,
                        sessionMinutes = intent.sessionMinutes
                    )
                }
            }
        }
    }
}
