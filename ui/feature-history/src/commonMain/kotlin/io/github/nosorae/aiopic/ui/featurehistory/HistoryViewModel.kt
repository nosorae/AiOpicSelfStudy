package io.github.nosorae.aiopic.ui.featurehistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HistoryUiState(
    val loading: Boolean = true,
    val sessions: List<HistorySession> = emptyList(),
    val weaknesses: List<HistoryWeakness> = emptyList(),
) {
    val empty: Boolean get() = !loading && sessions.isEmpty() && weaknesses.isEmpty()
}

/** 기록 탭(S13). 저장소 값을 그대로 보여 주는 화면이라 stateIn 으로 내보낸다. 회차 열기는 화면이 람다로 처리한다. */
@Inject
class HistoryViewModel(observeHistoryUseCase: ObserveHistoryUseCase) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> = observeHistoryUseCase()
        .map { HistoryUiState(loading = false, sessions = it.sessions, weaknesses = it.weaknesses) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
}
