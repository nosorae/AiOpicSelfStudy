package io.github.nosorae.aiopic.ui.featureexam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.question.model.Survey
import io.github.nosorae.aiopic.engine.question.model.SurveyOption
import io.github.nosorae.aiopic.engine.question.model.SurveyQuestion
import io.github.nosorae.aiopic.engine.question.model.Topic
import io.github.nosorae.aiopic.engine.question.model.TopicGroup
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import io.github.nosorae.aiopic.engine.session.model.Recalibration
import io.github.nosorae.aiopic.ui.design.MontageButton
import io.github.nosorae.aiopic.ui.design.MontageButtonSize
import io.github.nosorae.aiopic.ui.design.MontageButtonType
import io.github.nosorae.aiopic.ui.design.MontageButtonVariant
import io.github.nosorae.aiopic.ui.design.MontageListCell
import io.github.nosorae.aiopic.ui.design.MontageScreen
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageTheme
import io.github.nosorae.aiopic.ui.design.MontageTopBar
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel 을 관측해 이동을 처리하는 바깥 겹. Play 를 누르면 마이크 권한부터 받는다.
 * 나간 뒤와 시험이 끝난 뒤 어디로 갈지는 화면을 등록한 쪽이 [onExit], [onFinish] 로 정한다.
 */
@Composable
fun ExamRoute(viewModel: ExamViewModel, onExit: () -> Unit, onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnExit by rememberUpdatedState(onExit)
    val currentOnFinish by rememberUpdatedState(onFinish)
    LaunchedEffect(state.navigation) {
        when (state.navigation) {
            ExamDestination.Exit -> currentOnExit()
            ExamDestination.Finished -> currentOnFinish()
            null -> return@LaunchedEffect
        }
        viewModel.onIntent(ExamIntent.NavigationHandled)
    }
    val requestMic = rememberMicPermission()
    val openSettings = rememberOpenAppSettings()
    val scope = rememberCoroutineScope()
    ExamScreen(
        state = state,
        onOpenSettings = openSettings,
        onIntent = { intent ->
            if (intent == ExamIntent.PlayQuestion) {
                scope.launch { viewModel.onIntent(if (requestMic()) intent else ExamIntent.MicDenied) }
            } else {
                viewModel.onIntent(intent)
            }
        },
        modifier = modifier,
    )
}

/** 상태만 받아 그리는 안쪽 겹. 회차의 지금 단계에 맞는 화면 하나를 그린다. 미리보기와 스크린샷 테스트가 이 함수를 쓴다. */
@Composable
fun ExamScreen(state: ExamUiState, onIntent: (ExamIntent) -> Unit, modifier: Modifier = Modifier, onOpenSettings: () -> Unit = {}) {
    val onExit = { onIntent(ExamIntent.ExitExam) }
    when (val step = state.step) {
        null -> StepScaffold(title = "", onExit = onExit, modifier = modifier) {}

        is ExamStep.Survey -> SurveyStep(state = state, onIntent = onIntent, onExit = onExit, modifier = modifier)

        ExamStep.LevelSelect -> ChoiceStep(
            title = stringResource(Res.string.exam_level_title),
            description = stringResource(Res.string.exam_level_description),
            choices = (1..state.levelCount).map { stringResource(Res.string.exam_level_item, it) },
            selected = state.selectedLevel?.minus(1),
            onSelect = { onIntent(ExamIntent.SelectLevel(it + 1)) },
            onSubmit = { onIntent(ExamIntent.SubmitLevel) },
            saving = state.saving,
            onExit = onExit,
            modifier = modifier,
        )

        ExamStep.Recalibrate -> ChoiceStep(
            title = stringResource(Res.string.exam_recalibrate_title),
            description = stringResource(Res.string.exam_recalibrate_description),
            choices = Recalibration.entries.map { stringResource(it.label) },
            selected = state.selectedRecalibration?.ordinal,
            onSelect = { onIntent(ExamIntent.SelectRecalibration(Recalibration.entries[it])) },
            onSubmit = { onIntent(ExamIntent.SubmitRecalibration) },
            saving = state.saving,
            onExit = onExit,
            modifier = modifier,
        )

        ExamStep.Check, is ExamStep.Ask -> QuestionStep(
            title = if (step is ExamStep.Ask) {
                stringResource(Res.string.exam_question_title, state.questionNumber, state.questionCount)
            } else {
                stringResource(Res.string.exam_check_title)
            },
            description = if (step is ExamStep.Check) stringResource(Res.string.exam_check_description) else null,
            state = state,
            onPlay = { onIntent(ExamIntent.PlayQuestion) },
            onReplay = { onIntent(ExamIntent.ReplayQuestion) },
            onNext = { onIntent(ExamIntent.SubmitAnswer) },
            onOpenSettings = onOpenSettings,
            onExit = onExit,
            modifier = modifier,
        )
    }
}

