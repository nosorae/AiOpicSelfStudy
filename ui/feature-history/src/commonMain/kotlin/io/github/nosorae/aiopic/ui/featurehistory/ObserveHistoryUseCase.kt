package io.github.nosorae.aiopic.ui.featurehistory

import dev.zacsweers.metro.Inject
import io.github.nosorae.aiopic.engine.answer.model.AnalyzedBy
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.JobKind
import io.github.nosorae.aiopic.engine.answer.model.JobState
import io.github.nosorae.aiopic.engine.answer.port.ModelCatalog
import io.github.nosorae.aiopic.engine.answer.weakness.weaknessScores
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.GradeScale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/** 기록 목록의 한 줄. 모의고사 회차([HistorySession])와 한 문항 연습 답변([HistoryPractice])이 한 목록에 섞인다(F7.4). */
sealed interface HistoryEntry

/**
 * 기록의 회차 한 줄. [number] 는 끝난 회차를 처음부터 센 순번이다. [models] 는 분석한 모델의 표시 이름이다. 모델이 섞였으면 등급 변화가 실력 때문인지 모델 교체 때문인지
 * 가릴 수 있게 표시한다(F5.7). 등급을 기다리는 동안은 답한 수([answered]) 중 분석이 끝난 수([analyzed])를 보인다.
 */
data class HistorySession(
    val session: ExamSession,
    val models: List<String>,
    val answered: Int = 0,
    val analyzed: Int = 0,
    val number: Int = 0,
) : HistoryEntry {
    val mixedModels: Boolean get() = models.size > 1
}

/**
 * 한 문항 연습 답변 한 줄(F7.4). 연습은 한 문항이 한 단위라 답변 하나가 한 줄이다. [analysis] 로 점수, 분석 중, 실패를 가른다.
 * [model] 은 분석한 모델의 표시 이름이고 분석이 끝나기 전에는 null 이다. [job] 은 분석 작업 표(#205)의 상태로, 기다리는 동안 무엇을 기다리는지 가른다.
 */
data class HistoryPractice(val answerId: String, val typeName: String, val model: String?, val analysis: AnswerAnalysis, val job: JobState? = null) : HistoryEntry

/** 문항 유형별 약점 한 줄. [typeName] 은 사용자에게 보일 유형 이름이다. [rose] 는 지난번에 본 점수보다 오른 것을 처음 보는 중이다(#198). */
data class HistoryWeakness(val typeName: String, val score: Int, val rose: Boolean = false)

/** [entries] 는 최근 것부터다. [grades] 는 등급 눈금에 늘어놓을 시험 팩의 등급 이름(낮은 것부터)이다. */
data class History(val entries: List<HistoryEntry>, val weaknesses: List<HistoryWeakness>, val grades: List<String> = emptyList())

/**
 * 끝난 회차와 한 문항 연습 답변을 한 목록에 최근 것부터 놓는다(F7.4). 문항 유형별 약점은 한 문항 연습까지 모든 답변으로 낸다(F7.3).
 * 진행 중인 회차는 등급이 없어서 넣지 않는다. 틀림 표시가 붙은 답변은 약점에서 뺀다(F8.2).
 * 약점 점수는 볼 때마다 저장소에 남기고, 지난번에 본 점수보다 오른 유형은 [HistoryWeakness.rose] 로 표시한다(#198).
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
    operator fun invoke(): Flow<History> = flow {
        // 이번에 기록을 열기 전에 본 점수. 구독하는 동안은 이 값과 비교해서, 본 표시를 새로 남겨도 축하가 중간에 사라지지 않는다.
        val before = store.seen().first()
        val marked = before.toMutableMap()
        emitAll(history(before, marked))
    }

    private fun history(before: Map<String, Int>, marked: MutableMap<String, Int>) = combine(store.sessions(), store.answers(), store.flags(), store.jobs()) { sessions, answers, flags, jobs ->
        val answerJobs = jobs.filter { it.kind == JobKind.Answer }.associate { it.targetId to it.state }
        val bySession = answers.groupBy { it.examSessionId }
        val position = answers.withIndex().associate { (index, answer) -> answer.id to index }
        // ponytail: 저장소에 시각이 없어 답변이 저장된 순서로 시간을 대신한다. 회차는 첫 답변 자리에, 답변이 없는 회차는 앞 회차 바로 뒤에 둔다.
        // 시험 중간에 연습을 하면 그 연습은 회차 뒤에 놓인다. 정확한 시각이 필요해지면 회차와 답변에 저장 시각을 더한다.
        var last = -1
        val exams = sessions.filter { it.status != ExamStatus.InProgress }.mapIndexed { index, session ->
            val own = bySession[session.id].orEmpty()
            val models = own.mapNotNull { it.analysis.analyzedBy }.distinctBy { it.provider to it.modelId }
            last = own.minOfOrNull { position.getValue(it.id) } ?: last
            last to HistorySession(
                session,
                models.map { it.displayName() },
                answered = own.size,
                analyzed = own.count { it.analysis is AnswerAnalysis.Done || it.analysis is AnswerAnalysis.Unscorable },
                number = index + 1,
            )
        }
        val practices = answers.filter { it.examSessionId == null }.map { answer ->
            position.getValue(answer.id) to HistoryPractice(
                answer.id,
                answer.question.type.name(),
                answer.analysis.analyzedBy?.displayName(),
                answer.analysis,
                answerJobs[answer.id],
            )
        }
        val scores = weaknessScores(answers, flags)
        // 지금 본 점수를 남긴다. 다음에 열 때 이 값보다 오르면 축하한다. 처음 보는 유형은 비교할 값이 없어 남기기만 한다.
        scores.forEach { (type, score) ->
            val key = "weakness:${type.id}"
            if (marked[key] != score) {
                store.markSeen(key, score)
                marked[key] = score
            }
        }
        History(
            entries = (exams + practices).sortedBy { it.first }.map { it.second }.asReversed(),
            weaknesses = scores.map { (type, score) -> HistoryWeakness(type.name(), score, rose = before["weakness:${type.id}"]?.let { score > it } == true) },
            grades = gradeScale.grades,
        )
    }

    private fun QuestionType.name(): String = typeNames[this] ?: id

    private fun AnalyzedBy.displayName(): String = modelCatalog.models.firstOrNull { it.provider == provider && it.id == modelId }?.displayName ?: modelId
}

private val AnswerAnalysis.analyzedBy: AnalyzedBy?
    get() = when (this) {
        AnswerAnalysis.Pending, is AnswerAnalysis.Failed -> null
        is AnswerAnalysis.Done -> analyzedBy
        is AnswerAnalysis.Unscorable -> analyzedBy
    }
