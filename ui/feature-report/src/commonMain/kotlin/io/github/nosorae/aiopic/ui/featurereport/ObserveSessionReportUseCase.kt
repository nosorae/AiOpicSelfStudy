package io.github.nosorae.aiopic.ui.featurereport

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.answer.model.AnalyzedBy
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamSpec
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** 회차 리포트(F7.2)에 그릴 값. [answers] 는 회차에서 답한 순서다. */
data class SessionReport(val session: ExamSession, val answers: List<Answer>, val questionCount: Int) {
    /** 40분이 지나 답하지 못한 문항 수. 리포트 끝에 "시간 부족"으로 붙는다(F2.6). */
    val timeShortCount: Int get() = if (session.status == ExamStatus.TimeUp) (questionCount - answers.size).coerceAtLeast(0) else 0

    /** 이 회차 답변을 분석한 모델. 제공사와 모델 id 가 같으면 하나로 센다(F5.7). */
    val models: List<AnalyzedBy> get() = answers.mapNotNull { it.analysis.analyzedBy }.distinctBy { it.provider to it.modelId }

    /** 모델마다 채점 경향이 달라서, 섞였으면 회차 등급 옆에 표시해 등급을 그대로 믿지 않게 한다(F5.7). */
    val mixedModels: Boolean get() = models.size > 1
}

/** 분석 대기 중이면 null 이다. */
val AnswerAnalysis.analyzedBy: AnalyzedBy?
    get() = when (this) {
        AnswerAnalysis.Pending -> null
        is AnswerAnalysis.Done -> analyzedBy
        is AnswerAnalysis.Unscorable -> analyzedBy
    }

/** 회차와 그 답변을 구독해 리포트로 묶는다. 시험 모양([spec])은 앱 층이 시험 팩에서 넣는다. 회차가 없으면 null 이다. */
@Inject
class ObserveSessionReportUseCase(private val store: ReportStore, private val spec: ExamSpec) {
    operator fun invoke(sessionId: String): Flow<SessionReport?> = combine(store.session(sessionId), store.answers(sessionId)) { session, answers ->
        session ?: return@combine null
        val byId = answers.associateBy { it.id }
        val level = session.level ?: spec.levelCount
        SessionReport(
            session = session,
            answers = session.answerIds.mapNotNull(byId::get),
            questionCount = spec.steps.count { it is ExamStep.Ask && it.minLevel <= level },
        )
    }
}
