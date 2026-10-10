package io.github.nosorae.aiopic.ui.featureexam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import io.github.nosorae.aiopic.engine.session.model.Recalibration
import io.github.nosorae.aiopic.ui.design.MontageButton
import io.github.nosorae.aiopic.ui.design.MontageButtonSize
import io.github.nosorae.aiopic.ui.design.MontageButtonType
import io.github.nosorae.aiopic.ui.design.MontageButtonVariant
import io.github.nosorae.aiopic.ui.design.MontageChip
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

        is ExamStep.Survey -> SurveyStep(
            parts = state.surveyParts,
            answers = state.surveyAnswers,
            onAnswer = { part, option -> onIntent(ExamIntent.SelectSurveyAnswer(part, option)) },
            minTopics = step.minTopics,
            topics = state.topics,
            selected = state.selectedTopics,
            canSubmit = state.canSubmitSurvey,
            saving = state.saving,
            onToggle = { onIntent(ExamIntent.ToggleTopic(it)) },
            onSubmit = { onIntent(ExamIntent.SubmitSurvey) },
            onExit = onExit,
            modifier = modifier,
        )

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
            MontageTopBar(title = title) {
                MontageButton(
                    text = stringResource(Res.string.exam_exit),
                    onClick = onExit,
                    variant = MontageButtonVariant.Text,
                    type = MontageButtonType.Assistive,
                    size = MontageButtonSize.Medium,
                )
            }
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

/** Part 질문을 차례로 두고 마지막 Part 에서 관심 주제를 고른다. 다음으로 가는 조건은 마지막 Part 의 주제 수 하나다(F2.2). */
@Composable
private fun SurveyStep(
    parts: List<SurveyPart>,
    answers: Map<Int, Int>,
    onAnswer: (part: Int, option: Int) -> Unit,
    minTopics: Int,
    topics: List<String>,
    selected: Set<String>,
    canSubmit: Boolean,
    saving: Boolean,
    onToggle: (String) -> Unit,
    onSubmit: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StepScaffold(
        title = stringResource(Res.string.exam_survey_title),
        onExit = onExit,
        modifier = modifier,
        bottomBar = { NextButton(enabled = canSubmit, loading = saving, onClick = onSubmit) },
    ) {
        val partCount = parts.size + 1
        parts.forEachIndexed { part, survey ->
            PartTitle(part + 1, partCount)
            Description(survey.question)
            Column {
                survey.options.forEachIndexed { option, text ->
                    MontageListCell(title = text, selected = answers[part] == option, onClick = { onAnswer(part, option) }, modifier = Modifier.testTag("survey-$part-$option"))
                }
            }
        }
        PartTitle(partCount, partCount)
        Description(stringResource(Res.string.exam_survey_description, minTopics))
        Text(
            text = stringResource(Res.string.exam_survey_count, selected.size),
            style = MontageTheme.typography.label1.bold,
            color = if (canSubmit) MontageTheme.colors.foregroundBrandPrimary else MontageTheme.colors.foregroundNeutralSecondary,
            modifier = Modifier.padding(horizontal = Gutter),
        )
        FlowRow(
            modifier = Modifier.padding(horizontal = Gutter),
            horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8),
            verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8),
        ) {
            topics.forEachIndexed { index, topic ->
                MontageChip(text = topic, onClick = { onToggle(topic) }, active = topic in selected, modifier = Modifier.testTag("survey-topic-$index"))
            }
        }
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
                // 첫 문항은 이때 질문을 받아 오고(F6.1) 질문을 다 읽어야 녹음이 시작돼서 그동안 Play 가 진행 표시를 보인다.
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

@Composable
private fun Description(text: String) {
    Text(
        text = text,
        style = MontageTheme.typography.body1.regular,
        color = MontageTheme.colors.foregroundNeutralSecondary,
        modifier = Modifier.padding(horizontal = Gutter),
    )
}

@Composable
private fun PartTitle(part: Int, count: Int) {
    Text(
        text = stringResource(Res.string.exam_survey_part, part, count),
        style = MontageTheme.typography.label1.bold,
        color = MontageTheme.colors.foregroundNeutralSecondary,
        modifier = Modifier.padding(start = Gutter, end = Gutter, top = MontageSpacing.spacing12).semantics { heading() },
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
    private val topics = listOf("영화 보기", "공원 가기", "요리하기", "음악 감상", "해변 가기", "캠핑", "조깅", "헬스", "여행(국내)", "여행(해외)", "카페 가기", "독서", "게임", "쇼핑", "콘서트 보기")
    private val ask = ExamStep.Ask(QuestionType("routine"))

    private val surveyParts = listOf(
        SurveyPart("현재 귀하는 어느 분야에 종사하고 계십니까?", listOf("사업, 회사", "재택근무, 재택사업", "교사, 교육자", "일 경험 없음")),
        SurveyPart("현재 귀하는 학생이십니까?", listOf("예", "아니요")),
        SurveyPart(
            "현재 귀하는 어디에 살고 계십니까?",
            listOf("개인주택이나 아파트에 홀로 거주", "친구나 룸메이트와 함께 주택이나 아파트에 거주", "가족과 함께 주택이나 아파트에 거주", "학교 기숙사", "군대 막사"),
        ),
    )

    val survey = ExamUiState(
        step = ExamStep.Survey(minTopics = 12),
        surveyParts = surveyParts,
        surveyAnswers = mapOf(0 to 0, 1 to 1, 2 to 2),
        topics = topics,
        selectedTopics = topics.take(5).toSet(),
    )
    val surveyReady = survey.copy(selectedTopics = topics.take(12).toSet())
    val level = ExamUiState(step = ExamStep.LevelSelect, levelCount = 6, selectedLevel = 4)
    val check = ExamUiState(step = ExamStep.Check)
    val questionReady = ExamUiState(step = ask, questionNumber = 3, questionCount = 15, remaining = 36.minutes + 12.seconds)
    val questionSpeaking = questionReady.copy(phase = QuestionPhase.Speaking)
    val questionReplayable = questionReady.copy(phase = QuestionPhase.Recording, recorded = 3.seconds)
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
