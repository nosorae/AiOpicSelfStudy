package io.github.nosorae.aiopic.engine.aigemini

import io.github.nosorae.aiopic.engine.answer.port.AnalysisResult
import io.github.nosorae.aiopic.engine.answer.port.Prompt
import io.github.nosorae.aiopic.engine.answer.port.Recording
import io.github.nosorae.aiopic.engine.answer.port.SavedResponses
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.AnalysisError
import io.github.nosorae.aiopic.engine.session.model.GradeScale
import io.github.nosorae.aiopic.libs.llmgemini.GeminiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** 받은 응답 원문을 해석하기 전에 남기고, 남은 원문이 있으면 다시 부르지 않는다(#205). 저장한 응답과 가짜 클라이언트로만 돈다. */
class GeminiSavedResponseTest {
    private val model = GeminiModels.defaultModel
    private val prompt = Prompt(version = "answer-v1", text = "Analyze this answer.", responseSchema = """{"type":"object"}""")
    private val question = Question(id = "q1", text = "Please introduce yourself.", type = QuestionType("self-intro"), topic = "self")
    private val recording = Recording(bytes = byteArrayOf(1, 2, 3), mimeType = "audio/mp4", resumedAt = emptyList())
    private val scale = GradeScale(listOf("NL", "NM", "NH", "IL", "IM1", "IM2", "IM3", "IH", "AL"))

    /** DB 대신 단계마다 원문 하나. */
    private val stored = mutableMapOf<String, String>()
    private var requests = 0

    private fun analyzer(body: String = GeminiFixtures.ANSWER) = GeminiAnalyzer(
        client = GeminiClient(
            HttpClient(
                MockEngine {
                    requests++
                    respond(body)
                },
            ),
        ),
        apiKey = { "test-key" },
    )

    private fun saved() = SavedResponses({ stored[it] }, { stage, text -> stored[stage] = text })

    @Test
    fun `205 받은 원문을 남기고 다시 실행하면 남은 원문으로 끝내 다시 부르지 않는다`() = runTest {
        withContext(saved()) { analyzer().analyze(model, prompt, question, recording) }
        assertTrue(stored.getValue("answer").contains("transcript"))

        val again = withContext(saved()) { analyzer().analyze(model, prompt, question, recording) }

        assertIs<AnalysisResult.Success<*>>(again)
        assertEquals(1, requests)
    }

    @Test
    fun `205 남은 원문을 읽을 수 없으면 한 번 새로 받는다`() = runTest {
        stored["answer"] = """{"transcript":"Hi, my name"""

        val result = withContext(saved()) { analyzer().analyze(model, prompt, question, recording) }

        assertIs<AnalysisResult.Success<*>>(result)
        assertEquals(1, requests)
    }

    @Test
    fun `205 회차 등급도 원문을 남기고 다시 부르지 않는다`() = runTest {
        withContext(saved()) { analyzer(GeminiFixtures.SESSION_GRADE).grade(model, prompt, scale, listOf(recording)) }

        val again = withContext(saved()) { analyzer(GeminiFixtures.SESSION_GRADE).grade(model, prompt, scale, listOf(recording)) }

        assertIs<AnalysisResult.Success<*>>(again)
        assertEquals(1, requests)
        assertTrue("grade" in stored)
    }

    @Test
    fun `205 연결이 없으면 오프라인으로 돌려줘 재시도 횟수를 쓰지 않게 한다`() = runTest {
        val offline = GeminiAnalyzer(GeminiClient(HttpClient(MockEngine { throw IOException("no network") })), apiKey = { "test-key" })

        assertEquals(AnalysisResult.Failed(AnalysisError.Offline), offline.analyze(model, prompt, question, recording))
    }
}