/** 모든 단계가 같은 틀을 쓴다. 시험 중에는 탭이 없고 앞으로만 가서 위쪽에 "나가기" 하나만 둔다. */
@Composable
private fun StepScaffold(
    title: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    MontageScreen(
        modifier = modifier,
        topBar = {
            MontageTopBar(title = title) { ExitButton(onExit) }
        },
        bottomBar = { Row(Modifier.padding(horizontal = Gutter, vertical = MontageSpacing.spacing12)) { bottomBar() } },
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = MontageSpacing.spacing16),
            verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing12),
        ) {
            content()
        }
    }
}

/** 서베이를 한 화면에 질문 하나씩 넘긴다. 마지막 묶음에서 다음을 누르면 서베이를 낸다(F2.2). */
@Composable
private fun SurveyStep(state: ExamUiState, onIntent: (ExamIntent) -> Unit, onExit: () -> Unit, modifier: Modifier = Modifier) {
    // 회차를 읽으며 서베이를 먼저 채우고 단계를 정해서 서베이 단계에서는 늘 있다.
    val survey = state.survey ?: return
    MontageScreen(
        modifier = modifier,
        topBar = { MontageTopBar(title = stringResource(Res.string.exam_survey_title)) { ExitButton(onExit) } },
        bottomBar = {
            SurveyButtons(
                state = survey,
                submitText = stringResource(Res.string.exam_next_step),
                saving = state.saving,
                onPrevious = { onIntent(ExamIntent.PreviousSurveyScreen) },
                onNext = { onIntent(ExamIntent.NextSurveyScreen) },
                onSubmit = { onIntent(ExamIntent.SubmitSurvey) },
                modifier = Modifier.padding(horizontal = Gutter, vertical = MontageSpacing.spacing12),
            )
        },
    ) {
        SurveyFlow(survey, onChoose = { onIntent(ExamIntent.ChooseSurveyOption(it)) }, onToggle = { onIntent(ExamIntent.ToggleTopic(it)) })
    }
}

/** 하나를 골라 다음으로 가는 단계(수준 선택, 재조정). [selected] 는 [choices] 의 위치다. */
@Composable
private fun ChoiceStep(
    title: String,
    description: String,
    choices: List<String>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    onSubmit: () -> Unit,
    saving: Boolean,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StepScaffold(
        title = title,
        onExit = onExit,
        modifier = modifier,
        bottomBar = { NextButton(enabled = selected != null, loading = saving, onClick = onSubmit) },
    ) {
        Description(description)
        Column {
            choices.forEachIndexed { index, choice ->
                MontageListCell(title = choice, selected = index == selected, onClick = { onSelect(index) }, modifier = Modifier.testTag("exam-choice-$index"))
            }
        }
    }
}

