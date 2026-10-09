package io.github.nosorae.aiopic.ui.featurereport

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.session.model.SessionGrade
import io.github.nosorae.aiopic.ui.design.MontageListCell
import io.github.nosorae.aiopic.ui.design.MontageRadius
import io.github.nosorae.aiopic.ui.design.MontageScreen
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageTheme
import io.github.nosorae.aiopic.ui.design.MontageTopBar
import org.jetbrains.compose.resources.stringResource

/** 등급 범위. 체크 결과가 한 등급으로 모이면 그 등급 하나만 쓴다(F5.3). */
val SessionGrade.range: String get() = if (low == high) low else "$low ~ $high"

/** ViewModel 을 관측하는 바깥 겹. 답변을 누르면 [onOpenAnswer] 로 답변 리포트를 연다. */
@Composable
fun SessionReportRoute(viewModel: SessionReportViewModel, onOpenAnswer: (answerId: String) -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SessionReportScreen(state = state, onOpenAnswer = onOpenAnswer, modifier = modifier)
}

/** 등급 범위와 근거, 무너진 문항, 답변 목록 순으로 그린다(F7.2). 시간이 다 된 회차는 답하지 못한 문항을 끝에 "시간 부족"으로 붙인다. */
@Composable
fun SessionReportScreen(state: SessionReportUiState, onOpenAnswer: (answerId: String) -> Unit, modifier: Modifier = Modifier) {
    MontageScreen(modifier = modifier, topBar = { MontageTopBar(title = stringResource(Res.string.session_title)) }) {
        val report = state.report
        when {
            state.loading -> Unit
            report == null -> Paragraph(stringResource(Res.string.session_not_found), secondary = true)
            else -> SessionReportContent(report, onOpenAnswer)
        }
    }
}

@Composable
private fun SessionReportContent(report: SessionReport, onOpenAnswer: (String) -> Unit) {
    val grade = report.session.grade
    LazyColumn(Modifier.fillMaxWidth()) {
        item { GradeHeader(report) }
        if (grade != null && grade.reasons.isNotEmpty()) {
            item { SectionTitle(stringResource(Res.string.session_reasons)) }
            items(grade.reasons.size) { Paragraph(grade.reasons[it]) }
        }
        item { SectionTitle(stringResource(Res.string.session_answers, report.questionCount)) }
        itemsIndexed(report.answers, key = { _, answer -> answer.id }) { index, answer ->
            val number = index + 1
            AnswerRow(number, answer, broke = grade != null && number in grade.brokeAt, onClick = { onOpenAnswer(answer.id) })
        }
        items(report.timeShortCount) {
            MontageListCell(
                title = stringResource(Res.string.session_number, report.answers.size + it + 1),
                caption = stringResource(Res.string.session_time_short),
            )
        }
    }
}

@Composable
private fun GradeHeader(report: SessionReport) {
    val grade = report.session.grade
    val colors = MontageTheme.colors
    val typography = MontageTheme.typography
    Column {
        SectionTitle(stringResource(Res.string.session_grade))
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MontageSpacing.spacing20),
            horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing12),
            verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            if (grade != null) {
                Text(grade.range, style = typography.title1.bold, color = colors.foregroundNeutralStrong)
            } else {
                Text(stringResource(Res.string.session_grade_pending), style = typography.body1Reading.regular, color = colors.foregroundNeutralSecondary)
            }
            if (report.mixedModels) {
                Text(
                    text = stringResource(Res.string.session_models_mixed),
                    style = typography.label2.bold,
                    color = colors.foregroundNeutralPrimary,
                    modifier = Modifier
                        .border(1.dp, colors.lineNeutralPrimary, RoundedCornerShape(MontageRadius.radius8))
                        .padding(horizontal = MontageSpacing.spacing8, vertical = MontageSpacing.spacing4),
                )
            }
        }
        if (report.models.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.session_models, report.models.joinToString(", ") { it.label }),
                style = typography.label1.regular,
                color = colors.foregroundNeutralSecondary,
                modifier = Modifier.padding(horizontal = MontageSpacing.spacing20, vertical = MontageSpacing.spacing8),
            )
        }
    }
}

@Composable
private fun AnswerRow(number: Int, answer: Answer, broke: Boolean, onClick: () -> Unit) {
    val status = when (val analysis = answer.analysis) {
        AnswerAnalysis.Pending -> stringResource(Res.string.status_pending)
        is AnswerAnalysis.Done -> stringResource(Res.string.status_score, analysis.feedback.score, analysis.analyzedBy.label)
        is AnswerAnalysis.Unscorable -> stringResource(Res.string.status_unscorable, stringResource(analysis.reason.text), analysis.analyzedBy.label)
    }
    val caption = if (broke) "${stringResource(Res.string.session_broke)}, $status" else status
    MontageListCell(
        title = stringResource(Res.string.session_answer_title, number, answer.question.type.id),
        caption = caption,
        chevron = true,
        onClick = onClick,
    )
}

@Preview
@Composable
private fun SessionReportScreenPreview() {
    MontageTheme { SessionReportScreen(state = ReportSamples.mixedModels, onOpenAnswer = {}) }
}
