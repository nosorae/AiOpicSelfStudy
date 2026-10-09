package io.github.nosorae.aiopic.ui.featuresettings.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** [copied] 는 복사한 뒤 보여 주는 "복사했습니다" 안내다. */
data class FlagExportUiState(val loading: Boolean = true, val count: Int = 0, val json: String = "", val copied: Boolean = false)

sealed interface FlagExportIntent {
    /** 화면이 클립보드에 JSON 을 넣은 뒤 알린다. */
    data object JsonCopied : FlagExportIntent
}

/**
 * 틀림 표시 내보내기(S14, F8.3). 리포트에서 표시가 바뀌면 바로 반영한다.
 * 클립보드 쓰기는 플랫폼 API 라 화면(Route)이 하고, 쓴 뒤 [FlagExportIntent.JsonCopied] 로 안내 상태만 바꾼다.
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

    fun onIntent(intent: FlagExportIntent) {
        when (intent) {
            FlagExportIntent.JsonCopied -> _uiState.update { it.copy(copied = true) }
        }
    }
}
