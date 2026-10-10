package io.github.nosorae.aiopic.libs.llmgemini

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 질문 음성(#161). 실패는 모두 null 이고 부르는 쪽이 기기 음성으로 읽는다. */
class GeminiSpeechTest {
    private val sent = mutableListOf<HttpRequestData>()

    private fun client(status: HttpStatusCode, body: String) = GeminiClient(
        HttpClient(
            MockEngine { request ->
                sent += request
                respond(body, status)
            },
        ),
    )

    @Test
    fun `받은 오디오를 그대로 돌려준다`() = runTest {
        // "RIFF" 를 base64 로 담은 응답. 실제 응답은 24kHz 모노 16비트 WAV 다.
        val audio = client(HttpStatusCode.OK, """{"candidates":[{"content":{"parts":[{"inlineData":{"mimeType":"audio/wav","data":"UklGRg=="}}]}}]}""")
            .speech(apiKey = "test-key", model = "test-tts", voice = "Kore", text = "Tell me about yourself.")

        assertContentEquals("RIFF".encodeToByteArray(), audio)
    }

    @Test
    fun `목소리와 오디오 응답을 요청하고 키는 헤더로 보낸다`() = runTest {
        client(HttpStatusCode.OK, "{}").speech("test-key", "test-tts", "Kore", "Hi.")

        val request = sent.single()
        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/test-tts:generateContent", request.url.toString())
        assertEquals("test-key", request.headers["x-goog-api-key"])
        val config = Json.parseToJsonElement((request.body as TextContent).text).jsonObject["generationConfig"].toString()
        assertEquals("""{"responseModalities":["AUDIO"],"speechConfig":{"voiceConfig":{"prebuiltVoiceConfig":{"voiceName":"Kore"}}}}""", config)
        assertTrue(""""text":"Hi."""" in (request.body as TextContent).text)
    }

    @Test
    fun `한도 초과나 오디오 없는 응답은 null 이다`() = runTest {
        assertNull(client(HttpStatusCode.TooManyRequests, "{}").speech("test-key", "test-tts", "Kore", "Hi."))
        assertNull(client(HttpStatusCode.OK, """{"candidates":[{"content":{"parts":[{"text":"no"}]}}]}""").speech("test-key", "test-tts", "Kore", "Hi."))
    }
}
