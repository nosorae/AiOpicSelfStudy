package io.github.nosorae.aiopic.ui.featurepractice

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.ui.design.MontageButton
import io.github.nosorae.aiopic.ui.design.MontageButtonSize
import io.github.nosorae.aiopic.ui.design.MontageButtonType
import io.github.nosorae.aiopic.ui.design.MontageButtonVariant
import io.github.nosorae.aiopic.ui.design.MontageScreen
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageTheme
import io.github.nosorae.aiopic.ui.design.MontageTopBar
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel 을 관측해 이동을 처리하는 바깥 겹. Play 를 누르면 마이크 권한부터 받는다.
 * 나간 뒤와 답변을 낸 뒤 어디로 갈지는 화면을 등록한 쪽이 [onExit], [onFeedback] 으로 정한다. [onFeedback] 은 답변 리포트를 연다.
 */
@Composable
fun PracticeRoute(viewModel: PracticeViewModel, onExit: () -> Unit, onFeedback: (answerId: String) -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnExit by rememberUpdatedState(onExit)
    val currentOnFeedback by rememberUpdatedState(onFeedback)
    LaunchedEffect(state.navigation) {
        when (val destination = state.navigation) {
            PracticeDestination.Exit -> currentOnExit()
            is PracticeDestination.Feedback -> currentOnFeedback(destination.answerId)
            null -> return@LaunchedEffect
        }
        viewModel.onIntent(PracticeIntent.NavigationHandled)
    }
    val requestMic = rememberMicPermission()
    val scope = rememberCoroutineScope()
    PracticeScreen(
        state = state,
        onIntent = { intent ->
            if (intent == PracticeIntent.PlayQuestion) {
                scope.launch { viewModel.onIntent(if (requestMic()) intent else PracticeIntent.MicDenied) }
            } else {
                viewModel.onIntent(intent)
            }
        },
        modifier = modifier,
    )
}

/** 상태만 받아 그리는 안쪽 겹. 질문 글자는 보이지 않는다. 미리보기와 스크린샷 테스트가 이 함수를 쓴다. */
@Composable
fun PracticeScreen(state: PracticeUiState, onIntent: (PracticeIntent) -> Unit, modifier: Modifier = Modifier) {
    MontageScreen(
        modifier = modifier,
        topBar = {
            MontageTopBar(title = stringResource(Res.string.practice_title)) {
                MontageButton(
                    text = stringResource(Res.string.practice_exit),
                    onClick = { onIntent(PracticeIntent.ExitPractice) },
                    variant = MontageButtonVariant.Text,
                    type = MontageButtonType.Assistive,
                    size = MontageButtonSize.Medium,
                )
            }
        },
        bottomBar = {
            Row(Modifier.padding(horizontal = Gutter, vertical = MontageSpacing.spacing12)) {
                if (state.phase == PracticePhase.Recording) {
                    Row(horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8)) {
                        MontageButton(
                            text = stringResource(Res.string.practice_replay),
                            onClick = { onIntent(PracticeIntent.ReplayQuestion) },
                            enabled = state.canReplay,
                            type = MontageButtonType.Assistive,
                            modifier = Modifier.weight(1f),
                        )
                        MontageButton(
                            text = stringResource(Res.string.practice_next),
                            onClick = { onIntent(PracticeIntent.SubmitAnswer) },
                            enabled = state.canNext,
                            loading = state.saving,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    MontageButton(
                        text = stringResource(Res.string.practice_play),
                        onClick = { onIntent(PracticeIntent.PlayQuestion) },
                        // 질문을 고르는 동안과 질문을 읽는 동안 진행 표시를 보인다. 녹음은 다 읽은 뒤에 시작된다.
                        enabled = state.canPlay || state.preparing,
                        loading = state.preparing,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = MontageSpacing.spacing16),
            verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing12),
        ) {
            Description(stringResource(Res.string.practice_description))
            // 낼 질문이 없으면 지금 할 일이 없어서 오류 문구만 보인다.
            if (state.question != null || state.error == null) {
                Text(
                    text = when {
                        state.phase == PracticePhase.Recording -> stringResource(Res.string.practice_recording, state.recorded.clockText())
                        state.phase == PracticePhase.Speaking -> stringResource(Res.string.practice_speaking)
                        state.question == null -> stringResource(Res.string.practice_loading)
                        else -> stringResource(Res.string.practice_ready)
                    },
                    style = MontageTheme.typography.heading2.bold,
                    color = if (state.phase == PracticePhase.Recording) MontageTheme.colors.foregroundBrandPrimary else MontageTheme.colors.foregroundNeutralPrimary,
                    modifier = Modifier.padding(horizontal = Gutter),
                )
            }
            state.error?.let {
                Text(
                    text = stringResource(it.message),
                    style = MontageTheme.typography.body2.medium,
                    color = MontageTheme.colors.foregroundNegativePrimary,
                    modifier = Modifier.padding(horizontal = Gutter),
                )
            }
            Description(stringResource(Res.string.practice_rules))
        }
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

private val Gutter = MontageSpacing.spacing20

/** 분:초. 녹음 시간에 쓴다. */
private fun Duration.clockText(): String {
    val seconds = inWholeSeconds.coerceAtLeast(0)
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

private val PracticeError.message: StringResource
    get() = when (this) {
        PracticeError.MicUnavailable -> Res.string.practice_error_mic
        PracticeError.SpeechFailed -> Res.string.practice_error_speech
        PracticeError.QuestionUnavailable -> Res.string.practice_error_question
    }

/** 미리보기와 스크린샷 테스트가 함께 쓰는 고정 상태. 진행마다 하나다. */
internal object PracticeSamples {
    private val question = Question(id = "q1", text = "Tell me about your daily routine.", type = QuestionType("routine"), topic = "일상")

    val loading = PracticeUiState()
    val ready = PracticeUiState(question = question)
    val speaking = ready.copy(phase = PracticePhase.Speaking)
    val replayable = ready.copy(phase = PracticePhase.Recording, recorded = 3.seconds)
    val nextable = ready.copy(phase = PracticePhase.Recording, recorded = 12.seconds, replayUsed = true)
    val saving = nextable.copy(saving = true)
    val micError = ready.copy(error = PracticeError.MicUnavailable)
    val noQuestion = PracticeUiState(error = PracticeError.QuestionUnavailable)
}

@Preview
@Composable
private fun PracticeScreenPreview() {
    MontageTheme { PracticeScreen(state = PracticeSamples.replayable, onIntent = {}) }
}
