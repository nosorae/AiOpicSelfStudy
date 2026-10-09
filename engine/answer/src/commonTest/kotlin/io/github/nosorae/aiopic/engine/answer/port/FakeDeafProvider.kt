package io.github.nosorae.aiopic.engine.answer.port

import io.github.nosorae.aiopic.engine.answer.model.Correction
import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.model.Stability
import io.github.nosorae.aiopic.engine.question.model.Question
import kotlin.time.Duration.Companion.seconds

/**
 * 음성을 못 듣는 가짜 제공사(F5.9, ADR 0019). 첫 버전은 두 단계 경로를 이것으로만 끝까지 돌린다.
 * 받아쓰기와 글 분석을 함께 맡고, 정해 둔 전문과 분석을 돌려준다.
 */
class FakeDeafProvider(
    val transcript: Transcript = Transcript(
        text = "I live in a apartment.",
        words = listOf(
            TimedWord("I", 0.seconds, 0.2.seconds),
            TimedWord("live", 0.3.seconds, 0.6.seconds),
            TimedWord("in", 0.7.seconds, 0.8.seconds),
            TimedWord("a", 2.0.seconds, 2.1.seconds),
            TimedWord("apartment.", 2.2.seconds, 2.9.seconds),
        ),
    ),
    val analysis: TextAnalysis = TextAnalysis(
        corrections = listOf(Correction(original = "a apartment", corrected = "I live in an apartment.")),
        upgrades = emptyList(),
        tenseConsistent = true,
        sentenceCount = 1,
        roleplayQuestionCount = null,
        score = 60,
    ),
) : Transcriber,
    TranscriptAnalyzer,
    ModelCatalog {
    override val defaultModel = ModelInfo(provider = "fake-deaf", id = "deaf-1", displayName = "Deaf", hearsAudio = false, stability = Stability.Stable)
    override val models = listOf(defaultModel)

    /** 글 분석이 받은 전문, 받은 순서대로. */
    val analyzedTranscripts = mutableListOf<Transcript>()

    override suspend fun transcribe(recording: Recording): AnalysisResult<Transcript> = AnalysisResult.Success(transcript)

    override suspend fun analyze(model: ModelInfo, prompt: Prompt, question: Question, transcript: Transcript): AnalysisResult<TextAnalysis> {
        analyzedTranscripts += transcript
        return AnalysisResult.Success(analysis)
    }
}
