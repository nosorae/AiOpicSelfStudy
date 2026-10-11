package io.github.nosorae.aiopic.ui.featurepractice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 연습 탭. Play 를 누르기 전에 다음 문항을 어느 약한 유형에서 낼지 보인다(F4.1).
 * 유형은 [PickPracticeQuestionUseCase] 가 점수 순으로 정해서, 열리는 연습 화면이 다시 골라도 같은 유형이 나온다.
 */
// ponytail: 탭이 처음 그려질 때 한 번 읽는다. 연습을 마치고 돌아온 사이 분석이 끝나 점수가 바뀌어도 탭을 다시 열 때까지는 앞 값이다.
@Inject
class PracticeTabViewModel(pickPracticeQuestionUseCase: PickPracticeQuestionUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(PracticeTabUiState())
    val uiState: StateFlow<PracticeTabUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val weakness = pickPracticeQuestionUseCase()?.weakness
            _uiState.update { it.copy(weakness = weakness, loaded = true) }
        }
    }
}

/** [weakness] 는 다음 문항을 낼 약한 유형이고, 분석한 답변이 없어 모든 유형에서 고르면 null 이다. [loaded] 전에는 둘을 가를 수 없다. */
data class PracticeTabUiState(val weakness: PracticeWeakness? = null, val loaded: Boolean = false)
