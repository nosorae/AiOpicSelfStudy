package io.github.nosorae.aiopic.engine.answer.port

import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.question.model.Question

/**
 * 한 번 호출 경로. 음성을 직접 듣는 모델이 받아쓰기와 글 분석을 한 요청에서 끝내고 발음까지 본다(F5.9, ADR 0019).
 * 결과에 제공사, 모델 id, promptVersion 을 붙이는 일은 경로 고르기가 한다.
 */
interface AudioAnswerAnalyzer {
    suspend fun analyze(model: ModelInfo, prompt: Prompt, question: Question, recording: Recording): AnalysisResult<Feedback>
}
