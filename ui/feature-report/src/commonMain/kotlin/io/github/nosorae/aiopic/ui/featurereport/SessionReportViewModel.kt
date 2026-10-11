package io.github.nosorae.aiopic.ui.featurereport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** [gradeUp] 은 바로 앞 회차보다 오른 등급을 처음 보는 중이다(#198). 화면이 큰 축하를 한 번 한다. */
data class SessionReportUiState(val loading: Boolean = true, val report: SessionReport? = null, val gradeUp: Boolean = false)

sealed interface SessionReportIntent {
    /** 회차에서 분석하지 못한 답변과 등급을 다시 보낸다(#203). */
    data object RetryAnalysis : SessionReportIntent
}

/**
 * 회차 리포트 화면(S10). [sessionId] 는 화면 인자다. 답변 분석은 뒤에서 끝나므로 저장소를 계속 구독한다.
 * 답변 열기는 단순 이동이라 화면이 람다로 처리한다. 바로 앞 회차보다 등급이 오른 것을 처음 보면 큰 축하를 한다(#198).
 */
@AssistedInject
class SessionReportViewModel(
    @Assisted private val sessionId: String,
    observeSessionReportUseCase: ObserveSessionReportUseCase,
    private val retrySessionAnalysisUseCase: RetrySessionAnalysisUseCase,
    private val celebrateOnceUseCase: CelebrateOnceUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SessionReportUiState())
    val uiState: StateFlow<SessionReportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            var asked = false
            observeSessionReportUseCase(sessionId).collect { report ->
                _uiState.update { it.copy(loading = false, report = report) }
                // 오른 등급은 이 화면이 떠 있는 동안 계속 축하 중이다. 본 표시를 남긴 뒤에 다시 물으면 false 가 와서 축하가 중간에 사라진다.
                if (report?.gradeUp == true && !asked) {
                    asked = true
                    if (celebrateOnceUseCase("grade-up:$sessionId")) _uiState.update { it.copy(gradeUp = true) }
                }
            }
        }
    }

    fun onIntent(intent: SessionReportIntent) {
        when (intent) {
            SessionReportIntent.RetryAnalysis -> viewModelScope.launch { retrySessionAnalysisUseCase(sessionId) }
        }
    }

    @AssistedFactory
    fun interface Factory {
        fun create(sessionId: String): SessionReportViewModel
    }
}
