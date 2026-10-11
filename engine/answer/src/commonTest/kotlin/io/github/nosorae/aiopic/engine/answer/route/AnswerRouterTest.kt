package io.github.nosorae.aiopic.engine.answer.route

import io.github.nosorae.aiopic.engine.answer.model.AnalyzedBy
import io.github.nosorae.aiopic.engine.answer.model.Answer
import io.github.nosorae.aiopic.engine.answer.model.AnswerAnalysis
import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.model.Pause
import io.github.nosorae.aiopic.engine.answer.model.Pronunciation
import io.github.nosorae.aiopic.engine.answer.model.Stability
import io.github.nosorae.aiopic.engine.answer.model.UnscorableReason
import io.github.nosorae.aiopic.engine.session.model.AnalysisError
import io.github.nosorae.aiopic.engine.answer.port.AnalysisResult
import io.github.nosorae.aiopic.engine.answer.port.AudioAnswerAnalyzer
import io.github.nosorae.aiopic.engine.answer.port.FakeDeafProvider
import io.github.nosorae.aiopic.engine.answer.port.Prompt
import io.github.nosorae.aiopic.engine.answer.port.Recording
import io.github.nosorae.aiopic.engine.answer.port.TimedWord
import io.github.nosorae.aiopic.engine.answer.port.Transcriber
import io.github.nosorae.aiopic.engine.answer.port.Transcript
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class AnswerRouterTest {
    private val question = Question(id = "q1", text = "Tell me about your house.", type = QuestionType("description"), topic = "home")
    private val answer = Answer(id = "a1", question = question, examSessionId = "s1", chunkPaths = listOf("a1/0.m4a"))
    private val prompt = Prompt(version = "answer-v1", text = "analyze", responseSchema = "{}")
    private fun recording(resumedAt: List<Duration> = emptyList()) = Recording(ByteArray(0), "audio/mp4", resumedAt)

    private val hearingModel = ModelInfo(provider = "fake-hearing", id = "hear-1", displayName = "Hear", hearsAudio = true, stability = Stability.Stable)
    private val audioFeedback = Feedback(
        transcript = "I live in an apartment.",
        corrections = emptyList(),
        upgrades = emptyList(),
        tenseConsistent = true,
        sentenceCount = 1,
        wordsPerMinute = 150.0,
        pauses = emptyList(),
        roleplayQuestionCount = null,
        pronunciation = Pronunciation.Evaluated("Clear."),
        score = 80,
    )

    private class FakeHearingAnalyzer(val result: AnalysisResult<Feedback>) : AudioAnswerAnalyzer {
        var calls = 0

        override suspend fun analyze(model: ModelInfo, prompt: Prompt, question: Question, recording: Recording): AnalysisResult<Feedback> {
            calls++
            return result
        }
    }

    private fun router(audio: AudioAnswerAnalyzer, deaf: FakeDeafProvider = FakeDeafProvider(), transcriber: Transcriber = deaf) = AnswerRouter(audioAnalyzer = audio, transcriber = transcriber, transcriptAnalyzer = deaf)

    @Test
    fun `F5_9 음성을 듣는 모델이면 한 번 호출로 끝내고 받아쓰기를 부르지 않는다`() = runTest {
        val audio = FakeHearingAnalyzer(AnalysisResult.Success(audioFeedback))
        val deaf = FakeDeafProvider()

        val result = router(audio, deaf).analyze(answer, hearingModel, prompt, recording())

        assertEquals(AnswerAnalysis.Done(AnalyzedBy("fake-hearing", "hear-1", "answer-v1"), audioFeedback), answerOf(result).analysis)
        assertEquals(1, audio.calls)
        assertEquals(emptyList(), deaf.analyzedTranscripts)
    }

    @Test
    fun `F5_9 음성을 못 듣는 모델이면 받아쓴 전문을 글 분석에 보내고 끝까지 저장된다`() = runTest {
        val audio = FakeHearingAnalyzer(AnalysisResult.Success(audioFeedback))
        val deaf = FakeDeafProvider()

        val result = router(audio, deaf).analyze(answer, deaf.defaultModel, prompt, recording())

        val saved = Json.decodeFromString<Answer>(Json.encodeToString(answerOf(result)))
        val done = assertIs<AnswerAnalysis.Done>(saved.analysis)
        assertEquals(0, audio.calls)
        assertEquals(listOf(deaf.transcript), deaf.analyzedTranscripts)
        assertEquals("I live in a apartment.", done.feedback.transcript)
        assertEquals(deaf.analysis.corrections, done.feedback.corrections)
        assertEquals(60, done.feedback.score)
    }

    @Test
    fun `F5_9 두 단계 경로는 발음을 평가 안 함으로 둔다`() = runTest {
        val deaf = FakeDeafProvider()

        val result = router(FakeHearingAnalyzer(AnalysisResult.Success(audioFeedback)), deaf).analyze(answer, deaf.defaultModel, prompt, recording())

        assertEquals(Pronunciation.NotEvaluated, feedbackOf(result).pronunciation)
    }

    @Test
    fun `F5_9 두 단계 경로는 단어 사이 0_8초 이상 빈 곳을 멈춤으로 센다`() = runTest {
        val deaf = FakeDeafProvider()

        val result = router(FakeHearingAnalyzer(AnalysisResult.Success(audioFeedback)), deaf).analyze(answer, deaf.defaultModel, prompt, recording())

        // 가짜 전문은 "in"(0.8초 끝)과 "a"(2.0초 시작) 사이만 1.2초 비어 있다.
        assertEquals(listOf(Pause(start = 0.8.seconds, length = 1.2.seconds)), feedbackOf(result).pauses)
    }

    @Test
    fun `F3_4 이어 녹음을 시작한 위치가 든 빈 곳은 멈춤으로 세지 않는다`() = runTest {
        val deaf = FakeDeafProvider()

        val result = router(FakeHearingAnalyzer(AnalysisResult.Success(audioFeedback)), deaf)
            .analyze(answer, deaf.defaultModel, prompt, recording(resumedAt = listOf(1.5.seconds)))

        assertEquals(emptyList(), feedbackOf(result).pauses)
    }

    @Test
    fun `F5_9 두 단계 경로는 녹음 시작부터 마지막 단어 끝까지로 분당 단어 수를 낸다`() = runTest {
        val deaf = FakeDeafProvider()

        val result = router(FakeHearingAnalyzer(AnalysisResult.Success(audioFeedback)), deaf).analyze(answer, deaf.defaultModel, prompt, recording())

        // 단어 5개를 2.9초 동안 말했다.
        assertEquals(5 / 2.9 * 60, feedbackOf(result).wordsPerMinute, absoluteTolerance = 1e-9)
    }

    @Test
    fun `F5_7 두 단계 경로 결과에도 고른 제공사와 모델 id 와 promptVersion 이 붙는다`() = runTest {
        val deaf = FakeDeafProvider()

        val result = router(FakeHearingAnalyzer(AnalysisResult.Success(audioFeedback)), deaf).analyze(answer, deaf.defaultModel, prompt, recording())

        assertEquals(AnalyzedBy("fake-deaf", "deaf-1", "answer-v1"), assertIs<AnswerAnalysis.Done>(answerOf(result).analysis).analyzedBy)
    }

    @Test
    fun `F5_5 평가할 수 없다는 답도 분석한 모델을 붙여 저장한다`() = runTest {
        val audio = FakeHearingAnalyzer(AnalysisResult.Unscorable(UnscorableReason.Silent))

        val result = router(audio).analyze(answer, hearingModel, prompt, recording())

        assertEquals(
            AnswerAnalysis.Unscorable(AnalyzedBy("fake-hearing", "hear-1", "answer-v1"), UnscorableReason.Silent),
            answerOf(result).analysis,
        )
    }

    @Test
    fun `F5_6 받아쓰기가 실패하면 글 분석을 부르지 않고 실패를 그대로 돌려준다`() = runTest {
        val deaf = FakeDeafProvider()
        val failing = object : Transcriber {
            override suspend fun transcribe(recording: Recording) = AnalysisResult.Failed(AnalysisError.Retryable)
        }

        val result = router(FakeHearingAnalyzer(AnalysisResult.Success(audioFeedback)), deaf, failing).analyze(answer, deaf.defaultModel, prompt, recording())

        assertEquals(AnalysisResult.Failed(AnalysisError.Retryable), result)
        assertEquals(emptyList(), deaf.analyzedTranscripts)
    }

    @Test
    fun `단어가 하나도 없는 전문은 분당 단어 수가 0 이다`() = runTest {
        val deaf = FakeDeafProvider(transcript = Transcript(text = "", words = emptyList<TimedWord>()))

        val result = router(FakeHearingAnalyzer(AnalysisResult.Success(audioFeedback)), deaf).analyze(answer, deaf.defaultModel, prompt, recording())

        assertEquals(0.0, feedbackOf(result).wordsPerMinute)
    }

    private fun answerOf(result: AnalysisResult<Answer>) = assertIs<AnalysisResult.Success<Answer>>(result).value

    private fun feedbackOf(result: AnalysisResult<Answer>) = assertIs<AnswerAnalysis.Done>(answerOf(result).analysis).feedback
}
