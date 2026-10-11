package io.github.nosorae.aiopic.engine.answer.analysis

import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.port.AnalysisResult
import io.github.nosorae.aiopic.engine.answer.port.Prompt
import io.github.nosorae.aiopic.engine.answer.port.Recording
import io.github.nosorae.aiopic.engine.answer.port.SessionGrader
import io.github.nosorae.aiopic.engine.answer.store.OpicStore
import io.github.nosorae.aiopic.engine.session.model.AnalysisError
import io.github.nosorae.aiopic.engine.session.model.ExamSession
import io.github.nosorae.aiopic.engine.session.model.ExamStatus
import io.github.nosorae.aiopic.engine.session.model.GradeScale
import io.github.nosorae.aiopic.libs.workqueue.api.WorkHandler
import io.github.nosorae.aiopic.libs.workqueue.api.WorkQueue
import io.github.nosorae.aiopic.libs.workqueue.api.WorkResult
import kotlinx.coroutines.flow.first

/** 회차 등급을 보낼 때 앱이 채워 주는 것. 고른 모델, 답변 피드백을 채운 시험 팩의 등급 프롬프트, 등급 체계, 답한 순서의 녹음이다. */
data class GradeRequest(val model: ModelInfo, val prompt: Prompt, val scale: GradeScale, val recordings: List<Recording>)

private const val GRADE_PREFIX = "grade:"

/** 대기열에서 회차 등급 요청의 id 이자 payload. 답변 id 와 겹치지 않게 앞에 표시를 붙인다. */
fun gradePayload(sessionId: String): String = GRADE_PREFIX + sessionId

/**
 * 분석 대기열 하나가 답변 분석과 회차 등급(F5.3)을 함께 처리한다. 답변 id 는 [answers] 에 넘기고,
 * 그 답변으로 회차의 답변이 모두 분석되면 등급 요청을 같은 대기열에 넣는다. 한꺼번에 보내지 않아 무료 키의 분당 한도를 지킨다.
 * 동의 확인과 다시 보내기 규칙(F1.3, F5.6)은 답변 분석과 같고, 이미 등급이 있는 회차는 다시 보내지 않는다.
 */
class SessionGradeHandler(
    private val store: OpicStore,
    private val queue: WorkQueue,
    private val answers: WorkHandler,
    private val grader: SessionGrader,
    private val consented: suspend () -> Boolean,
    private val request: suspend (List<Answer>) -> GradeRequest,
) : WorkHandler {
    override suspend fun handle(payload: String): WorkResult {
        if (!payload.startsWith(GRADE_PREFIX)) {
            val result = answers.handle(payload)
            // ponytail: 답변 하나의 회차를 찾으려고 전체를 읽는다. 답변 분석 처리기와 같은 한계다.
            if (result == WorkResult.Success) store.answers().first().firstOrNull { it.id == payload }?.examSessionId?.let { requestGradeIfReady(store, queue, it) }
            return result
        }
        if (!consented()) return WorkResult.Failure
        val sessionId = payload.removePrefix(GRADE_PREFIX)
        val session = store.session(sessionId).first() ?: return WorkResult.Success
        if (session.grade != null) return WorkResult.Success
        val answered = analyzedAnswers(store, session) ?: return WorkResult.Success
        val (model, prompt, scale, recordings) = request(answered)
        return when (val result = grader.grade(model, prompt, scale, recordings)) {
            is AnalysisResult.Success -> {
                // 등급을 기다리는 동안 회차가 바뀌었을 수 있어 최신 값에 등급만 얹는다.
                store.save((store.session(sessionId).first() ?: session).copy(grade = result.value, gradeError = null))
                WorkResult.Success
            }

            is AnalysisResult.Unscorable -> WorkResult.Failure

            is AnalysisResult.Failed -> {
                // 화면이 등급 기다림을 끝내고 다시 분석하기를 보이게 남긴다(#203). 다시 보내 받으면 등급이 덮는다.
                store.save((store.session(sessionId).first() ?: session).copy(gradeError = result.error))
                if (result.error == AnalysisError.Retryable) WorkResult.Retry else WorkResult.Failure
            }
        }
    }
}

/**
 * 회차가 끝났고 답한 답변이 모두 분석됐으면(Done 또는 평가할 수 없음) 등급 요청을 대기열에 넣는다(F5.3).
 * 등급을 내지 못한 회차는 사용자가 다시 분석하기를 누를 때까지 넣지 않는다([retrySession]).
 * 시간 부족으로 끝난 회차(F2.6)는 답한 문항만 본다. 마지막 답변 분석이 회차가 끝나기 전에 끝날 수 있어서
 * 답변 분석 뒤와 회차 저장 뒤 양쪽에서 부른다. 같은 id 는 대기열이 한 번만 넣는다.
 */
suspend fun requestGradeIfReady(store: OpicStore, queue: WorkQueue, sessionId: String) {
    val session = store.session(sessionId).first() ?: return
    if (session.grade != null || session.gradeError != null || analyzedAnswers(store, session) == null) return
    queue.enqueue(gradePayload(sessionId), gradePayload(sessionId))
}

/** 등급을 낼 수 있으면 답한 순서의 답변, 아니면 null. */
private suspend fun analyzedAnswers(store: OpicStore, session: ExamSession): List<Answer>? {
    if (session.status == ExamStatus.InProgress || session.answerIds.isEmpty()) return null
    val byId = store.answers(session.id).first().associateBy { it.id }
    val answers = session.answerIds.mapNotNull(byId::get)
    return answers.takeIf { it.size == session.answerIds.size && it.all { answer -> answer.analysis is AnswerAnalysis.Done || answer.analysis is AnswerAnalysis.Unscorable } }
}

/** 분석하지 못한 답변을 기다림으로 돌려 대기열에 다시 넣는다(#203). 실패가 아니면 아무것도 하지 않는다. */
suspend fun retryAnalysis(store: OpicStore, queue: WorkQueue, answerId: String) {
    val answer = store.answers().first().firstOrNull { it.id == answerId && it.analysis is AnswerAnalysis.Failed } ?: return
    store.save(answer.copy(analysis = AnswerAnalysis.Pending))
    queue.enqueue(answerId, answerId)
}

/** 회차에서 분석하지 못한 답변을 모두 다시 보내고, 등급을 내지 못했으면 등급도 다시 요청한다(#203). */
suspend fun retrySession(store: OpicStore, queue: WorkQueue, sessionId: String) {
    store.answers(sessionId).first().filter { it.analysis is AnswerAnalysis.Failed }.forEach { retryAnalysis(store, queue, it.id) }
    val session = store.session(sessionId).first() ?: return
    if (session.gradeError == null) return
    store.save(session.copy(gradeError = null))
    requestGradeIfReady(store, queue, sessionId)
}

/**
 * 앱을 켤 때 부른다. 기다림인데 대기열에 없는 답변과 등급 요청을 다시 넣는다(#203).
 * 음성 전송 동의를 철회해 대기열을 비웠거나, 실패를 남기기 전 버전이 버린 답변이 화면에서 끝없이 기다리지 않게 한다.
 * 같은 id 는 대기열이 한 번만 넣는다.
 */
suspend fun requeuePending(store: OpicStore, queue: WorkQueue) {
    store.answers().first().filter { it.analysis == AnswerAnalysis.Pending }.forEach { queue.enqueue(it.id, it.id) }
    store.sessions().first().forEach { requestGradeIfReady(store, queue, it.id) }
}
