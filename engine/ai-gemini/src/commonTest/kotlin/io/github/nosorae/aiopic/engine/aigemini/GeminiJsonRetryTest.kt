package io.github.nosorae.aiopic.engine.aigemini

import io.github.nosorae.aiopic.engine.answer.port.AnalysisError
import io.github.nosorae.aiopic.engine.answer.port.AnalysisResult
import io.github.nosorae.aiopic.engine.answer.port.Prompt
import io.github.nosorae.aiopic.engine.answer.port.Recording
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.GradeScale
import io.github.nosorae.aiopic.libs.llmgemini.GeminiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** 읽을 수 없는 JSON 응답을 한 번 다시 요청한다(F5.4). 저장한 응답과 가짜 클라이언트로만 돈다. */
class GeminiJsonRetryTest {
    private val model = GeminiModels.defaultModel
    private val prompt = Prompt(version = "answer-v1", text = "Analyze this answer.", responseSchema = """{"type":"object"}""")
    private val question = Question(id = "q1", text = "Please introduce yourself.", type = QuestionType("self-intro"), topic = "self")
    private val recording = Recording(bytes = byteArrayOf(1, 2, 3), mimeType = "audio/mp4", resumedAt = emptyList())
    private val scale = GradeScale(listOf("NL", "NM", "NH", "IL", "IM1", "IM2", "IM3", "IH", "AL"))

    /** 출력 토큰 한도에 걸려 중간에 끊긴 응답. */
    private val truncated = envelope("""{"transcript":"Hi, my name is Alex and I am""")

    private var requests = 0

    /** 응답 본문을 차례로 돌려준다. 마지막 것은 그 뒤로도 계속 돌려준다. */
    private fun analyzer(vararg bodies: String) = GeminiAnalyzer(
        client = GeminiClient(HttpClient(MockEngine { respond(bodies[minOf(++requests, bodies.size) - 1]) })),
        apiKey = { "test-key" },
    )

    private fun envelope(text: String) = buildJsonObject {
        putJsonArray("candidates") { addJsonObject { putJsonObject("content") { putJsonArray("parts") { addJsonObject { put("text", text) } } } } }
    }.toString()

    @Test
    fun `F5_4 잘린 JSON 다음에 맞는 응답이 오면 성공하고 요청은 2번이다`() = runTest {
        val result = analyzer(truncated, GeminiFixtures.ANSWER).analyze(model, prompt, question, recording)

        assertIs<AnalysisResult.Success<*>>(result)
        assertEquals(2, requests)
    }

    @Test
    fun `F5_4 두 번 다 깨지면 Rejected 이고 요청은 2번에서 멈춘다`() = runTest {
        val result = analyzer(truncated).analyze(model, prompt, question, recording)

        assertEquals(AnalysisResult.Failed(AnalysisError.Rejected) to 2, result to requests)
    }

    @Test
    fun `F5_4 처음부터 맞으면 요청은 1번이다`() = runTest {
        analyzer(GeminiFixtures.ANSWER).analyze(model, prompt, question, recording)

        assertEquals(1, requests)
    }

    @Test
    fun `F5_4 회차 등급도 잘린 JSON 이면 한 번 다시 요청한다`() = runTest {
        val result = analyzer(truncated, GeminiFixtures.SESSION_GRADE).grade(model, prompt, scale, listOf(recording))

        assertIs<AnalysisResult.Success<*>>(result)
        assertEquals(2, requests)
    }

    @Test
    fun `F5_4 회차 등급이 두 번 다 깨지면 Rejected 이다`() = runTest {
        val result = analyzer(truncated).grade(model, prompt, scale, listOf(recording))

        assertEquals(AnalysisResult.Failed(AnalysisError.Rejected) to 2, result to requests)
    }
}
