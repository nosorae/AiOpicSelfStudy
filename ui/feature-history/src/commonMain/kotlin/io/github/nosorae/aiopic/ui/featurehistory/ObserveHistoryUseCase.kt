package io.github.nosorae.aiopic.ui.featurehistory

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.answer.model.AnalyzedBy
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.port.ModelCatalog
import io.github.nosorae.aiopic.engine.answer.weakness.weaknessScores
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.GradeScale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * 기록의 회차 한 줄. [models] 는 분석한 모델의 표시 이름이다. 모델이 섞였으면 등급 변화가 실력 때문인지 모델 교체 때문인지
 * 가릴 수 있게 표시한다(F5.7). 등급을 기다리는 동안은 답한 수([answered]) 중 분석이 끝난 수([analyzed])를 보인다.
 */
data class HistorySession(val session: ExamSession, val models: List<String>, val answered: Int = 0, val analyzed: Int = 0) {
    val mixedModels: Boolean get() = models.size > 1
}

/** 문항 유형별 약점 한 줄. [typeName] 은 사용자에게 보일 유형 이름이다. */
data class HistoryWeakness(val typeName: String, val score: Int)

/** [grades] 는 등급 눈금에 늘어놓을 시험 팩의 등급 이름(낮은 것부터)이다. */
data class History(val sessions: List<HistorySession>, val weaknesses: List<HistoryWeakness>, val grades: List<String> = emptyList())

/**
 * 끝난 회차를 저장한 순서대로, 문항 유형별 약점은 한 문항 연습까지 모든 답변으로 낸다(F7.3).
 * 진행 중인 회차는 등급이 없어서 넣지 않는다. 틀림 표시가 붙은 답변은 약점에서 뺀다(F8.2).
 *
 * 유형 이름([typeNames])과 등급 눈금([gradeScale])은 시험 팩이 정해서 앱 층이 넣는다. 모델은 앱이 들고 있는 목록의 표시 이름을 쓴다.
 * 이름을 모르는 유형과 목록에서 빠진 모델은 id 를 그대로 보인다.
 */
@Inject
class ObserveHistoryUseCase(
    private val store: HistoryStore,
    private val typeNames: Map<QuestionType, String>,
    private val modelCatalog: ModelCatalog,
    private val gradeScale: GradeScale,
) {
    operator fun invoke(): Flow<History> = combine(store.sessions(), store.answers(), store.flags()) { sessions, answers, flags ->
        val bySession = answers.groupBy { it.examSessionId }
        History(
            sessions = sessions.filter { it.status != ExamStatus.InProgress }.map { session ->
                val own = bySession[session.id].orEmpty()
                val models = own.mapNotNull { it.analysis.analyzedBy }.distinctBy { it.provider to it.modelId }
                HistorySession(session, models.map { it.displayName() }, answered = own.size, analyzed = own.count { it.analysis != AnswerAnalysis.Pending })
            },
            weaknesses = weaknessScores(answers, flags).map { HistoryWeakness(typeNames[it.type] ?: it.type.id, it.score) },
            grades = gradeScale.grades,
        )
    }

    private fun AnalyzedBy.displayName(): String = modelCatalog.models.firstOrNull { it.provider == provider && it.id == modelId }?.displayName ?: modelId
}

private val AnswerAnalysis.analyzedBy: AnalyzedBy?
    get() = when (this) {
        AnswerAnalysis.Pending -> null
        is AnswerAnalysis.Done -> analyzedBy
        is AnswerAnalysis.Unscorable -> analyzedBy
    }
