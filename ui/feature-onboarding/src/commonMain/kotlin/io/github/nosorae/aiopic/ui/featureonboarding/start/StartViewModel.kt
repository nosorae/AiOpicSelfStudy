package io.github.nosorae.aiopic.ui.featureonboarding.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StartUiState(val destination: StartDestination? = null) {
    /** Android `setKeepOnScreenCondition` 과 iOS 실행 화면이 이 값이 false 가 될 때까지 기다린다. */
    val keepSplash: Boolean get() = destination == null
}

/**
 * 시작 분기(S0). 그리는 화면이 없고 앱 층이 [StartUiState.destination] 으로 첫 백스택을 만든다.
 * ViewModel 에 두어 스플래시가 떠 있는 사이 화면이 다시 만들어져도 분기를 한 번만 한다.
 */
@Inject
class StartViewModel(decideStartUseCase: DecideStartUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(StartUiState())
    val uiState: StateFlow<StartUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val destination = decideStartUseCase()
            _uiState.update { it.copy(destination = destination) }
        }
    }
}
