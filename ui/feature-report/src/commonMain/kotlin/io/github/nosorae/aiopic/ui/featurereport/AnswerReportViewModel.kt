package io.github.nosorae.aiopic.ui.featurereport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.github.nosorae.aiopic.engine.answer.model.Answer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnswerReportUiState(val loading: Boolean = true, val answer: Answer? = null)

/** 답변 리포트 화면(S11). 분석 대기 중에 열어도 분석이 끝나면 바로 피드백으로 바뀐다. */
@AssistedInject
class AnswerReportViewModel(
    @Assisted answerId: String,
    observeAnswerUseCase: ObserveAnswerUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnswerReportUiState())
    val uiState: StateFlow<AnswerReportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeAnswerUseCase(answerId).collect { answer -> _uiState.update { it.copy(loading = false, answer = answer) } }
        }
    }

    @AssistedFactory
    fun interface Factory {
        fun create(answerId: String): AnswerReportViewModel
    }
}
