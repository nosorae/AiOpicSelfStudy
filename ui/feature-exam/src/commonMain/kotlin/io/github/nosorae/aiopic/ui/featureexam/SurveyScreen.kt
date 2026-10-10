package io.github.nosorae.aiopic.ui.featureexam

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.nosorae.aiopic.ui.design.MontageScreen
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageTopBar
import org.jetbrains.compose.resources.stringResource

/** 설정에서 여는 서베이의 바깥 겹. 저장을 마치면 [onDone] 으로 설정에 돌아간다. 저장하지 않고 나가면 바꾼 답은 버린다. */
@Composable
fun SurveyRoute(viewModel: SurveyViewModel, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(state.done) {
        if (!state.done) return@LaunchedEffect
        currentOnDone()
        viewModel.onIntent(SurveyIntent.NavigationHandled)
    }
    SurveyScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** 모의시험 서베이와 같은 흐름이다. 마지막 묶음의 버튼만 "저장하기" 다. */
@Composable
fun SurveyScreen(state: SurveyUiState, onIntent: (SurveyIntent) -> Unit, modifier: Modifier = Modifier) {
    MontageScreen(
        modifier = modifier,
        topBar = { MontageTopBar(title = stringResource(Res.string.exam_survey_title)) },
        bottomBar = {
            state.survey?.let { survey ->
                SurveyButtons(
                    state = survey,
                    submitText = stringResource(Res.string.survey_save),
                    saving = state.saving,
                    onPrevious = { onIntent(SurveyIntent.Previous) },
                    onNext = { onIntent(SurveyIntent.Next) },
                    onSubmit = { onIntent(SurveyIntent.Submit) },
                    modifier = Modifier.padding(horizontal = MontageSpacing.spacing20, vertical = MontageSpacing.spacing12),
                )
            }
        },
    ) {
        state.survey?.let { survey ->
            SurveyFlow(survey, onChoose = { onIntent(SurveyIntent.ChooseOption(it)) }, onToggle = { onIntent(SurveyIntent.ToggleTopic(it)) })
        }
    }
}
