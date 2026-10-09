package io.github.nosorae.aiopic.engine.answer.port

import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.model.Stability
import io.github.nosorae.aiopic.engine.question.model.Question

/** 음성을 직접 듣는 가짜 제공사. 한 번 호출 경로를 테스트할 때 [result] 를 그대로 돌려준다. */
class FakeAudioProvider(var result: AnalysisResult<Feedback>) :
    AudioAnswerAnalyzer,
    ModelCatalog {
    override val defaultModel = ModelInfo(provider = "fake-audio", id = "audio-1", displayName = "Audio", hearsAudio = true, stability = Stability.Stable)
    override val models = listOf(defaultModel)

    override suspend fun analyze(model: ModelInfo, prompt: Prompt, question: Question, recording: Recording): AnalysisResult<Feedback> = result
}
