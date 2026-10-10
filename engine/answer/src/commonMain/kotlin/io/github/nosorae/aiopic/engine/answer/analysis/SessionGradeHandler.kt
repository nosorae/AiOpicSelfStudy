package io.github.nosorae.aiopic.engine.answer.analysis

import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.port.AnalysisError
import io.github.nosorae.aiopic.engine.answer.port.AnalysisResult
import io.github.nosorae.aiopic.engine.answer.port.Prompt
import io.github.nosorae.aiopic.engine.answer.port.Recording
import io.github.nosorae.aiopic.engine.answer.port.SessionGrader
import io.github.nosorae.aiopic.engine.answer.store.OpicStore
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
                store.save((store.session(sessionId).first() ?: session).copy(grade = result.value))
                WorkResult.Success
            }

            is AnalysisResult.Unscorable -> WorkResult.Failure

            is AnalysisResult.Failed -> if (result.error == AnalysisError.Retryable) WorkResult.Retry else WorkResult.Failure
        }
    }
}

/**
 * 회차가 끝났고 답한 답변이 모두 분석됐으면(Done 또는 평가할 수 없음) 등급 요청을 대기열에 넣는다(F5.3).
 * 시간 부족으로 끝난 회차(F2.6)는 답한 문항만 본다. 마지막 답변 분석이 회차가 끝나기 전에 끝날 수 있어서
 * 답변 분석 뒤와 회차 저장 뒤 양쪽에서 부른다. 같은 id 는 대기열이 한 번만 넣는다.
 */
suspend fun requestGradeIfReady(store: OpicStore, queue: WorkQueue, sessionId: String) {
    val session = store.session(sessionId).first() ?: return
    if (session.grade != null || analyzedAnswers(store, session) == null) return
    queue.enqueue(gradePayload(sessionId), gradePayload(sessionId))
}

/** 등급을 낼 수 있으면 답한 순서의 답변, 아니면 null. */
private suspend fun analyzedAnswers(store: OpicStore, session: ExamSession): List<Answer>? {
    if (session.status == ExamStatus.InProgress || session.answerIds.isEmpty()) return null
    val byId = store.answers(session.id).first().associateBy { it.id }
    val answers = session.answerIds.mapNotNull(byId::get)
    return answers.takeIf { it.size == session.answerIds.size && it.none { answer -> answer.analysis == AnswerAnalysis.Pending } }
}
