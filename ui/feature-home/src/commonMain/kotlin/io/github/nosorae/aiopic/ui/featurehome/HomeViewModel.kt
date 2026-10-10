package io.github.nosorae.aiopic.ui.featurehome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 홈은 기록 화면이 아니라서 저장소 Flow 를 init 에서 구독해 update 로 옮긴다(ADR 0005). */
@Inject
class HomeViewModel(observeHomeUseCase: ObserveHomeUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeHomeUseCase().collect { home ->
                _uiState.update { it.copy(resumeSessionId = home.resumeSessionId, pastSessions = home.pastSessions) }
            }
        }
    }
}
