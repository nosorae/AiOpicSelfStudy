package io.github.nosorae.aiopic.ui.featurereport

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.nosorae.aiopic.engine.answer.model.AnalysisJob
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.JobState
import io.github.nosorae.aiopic.engine.session.model.SessionGrade
import io.github.nosorae.aiopic.ui.design.MontageCelebration
import io.github.nosorae.aiopic.ui.design.MontageCelebrationSize
import io.github.nosorae.aiopic.ui.design.MontageGatheringDots
import io.github.nosorae.aiopic.ui.design.MontageGradeScale
import io.github.nosorae.aiopic.ui.design.MontageListCell
import io.github.nosorae.aiopic.ui.design.MontageRadius
import io.github.nosorae.aiopic.ui.design.MontageScreen
import io.github.nosorae.aiopic.ui.design.MontageSpacing
import io.github.nosorae.aiopic.ui.design.MontageStepTrail
import io.github.nosorae.aiopic.ui.design.MontageTheme
import io.github.nosorae.aiopic.ui.design.MontageTopBar
import org.jetbrains.compose.resources.stringResource

/** 등급 범위. 체크 결과가 한 등급으로 모이면 그 등급 하나만 쓴다(F5.3). */
val SessionGrade.range: String get() = if (low == high) low else "$low ~ $high"

/** ViewModel 을 관측하는 바깥 겹. 답변을 누르면 [onOpenAnswer] 로 답변 리포트를 연다. */
@Composable
fun SessionReportRoute(viewModel: SessionReportViewModel, onOpenAnswer: (answerId: String) -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SessionReportScreen(state = state, onOpenAnswer = onOpenAnswer, onIntent = viewModel::onIntent, modifier = modifier)
}

/**
 * 등급 범위와 근거, 무너진 문항, 답변 목록 순으로 그린다(F7.2). 시간이 다 된 회차는 답하지 못한 문항을 끝에 "시간 부족"으로 붙인다.
 * 새로 시작해 끝낸 미완료 회차는 등급 옆에 미완료라고 쓰고 남은 문항을 "답하지 않음"으로 붙인다. 답한 문항이 없으면 기다림 대신
 * 등급이 없다는 문구만 보인다(#223).
 * 이 화면의 움직임은 하나다. 등급이 나왔으면 등급 눈금의 구슬, 분석을 기다리는 중이면 분석한 답변 수만큼 차는 진행 점,
 * 분석은 다 끝나고 등급만 기다리면 모이는 점이다. 분석이나 등급이 실패하면 기다림 문구 대신 실패 문구와 다시 분석하기를 보인다(#203). 등급 구슬은 회차와 등급마다 한 번만 움직인다.
 * 보는 중에 등급이 나오면 구슬이 멈춘 뒤 큰 축하가 한 번 터진다.
 */
@Composable
fun SessionReportScreen(state: SessionReportUiState, onOpenAnswer: (answerId: String) -> Unit, onIntent: (SessionReportIntent) -> Unit, modifier: Modifier = Modifier) {
    MontageScreen(modifier = modifier, topBar = { MontageTopBar(title = stringResource(Res.string.session_title)) }) {
        val report = state.report
        when {
            state.loading -> Unit
            report == null -> Paragraph(stringResource(Res.string.session_not_found), secondary = true)
            else -> SessionReportContent(report, onOpenAnswer) { onIntent(SessionReportIntent.RetryAnalysis) }
        }
    }
}

@Composable
private fun SessionReportContent(report: SessionReport, onOpenAnswer: (String) -> Unit, onRetryClick: () -> Unit) {
    val grade = report.session.grade
    LazyColumn(Modifier.fillMaxWidth()) {
        item { GradeHeader(report) }
        if (report.canRetry) item { RetryButton(onRetryClick, tag = "session-retry") }
        // 답한 문항 없이 끝난 미완료 회차는 보일 줄이 없다. 머리의 문구가 그 사실을 전한다.
        if (report.answers.isEmpty() && report.unansweredCount == 0) return@LazyColumn
        if (grade != null && grade.reasons.isNotEmpty()) {
            item { SectionTitle(stringResource(Res.string.session_reasons)) }
            items(grade.reasons.size) { Paragraph(grade.reasons[it]) }
        }
        item { SectionTitle(stringResource(Res.string.session_answers, report.questionCount)) }
        itemsIndexed(report.answers, key = { _, answer -> answer.id }) { index, answer ->
            val number = index + 1
            AnswerRow(number, answer, report.job(answer.id), report.names, broke = grade != null && number in grade.brokeAt, onClick = { onOpenAnswer(answer.id) })
        }
        items(report.unansweredCount) {
            MontageListCell(
                title = stringResource(Res.string.session_number, report.answers.size + it + 1),
                caption = stringResource(if (report.abandoned) Res.string.session_unanswered else Res.string.session_time_short),
            )
        }
    }
}

