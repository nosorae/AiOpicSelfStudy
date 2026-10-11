package io.github.nosorae.aiopic.engine.answer.route

import io.github.nosorae.aiopic.engine.answer.model.AnalyzedBy
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.model.Pause
import io.github.nosorae.aiopic.engine.answer.model.Pronunciation
import io.github.nosorae.aiopic.engine.answer.port.AnalysisResult
import io.github.nosorae.aiopic.engine.answer.port.AudioAnswerAnalyzer
import io.github.nosorae.aiopic.engine.answer.port.Prompt
import io.github.nosorae.aiopic.engine.answer.port.Recording
import io.github.nosorae.aiopic.engine.answer.port.SavedResponses
import io.github.nosorae.aiopic.engine.answer.port.TimedWord
import io.github.nosorae.aiopic.engine.answer.port.Transcriber
import io.github.nosorae.aiopic.engine.answer.port.Transcript
import io.github.nosorae.aiopic.engine.answer.port.TranscriptAnalyzer
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

/**
 * 분석 경로 고르기(F5.9, ADR 0019). 고른 모델이 음성을 직접 들으면 한 번 호출로 끝내고, 못 들으면 받아쓴 전문을 글 분석에 보낸다.
 * 두 단계 경로는 발음을 "평가 안 함"으로 두고 멈춤과 말 속도를 단어 시각으로 계산한다.
 * 어느 경로든 결과에 제공사, 모델 id, promptVersion 을 붙여 답변에 넣는다(F5.7). 실패하면 답변을 바꾸지 않고 실패를 돌려준다.
 * 두 단계 경로는 받아쓴 결과를 [SavedResponses] 에 남겨, 둘째 단계가 실패해 다시 보내도 받아쓰기를 다시 부르지 않는다(#205).
 */
class AnswerRouter(
    private val audioAnalyzer: AudioAnswerAnalyzer,
    private val transcriber: Transcriber,
    private val transcriptAnalyzer: TranscriptAnalyzer,
) {
    suspend fun analyze(answer: Answer, model: ModelInfo, prompt: Prompt, recording: Recording): AnalysisResult<Answer> {
        val result = if (model.hearsAudio) {
            audioAnalyzer.analyze(model, prompt, answer.question, recording)
        } else {
            analyzeInTwoSteps(answer, model, prompt, recording)
        }
        val analyzedBy = AnalyzedBy(provider = model.provider, modelId = model.id, promptVersion = prompt.version)
        return when (result) {
            is AnalysisResult.Success -> AnalysisResult.Success(answer.copy(analysis = AnswerAnalysis.Done(analyzedBy, result.value)))
            is AnalysisResult.Unscorable -> AnalysisResult.Success(answer.copy(analysis = AnswerAnalysis.Unscorable(analyzedBy, result.reason)))
            is AnalysisResult.Failed -> result
        }
    }

    private suspend fun analyzeInTwoSteps(answer: Answer, model: ModelInfo, prompt: Prompt, recording: Recording): AnalysisResult<Feedback> {
        val saved = currentCoroutineContext()[SavedResponses]
        val transcript = saved?.take(TRANSCRIPT)?.let { Json.decodeFromString<Transcript>(it) } ?: when (val result = transcriber.transcribe(recording)) {
            is AnalysisResult.Success -> result.value.also { saved?.save(TRANSCRIPT, Json.encodeToString(it)) }
            is AnalysisResult.Unscorable -> return result
            is AnalysisResult.Failed -> return result
        }
        val text = when (val result = transcriptAnalyzer.analyze(model, prompt, answer.question, transcript)) {
            is AnalysisResult.Success -> result.value
            is AnalysisResult.Unscorable -> return result
            is AnalysisResult.Failed -> return result
        }
        return AnalysisResult.Success(
            Feedback(
                transcript = transcript.text,
                corrections = text.corrections,
                upgrades = text.upgrades,
                tenseConsistent = text.tenseConsistent,
                sentenceCount = text.sentenceCount,
                wordsPerMinute = wordsPerMinute(transcript.words),
                pauses = pauses(transcript.words, recording.resumedAt),
                roleplayQuestionCount = text.roleplayQuestionCount,
                pronunciation = Pronunciation.NotEvaluated,
                score = text.score,
            ),
        )
    }
}

private const val TRANSCRIPT = "transcript"

/** 단어 사이가 이만큼 비면 멈춤으로 센다. 미리 확인 노드 s1 에서 실제 멈춤을 무음 0.8초 이상으로 잡고 단어 시각과 맞춰 본 값이다. */
private val PAUSE_GAP = 800.milliseconds

/** 이어 녹음을 시작한 위치가 든 빈 곳은 앱이 꺼졌던 구간이라 멈춤으로 세지 않는다(F3.4). */
private fun pauses(words: List<TimedWord>, resumedAt: List<Duration>): List<Pause> = words.zipWithNext()
    .filter { (a, b) -> b.start - a.end >= PAUSE_GAP && resumedAt.none { it in a.end..b.start } }
    .map { (a, b) -> Pause(start = a.end, length = b.start - a.end) }

/** 녹음 시작부터 마지막 단어 끝까지를 말한 시간으로 본다. 멈춘 시간도 넣은 속도다. */
private fun wordsPerMinute(words: List<TimedWord>): Double {
    val spoken = words.lastOrNull()?.end ?: return 0.0
    return if (spoken <= Duration.ZERO) 0.0 else words.size * 60 / spoken.toDouble(DurationUnit.SECONDS)
}
