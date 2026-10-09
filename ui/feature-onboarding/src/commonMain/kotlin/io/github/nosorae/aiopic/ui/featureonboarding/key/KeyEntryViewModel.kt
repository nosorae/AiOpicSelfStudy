package io.github.nosorae.aiopic.ui.featureonboarding.key

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.github.nosorae.aiopic.ui.featureonboarding.DomainResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 키 입력 화면(S1). [provider] 는 화면 인자다. 설정의 제공사별 키 관리에서 들어오면 그 제공사가 정해져 있고,
 * 처음 설정에서는 null 이라 사용자가 고른다.
 */
@AssistedInject
class KeyEntryViewModel(
    @Assisted provider: KeyProvider?,
    private val saveKeyUseCase: SaveKeyUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(KeyEntryUiState(selectedProvider = provider))
    val uiState: StateFlow<KeyEntryUiState> = _uiState.asStateFlow()

    fun onIntent(intent: KeyEntryIntent) {
        when (intent) {
            is KeyEntryIntent.SelectProvider -> _uiState.update { it.copy(selectedProvider = intent.provider) }
            is KeyEntryIntent.ChangeKey -> _uiState.update { it.copy(key = intent.key, error = null) }
            KeyEntryIntent.SubmitKey -> submit()
            KeyEntryIntent.NavigationHandled -> _uiState.update { it.copy(navigation = null) }
        }
    }

    private fun submit() {
        val state = _uiState.value
        val provider = state.selectedProvider ?: return
        if (!state.canSubmit) return
        _uiState.update { it.copy(checking = true, error = null) }
        viewModelScope.launch {
            val result = saveKeyUseCase(provider, state.key)
            _uiState.update {
                when (result) {
                    is DomainResult.Success -> it.copy(checking = false, navigation = KeyEntryDestination.KeySaved)
                    is DomainResult.Failure -> it.copy(checking = false, error = result.error)
                }
            }
        }
    }

    @AssistedFactory
    fun interface Factory {
        fun create(provider: KeyProvider?): KeyEntryViewModel
    }
}
