package io.github.nosorae.aiopic.ui.featurereport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.github.nosorae.aiopic.engine.answer.model.AnalysisJob
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.ui.featurereport.flag.MarkCorrectionWrongUseCase
import io.github.nosorae.aiopic.ui.featurereport.flag.ObserveAnswerReportUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * [wrongCorrections] 는 "틀림"을 누른 교정의 `Feedback.corrections` 안 순서다(F8.1). [job] 은 답변의 분석 작업이다(#205).
 * [scriptPlayback] 과 [scriptCopied] 는 최종 스크립트 듣기와 복사다(#217).
 */
data class AnswerReportUiState(
    val loading: Boolean = true,
    val answer: Answer? = null,
    val wrongCorrections: Set<Int> = emptySet(),
    val names: ReportNames = ReportNames(),
    val job: AnalysisJob? = null,
    val scriptPlayback: ScriptPlayback = ScriptPlayback.Idle,
    val scriptCopied: Boolean = false,
)

sealed interface AnswerReportIntent {
    /** 교정 하나의 "틀림" 표시를 붙이거나 뗀다. */
    data class ToggleCorrectionWrong(val index: Int) : AnswerReportIntent

    /** 분석하지 못한 답변을 다시 보낸다(#203). */
    data object RetryAnalysis : AnswerReportIntent

    /** 최종 스크립트를 읽는다. 읽는 중이거나 음성을 받는 중이면 멈춘다(#217). */
    data object ListenScript : AnswerReportIntent

    /** 화면이 최종 스크립트를 클립보드에 넣었다(#217). 클립보드는 플랫폼 API 라 화면(Route)이 쓴다. */
    data object ScriptCopied : AnswerReportIntent
}

/** 답변 리포트 화면(S11). 분석 대기 중에 열어도 분석이 끝나면 바로 피드백으로 바뀐다. */
@AssistedInject
class AnswerReportViewModel(
    @Assisted private val answerId: String,
    observeAnswerReportUseCase: ObserveAnswerReportUseCase,
    private val markCorrectionWrongUseCase: MarkCorrectionWrongUseCase,
    private val retryAnswerAnalysisUseCase: RetryAnswerAnalysisUseCase,
    private val listenScriptUseCase: ListenScriptUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnswerReportUiState())
    val uiState: StateFlow<AnswerReportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeAnswerReportUseCase(answerId).collect { report ->
                _uiState.update { it.copy(loading = false, answer = report.answer, wrongCorrections = report.wrongCorrections, names = report.names, job = report.job) }
            }
        }
    }

    fun onIntent(intent: AnswerReportIntent) {
        when (intent) {
            is AnswerReportIntent.ToggleCorrectionWrong -> viewModelScope.launch {
                markCorrectionWrongUseCase(answerId, intent.index, wrong = intent.index !in _uiState.value.wrongCorrections)
            }

            AnswerReportIntent.RetryAnalysis -> viewModelScope.launch { retryAnswerAnalysisUseCase(answerId) }

            AnswerReportIntent.ListenScript -> listen()

            AnswerReportIntent.ScriptCopied -> _uiState.update { it.copy(scriptCopied = true) }
        }
    }

    // 화면을 떠나면 viewModelScope 가 끝나 읽기도 멈춘다.
    private var listening: Job? = null

    private fun listen() {
        listening?.let {
            it.cancel()
            listening = null
            _uiState.update { state -> state.copy(scriptPlayback = ScriptPlayback.Idle) }
            return
        }
        val script = (_uiState.value.answer?.analysis as? AnswerAnalysis.Done)?.feedback?.finalScript ?: return
        listening = viewModelScope.launch {
            listenScriptUseCase(script).collect { playback -> _uiState.update { it.copy(scriptPlayback = playback) } }
            listening = null
        }
    }

    @AssistedFactory
    fun interface Factory {
        fun create(answerId: String): AnswerReportViewModel
    }
}
