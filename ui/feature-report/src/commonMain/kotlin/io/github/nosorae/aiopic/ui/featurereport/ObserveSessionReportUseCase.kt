package io.github.nosorae.aiopic.ui.featurereport

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.answer.model.AnalysisJob
import io.github.nosorae.aiopic.engine.answer.model.AnalyzedBy
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.JobKind
import io.github.nosorae.aiopic.engine.answer.port.ModelCatalog
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamSpec
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import io.github.nosorae.aiopic.engine.session.model.GradeScale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** 회차 리포트(F7.2)에 그릴 값. [answers] 는 회차에서 답한 순서다. [jobs] 는 이 회차 답변과 등급의 분석 작업이다(#205). */
data class SessionReport(
    val session: ExamSession,
    val answers: List<Answer>,
    val questionCount: Int,
    val names: ReportNames = ReportNames(),
    val jobs: List<AnalysisJob> = emptyList(),
) {
    /** 답변 하나의 분석 작업. 기다리는 답변 줄에 무엇을 기다리는지 쓴다. */
    fun job(answerId: String): AnalysisJob? = jobs.firstOrNull { it.kind == JobKind.Answer && it.targetId == answerId }

    /** 등급 작업. 없으면 아직 넣지 않았다. */
    val gradeJob: AnalysisJob? get() = jobs.firstOrNull { it.kind == JobKind.Grade }

    /** 40분이 지나 답하지 못한 문항 수. 리포트 끝에 "시간 부족"으로 붙는다(F2.6). */
    val timeShortCount: Int get() = if (session.status == ExamStatus.TimeUp) (questionCount - answers.size).coerceAtLeast(0) else 0

    /** 이 회차 답변을 분석한 모델. 제공사와 모델 id 가 같으면 하나로 센다(F5.7). */
    val models: List<AnalyzedBy> get() = answers.mapNotNull { it.analysis.analyzedBy }.distinctBy { it.provider to it.modelId }

    /** 모델마다 채점 경향이 달라서, 섞였으면 회차 등급 옆에 표시해 등급을 그대로 믿지 않게 한다(F5.7). */
    val mixedModels: Boolean get() = models.size > 1

    /** 분석이 끝난 답변 수(평가 불가 포함). 회차 등급은 모든 답변이 끝나야 나와서, 기다리는 동안 이 수만큼 진행을 보인다. */
    val analyzedCount: Int get() = answers.count { it.analysis is AnswerAnalysis.Done || it.analysis is AnswerAnalysis.Unscorable }

    /** 보냈지만 결과를 받지 못한 답변 수(#203). 하나라도 있으면 등급을 요청하지 않는다. */
    val failedCount: Int get() = answers.count { it.analysis is AnswerAnalysis.Failed }

    /** 답변 분석은 다 끝났고 회차 등급만 기다리는 중. 등급은 모든 답변이 분석된 뒤에야 요청해서, 대기열에 있거나 모델이 내는 동안 이 상태다. */
    val grading: Boolean get() = session.grade == null && session.gradeError == null && answers.isNotEmpty() && analyzedCount == answers.size

    /** 기다려도 더 나올 것이 없어 다시 분석하기를 보일 때(#203). */
    val canRetry: Boolean get() = session.grade == null && (failedCount > 0 || session.gradeError != null)
}

/** 분석 대기 중이거나 실패했으면 null 이다. */
val AnswerAnalysis.analyzedBy: AnalyzedBy?
    get() = when (this) {
        AnswerAnalysis.Pending, is AnswerAnalysis.Failed -> null
        is AnswerAnalysis.Done -> analyzedBy
        is AnswerAnalysis.Unscorable -> analyzedBy
    }

/** 회차와 그 답변을 구독해 리포트로 묶는다. 시험 모양([spec]), 유형 이름([typeNames]), 등급 눈금([gradeScale])은 앱 층이 시험 팩에서 넣는다. 회차가 없으면 null 이다. */
@Inject
class ObserveSessionReportUseCase(
    private val store: ReportStore,
    private val spec: ExamSpec,
    private val typeNames: Map<QuestionType, String>,
    private val modelCatalog: ModelCatalog,
    private val gradeScale: GradeScale,
) {
    operator fun invoke(sessionId: String): Flow<SessionReport?> = combine(store.session(sessionId), store.answers(sessionId), store.jobs()) { session, answers, jobs ->
        session ?: return@combine null
        val byId = answers.associateBy { it.id }
        val level = session.level ?: spec.levelCount
        SessionReport(
            session = session,
            answers = session.answerIds.mapNotNull(byId::get),
            questionCount = spec.steps.count { it is ExamStep.Ask && it.minLevel <= level },
            names = ReportNames(typeNames, modelCatalog.models, gradeScale.grades),
            jobs = jobs.filter { if (it.kind == JobKind.Grade) it.targetId == sessionId else it.targetId in byId },
        )
    }
}
