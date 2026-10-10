package io.github.nosorae.aiopic.ui.featureexam

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.nosorae.aiopic.ui.design.MontageConfirmDialog
import io.github.nosorae.aiopic.ui.design.MontageIconButton
import io.github.nosorae.aiopic.ui.design.MontageScreen
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageTopBar
import io.github.nosorae.aiopic.ui.design.icon_normal_close
import org.jetbrains.compose.resources.stringResource
import io.github.nosorae.aiopic.ui.design.Res as DesignRes

/** 설정에서 여는 서베이의 바깥 겹. 저장을 마치거나 X 로 나가면 [onDone] 으로 설정에 돌아간다. 저장하지 않고 나가면 바꾼 답은 버린다. */
@Composable
fun SurveyRoute(viewModel: SurveyViewModel, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(state.done) {
        if (!state.done) return@LaunchedEffect
        currentOnDone()
        viewModel.onIntent(SurveyIntent.NavigationHandled)
    }
    SurveyScreen(state = state, onIntent = viewModel::onIntent, onClose = currentOnDone, modifier = modifier)
}

/**
 * 모의시험 서베이와 같은 흐름이다. 마지막 묶음의 버튼만 "저장하기" 다.
 * 위쪽 X 와 시스템 뒤로가기는 바꾼 답이 있을 때만 버려진다고 묻고, 없으면 바로 [onClose] 한다.
 */
@Composable
fun SurveyScreen(state: SurveyUiState, onIntent: (SurveyIntent) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var confirmingExit by rememberSaveable { mutableStateOf(false) }
    val close = { if (state.edited) confirmingExit = true else onClose() }
    // ponytail: 실험 API 다. 바뀌면 navigationevent 의 NavigationBackHandler 로 옮긴다.
    @OptIn(ExperimentalComposeUiApi::class)
    BackHandler(enabled = state.edited) { confirmingExit = true }
    if (confirmingExit) {
        MontageConfirmDialog(
            title = stringResource(Res.string.survey_exit_title),
            message = stringResource(Res.string.survey_exit_message),
            confirm = stringResource(Res.string.exam_exit),
            cancel = stringResource(Res.string.exam_exit_cancel),
            onConfirm = {
                confirmingExit = false
                onClose()
            },
            onCancel = { confirmingExit = false },
        )
    }
    MontageScreen(
        modifier = modifier,
        topBar = {
            MontageTopBar(title = stringResource(Res.string.exam_survey_title)) {
                MontageIconButton(DesignRes.drawable.icon_normal_close, stringResource(Res.string.exam_exit), close, Modifier.testTag("survey-exit"))
            }
        },
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

/** 스크린샷과 화면 테스트가 쓰는 고정 상태. 서베이는 모의시험 서베이 견본을 그대로 쓴다. */
internal object SurveySamples {
    private val survey = ExamSamples.survey.survey!!
    val unchanged = SurveyUiState(survey = survey, loaded = survey)
    val edited = unchanged.copy(survey = survey.copy(choices = mapOf("work" to "teacher")))
}
