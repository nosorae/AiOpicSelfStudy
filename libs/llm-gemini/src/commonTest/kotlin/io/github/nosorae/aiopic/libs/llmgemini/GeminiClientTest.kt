package io.github.nosorae.aiopic.libs.llmgemini

import io.github.nosorae.aiopic.libs.llmgemini.api.AudioPart
import io.github.nosorae.aiopic.libs.llmgemini.api.LlmRequest
import io.github.nosorae.aiopic.libs.llmgemini.api.LlmResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class GeminiClientTest {
    private val request = LlmRequest(
        apiKey = "test-key",
        model = "test-model",
        prompt = "Analyze.",
        audio = listOf(AudioPart(byteArrayOf(1, 2, 3), "audio/mp4")),
        responseSchema = """{"type":"object"}""",
    )

    private val sent = mutableListOf<HttpRequestData>()

    private fun client(status: HttpStatusCode, body: String) = GeminiClient(
        HttpClient(
            MockEngine { request ->
                sent += request
                respond(body, status)
            },
        ),
    )

    private fun sentBody(): JsonObject = Json.parseToJsonElement((sent.single().body as TextContent).text).jsonObject

    @Test
    fun `모델이 돌려준 글을 그대로 돌려준다`() = runTest {
        val result = client(HttpStatusCode.OK, """{"candidates":[{"content":{"parts":[{"text":"{\"a\":1}","thoughtSignature":"x"}]}}]}""").generate(request)

        assertEquals(LlmResult.Success("""{"a":1}"""), result)
    }

    @Test
    fun `생각 조각은 결과 글에 넣지 않는다`() = runTest {
        val result = client(HttpStatusCode.OK, """{"candidates":[{"content":{"parts":[{"text":"thinking","thought":true},{"text":"answer"}]}}]}""").generate(request)

        assertEquals(LlmResult.Success("answer"), result)
    }

    @Test
    fun `후보가 없는 응답은 거절로 돌려준다`() = runTest {
        val result = client(HttpStatusCode.OK, """{"promptFeedback":{"blockReason":"SAFETY"}}""").generate(request)

        assertEquals(LlmResult.Rejected(200, "SAFETY"), result)
    }

    @Test
    fun `키는 주소가 아니라 헤더로 보낸다`() = runTest {
        client(HttpStatusCode.OK, """{"candidates":[]}""").generate(request)

        assertEquals("test-key" to false, sent.single().headers["x-goog-api-key"] to ("test-key" in sent.single().url.toString()))
    }

    @Test
    fun `고른 모델의 generateContent 로 보낸다`() = runTest {
        client(HttpStatusCode.OK, """{"candidates":[]}""").generate(request)

        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/test-model:generateContent", sent.single().url.toString())
    }

    @Test
    fun `스키마를 주면 JSON 응답 형식과 스키마를 함께 보낸다`() = runTest {
        client(HttpStatusCode.OK, """{"candidates":[]}""").generate(request)

        assertEquals(
            """{"text":{"mimeType":"APPLICATION_JSON","schema":{"type":"object"}}}""",
            sentBody().getValue("generationConfig").jsonObject.getValue("responseFormat").toString(),
        )
    }

    @Test
    fun `스키마가 없으면 생성 설정을 보내지 않는다`() = runTest {
        client(HttpStatusCode.OK, """{"candidates":[]}""").generate(request.copy(responseSchema = null))

        assertEquals(false, "generationConfig" in sentBody())
    }

    @Test
    fun `녹음은 프롬프트 뒤에 base64 로 넣는다`() = runTest {
        client(HttpStatusCode.OK, """{"candidates":[]}""").generate(request)

        val parts = sentBody().getValue("contents").jsonArray.single().jsonObject.getValue("parts").jsonArray
        assertEquals(
            listOf("""{"text":"Analyze."}""", """{"inlineData":{"mimeType":"audio/mp4","data":"AQID"}}"""),
            parts.map { it.toString() },
        )
    }

    @Test
    fun `F5_8 모델이 없다는 실제 응답은 ModelNotFound 다`() = runTest {
        assertEquals(LlmResult.ModelNotFound, client(HttpStatusCode.NotFound, GeminiErrorFixtures.MODEL_NOT_FOUND).generate(request))
    }

    @Test
    fun `키가 틀렸다는 실제 응답은 InvalidKey 다`() = runTest {
        assertEquals(LlmResult.InvalidKey, client(HttpStatusCode.BadRequest, GeminiErrorFixtures.INVALID_KEY).generate(request))
    }

    @Test
    fun `F5_6 하루 한도 초과 실제 응답은 다시 보낼 수 있다`() = runTest {
        assertEquals(LlmResult.Retryable(429), client(HttpStatusCode.TooManyRequests, GeminiErrorFixtures.QUOTA).generate(request))
    }

    @Test
    fun `F5_6 모델 과부하 실제 응답은 다시 보낼 수 있다`() = runTest {
        assertEquals(LlmResult.Retryable(503), client(HttpStatusCode.ServiceUnavailable, GeminiErrorFixtures.OVERLOADED).generate(request))
    }

    @Test
    fun `F5_6 연결이 없으면 상태 코드 없이 다시 보낼 수 있다`() = runTest {
        val offline = GeminiClient(HttpClient(MockEngine { throw IOException("offline") }))

        assertEquals(LlmResult.Retryable(null), offline.generate(request))
    }

    @Test
    fun `그 밖의 400 은 오류 메시지와 함께 거절로 돌려준다`() = runTest {
        val result = client(HttpStatusCode.BadRequest, """{"error":{"code":400,"message":"Invalid schema.","status":"INVALID_ARGUMENT"}}""").generate(request)

        assertEquals(LlmResult.Rejected(400, "Invalid schema."), result)
    }

    @Test
    fun `216 생각 수준을 주면 생성 설정에 넣는다`() = runTest {
        client(HttpStatusCode.OK, """{"candidates":[]}""").generate(request.copy(responseSchema = null, thinkingLevel = "low"))

        assertEquals("""{"thinkingConfig":{"thinkingLevel":"low"}}""", sentBody().getValue("generationConfig").toString())
    }

    @Test
    fun `216 생각 수준이 없으면 모델 기본값을 쓴다`() = runTest {
        client(HttpStatusCode.OK, """{"candidates":[]}""").generate(request)

        assertEquals(false, "thinkingConfig" in sentBody().getValue("generationConfig").jsonObject)
    }

    @Test
    fun `216 녹음을 실은 요청은 플랫폼 업로드로 보내고 응답을 그대로 해석한다`() = runTest {
        val uploaded = mutableListOf<Triple<String, String, String>>()
        val upload = GeminiUpload { url, apiKey, body ->
            uploaded += Triple(url, apiKey, body)
            RawResponse(200, """{"candidates":[{"content":{"parts":[{"text":"done"}]}}]}""")
        }
        val client = GeminiClient(
            HttpClient(
                MockEngine { request ->
                    sent += request
                    respond("", HttpStatusCode.OK)
                },
            ),
            upload,
        )

        val result = client.generate(request)

        assertEquals(LlmResult.Success("done"), result)
        assertEquals(emptyList(), sent)
        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/test-model:generateContent" to "test-key", uploaded.single().first to uploaded.single().second)
        assertEquals(true, "inlineData" in uploaded.single().third)
    }

    @Test
    fun `216 녹음이 없는 요청은 업로드를 쓰지 않는다`() = runTest {
        val client = GeminiClient(
            HttpClient(
                MockEngine { request ->
                    sent += request
                    respond("""{"candidates":[]}""", HttpStatusCode.OK)
                },
            ),
            { _, _, _ -> error("업로드를 쓰면 안 된다") },
        )

        client.generate(request.copy(audio = emptyList()))

        assertEquals(1, sent.size)
    }

    @Test
    fun `216 업로드가 연결하지 못하면 상태 코드 없이 다시 보낼 수 있다`() = runTest {
        val client = GeminiClient(HttpClient(MockEngine { error("쓰지 않는다") }), { _, _, _ -> null })

        assertEquals(LlmResult.Retryable(null), client.generate(request))
    }
}