/** 질문 글자는 보이지 않는다(F2.3). 남은 시간, 지금 하는 일, 문항 규칙만 보인다. */
@Composable
private fun QuestionStep(
    title: String,
    description: String?,
    state: ExamUiState,
    onPlay: () -> Unit,
    onReplay: () -> Unit,
    onNext: () -> Unit,
    onOpenSettings: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StepScaffold(
        title = title,
        onExit = onExit,
        modifier = modifier,
        bottomBar = {
            if (state.phase == QuestionPhase.Recording) {
                Row(horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8)) {
                    MontageButton(
                        text = stringResource(Res.string.exam_replay),
                        onClick = onReplay,
                        enabled = state.canReplay,
                        type = MontageButtonType.Assistive,
                        modifier = Modifier.weight(1f).testTag("exam-replay"),
                    )
                    MontageButton(
                        text = stringResource(Res.string.exam_next),
                        onClick = onNext,
                        enabled = state.canNext,
                        loading = state.saving,
                        modifier = Modifier.weight(1f).testTag("exam-next"),
                    )
                }
            } else {
                // 질문을 다 읽어야 녹음이 시작돼서 그동안 Play 가 진행 표시를 보인다. 모델 질문을 기다리면 위에 불러오는 중 안내가 뜬다(F6.1).
                MontageButton(
                    text = stringResource(if (state.error in RetryErrors) Res.string.exam_retry else Res.string.exam_play),
                    onClick = onPlay,
                    loading = state.phase == QuestionPhase.Speaking,
                    modifier = Modifier.fillMaxWidth().testTag("exam-play"),
                )
            }
        },
    ) {
        state.remaining?.let {
            Text(
                text = stringResource(Res.string.exam_remaining, it.clockText()),
                style = MontageTheme.typography.label1.bold,
                color = MontageTheme.colors.foregroundNeutralSecondary,
                modifier = Modifier.padding(horizontal = Gutter),
            )
        }
        description?.let { Description(it) }
        val notice = state.questionNotice
        if (notice is QuestionNotice.Making) {
            val making = if (notice.recalibrated) Res.string.exam_question_making_difficulty else Res.string.exam_question_making_topics
            QuestionLoading(stringResource(making, notice.first, notice.last), modifier = Modifier.padding(horizontal = Gutter))
        } else if (notice == QuestionNotice.Slow) {
            QuestionLoading(stringResource(Res.string.exam_question_slow), modifier = Modifier.padding(horizontal = Gutter))
        } else {
            PhaseText(state)
        }
        if (state.questionNotice == QuestionNotice.BuiltIn) Description(stringResource(Res.string.exam_question_built_in))
        state.error?.let {
            Text(
                text = stringResource(it.message),
                style = MontageTheme.typography.body2.medium,
                color = MontageTheme.colors.foregroundNegativePrimary,
                modifier = Modifier.padding(horizontal = Gutter).testTag("exam-error"),
            )
        }
        if (state.error == ExamError.MicDenied) {
            MontageButton(
                text = stringResource(Res.string.exam_open_settings),
                onClick = onOpenSettings,
                variant = MontageButtonVariant.Outlined,
                type = MontageButtonType.Assistive,
                size = MontageButtonSize.Medium,
                modifier = Modifier.padding(horizontal = Gutter).testTag("exam-open-settings"),
            )
        }
        Description(stringResource(Res.string.exam_rules))
    }
}

/** 문항의 지금 진행(대기, 질문 듣기, 녹음). */
@Composable
private fun PhaseText(state: ExamUiState) {
    Text(
        text = when (state.phase) {
            QuestionPhase.Ready -> stringResource(Res.string.exam_ready)

            QuestionPhase.Speaking -> stringResource(Res.string.exam_speaking)

            QuestionPhase.Recording -> if (state.answerTimer) {
                stringResource(Res.string.exam_recording_timer, state.recorded.clockText())
            } else {
                stringResource(Res.string.exam_recording)
            }
        },
        style = MontageTheme.typography.heading2.bold,
        color = if (state.phase == QuestionPhase.Recording) MontageTheme.colors.foregroundBrandPrimary else MontageTheme.colors.foregroundNeutralPrimary,
        modifier = Modifier.padding(horizontal = Gutter).testTag("exam-phase"),
    )
}

@Composable
private fun Description(text: String) {
    Text(
        text = text,
        style = MontageTheme.typography.body1.regular,
        color = MontageTheme.colors.foregroundNeutralSecondary,
        modifier = Modifier.padding(horizontal = Gutter),
    )
}

/** 시험 중에는 탭이 없고 앞으로만 가서 위쪽에 "나가기" 하나만 둔다. */
@Composable
private fun ExitButton(onExit: () -> Unit) {
    MontageButton(
        text = stringResource(Res.string.exam_exit),
        onClick = onExit,
        variant = MontageButtonVariant.Text,
        type = MontageButtonType.Assistive,
        size = MontageButtonSize.Medium,
    )
}

/** 저장하는 동안([loading]) 진행 표시를 보인다. 그동안 다른 입력은 ViewModel 이 받지 않는다. */
@Composable
private fun NextButton(enabled: Boolean, loading: Boolean, onClick: () -> Unit) {
    MontageButton(
        text = stringResource(Res.string.exam_next_step),
        onClick = onClick,
        enabled = enabled,
        loading = loading,
        modifier = Modifier.fillMaxWidth().testTag("exam-next-step"),
    )
}

private val Gutter = MontageSpacing.spacing20