@Composable
private fun GradeHeader(report: SessionReport) {
    val grade = report.session.grade
    val colors = MontageTheme.colors
    val typography = MontageTheme.typography
    val scale = Modifier.padding(horizontal = MontageSpacing.spacing20, vertical = MontageSpacing.spacing8)
    // 이 화면을 보는 동안 등급이 나왔을 때만 축하한다. 이미 등급이 나온 회차를 다시 여는 것은 상태가 바뀐 게 아니다.
    val waited = rememberSaveable { grade == null }
    Column {
        SectionTitle(stringResource(Res.string.session_grade))
        val names = report.names.grades
        when {
            grade != null && grade.low in names && grade.high in names -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                MontageGradeScale(
                    names,
                    names.indexOf(grade.low),
                    names.indexOf(grade.high),
                    description = grade.range,
                    modifier = scale,
                    playOnceKey = "session-grade:${report.session.id}:${grade.range}",
                )
                if (waited) {
                    MontageCelebration(
                        MontageCelebrationSize.Large,
                        Modifier.matchParentSize().wrapContentSize(unbounded = true).requiredSize(CELEBRATION_SIZE),
                        playOnceKey = "session-grade-celebration:${report.session.id}",
                        delayMillis = GRADE_SETTLE_MILLIS,
                    )
                }
            }

            report.grading -> MontageGatheringDots(gradeStatus(report), scale)

            grade == null && report.answers.isNotEmpty() -> MontageStepTrail(total = report.answers.size, done = report.analyzedCount, modifier = scale)
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MontageSpacing.spacing20),
            horizontalArrangement = Arrangement.spacedBy(MontageSpacing.spacing12),
            verticalArrangement = Arrangement.spacedBy(MontageSpacing.spacing8),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            if (grade != null) {
                Text(grade.range, style = typography.title1.bold, color = colors.foregroundNeutralStrong)
            } else {
                val gradeError = report.session.gradeError
                val waiting = if (report.failedCount > 0) {
                    stringResource(Res.string.session_failed, report.failedCount)
                } else if (gradeError != null) {
                    stringResource(Res.string.session_grade_failed, stringResource(gradeError.text))
                } else if (report.answers.isEmpty()) {
                    // 분석할 답이 없어 등급은 나오지 않는다. 기다림 문구를 쓰면 끝나지 않는 기다림이 된다.
                    stringResource(Res.string.session_no_answers)
                } else if (report.grading) {
                    gradeStatus(report)
                } else {
                    stringResource(Res.string.session_grade_waiting, report.answers.size, report.analyzedCount)
                }
                Text(waiting, style = typography.body1Reading.regular, color = colors.foregroundNeutralSecondary)
            }
            if (report.abandoned) Badge(stringResource(Res.string.session_abandoned))
            if (report.mixedModels) Badge(stringResource(Res.string.session_models_mixed))
        }
        if (report.models.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.session_models, report.models.joinToString(", ", transform = report.names::model)),
                style = typography.label1.regular,
                color = colors.foregroundNeutralSecondary,
                modifier = Modifier.padding(horizontal = MontageSpacing.spacing20, vertical = MontageSpacing.spacing8),
            )
        }
    }
}

/** 등급 옆에 붙는 짧은 표시. 미완료 회차와 모델 섞임에 쓴다. */
@Composable
private fun Badge(text: String) {
    val colors = MontageTheme.colors
    Text(
        text = text,
        style = MontageTheme.typography.label2.bold,
        color = colors.foregroundNeutralPrimary,
        modifier = Modifier
            .border(1.dp, colors.lineNeutralPrimary, RoundedCornerShape(MontageRadius.radius8))
            .padding(horizontal = MontageSpacing.spacing8, vertical = MontageSpacing.spacing4),
    )
}

/** 등급을 기다리는 동안의 문구. 연결이나 다시 보내기를 기다리면 그것을 쓰고, 아니면 등급을 내는 중이라고 쓴다(#205). */
@Composable
private fun gradeStatus(report: SessionReport): String = when (report.gradeJob?.state) {
    JobState.WaitingNetwork, JobState.RetryScheduled -> jobStatus(report.gradeJob)
    else -> stringResource(Res.string.session_grade_grading)
}

@Composable
private fun AnswerRow(number: Int, answer: Answer, job: AnalysisJob?, names: ReportNames, broke: Boolean, onClick: () -> Unit) {
    val status = when (val analysis = answer.analysis) {
        AnswerAnalysis.Pending -> jobStatus(job)
        is AnswerAnalysis.Failed -> stringResource(Res.string.status_failed)
        is AnswerAnalysis.Done -> stringResource(Res.string.status_score, analysis.feedback.score, names.model(analysis.analyzedBy))
        is AnswerAnalysis.Unscorable -> stringResource(Res.string.status_unscorable, stringResource(analysis.reason.text), names.model(analysis.analyzedBy))
    }
    val caption = if (broke) "${stringResource(Res.string.session_broke)}, $status" else status
    MontageListCell(
        title = stringResource(Res.string.session_answer_title, number, names.type(answer.question.type)),
        caption = caption,
        chevron = true,
        onClick = onClick,
    )
}

@Preview
@Composable
private fun SessionReportScreenPreview() {
    MontageTheme { SessionReportScreen(state = ReportSamples.mixedModels, onOpenAnswer = {}, onIntent = {}) }
}

/** 등급 구슬이 스프링으로 미끄러져 멈추는 데 걸리는 시간. 축하는 그 뒤에 터진다. */
private const val GRADE_SETTLE_MILLIS = 1_000

private val CELEBRATION_SIZE = 280.dp
