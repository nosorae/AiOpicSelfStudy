package io.github.nosorae.aiopic.ui.featureexam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 설정에서 여는 서베이. 모의시험 서베이와 같은 흐름으로 저장한 답을 보고 바꾼다.
 * 낸 답은 다음 모의시험 서베이를 미리 채우고 한 문항 연습 주제에 쓴다.
 */
@Inject
class SurveyViewModel(
    private val loadSurveyAnswersUseCase: LoadSurveyAnswersUseCase,
    private val saveSurveyAnswersUseCase: SaveSurveyAnswersUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SurveyUiState())
    val uiState: StateFlow<SurveyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val survey = loadSurveyAnswersUseCase()
            _uiState.update { it.copy(survey = survey, loaded = survey) }
        }
    }

    fun onIntent(intent: SurveyIntent) {
        val state = _uiState.value
        val survey = state.survey ?: return
        if (state.saving && intent != SurveyIntent.NavigationHandled) return
        when (intent) {
            is SurveyIntent.ChooseOption -> _uiState.update { it.copy(survey = survey.choose(intent.option)) }

            is SurveyIntent.ToggleTopic -> _uiState.update { it.copy(survey = survey.toggle(intent.topic)) }

            SurveyIntent.Next -> _uiState.update { it.copy(survey = survey.next()) }

            SurveyIntent.Previous -> _uiState.update { it.copy(survey = survey.back()) }

            SurveyIntent.Submit -> if (survey.canSubmit) {
                _uiState.update { it.copy(saving = true) }
                viewModelScope.launch {
                    saveSurveyAnswersUseCase(survey.answers())
                    _uiState.update { it.copy(saving = false, done = true) }
                }
            }

            SurveyIntent.NavigationHandled -> _uiState.update { it.copy(done = false) }
        }
    }
}

data class SurveyUiState(
    /** 저장한 답을 읽기 전에는 null 이다. */
    val survey: SurveyState? = null,
    val saving: Boolean = false,
    /** 저장을 마쳤다. 화면이 설정으로 돌아간다. */
    val done: Boolean = false,
    /** 처음 읽은 저장한 답. [edited] 를 가른다. */
    val loaded: SurveyState? = null,
) {
    /** 저장한 답과 다른 답이 있다. 저장하지 않고 나가면 버려지므로 나가기 전에 묻는다. */
    val edited: Boolean get() = survey != null && survey.answers() != loaded?.answers()
}

sealed interface SurveyIntent {
    data class ChooseOption(val option: String) : SurveyIntent

    data class ToggleTopic(val topic: String) : SurveyIntent

    data object Next : SurveyIntent

    data object Previous : SurveyIntent

    data object Submit : SurveyIntent

    data object NavigationHandled : SurveyIntent
}
