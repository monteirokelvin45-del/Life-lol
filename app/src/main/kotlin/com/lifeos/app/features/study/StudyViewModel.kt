package com.lifeos.app.features.study

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.core.ai.extractor.DocumentTextExtractor
import com.lifeos.core.domain.algorithm.BehavioralRetentionEngine
import com.lifeos.core.domain.model.BookDomainModel
import com.lifeos.core.domain.model.BookQuoteDomainModel
import com.lifeos.core.domain.model.KnowledgeBurndownProjection
import com.lifeos.core.domain.model.StudyTopicDomainModel
import com.lifeos.core.domain.repository.LifeOsRepository
import com.lifeos.core.domain.usecase.CompleteStudyReviewUseCase
import com.lifeos.core.domain.usecase.LogReadingSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActiveReadingTimerState(
    val activeBookId: Long? = null,
    val elapsedSeconds: Int = 0
)

data class StudyUiState(
    val dueTopics: List<StudyTopicDomainModel> = emptyList(),
    val allTopics: List<StudyTopicDomainModel> = emptyList(),
    val activeBooks: List<BookDomainModel> = emptyList(),
    val quotes: List<BookQuoteDomainModel> = emptyList(),
    val burndownProjection: KnowledgeBurndownProjection = BehavioralRetentionEngine.calculateKnowledgeBurndown(null),
    val readingTimer: ActiveReadingTimerState = ActiveReadingTimerState(),
    val scannedQuoteDraft: String? = null,
    val isScanningOcr: Boolean = false
)

sealed interface StudyIntent {
    data class ReviewTopic(val topicId: Long, val rememberedWell: Boolean) : StudyIntent
    data class AddTopic(val subject: String, val topic: String) : StudyIntent
    data class StartReadingTimer(val bookId: Long) : StudyIntent
    data class FinishReadingTimer(val bookId: Long, val stoppedAtPage: Int) : StudyIntent
    data class LogReading(
        val bookId: Long,
        val newCurrentPage: Int,
        val sessionMinutes: Int
    ) : StudyIntent
    data class ScanBookPageBitmap(val bitmap: Bitmap) : StudyIntent
    data class SaveScannedQuote(val bookId: Long, val quoteText: String, val pageNumber: Int) : StudyIntent
    data object DismissScannedDraft : StudyIntent
}

@HiltViewModel
class StudyViewModel @Inject constructor(
    private val repository: LifeOsRepository,
    private val completeStudyReviewUseCase: CompleteStudyReviewUseCase,
    private val logReadingSessionUseCase: LogReadingSessionUseCase,
    private val documentTextExtractor: DocumentTextExtractor
) : ViewModel() {

    private val readingTimerFlow = MutableStateFlow(ActiveReadingTimerState())
    private val scannedDraftFlow = MutableStateFlow<String?>(null)
    private val scanningFlow = MutableStateFlow(false)
    private var stopwatchJob: Job? = null

    private val dataFlow = combine(
        repository.observeDueStudyTopics(),
        repository.observeAllStudyTopics(),
        repository.observeActiveBooks(),
        repository.observeBookQuotes()
    ) { due, all, books, quotes ->
        StudyDataQuad(due, all, books, quotes)
    }

    val uiState: StateFlow<StudyUiState> = combine(
        dataFlow,
        readingTimerFlow,
        scannedDraftFlow,
        scanningFlow
    ) { quad, timer, draft, scanning ->
        StudyUiState(
            dueTopics = quad.due,
            allTopics = quad.all,
            activeBooks = quad.books,
            quotes = quad.quotes,
            burndownProjection = BehavioralRetentionEngine.calculateKnowledgeBurndown(quad.books.firstOrNull()),
            readingTimer = timer,
            scannedQuoteDraft = draft,
            isScanningOcr = scanning
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
            is StudyIntent.StartReadingTimer -> {
                startStopwatch(intent.bookId)
            }
            is StudyIntent.FinishReadingTimer -> {
                val elapsedMinutes = ((readingTimerFlow.value.elapsedSeconds / 60).coerceAtLeast(1))
                stopwatchJob?.cancel()
                readingTimerFlow.value = ActiveReadingTimerState()
                viewModelScope.launch {
                    logReadingSessionUseCase(
                        bookId = intent.bookId,
                        newCurrentPage = intent.stoppedAtPage,
                        sessionMinutes = elapsedMinutes
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
            is StudyIntent.ScanBookPageBitmap -> {
                viewModelScope.launch {
                    scanningFlow.value = true
                    val extracted = runCatching {
                        documentTextExtractor.extractTextFromBitmap(intent.bitmap)
                    }.getOrDefault("")
                    scannedDraftFlow.value = extracted.ifBlank {
                        "Digite ou ajuste a citação capturada da página..."
                    }
                    scanningFlow.value = false
                }
            }
            is StudyIntent.SaveScannedQuote -> {
                viewModelScope.launch {
                    repository.saveBookQuote(
                        bookId = intent.bookId,
                        quoteText = intent.quoteText,
                        pageNumber = intent.pageNumber
                    )
                    scannedDraftFlow.value = null
                }
            }
            StudyIntent.DismissScannedDraft -> {
                scannedDraftFlow.value = null
            }
        }
    }

    private fun startStopwatch(bookId: Long) {
        stopwatchJob?.cancel()
        readingTimerFlow.value = ActiveReadingTimerState(activeBookId = bookId, elapsedSeconds = 0)
        stopwatchJob = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                val curr = readingTimerFlow.value
                if (curr.activeBookId != bookId) break
                readingTimerFlow.value = curr.copy(elapsedSeconds = curr.elapsedSeconds + 1)
            }
        }
    }

    private data class StudyDataQuad(
        val due: List<StudyTopicDomainModel>,
        val all: List<StudyTopicDomainModel>,
        val books: List<BookDomainModel>,
        val quotes: List<BookQuoteDomainModel>
    )
}