/** 분:초. 40분 타이머와 녹음 시간에 쓴다. */
private fun Duration.clockText(): String {
    val seconds = inWholeSeconds.coerceAtLeast(0)
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

private val Recalibration.label: StringResource
    get() = when (this) {
        Recalibration.Easier -> Res.string.exam_recalibrate_easier
        Recalibration.Same -> Res.string.exam_recalibrate_same
        Recalibration.Harder -> Res.string.exam_recalibrate_harder
    }

/** 같은 문항을 다시 녹음하면 풀리는 오류. 이때 Play 가 "다시 녹음" 이 된다. */
private val RetryErrors = setOf(ExamError.MicUnavailable, ExamError.RecordingFailed)

private val ExamError.message: StringResource
    get() = when (this) {
        ExamError.MicDenied -> Res.string.exam_error_mic_denied
        ExamError.MicUnavailable -> Res.string.exam_error_mic
        ExamError.RecordingFailed -> Res.string.exam_error_recording
        ExamError.SpeechFailed -> Res.string.exam_error_speech
        ExamError.QuestionUnavailable -> Res.string.exam_error_question
    }

/** 미리보기와 스크린샷 테스트가 함께 쓰는 고정 상태. 화면마다 하나, 문항 화면은 진행마다 하나다. */
internal object ExamSamples {
    private val ask = ExamStep.Ask(QuestionType("routine"))

    private val surveySample = Survey(
        questions = listOf(
            SurveyQuestion(
                "work",
                "지금 하는 일은 어느 쪽에 가깝나요?",
                listOf(SurveyOption("company", "회사나 사업"), SurveyOption("home-office", "집에서 하는 일이나 사업"), SurveyOption("teacher", "가르치는 일"), SurveyOption("none", "일해 본 적 없어요")),
            ),
        ),
        groups = listOf(
            TopicGroup("leisure", "쉬는 날에는 주로 무엇을 하나요?", 2, listOf("영화보기", "클럽/나이트클럽 가기", "공연보기", "콘서트보기", "박물관가기", "공원가기", "캠핑하기", "해변가기", "스포츠 관람", "주거 개선").map { Topic(it, it) }),
            TopicGroup("trips", "휴가나 출장은 어떻게 보내나요?", 1, listOf("국내출장", "해외출장", "집에서 보내는 휴가", "국내 여행", "해외 여행").map { Topic(it, it) }),
        ),
        minTopics = 12,
    )

    val survey = ExamUiState(step = ExamStep.Survey(minTopics = 12), survey = SurveyState(surveySample, choices = mapOf("work" to "company")))
    val surveyGroup = survey.copy(survey = survey.survey!!.copy(screen = 1, topics = setOf("영화보기", "공연보기", "스포츠 관람")))
    val surveyShort = survey.copy(survey = survey.survey!!.copy(screen = 2, topics = setOf("영화보기", "공연보기", "국내 여행")))
    val surveyReady = survey.copy(survey = survey.survey!!.copy(screen = 2, topics = surveySample.groups[0].topics.map { it.id }.toSet() + "국내 여행" + "해외 여행"))
    val level = ExamUiState(step = ExamStep.LevelSelect, levelCount = 6, selectedLevel = 4)
    val check = ExamUiState(step = ExamStep.Check)
    val questionReady = ExamUiState(step = ask, questionNumber = 3, questionCount = 15, remaining = 36.minutes + 12.seconds)
    val questionSpeaking = questionReady.copy(phase = QuestionPhase.Speaking)
    val questionLoading = questionSpeaking.copy(questionNumber = 2, questionNotice = QuestionNotice.Making(2, 7, recalibrated = false))
    val questionLoadingRecalibrated = questionSpeaking.copy(questionNumber = 8, questionNotice = QuestionNotice.Making(8, 15, recalibrated = true))
    val questionSlow = questionSpeaking.copy(questionNotice = QuestionNotice.Slow)
    val questionReplayable = questionReady.copy(phase = QuestionPhase.Recording, recorded = 3.seconds)
    val questionBuiltIn = questionReplayable.copy(questionNotice = QuestionNotice.BuiltIn)
    val questionNextable = questionReady.copy(phase = QuestionPhase.Recording, recorded = 12.seconds, replayUsed = true)
    val questionSaving = questionNextable.copy(saving = true)
    val surveySaving = surveyReady.copy(saving = true)
    val questionTimer = questionReplayable.copy(answerTimer = true)
    val micError = check.copy(error = ExamError.MicDenied)
    val recordingError = questionReady.copy(error = ExamError.RecordingFailed)
    val recalibrate = ExamUiState(step = ExamStep.Recalibrate, remaining = 21.minutes, selectedRecalibration = Recalibration.Same)
}

@Preview
@Composable
private fun ExamScreenPreview() {
    MontageTheme { ExamScreen(state = ExamSamples.questionReplayable, onIntent = {}) }
}
