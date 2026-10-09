package io.github.nosorae.aiopic.engine.answer.model

import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AnswerTest {
    private val question = Question(id = "q1", text = "Tell me about your house.", type = QuestionType("description"), topic = "home")
    private val analyzedBy = AnalyzedBy(provider = "gemini", modelId = "test-model", promptVersion = "answer-v1")

    private fun feedback(pronunciation: Pronunciation) = Feedback(
        transcript = "I live in a apartment.",
        corrections = listOf(Correction(original = "a apartment", corrected = "I live in an apartment.")),
        upgrades = listOf(Upgrade(original = "I live in an apartment.", better = "I've been living in a cozy apartment.")),
        tenseConsistent = true,
        sentenceCount = 1,
        wordsPerMinute = 120.5,
        pauses = listOf(Pause(start = 1.5.seconds, length = 800.milliseconds)),
        roleplayQuestionCount = null,
        pronunciation = pronunciation,
        score = 70,
    )

    private fun answer(analysis: AnswerAnalysis) = Answer(
        id = "a1",
        question = question,
        examSessionId = "s1",
        chunkPaths = listOf("a1/0.m4a", "a1/1.m4a"),
        resumedAt = listOf(5.seconds),
        analysis = analysis,
    )

    private fun roundTrip(answer: Answer) = Json.decodeFromString<Answer>(Json.encodeToString(answer))

    @Test
    fun `분석 전 답변은 저장했다 읽어도 같다`() {
        val answer = answer(AnswerAnalysis.Pending)

        assertEquals(answer, roundTrip(answer))
    }

    @Test
    fun `F5_7 분석한 답변은 제공사와 모델 id 와 promptVersion 까지 저장했다 읽어도 같다`() {
        val answer = answer(AnswerAnalysis.Done(analyzedBy, feedback(Pronunciation.Evaluated("Clear vowels."))))

        assertEquals(answer, roundTrip(answer))
    }

    @Test
    fun `F5_9 발음을 평가하지 않은 피드백도 저장했다 읽어도 같다`() {
        val answer = answer(AnswerAnalysis.Done(analyzedBy, feedback(Pronunciation.NotEvaluated)))

        assertEquals(answer, roundTrip(answer))
    }

    @Test
    fun `F5_5 평가할 수 없는 답변은 이유 코드와 함께 저장했다 읽어도 같다`() {
        val answer = answer(AnswerAnalysis.Unscorable(analyzedBy, UnscorableReason.TooShort))

        assertEquals(answer, roundTrip(answer))
    }

    @Test
    fun `한 문항 연습 답변은 회차 id 가 없다`() {
        val answer = answer(AnswerAnalysis.Pending).copy(examSessionId = null)

        assertEquals(answer, roundTrip(answer))
    }

    @Test
    fun `F5_8 모델 정보는 저장했다 읽어도 같다`() {
        val model = ModelInfo(provider = "gemini", id = "test-model", displayName = "Test", hearsAudio = true, stability = Stability.Preview)

        assertEquals(model, Json.decodeFromString<ModelInfo>(Json.encodeToString(model)))
    }

    @Test
    fun `분석 결과 종류는 JSON 의 type 값으로 구분한다`() {
        val json = Json.encodeToString<AnswerAnalysis>(AnswerAnalysis.Pending)

        assertTrue("\"type\":\"pending\"" in json, json)
    }
}
