package io.github.nosorae.aiopic.apps.opic.shared.data

import io.github.nosorae.aiopic.apps.opic.shared.AppSettings
import io.github.nosorae.aiopic.engine.answer.analysis.requestGradeIfReady
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.store.OpicStore
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.question.pick.pickQuestions
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamSpec
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.ExamStep
import io.github.nosorae.aiopic.libs.workqueue.api.WorkQueue
import io.github.nosorae.aiopic.ui.featureexam.ExamRepository
import kotlinx.coroutines.flow.first
import kotlin.random.Random

/**
 * 시험 화면의 저장소. 회차와 답변은 [OpicStore] 에 두고(F3.1), 답변은 저장하자마자 분석 대기열에 넣는다(F5.1).
 * 등급 요청(F5.3)도 같은 대기열에 넣는다.
 * 질문은 [generate] 로 모델에 받고(F6.1), 실패하면 [pool] 에서 고른다(F6.2). 서베이 밖 주제는 [maxOutsideTopics] 개까지 받는다.
 * 녹음은 [recordingsRoot] 아래 `회차 id/단계 번호` 폴더에 쓴다. 단계마다 폴더가 하나라서 앱을 다시 켜도 같은 폴더에 이어 녹음한다(F3.3).
 */
class StoreExamRepository(
    private val store: OpicStore,
    private val queue: WorkQueue,
    private val spec: ExamSpec,
    private val pool: suspend () -> List<Question>,
    private val recordingsRoot: String,
    private val settings: AppSettings,
    private val maxOutsideTopics: Int,
    /** 회차와 문항 유형 순서로 모델에 질문을 받는다. 키, 동의가 없거나 실패하면 null 이다. */
    private val generate: suspend (ExamSession, List<QuestionType>) -> List<Question>?,
) : ExamRepository {
    override suspend fun session(id: String): ExamSession? = store.session(id).first()

    /** 회차가 끝날 때 답변 분석이 이미 다 끝나 있으면(시간 부족으로 끝난 경우 등) 여기서 등급을 요청한다(F5.3). */
    override suspend fun save(session: ExamSession) {
        store.save(session)
        if (session.status != ExamStatus.InProgress) requestGradeIfReady(store, queue, session.id)
    }

    override suspend fun lastSurveyTopics(): List<String> = store.sessions().first().lastOrNull { it.surveyTopics.isNotEmpty() }?.surveyTopics.orEmpty()

    /**
     * 회차 전체 질문을 처음 한 번 골라 회차 id 로 저장하고 지금 단계 몫을 돌려준다. 모델 출제는 부를 때마다 결과가 달라서,
     * 앱을 다시 켜고 이어 봐도 같은 질문이 나오게 저장한 목록을 먼저 읽는다(F3.3). 풀에서 고를 때는 회차 id 로 씨앗을 고정한다.
     * 점검의 연습 문항은 첫 채점 문항과 같은 유형이고 채점 문항과 겹치지 않는다.
     */
    override suspend fun question(session: ExamSession): Question? {
        val level = session.level ?: return null
        val firstType = spec.steps.firstNotNullOfOrNull { (it as? ExamStep.Ask)?.questionType } ?: return null
        val slots = spec.steps.withIndex().mapNotNull { (index, step) ->
            when {
                step is ExamStep.Check -> index to firstType
                step is ExamStep.Ask && step.minLevel <= level -> index to step.questionType
                else -> null
            }
        }
        val position = slots.indexOfFirst { it.first == session.stepIndex }.takeIf { it >= 0 } ?: return null
        val order = slots.map { it.second }
        val picked = settings.examQuestions(session.id)?.takeIf { saved -> saved.map { it.type } == order }
            ?: pickQuestions(order, session.surveyTopics, pool(), maxOutsideTopics, Random(session.id.hashCode())) { generate(session, order) }
                ?.also { settings.saveExamQuestions(session.id, it) }
        return picked?.get(position)
    }

    override fun recordingDirectory(session: ExamSession): String = "$recordingsRoot/${session.id}/${session.stepIndex}"

    override suspend fun save(answer: Answer) {
        store.save(answer)
        queue.enqueue(answer.id, answer.id)
    }
}
