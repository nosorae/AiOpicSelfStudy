package io.github.nosorae.aiopic.ui.featureexam

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import io.github.nosorae.aiopic.engine.question.model.SurveyQuestion
import io.github.nosorae.aiopic.engine.question.model.TopicGroup
import io.github.nosorae.aiopic.libs.haptics.api.HapticEffect
import io.github.nosorae.aiopic.ui.design.LocalHaptics
import io.github.nosorae.aiopic.ui.design.MontageButton
import io.github.nosorae.aiopic.ui.design.MontageButtonType
import io.github.nosorae.aiopic.ui.design.MontageChip
import io.github.nosorae.aiopic.ui.design.MontageCountUp
import io.github.nosorae.aiopic.ui.design.MontageListCell
import io.github.nosorae.aiopic.ui.design.MontageMotion
import io.github.nosorae.aiopic.ui.design.MontageProgressBar
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageTheme
import org.jetbrains.compose.resources.stringResource

/**
 * 서베이 한 화면. 맨 위 진행 막대 아래에 질문 하나 또는 주제 묶음 하나를 그리고, 화면이 바뀌면 옆으로 밀려난다.
 * 앞으로 가면 새 화면이 오른쪽에서, 뒤로 가면 왼쪽에서 들어온다. 모의시험의 서베이 단계와 설정의 서베이가 같이 쓴다.
 */
@Composable
fun SurveyFlow(state: SurveyState, onChoose: (String) -> Unit, onToggle: (String) -> Unit, modifier: Modifier = Modifier) {
    val slide = MontageMotion.spec<IntOffset>()
    Column(modifier.fillMaxSize()) {
        MontageProgressBar(state.progress, Modifier.testTag("survey-progress"))
        AnimatedContent(
            targetState = state.screen,
            transitionSpec = {
                val forward = if (targetState > initialState) 1 else -1
                slideInHorizontally(slide) { forward * it } togetherWith slideOutHorizontally(slide) { -forward * it }
            },
            modifier = Modifier.weight(1f),
            label = "survey-screen",
        ) { screen ->
            val shown = state.copy(screen = screen)
            val scrolling = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = MontageSpacing.spacing24)
            shown.question?.let { QuestionScreen(it, shown.choices[it.id], onChoose, scrolling) }
            shown.group?.let { GroupScreen(shown, it, onToggle, scrolling) }
        }
    }
}

/** 서베이 아래 버튼 띠. 첫 화면이 아니면 이전, 묶음 화면이면 다음(마지막 화면은 [submitText]) 을 둔다. 하나만 고르는 질문은 고르면 바로 넘어가서 다음이 없다. */
@Composable
fun SurveyButtons(
    state: SurveyState,
    submitText: String,
    saving: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8)) {
        if (state.screen > 0) {
            MontageButton(
                text = stringResource(Res.string.survey_previous),
                onClick = onPrevious,
                type = MontageButtonType.Assistive,
                modifier = Modifier.weight(1f).testTag("survey-previous"),
            )
        }
        if (state.group != null) {
            MontageButton(
                text = if (state.isLast) submitText else stringResource(Res.string.survey_next),
                onClick = if (state.isLast) onSubmit else onNext,
                enabled = state.canNext,
                loading = saving,
                modifier = Modifier.weight(1f).testTag("survey-next"),
            )
        }
    }
}

@Composable
private fun QuestionScreen(question: SurveyQuestion, chosen: String?, onChoose: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing12)) {
        Title(question.text)
        Column {
            question.options.forEachIndexed { index, option ->
                MontageListCell(
                    title = option.label,
                    selected = option.id == chosen,
                    onClick = { onChoose(option.id) },
                    modifier = Modifier.testTag("survey-option-${question.id}-$index"),
                )
            }
        }
    }
}

@Composable
private fun GroupScreen(state: SurveyState, group: TopicGroup, onToggle: (String) -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHaptics.current
    val count = state.selectedIn(group)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing12)) {
        Title(group.title)
        Help(stringResource(Res.string.survey_group_help, group.min))
        Text(
            text = stringResource(Res.string.survey_group_count, count),
            style = MontageTheme.typography.label1.bold,
            color = if (count >= group.min) MontageTheme.colors.foregroundBrandPrimary else MontageTheme.colors.foregroundNeutralSecondary,
            modifier = Modifier.padding(horizontal = Gutter),
        )
        FlowRow(
            modifier = Modifier.padding(horizontal = Gutter),
            horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8),
            verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8),
        ) {
            group.topics.forEach { topic ->
                MontageChip(
                    text = topic.label,
                    onClick = {
                        haptics.play(HapticEffect.Selection)
                        onToggle(topic.id)
                    },
                    active = topic.id in state.topics,
                    modifier = Modifier.testTag("survey-topic-${topic.id}"),
                )
            }
        }
        if (state.isLast) Total(state)
    }
}

/** 마지막 묶음 화면에서 모든 묶음에서 고른 수를 숫자로 보인다. 모자라면 앞 묶음으로 돌아가라고 알린다. */
@Composable
private fun Total(state: SurveyState) {
    val enough = state.topics.size >= state.survey.minTopics
    val color = if (enough) MontageTheme.colors.foregroundBrandPrimary else MontageTheme.colors.foregroundNeutralPrimary
    Column(Modifier.padding(top = MontageSpacing.spacing24), verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8)) {
        Row(
            modifier = Modifier.padding(horizontal = Gutter),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing4),
        ) {
            MontageCountUp(state.topics.size, Modifier.testTag("survey-total"), style = MontageTheme.typography.title2.bold, color = color)
            Text(
                text = stringResource(Res.string.survey_total_suffix, state.survey.minTopics),
                style = MontageTheme.typography.body2.medium,
                color = MontageTheme.colors.foregroundNeutralSecondary,
                // 큰 글자에서 숫자를 밀어내지 않고 줄을 바꾼다.
                modifier = Modifier.weight(1f, fill = false).padding(bottom = MontageSpacing.spacing4),
            )
        }
        if (!enough) Help(stringResource(Res.string.survey_total_short))
    }
}

@Composable
private fun Title(text: String) {
    Text(
        text = text,
        style = MontageTheme.typography.title3.bold,
        color = MontageTheme.colors.foregroundNeutralPrimary,
        modifier = Modifier.padding(horizontal = Gutter).semantics { heading() },
    )
}

@Composable
private fun Help(text: String) {
    Text(
        text = text,
        style = MontageTheme.typography.body1.regular,
        color = MontageTheme.colors.foregroundNeutralSecondary,
        modifier = Modifier.padding(horizontal = Gutter),
    )
}

private val Gutter = MontageSpacing.spacing20
