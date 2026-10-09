package io.github.nosorae.aiopic.ui.featuresettings.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FlagExportUiState(val loading: Boolean = true, val count: Int = 0, val json: String = "")

/**
 * 틀림 표시 내보내기(S14, F8.3). 리포트에서 표시가 바뀌면 바로 반영한다.
 * 복사는 화면이 클립보드에 바로 하는 일이라 Intent 가 없다.
 */
@Inject
class FlagExportViewModel(observeFlagExportUseCase: ObserveFlagExportUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(FlagExportUiState())
    val uiState: StateFlow<FlagExportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeFlagExportUseCase().collect { export -> _uiState.update { it.copy(loading = false, count = export.count, json = export.json) } }
        }
    }
}
