package io.github.nosorae.aiopic.ui.featurereport

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.nosorae.aiopic.engine.answer.model.AnalysisJob
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.Pronunciation
import io.github.nosorae.aiopic.ui.design.MontageCelebration
import io.github.nosorae.aiopic.ui.design.MontageCelebrationSize
import io.github.nosorae.aiopic.ui.design.MontageCountUp
import io.github.nosorae.aiopic.ui.design.MontageGatheringDots
import io.github.nosorae.aiopic.ui.design.MontageListCell
import io.github.nosorae.aiopic.ui.design.MontageScreen
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageTheme
import io.github.nosorae.aiopic.ui.design.MontageTopBar
import io.github.nosorae.aiopic.ui.featurereport.flag.CorrectionRow
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun AnswerReportRoute(viewModel: AnswerReportViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // ponytail: 내보내기 화면처럼 예전 클립보드 API 하나로 두 플랫폼을 덮는다. 새 API 는 넣는 값을 플랫폼마다 따로 만들어야 한다.
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    AnswerReportScreen(
        state = state,
        onIntent = { intent ->
            if (intent == AnswerReportIntent.ScriptCopied) {
                ((state.answer?.analysis as? AnswerAnalysis.Done)?.feedback?.finalScript)?.let { clipboard.setText(AnnotatedString(it)) }
            }
            viewModel.onIntent(intent)
        },
        modifier = modifier,
    )
}

/**
 * 답변 하나의 피드백(F5.2, F7.1). 모의고사와 한 문항 연습이 같은 화면을 쓴다. 분석한 제공사와 모델을 맨 위에 둔다(F5.7).
 * 받아쓴 글만 분석한 경로면 발음 칸에 "평가 안 함"을 쓴다(F5.9). 교정마다 "틀림"을 누를 수 있다(F8.1).
 * 기다리는 동안 분석 작업이 무엇을 기다리는지(차례, 보내는 중, 연결, 다시 보낼 때까지 남은 초) 쓰고(#205), 분석하지 못했으면 이유와 다시 분석하기를 보인다(#203).
 * 이 화면의 움직임은 하나다. 분석을 기다리는 동안은 모이는 점, 분석이 끝났으면 굴러 올라가는 점수다(답변마다 한 번만).
 * 보는 중에 분석이 끝나면 점수 둘레에 작은 축하가 한 번 터진다.
 * 표현 교정 아래에 내용 교정을, 맨 끝에 최종 스크립트를 둔다(#217). 프롬프트 v3 전에 분석한 답변은 최종 스크립트가 없어 두 칸을 모두 숨긴다.
 */
@Composable
fun AnswerReportScreen(state: AnswerReportUiState, onIntent: (AnswerReportIntent) -> Unit, modifier: Modifier = Modifier) {
    MontageScreen(modifier = modifier, topBar = { MontageTopBar(title = stringResource(Res.string.answer_title)) }) {
        val answer = state.answer
        when {
            state.loading -> Unit

            answer == null -> Paragraph(stringResource(Res.string.answer_not_found), secondary = true)

            else -> {
                // 이 화면을 보는 동안 분석이 끝났을 때만 축하한다. 이미 분석된 답변을 다시 여는 것은 상태가 바뀐 게 아니다.
                val waited = rememberSaveable(answer.id) { answer.analysis == AnswerAnalysis.Pending }
                LazyColumn(Modifier.fillMaxWidth()) {
                    answerContent(
                        answer,
                        state.names,
                        state.wrongCorrections,
                        state.job,
                        state.scriptPlayback,
                        state.scriptCopied,
                        waited,
                        onWrongClick = { onIntent(AnswerReportIntent.ToggleCorrectionWrong(it)) },
                        onRetryClick = { onIntent(AnswerReportIntent.RetryAnalysis) },
                        onCopyClick = { onIntent(AnswerReportIntent.ScriptCopied) },
                        onListenClick = { onIntent(AnswerReportIntent.ListenScript) },
                    )
                }
            }
        }
    }
}

