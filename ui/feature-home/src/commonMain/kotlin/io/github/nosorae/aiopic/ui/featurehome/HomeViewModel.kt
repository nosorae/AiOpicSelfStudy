package io.github.nosorae.aiopic.ui.featurehome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** 홈은 저장된 회차를 그대로 보여 주기만 하고 사용자 동작이 상태를 바꾸지 않아서 stateIn 으로 내보낸다(architecture 2절). */
@Inject
class HomeViewModel(observeHomeUseCase: ObserveHomeUseCase) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = observeHomeUseCase().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
