package io.github.nosorae.aiopic.engine.answer.analysis

import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.port.AnalysisError
import io.github.nosorae.aiopic.engine.answer.port.AnalysisResult
import io.github.nosorae.aiopic.engine.answer.port.Prompt
import io.github.nosorae.aiopic.engine.answer.port.Recording
import io.github.nosorae.aiopic.engine.answer.route.AnswerRouter
import io.github.nosorae.aiopic.engine.answer.store.OpicStore
import io.github.nosorae.aiopic.libs.workqueue.api.WorkHandler
import io.github.nosorae.aiopic.libs.workqueue.api.WorkResult
import kotlinx.coroutines.flow.first

/** 답변 하나를 보낼 때 앱이 채워 주는 것. 고른 모델(F5.8), 시험 팩의 프롬프트, 조각을 이어 붙인 녹음이다. */
data class AnalysisRequest(val model: ModelInfo, val prompt: Prompt, val recording: Recording)

/**
 * 분석 대기열이 꺼낸 답변 하나를 분석해 저장한다(F5.1). 대기열의 payload 는 답변 id 다.
 * 음성 전송 동의가 없으면 보내지 않는다(F1.3, ADR 0014). 429, 408, 5xx, 오프라인만 다시 보내고(F5.6)
 * 나머지 실패는 버린다. 어느 경우든 답변과 녹음은 남고 분석 전 상태로 둔다.
 */
class AnswerAnalysisHandler(
    private val store: OpicStore,
    private val router: AnswerRouter,
    private val consented: suspend () -> Boolean,
    private val request: suspend (Answer) -> AnalysisRequest,
) : WorkHandler {
    override suspend fun handle(payload: String): WorkResult {
        if (!consented()) return WorkResult.Failure
        // ponytail: 답변 하나를 찾으려고 전체를 읽는다. 답변이 수천 개로 늘면 저장소에 id 로 읽는 쿼리를 더한다.
        val answer = store.answers().first().firstOrNull { it.id == payload }
        if (answer?.analysis != AnswerAnalysis.Pending) return WorkResult.Success
        val (model, prompt, recording) = request(answer)
        return when (val result = router.analyze(answer, model, prompt, recording)) {
            is AnalysisResult.Success -> {
                store.save(result.value)
                WorkResult.Success
            }

            is AnalysisResult.Unscorable -> WorkResult.Failure

            is AnalysisResult.Failed -> if (result.error == AnalysisError.Retryable) WorkResult.Retry else WorkResult.Failure
        }
    }
}
