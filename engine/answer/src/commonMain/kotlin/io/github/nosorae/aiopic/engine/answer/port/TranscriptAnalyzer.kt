package io.github.nosorae.aiopic.engine.answer.port

import io.github.nosorae.aiopic.engine.answer.model.Correction
import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.model.Upgrade
import io.github.nosorae.aiopic.engine.question.model.Question

/**
 * 두 단계 경로의 둘째 단계. 받아쓴 전문만 보고 글을 분석한다(F5.9, ADR 0019).
 * 음성을 듣지 않으니 발음, 멈춤, 말 속도는 여기서 내지 않는다. 경로 고르기가 단어 시각으로 계산해 피드백을 채운다.
 */
interface TranscriptAnalyzer {
    suspend fun analyze(model: ModelInfo, prompt: Prompt, question: Question, transcript: Transcript): AnalysisResult<TextAnalysis>
}

/** 글만 보고 낼 수 있는 피드백 항목. 뜻은 [io.github.nosorae.aiopic.engine.answer.model.Feedback] 의 같은 이름 항목과 같다. */
data class TextAnalysis(
    val corrections: List<Correction>,
    val upgrades: List<Upgrade>,
    val tenseConsistent: Boolean,
    val sentenceCount: Int,
    val roleplayQuestionCount: Int?,
    val score: Int,
)
