package io.github.nosorae.aiopic.engine.aigemini

import io.github.nosorae.aiopic.engine.answer.port.Prompt
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.libs.llmgemini.GeminiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 출제 응답을 질문 목록으로 읽는다(F6.1). 저장한 실제 응답과 가짜 클라이언트로만 돈다. */
class GeminiQuestionWriterTest {
    private val prompt = Prompt(version = "questions-v2", text = "Write questions.", responseSchema = """{"type":"object"}""")

    /** 저장한 응답을 만든 요청의 순서. 0번이 연습 문항이다. */
    private val order = (listOf("self-intro", "self-intro") + List(3) { listOf("description", "routine", "past-experience") }.flatten() + listOf("roleplay-ask", "roleplay-solve")).map(::QuestionType)

    private var requests = 0

    private fun writer(body: String, status: HttpStatusCode = HttpStatusCode.OK, key: String? = "test-key") = GeminiQuestionWriter(
        client = GeminiClient(
            HttpClient(
                MockEngine {
                    requests++
                    respond(body, status)
                },
            ),
        ),
        apiKey = { key },
    )

    @Test
    fun `F6_1 저장한 응답을 번호 순서의 질문 목록으로 읽고 id 앞에 회차 표시를 붙인다`() = runTest {
        val questions = writer(GeminiFixtures.QUESTIONS).write(GeminiModels.defaultModel, prompt, order, idPrefix = "s1-")!!

        assertEquals(order, questions.map { it.type })
        assertEquals((0..12).map { "s1-$it" }, questions.map { it.id })
        assertEquals("self", questions.first().topic)
        assertEquals("cafes", questions[2].topic)
    }

    @Test
    fun `F6_2 유형 순서가 다른 응답은 거절한다`() = runTest {
        assertNull(writer(GeminiFixtures.QUESTIONS).write(GeminiModels.defaultModel, prompt, order.reversed(), idPrefix = "s1-"))
    }

    @Test
    fun `F6_2 요청이 실패하거나 JSON 을 못 읽거나 키가 없으면 null 이다`() = runTest {
        assertNull(writer("{}", HttpStatusCode.ServiceUnavailable).write(GeminiModels.defaultModel, prompt, order, "s1-"))
        assertNull(writer(GeminiFixtures.ANSWER).write(GeminiModels.defaultModel, prompt, order, "s1-"))
        assertEquals(2, requests)
        assertNull(writer(GeminiFixtures.QUESTIONS, key = null).write(GeminiModels.defaultModel, prompt, order, "s1-"))
        assertEquals(2, requests)
    }
}
