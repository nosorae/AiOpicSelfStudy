package io.github.nosorae.aiopic.engine.answer.port

import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.seconds

class FakeDeafProviderTest {
    private val question = Question(id = "q1", text = "Tell me about your house.", type = QuestionType("description"), topic = "home")
    private val prompt = Prompt(version = "answer-v1", text = "Analyze.", responseSchema = "{}")
    private val recording = Recording(bytes = byteArrayOf(1, 2), mimeType = "audio/mp4", resumedAt = emptyList())

    @Test
    fun `F5_9 음성을 못 듣는 가짜 제공사의 모델은 음성 직접 분석이 꺼져 있다`() {
        val provider = FakeDeafProvider()

        assertFalse(provider.defaultModel.hearsAudio)
    }

    @Test
    fun `F5_9 받아쓴 전문을 글 분석에 넘기면 그 전문으로 분석한다`() = runTest {
        val provider = FakeDeafProvider()
        val transcript = (provider.transcribe(recording) as AnalysisResult.Success).value

        val result = provider.analyze(provider.defaultModel, prompt, question, transcript)

        assertEquals(AnalysisResult.Success(provider.analysis), result)
        assertEquals(listOf(transcript), provider.analyzedTranscripts)
    }

    @Test
    fun `받아쓰기 결과에 단어마다 시각이 있다`() = runTest {
        val provider = FakeDeafProvider()

        val transcript = (provider.transcribe(recording) as AnalysisResult.Success).value

        assertEquals(TimedWord("I", 0.seconds, 0.2.seconds), transcript.words.first())
    }
}
