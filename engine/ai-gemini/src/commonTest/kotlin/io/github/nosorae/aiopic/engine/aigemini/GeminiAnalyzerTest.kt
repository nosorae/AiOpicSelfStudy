package io.github.nosorae.aiopic.engine.aigemini

import io.github.nosorae.aiopic.engine.answer.model.ContentFix
import io.github.nosorae.aiopic.engine.answer.model.Feedback
import io.github.nosorae.aiopic.engine.answer.model.Pronunciation
import io.github.nosorae.aiopic.engine.answer.model.Stability
import io.github.nosorae.aiopic.engine.answer.model.UnscorableReason
import io.github.nosorae.aiopic.engine.answer.port.AnalysisResult
import io.github.nosorae.aiopic.engine.answer.port.Prompt
import io.github.nosorae.aiopic.engine.answer.port.Recording
import io.github.nosorae.aiopic.engine.question.model.Question
import io.github.nosorae.aiopic.engine.question.model.QuestionType
import io.github.nosorae.aiopic.engine.session.model.AnalysisError
import io.github.nosorae.aiopic.engine.session.model.GradeCheck
import io.github.nosorae.aiopic.engine.session.model.GradeScale
import io.github.nosorae.aiopic.engine.session.model.SessionGrade
import io.github.nosorae.aiopic.libs.llmgemini.GeminiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GeminiAnalyzerTest {
    private val model = GeminiModels.defaultModel
    private val prompt = Prompt(version = "answer-v1", text = "Analyze this answer.", responseSchema = """{"type":"object"}""")
    private val question = Question(id = "q1", text = "Please introduce yourself.", type = QuestionType("self-intro"), topic = "self")
    private val recording = Recording(bytes = byteArrayOf(1, 2, 3), mimeType = "audio/mp4", resumedAt = emptyList())
    private val scale = GradeScale(listOf("NL", "NM", "NH", "IL", "IM1", "IM2", "IM3", "IH", "AL"))

    private val sent = mutableListOf<HttpRequestData>()

    /** 응답 본문을 차례로 돌려준다. 마지막 것은 그 뒤로도 계속 돌려준다. */
    private fun analyzer(vararg bodies: Pair<HttpStatusCode, String>, key: String? = "test-key") = GeminiAnalyzer(
        client = GeminiClient(
            HttpClient(
                MockEngine { request ->
                    sent += request
                    val (status, body) = bodies[minOf(sent.size, bodies.size) - 1]
                    respond(body, status)
                },
            ),
        ),
        apiKey = { key },
    )

    private fun ok(body: String) = HttpStatusCode.OK to body

    /** 모델이 [text] 를 돌려준 응답 본문. */
    private fun envelope(text: String) = buildJsonObject {
        putJsonArray("candidates") { addJsonObject { putJsonObject("content") { putJsonArray("parts") { addJsonObject { put("text", text) } } } } }
    }.toString()

    private val realAnswerText = Json.parseToJsonElement(GeminiFixtures.ANSWER).jsonObject.getValue("candidates").jsonArray[0]
        .jsonObject.getValue("content").jsonObject.getValue("parts").jsonArray[0].jsonObject.getValue("text").jsonPrimitive.content

    /** 실제 답변 응답에 전문에 없는 교정 하나를 넣은 글. */
    private val answerWithMadeUpCorrection = Json.parseToJsonElement(realAnswerText).jsonObject.let { answer ->
        buildJsonObject {
            answer.forEach { (key, value) -> if (key != "corrections") put(key, value) }
            putJsonArray("corrections") {
                addJsonObject {
                    put("original", "I has went")
                    put("corrected", "I went.")
                }
            }
        }.toString()
    }

    @Test
    fun `F5_2 저장한 실제 답변 응답을 피드백으로 읽는다`() = runTest {
        val result = analyzer(ok(GeminiFixtures.ANSWER)).analyze(model, prompt, question, recording)

        val feedback = assertIs<AnalysisResult.Success<*>>(result).value as Feedback
        assertEquals(
            listOf(16, 141.0, 100, true, null),
            listOf(feedback.sentenceCount, feedback.wordsPerMinute, feedback.score, feedback.tenseConsistent, feedback.roleplayQuestionCount),
        )
        assertTrue(feedback.transcript.startsWith("Hi, my name is Alex"))
        assertIs<Pronunciation.Evaluated>(feedback.pronunciation)
    }

    @Test
    fun `217 내용 교정과 최종 스크립트를 피드백에 담는다`() = runTest {
        val text = Json.parseToJsonElement(realAnswerText).jsonObject.let { answer ->
            buildJsonObject {
                answer.forEach { (key, value) -> put(key, value) }
                putJsonArray("contentFixes") {
                    addJsonObject {
                        put("original", "I usually work from home")
                        put("problem", "왜 집에서 일하는지 이유가 없다.")
                        put("corrected", "I usually work from home because my team is spread across cities.")
                    }
                }
                put("finalScript", "Hi, my name is Alex.")
            }.toString()
        }

        val feedback = assertIs<AnalysisResult.Success<Feedback>>(analyzer(ok(envelope(text))).analyze(model, prompt, question, recording)).value
        assertEquals(listOf("I usually work from home") to "Hi, my name is Alex.", feedback.contentFixes.map { it.original } to feedback.finalScript)
    }

    @Test
    fun `217 두 항목이 없는 옛 응답은 빈 목록과 null 로 읽는다`() = runTest {
        val feedback = assertIs<AnalysisResult.Success<Feedback>>(analyzer(ok(GeminiFixtures.ANSWER)).analyze(model, prompt, question, recording)).value

        assertEquals(emptyList<ContentFix>() to null, feedback.contentFixes to feedback.finalScript)
    }

    @Test
    fun `F5_3 저장한 실제 회차 등급 응답을 체크리스트와 등급 범위로 읽는다`() = runTest {
        val result = analyzer(ok(GeminiFixtures.SESSION_GRADE)).grade(model, prompt, scale, List(3) { recording })

        val grade = assertIs<AnalysisResult.Success<SessionGrade>>(result).value
        assertEquals(Triple("IH", "IH", 10), Triple(grade.low, grade.high, grade.checks.size))
        assertEquals(GradeCheck("C3", met = false), grade.checks[2])
    }

    @Test
    fun `F5_4 전문에 없는 교정은 한 번 다시 요청하고 그래도 있으면 버린다`() = runTest {
        val result = analyzer(ok(envelope(answerWithMadeUpCorrection))).analyze(model, prompt, question, recording)

        val feedback = assertIs<AnalysisResult.Success<Feedback>>(result).value
        assertEquals(emptyList(), feedback.corrections)
        assertEquals(2, sent.size)
    }

    /** 실제 답변 응답의 점수를 70 으로 바꾸고 전문에 있는 교정 하나에 [deducted] 를 붙인 글. */
    private fun answerWithDeducted(deducted: Int) = Json.parseToJsonElement(realAnswerText).jsonObject.let { answer ->
        buildJsonObject {
            answer.forEach { (key, value) -> if (key != "corrections" && key != "score") put(key, value) }
            put("score", 70)
            putJsonArray("corrections") {
                addJsonObject {
                    put("original", "I usually work from home")
                    put("corrected", "I usually work from home.")
                    put("deducted", deducted)
                }
            }
        }.toString()
    }

    @Test
    fun `F8_2 교정마다 깎은 점수를 피드백에 담는다`() = runTest {
        val result = analyzer(ok(envelope(answerWithDeducted(20)))).analyze(model, prompt, question, recording)

        assertEquals(listOf(20), assertIs<AnalysisResult.Success<Feedback>>(result).value.corrections.map { it.deducted })
        assertEquals(1, sent.size)
    }

    @Test
    fun `F8_2 깎은 점수의 합이 100 - score 를 넘으면 다시 요청하고 그래도 넘으면 깎은 점수만 버린다`() = runTest {
        val result = analyzer(ok(envelope(answerWithDeducted(31)))).analyze(model, prompt, question, recording)

        assertEquals(listOf(null), assertIs<AnalysisResult.Success<Feedback>>(result).value.corrections.map { it.deducted })
        assertEquals(2, sent.size)
    }

    @Test
    fun `F5_4 다시 요청한 응답이 통과하면 그 응답을 쓴다`() = runTest {
        val result = analyzer(ok(envelope(answerWithMadeUpCorrection)), ok(GeminiFixtures.ANSWER)).analyze(model, prompt, question, recording)

        assertEquals(16, (result as AnalysisResult.Success).value.sentenceCount)
    }

    @Test
    fun `F5_4 등급 체계에 없는 등급이 두 번 오면 거절한다`() = runTest {
        val bad = envelope("""{"checks":[],"low":"IH","high":"S","reasons":[],"brokeAt":[]}""")

        assertEquals(AnalysisResult.Failed(AnalysisError.Rejected), analyzer(ok(bad)).grade(model, prompt, scale, listOf(recording)))
    }

    @Test
    fun `F5_5 평가할 수 없다는 이유 코드를 돌려받으면 Unscorable 이다`() = runTest {
        val tooShort = envelope(
            """{"transcript":"","corrections":[],"upgrades":[],"tenseConsistent":false,"sentenceCount":0,"wordsPerMinute":0,"pauses":[],"pronunciation":"","score":0,"unscorableReason":"too_short"}""",
        )

        assertEquals(AnalysisResult.Unscorable(UnscorableReason.TooShort), analyzer(ok(tooShort)).analyze(model, prompt, question, recording))
    }

    @Test
    fun `F5_8 고른 모델이 없다는 응답이면 ModelUnavailable 이다`() = runTest {
        val result = analyzer(HttpStatusCode.NotFound to """{"error":{"code":404,"status":"NOT_FOUND"}}""").analyze(model, prompt, question, recording)

        assertEquals(AnalysisResult.Failed(AnalysisError.ModelUnavailable), result)
    }

    @Test
    fun `F5_6 429 는 다시 보낼 수 있는 실패다`() = runTest {
        val result = analyzer(HttpStatusCode.TooManyRequests to "{}").analyze(model, prompt, question, recording)

        assertEquals(AnalysisResult.Failed(AnalysisError.Retryable), result)
    }

    @Test
    fun `키가 없으면 요청하지 않고 InvalidKey 다`() = runTest {
        val result = analyzer(ok(GeminiFixtures.ANSWER), key = null).analyze(model, prompt, question, recording)

        assertEquals(AnalysisResult.Failed(AnalysisError.InvalidKey) to 0, result to sent.size)
    }

    @Test
    fun `JSON 이 아닌 글이 오면 거절한다`() = runTest {
        assertEquals(AnalysisResult.Failed(AnalysisError.Rejected), analyzer(ok(envelope("| table |"))).analyze(model, prompt, question, recording))
    }

    @Test
    fun `고른 모델로 프롬프트와 녹음과 스키마를 보낸다`() = runTest {
        analyzer(ok(GeminiFixtures.ANSWER)).analyze(model, prompt, question, recording)

        val body = Json.parseToJsonElement((sent.single().body as TextContent).text).jsonObject
        val parts = body.getValue("contents").jsonArray[0].jsonObject.getValue("parts").jsonArray
        assertTrue(sent.single().url.encodedPath.endsWith("/models/${model.id}:generateContent"))
        assertEquals("Analyze this answer.", parts[0].jsonObject.getValue("text").jsonPrimitive.content)
        assertEquals("audio/mp4", parts[1].jsonObject.getValue("inlineData").jsonObject.getValue("mimeType").jsonPrimitive.content)
        assertTrue("generationConfig" in body)
    }

    @Test
    fun `F5_3 회차 등급은 녹음을 모두 한 요청에 넣는다`() = runTest {
        analyzer(ok(GeminiFixtures.SESSION_GRADE)).grade(model, prompt, scale, List(15) { recording })

        val parts = Json.parseToJsonElement((sent.single().body as TextContent).text).jsonObject.getValue("contents").jsonArray[0].jsonObject.getValue("parts").jsonArray
        assertEquals(16, parts.size)
    }

    @Test
    fun `F5_7 기본 모델은 목록 안의 안정 모델이다`() {
        assertEquals(Stability.Stable to true, GeminiModels.defaultModel.stability to (GeminiModels.defaultModel in GeminiModels.models))
    }

    @Test
    fun `F5_8 목록의 모델은 모두 gemini 제공사이고 음성을 직접 듣는다`() {
        assertTrue(GeminiModels.models.all { it.provider == GeminiModels.PROVIDER && it.hearsAudio })
    }

    @Test
    fun `목록의 모델 id 는 겹치지 않는다`() {
        assertEquals(GeminiModels.models.size, GeminiModels.models.map { it.id }.toSet().size)
    }

    private fun sentGenerationConfig() = Json.parseToJsonElement((sent.last().body as TextContent).text).jsonObject.getValue("generationConfig").jsonObject

    @Test
    fun `216 답변 분석은 생각 수준을 낮춰 보낸다`() = runTest {
        analyzer(ok(GeminiFixtures.ANSWER)).analyze(model, prompt, question, recording)

        assertEquals("""{"thinkingLevel":"low"}""", sentGenerationConfig()["thinkingConfig"].toString())
    }

    @Test
    fun `216 회차 등급은 모델 기본 생각 수준으로 보낸다`() = runTest {
        analyzer(ok(GeminiFixtures.SESSION_GRADE)).grade(model, prompt, scale, List(3) { recording })

        assertEquals(null, sentGenerationConfig()["thinkingConfig"])
    }
}
