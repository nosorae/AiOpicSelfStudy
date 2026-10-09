package io.github.nosorae.aiopic.ui.featurereport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.ui.featurereport.flag.MarkCorrectionWrongUseCase
import io.github.nosorae.aiopic.ui.featurereport.flag.ObserveAnswerReportUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** [wrongCorrections] 는 "틀림"을 누른 교정의 `Feedback.corrections` 안 순서다(F8.1). */
data class AnswerReportUiState(val loading: Boolean = true, val answer: Answer? = null, val wrongCorrections: Set<Int> = emptySet())

sealed interface AnswerReportIntent {
    /** 교정 하나의 "틀림" 표시를 붙이거나 뗀다. */
    data class ToggleCorrectionWrong(val index: Int) : AnswerReportIntent
}

/** 답변 리포트 화면(S11). 분석 대기 중에 열어도 분석이 끝나면 바로 피드백으로 바뀐다. */
@AssistedInject
class AnswerReportViewModel(
    @Assisted private val answerId: String,
    observeAnswerReportUseCase: ObserveAnswerReportUseCase,
    private val markCorrectionWrongUseCase: MarkCorrectionWrongUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnswerReportUiState())
    val uiState: StateFlow<AnswerReportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeAnswerReportUseCase(answerId).collect { report ->
                _uiState.update { it.copy(loading = false, answer = report.answer, wrongCorrections = report.wrongCorrections) }
            }
        }
    }

    fun onIntent(intent: AnswerReportIntent) {
        when (intent) {
            is AnswerReportIntent.ToggleCorrectionWrong -> viewModelScope.launch {
                markCorrectionWrongUseCase(answerId, intent.index, wrong = intent.index !in _uiState.value.wrongCorrections)
            }
        }
    }

    @AssistedFactory
    fun interface Factory {
        fun create(answerId: String): AnswerReportViewModel
    }
}
