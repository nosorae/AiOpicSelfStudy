package io.github.nosorae.aiopic.ui.featurehistory

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.answer.model.AnalyzedBy
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.weakness.TypeWeakness
import io.github.nosorae.aiopic.engine.answer.weakness.weaknessScores
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** 기록의 회차 한 줄. 모델이 섞였으면 등급 변화가 실력 때문인지 모델 교체 때문인지 가릴 수 있게 표시한다(F5.7). */
data class HistorySession(val session: ExamSession, val models: List<AnalyzedBy>) {
    val mixedModels: Boolean get() = models.size > 1
}

data class History(val sessions: List<HistorySession>, val weaknesses: List<TypeWeakness>)

/**
 * 끝난 회차를 저장한 순서대로, 문항 유형별 약점은 한 문항 연습까지 모든 답변으로 낸다(F7.3).
 * 진행 중인 회차는 등급이 없어서 넣지 않는다. 틀림 표시가 붙은 답변은 약점에서 뺀다(F8.2).
 */
@Inject
class ObserveHistoryUseCase(private val store: HistoryStore) {
    operator fun invoke(): Flow<History> = combine(store.sessions(), store.answers(), store.flags()) { sessions, answers, flags ->
        val bySession = answers.groupBy { it.examSessionId }
        History(
            sessions = sessions.filter { it.status != ExamStatus.InProgress }.map { session ->
                val models = bySession[session.id].orEmpty().mapNotNull { it.analysis.analyzedBy }.distinctBy { it.provider to it.modelId }
                HistorySession(session, models)
            },
            weaknesses = weaknessScores(answers, flags),
        )
    }
}

private val AnswerAnalysis.analyzedBy: AnalyzedBy?
    get() = when (this) {
        AnswerAnalysis.Pending -> null
        is AnswerAnalysis.Done -> analyzedBy
        is AnswerAnalysis.Unscorable -> analyzedBy
    }
