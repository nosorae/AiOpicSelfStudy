package io.github.nosorae.aiopic.libs.llmgemini

import io.github.nosorae.aiopic.libs.llmgemini.api.LlmRequest
import io.github.nosorae.aiopic.libs.llmgemini.api.LlmResult
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.io.encoding.Base64

/**
 * Gemini `generateContent` 를 부르는 HTTP 클라이언트. 분석 포트와 프롬프트를 모르고 요청 하나를 결과 하나로 바꾼다.
 * 기본 [HttpClient] 는 플랫폼 엔진(Android HttpURLConnection, iOS NSURLSession)을 쓴다.
 * [upload] 가 있으면 녹음을 실은 요청(분석, 등급)은 그 길로 보낸다. 앱이 멈춰도 전송이 끝나야 하는 요청이다(#216).
 */
public class GeminiClient(private val http: HttpClient = HttpClient(), private val upload: GeminiUpload? = null) {
    public suspend fun generate(request: LlmRequest): LlmResult {
        // 본문은 try 밖에서 만든다. 스키마 글이 JSON 이 아니면 연결 없음으로 숨지 않고 바로 드러나야 한다.
        val body = body(request).toString()
        val response = (
            if (upload != null && request.audio.isNotEmpty()) upload.post(url(request.model), request.apiKey, body) else post(request.apiKey, request.model, body)
            ) ?: return LlmResult.Retryable(null)
        val status = response.status
        val json = runCatching { Json.parseToJsonElement(response.body).jsonObject }.getOrDefault(JsonObject(emptyMap()))
        return when {
            status == 200 -> success(json)
            status == 401 || status == 403 || (status == 400 && "API_KEY_INVALID" in json.toString()) -> LlmResult.InvalidKey
            status == 404 -> LlmResult.ModelNotFound
            status == 408 || status == 429 || status >= 500 -> LlmResult.Retryable(status)
            else -> LlmResult.Rejected(status, json["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull.orEmpty())
        }
    }

    /**
     * [text] 를 [voice] 목소리로 읽은 오디오. 응답 그대로 24kHz 모노 16비트 WAV 다.
     * 연결 없음, 한도 초과, 오디오 없는 응답 모두 null 이다. 부르는 쪽은 기기 음성으로 읽어서 실패 이유를 나누지 않는다.
     */
    public suspend fun speech(apiKey: String, model: String, voice: String, text: String): ByteArray? {
        val body = buildJsonObject {
            putJsonArray("contents") { addJsonObject { putJsonArray("parts") { addJsonObject { put("text", text) } } } }
            putJsonObject("generationConfig") {
                putJsonArray("responseModalities") { add("AUDIO") }
                putJsonObject("speechConfig") { putJsonObject("voiceConfig") { putJsonObject("prebuiltVoiceConfig") { put("voiceName", voice) } } }
            }
        }
        val response = post(apiKey, model, body.toString())?.takeIf { it.status == 200 } ?: return null
        return runCatching {
            val data = Json.parseToJsonElement(response.body).jsonObject["candidates"]!!.jsonArray[0].jsonObject["content"]!!.jsonObject["parts"]!!.jsonArray
                .firstNotNullOf { it.jsonObject["inlineData"]?.jsonObject?.get("data")?.jsonPrimitive?.content }
            Base64.decode(data)
        }.getOrNull()
    }

    /** 연결하지 못하면 null 이다. */
    private suspend fun post(apiKey: String, model: String, body: String): RawResponse? = try {
        http.post(url(model)) {
            // 키를 주소에 넣으면 접속 기록에 남아서 헤더로 보낸다.
            header("x-goog-api-key", apiKey)
            setBody(TextContent(body, ContentType.Application.Json))
        }.let { RawResponse(it.status.value, it.bodyAsText()) }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // ponytail: 연결 오류를 플랫폼마다 가르지 않고 모두 연결 없음으로 본다. 다시 보내도 안 되는 오류가 섞이면 여기서 나눈다.
        null
    }

    private fun success(json: JsonObject): LlmResult {
        val parts = json["candidates"]?.jsonArray?.firstOrNull()?.jsonObject?.get("content")?.jsonObject?.get("parts")?.jsonArray
            ?: return LlmResult.Rejected(200, json["promptFeedback"]?.jsonObject?.get("blockReason")?.jsonPrimitive?.contentOrNull ?: "no candidates")
        return LlmResult.Success(
            parts.map { it.jsonObject }
                .filter { it["thought"]?.jsonPrimitive?.booleanOrNull != true }
                .joinToString("") { it["text"]?.jsonPrimitive?.contentOrNull.orEmpty() },
        )
    }

    private fun body(request: LlmRequest) = buildJsonObject {
        putJsonArray("contents") {
            addJsonObject {
                put("role", "user")
                putJsonArray("parts") {
                    addJsonObject { put("text", request.prompt) }
                    request.audio.forEach { audio ->
                        addJsonObject {
                            putJsonObject("inlineData") {
                                put("mimeType", audio.mimeType)
                                put("data", Base64.encode(audio.bytes))
                            }
                        }
                    }
                }
            }
        }
        if (request.responseSchema == null && request.thinkingLevel == null) return@buildJsonObject
        putJsonObject("generationConfig") {
            request.responseSchema?.let { schema ->
                // mimeType 은 대문자여야 하고(`application/json` 은 400), 스키마를 함께 줘야 마크다운 표가 아닌 JSON 이 온다(설계 11절).
                putJsonObject("responseFormat") {
                    putJsonObject("text") {
                        put("mimeType", "APPLICATION_JSON")
                        put("schema", Json.parseToJsonElement(schema))
                    }
                }
            }
            request.thinkingLevel?.let { putJsonObject("thinkingConfig") { put("thinkingLevel", it) } }
        }
    }

    private fun url(model: String) = "$BASE_URL/models/$model:generateContent"

    private companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
    }
}
