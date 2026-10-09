package io.github.nosorae.aiopic.engine.aigemini

import io.github.nosorae.aiopic.engine.answer.check.checkFeedback
import io.github.nosorae.aiopic.engine.answer.check.checkSessionGrade
import io.github.nosorae.aiopic.engine.answer.check.requestChecked
import io.github.nosorae.aiopic.engine.answer.model.Correction
import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.model.Pause
import io.github.nosorae.aiopic.engine.answer.model.Pronunciation
import io.github.nosorae.aiopic.engine.answer.model.UnscorableReason
import io.github.nosorae.aiopic.engine.answer.model.Upgrade
import io.github.nosorae.aiopic.engine.answer.port.AnalysisError
import io.github.nosorae.aiopic.engine.answer.port.AnalysisResult
import io.github.nosorae.aiopic.engine.answer.port.AudioAnswerAnalyzer
import io.github.nosorae.aiopic.engine.answer.port.Prompt
import io.github.nosorae.aiopic.engine.answer.port.Recording
import io.github.nosorae.aiopic.engine.answer.port.SessionGrader
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.session.model.GradeScale
import io.github.nosorae.aiopic.engine.session.model.SessionGrade
import io.github.nosorae.aiopic.libs.llmgemini.GeminiClient
import io.github.nosorae.aiopic.libs.llmgemini.api.AudioPart
import io.github.nosorae.aiopic.libs.llmgemini.api.LlmRequest
import io.github.nosorae.aiopic.libs.llmgemini.api.LlmResult
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

/**
 * Gemini 한 번 호출 경로(ADR 0019). 녹음을 직접 들려주고 답변 피드백과 회차 등급을 받는다.
 * 받은 값은 출력 검사(F5.4)를 거치고, 통과 못하면 한 번 다시 요청한다.
 * [prompt] 본문은 부르는 쪽이 문항과 이어 녹음 위치를 채운 글이다.
 */
class GeminiAnalyzer(
    private val client: GeminiClient,
    /** 저장된 Gemini 키. 없으면 요청하지 않고 [AnalysisError.InvalidKey] 로 끝낸다. */
    private val apiKey: suspend () -> String?,
) : AudioAnswerAnalyzer,
    SessionGrader {
    override suspend fun analyze(model: ModelInfo, prompt: Prompt, question: Question, recording: Recording): AnalysisResult<Feedback> = requestChecked(::checkFeedback) {
        when (val result = generate(model, prompt, listOf(recording), AnswerWire.serializer())) {
            is AnalysisResult.Success -> result.value.toResult()
            is AnalysisResult.Unscorable -> result
            is AnalysisResult.Failed -> result
        }
    }

    override suspend fun grade(model: ModelInfo, prompt: Prompt, scale: GradeScale, recordings: List<Recording>): AnalysisResult<SessionGrade> = requestChecked({ checkSessionGrade(it, scale, recordings.size) }) {
        generate(model, prompt, recordings, SessionGrade.serializer())
    }

    private suspend fun <T> generate(model: ModelInfo, prompt: Prompt, recordings: List<Recording>, serializer: KSerializer<T>): AnalysisResult<T> {
        val key = apiKey() ?: return AnalysisResult.Failed(AnalysisError.InvalidKey)
        val request = LlmRequest(
            apiKey = key,
            model = model.id,
            prompt = prompt.text,
            audio = recordings.map { AudioPart(it.bytes, it.mimeType) },
            responseSchema = prompt.responseSchema,
        )
        return when (val result = client.generate(request)) {
            // ponytail: 읽을 수 없는 JSON 은 다시 요청하지 않고 거절한다. 스키마를 주면 드물어서다. 잘린 응답이 잦으면 출력 검사처럼 한 번 다시 보낸다.
            is LlmResult.Success -> try {
                AnalysisResult.Success(json.decodeFromString(serializer, result.text))
            } catch (_: SerializationException) {
                AnalysisResult.Failed(AnalysisError.Rejected)
            } catch (_: IllegalArgumentException) {
                AnalysisResult.Failed(AnalysisError.Rejected)
            }

            is LlmResult.Retryable -> AnalysisResult.Failed(AnalysisError.Retryable)

            LlmResult.InvalidKey -> AnalysisResult.Failed(AnalysisError.InvalidKey)

            LlmResult.ModelNotFound -> AnalysisResult.Failed(AnalysisError.ModelUnavailable)

            is LlmResult.Rejected -> AnalysisResult.Failed(AnalysisError.Rejected)
        }
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}

/**
 * 답변 분석 응답의 JSON 모양. 시험 팩의 응답 클래스(스키마 기준)와 필드가 같아야 한다. 엔진은 시험 팩을 의존하지 못해서 따로 둔다.
 * 시간은 모델이 쓰기 쉬운 초 단위 숫자로 받는다.
 */
@Serializable
private class AnswerWire(
    val transcript: String,
    val corrections: List<Correction>,
    val upgrades: List<Upgrade>,
    val tenseConsistent: Boolean,
    val sentenceCount: Int,
    val wordsPerMinute: Double,
    val pauses: List<PauseWire>,
    val roleplayQuestionCount: Int? = null,
    val pronunciation: String,
    val score: Int,
    val unscorableReason: UnscorableReason? = null,
) {
    fun toResult(): AnalysisResult<Feedback> = unscorableReason?.let { AnalysisResult.Unscorable(it) } ?: AnalysisResult.Success(
        Feedback(
            transcript = transcript,
            corrections = corrections,
            upgrades = upgrades,
            tenseConsistent = tenseConsistent,
            sentenceCount = sentenceCount,
            wordsPerMinute = wordsPerMinute,
            pauses = pauses.map { Pause(start = it.startSeconds.seconds, length = it.lengthSeconds.seconds) },
            roleplayQuestionCount = roleplayQuestionCount,
            pronunciation = Pronunciation.Evaluated(pronunciation),
            score = score,
        ),
    )
}

@Serializable
private class PauseWire(val startSeconds: Double, val lengthSeconds: Double)
