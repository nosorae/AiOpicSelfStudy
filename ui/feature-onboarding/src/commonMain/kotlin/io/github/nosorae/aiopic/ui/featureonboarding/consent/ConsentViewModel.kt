package io.github.nosorae.aiopic.ui.featureonboarding.consent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 음성 전송 동의 화면(S2). 동의는 [ConsentIntent.GiveConsent] 하나로만 저장된다. */
@Inject
class ConsentViewModel(private val giveVoiceConsentUseCase: GiveVoiceConsentUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(ConsentUiState())
    val uiState: StateFlow<ConsentUiState> = _uiState.asStateFlow()

    fun onIntent(intent: ConsentIntent) {
        when (intent) {
            ConsentIntent.GiveConsent -> viewModelScope.launch {
                giveVoiceConsentUseCase()
                _uiState.update { it.copy(navigation = ConsentDestination.ConsentGiven) }
            }

            ConsentIntent.NavigationHandled -> _uiState.update { it.copy(navigation = null) }
        }
    }
}
