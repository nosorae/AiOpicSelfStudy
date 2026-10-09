package io.github.nosorae.aiopic.ui.featurereport

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.Pronunciation
import io.github.nosorae.aiopic.ui.design.MontageListCell
import io.github.nosorae.aiopic.ui.design.MontageScreen
import io.github.nosorae.aiopic.ui.design.MontageTheme
import io.github.nosorae.aiopic.ui.design.MontageTopBar
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun AnswerReportRoute(viewModel: AnswerReportViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AnswerReportScreen(state = state, modifier = modifier)
}

/**
 * 답변 하나의 피드백(F5.2, F7.1). 모의시험과 한 문항 연습이 같은 화면을 쓴다. 분석한 제공사와 모델을 맨 위에 둔다(F5.7).
 * 받아쓴 글만 분석한 경로면 발음 칸에 "평가 안 함"을 쓴다(F5.9).
 */
@Composable
fun AnswerReportScreen(state: AnswerReportUiState, modifier: Modifier = Modifier) {
    MontageScreen(modifier = modifier, topBar = { MontageTopBar(title = stringResource(Res.string.answer_title)) }) {
        val answer = state.answer
        when {
            state.loading -> Unit
            answer == null -> Paragraph(stringResource(Res.string.answer_not_found), secondary = true)
            else -> LazyColumn(Modifier.fillMaxWidth()) { answerContent(answer) }
        }
    }
}

private fun LazyListScope.answerContent(answer: Answer) {
    item {
        answer.analysis.analyzedBy?.let { Paragraph(stringResource(Res.string.answer_analyzed_by, it.label), secondary = true) }
        SectionTitle(stringResource(Res.string.answer_question))
        Paragraph(answer.question.text)
    }
    when (val analysis = answer.analysis) {
        AnswerAnalysis.Pending -> item { Paragraph(stringResource(Res.string.answer_pending), secondary = true) }

        is AnswerAnalysis.Unscorable -> item {
            SectionTitle(stringResource(Res.string.answer_unscorable))
            Paragraph(stringResource(analysis.reason.text))
            Paragraph(stringResource(Res.string.answer_unscorable_help), secondary = true)
        }

        is AnswerAnalysis.Done -> feedback(analysis.feedback)
    }
}

private fun LazyListScope.feedback(feedback: Feedback) {
    item {
        SectionTitle(stringResource(Res.string.answer_transcript))
        Paragraph(feedback.transcript)
    }
    item { SectionTitle(stringResource(Res.string.answer_corrections)) }
    if (feedback.corrections.isEmpty()) item { Paragraph(stringResource(Res.string.answer_corrections_none), secondary = true) }
    items(feedback.corrections.size) {
        val correction = feedback.corrections[it]
        MontageListCell(
            title = stringResource(Res.string.answer_corrected, correction.corrected),
            caption = stringResource(Res.string.answer_original, correction.original),
        )
    }
    item { SectionTitle(stringResource(Res.string.answer_upgrades)) }
    if (feedback.upgrades.isEmpty()) item { Paragraph(stringResource(Res.string.answer_upgrades_none), secondary = true) }
    items(feedback.upgrades.size) {
        val upgrade = feedback.upgrades[it]
        MontageListCell(
            title = stringResource(Res.string.answer_better, upgrade.better),
            caption = stringResource(Res.string.answer_original, upgrade.original),
        )
    }
    item {
        SectionTitle(stringResource(Res.string.answer_metrics))
        Metric(
            stringResource(Res.string.answer_tense),
            stringResource(if (feedback.tenseConsistent) Res.string.answer_tense_kept else Res.string.answer_tense_broken),
        )
        Metric(stringResource(Res.string.answer_sentences), stringResource(Res.string.answer_sentences_value, feedback.sentenceCount))
        Metric(stringResource(Res.string.answer_speed), stringResource(Res.string.answer_speed_value, feedback.wordsPerMinute.roundToInt()))
        val pausedSeconds = feedback.pauses.sumOf { it.length.inWholeMilliseconds }.toDouble().div(1000).roundToInt()
        Metric(stringResource(Res.string.answer_pauses), stringResource(Res.string.answer_pauses_value, feedback.pauses.size, pausedSeconds))
        feedback.roleplayQuestionCount?.let {
            Metric(stringResource(Res.string.answer_roleplay), stringResource(Res.string.answer_roleplay_value, it))
        }
        when (val pronunciation = feedback.pronunciation) {
            Pronunciation.NotEvaluated -> {
                Metric(stringResource(Res.string.answer_pronunciation), stringResource(Res.string.answer_pronunciation_not_evaluated))
                Paragraph(stringResource(Res.string.answer_pronunciation_not_evaluated_help), secondary = true)
            }

            is Pronunciation.Evaluated -> {
                SectionTitle(stringResource(Res.string.answer_pronunciation))
                Paragraph(pronunciation.comment)
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    MontageListCell(title = label) {
        Text(value, style = MontageTheme.typography.body2.bold, color = MontageTheme.colors.foregroundNeutralPrimary)
    }
}

@Preview
@Composable
private fun AnswerReportScreenPreview() {
    MontageTheme { AnswerReportScreen(state = ReportSamples.pronunciationNotEvaluated) }
}
