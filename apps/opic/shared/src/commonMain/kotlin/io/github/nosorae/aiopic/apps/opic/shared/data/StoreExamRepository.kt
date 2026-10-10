package io.github.nosorae.aiopic.apps.opic.shared.data

import io.github.nosorae.aiopic.engine.answer.analysis.requestGradeIfReady
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.store.OpicStore
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.pick.pickFromPool
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
 * 녹음은 [recordingsRoot] 아래 `회차 id/단계 번호` 폴더에 쓴다. 단계마다 폴더가 하나라서 앱을 다시 켜도 같은 폴더에 이어 녹음한다(F3.3).
 */
class StoreExamRepository(
    private val store: OpicStore,
    private val queue: WorkQueue,
    private val spec: ExamSpec,
    private val pool: suspend () -> List<Question>,
    private val recordingsRoot: String,
) : ExamRepository {
    override suspend fun session(id: String): ExamSession? = store.session(id).first()

    /** 회차가 끝날 때 답변 분석이 이미 다 끝나 있으면(시간 부족으로 끝난 경우 등) 여기서 등급을 요청한다(F5.3). */
    override suspend fun save(session: ExamSession) {
        store.save(session)
        if (session.status != ExamStatus.InProgress) requestGradeIfReady(store, queue, session.id)
    }

    override suspend fun lastSurveyTopics(): List<String> = store.sessions().first().lastOrNull { it.surveyTopics.isNotEmpty() }?.surveyTopics.orEmpty()

    /**
     * 회차 전체 질문을 회차 id 로 씨앗을 고정한 난수로 풀에서 고르고 지금 단계 몫을 돌려준다(F6.2).
     * 같은 회차, 수준, 서베이면 언제 골라도 결과가 같아서 고른 질문을 따로 저장하지 않는다.
     * 점검의 연습 문항은 첫 채점 문항과 같은 유형에서 고르고 채점 문항과 겹치지 않는다.
     */
    // ponytail: 모델 출제(F6.1)를 쓰지 않고 풀에서만 고른다. 출제를 붙이면 결과가 매번 달라서 고른 질문을 회차와 함께 저장한다.
    // 사용자가 문항 파일(F6.3)을 바꾸면 진행 중인 회차의 질문도 바뀐다.
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
        val picked = pickFromPool(pool(), slots.map { it.second }, session.surveyTopics, Random(session.id.hashCode()))
        return picked?.get(position)
    }

    override fun recordingDirectory(session: ExamSession): String = "$recordingsRoot/${session.id}/${session.stepIndex}"

    override suspend fun save(answer: Answer) {
        store.save(answer)
        queue.enqueue(answer.id, answer.id)
    }
}