private fun LazyListScope.answerContent(
    answer: Answer,
    names: ReportNames,
    wrongCorrections: Set<Int>,
    job: AnalysisJob?,
    scriptPlayback: ScriptPlayback,
    scriptCopied: Boolean,
    celebrate: Boolean,
    onWrongClick: (index: Int) -> Unit,
    onRetryClick: () -> Unit,
    onCopyClick: () -> Unit,
    onListenClick: () -> Unit,
) {
    item {
        answer.analysis.analyzedBy?.let { Paragraph(stringResource(Res.string.answer_analyzed_by, names.model(it)), secondary = true) }
        SectionTitle(stringResource(Res.string.answer_question))
        Paragraph(answer.question.text)
    }
    when (val analysis = answer.analysis) {
        AnswerAnalysis.Pending -> item {
            val status = jobStatus(job)
            MontageGatheringDots(status, Modifier.padding(horizontal = MontageSpacing.spacing20, vertical = MontageSpacing.spacing8))
            Paragraph(status)
            Paragraph(stringResource(Res.string.answer_pending), secondary = true)
        }

        is AnswerAnalysis.Unscorable -> item {
            SectionTitle(stringResource(Res.string.answer_unscorable))
            Paragraph(stringResource(analysis.reason.text))
            Paragraph(stringResource(Res.string.answer_unscorable_help), secondary = true)
        }

        // 기다림을 끝내고 이유와 다시 분석하기를 보인다(#203). 다시 누르면 기다림으로 돌아가 모이는 점이 다시 보인다.
        is AnswerAnalysis.Failed -> item {
            SectionTitle(stringResource(Res.string.answer_failed))
            Paragraph(stringResource(analysis.error.text))
            RetryButton(onRetryClick, tag = "answer-retry")
        }

        is AnswerAnalysis.Done -> feedback(answer.id, analysis.feedback, wrongCorrections, scriptPlayback, scriptCopied, celebrate, onWrongClick, onCopyClick, onListenClick)
    }
}

private fun LazyListScope.feedback(
    answerId: String,
    feedback: Feedback,
    wrongCorrections: Set<Int>,
    scriptPlayback: ScriptPlayback,
    scriptCopied: Boolean,
    celebrate: Boolean,
    onWrongClick: (index: Int) -> Unit,
    onCopyClick: () -> Unit,
    onListenClick: () -> Unit,
) {
    item {
        SectionTitle(stringResource(Res.string.answer_score))
        Box(Modifier.padding(horizontal = MontageSpacing.spacing20), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.Bottom) {
                val style = MontageTheme.typography.title1.bold
                MontageCountUp(feedback.score, style = style, color = MontageTheme.colors.foregroundNeutralStrong, playOnceKey = "answer-score:$answerId:${feedback.score}")
                Text(stringResource(Res.string.answer_score_unit), style = style, color = MontageTheme.colors.foregroundNeutralStrong)
            }
            if (celebrate) {
                MontageCelebration(MontageCelebrationSize.Small, Modifier.matchParentSize().wrapContentSize(unbounded = true).requiredSize(MontageSpacing.spacing48 * 2), playOnceKey = "answer-celebration:$answerId")
            }
        }
    }
    item {
        SectionTitle(stringResource(Res.string.answer_transcript))
        Paragraph(feedback.transcript)
    }
    item { SectionTitle(stringResource(Res.string.answer_corrections)) }
    if (feedback.corrections.isEmpty()) item { Paragraph(stringResource(Res.string.answer_corrections_none), secondary = true) }
    items(feedback.corrections.size) {
        CorrectionRow(feedback.corrections[it], wrong = it in wrongCorrections, onWrongClick = { onWrongClick(it) })
    }
    // 옛 답변은 최종 스크립트가 없다. 그때는 내용 교정을 보지 않은 것이라 "고칠 곳이 없다"고 쓰지 않고 칸을 숨긴다.
    if (feedback.finalScript != null) {
        item {
            SectionTitle(stringResource(Res.string.answer_content_fixes))
            Paragraph(stringResource(if (feedback.contentFixes.isEmpty()) Res.string.answer_content_fixes_none else Res.string.answer_content_fixes_help), secondary = true)
        }
        items(feedback.contentFixes.size) { ContentFixRow(feedback.contentFixes[it]) }
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
    feedback.finalScript?.let { text ->
        item {
            SectionTitle(stringResource(Res.string.answer_script))
            FinalScript(answerId, text, scriptPlayback, scriptCopied, onCopyClick, onListenClick)
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
    MontageTheme { AnswerReportScreen(state = ReportSamples.pronunciationNotEvaluated, onIntent = {}) }
}
