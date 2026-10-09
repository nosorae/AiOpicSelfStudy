package io.github.nosorae.aiopic.tools.eval

import kotlinx.serialization.json.Json
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
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64

/**
 * Gemini `generateContent` 를 한 번 부른다. 녹음은 [say] 가 만든 AIFF 다. 요청 모양은 앱의 GeminiClient 와 같다. 실패하면 상태 코드와 본문을 담아 멈춘다.
 * 명령줄 도구라 다시 보내지 않고, 429 가 나오면 다음 날 다시 돌린다.
 */
fun askGemini(apiKey: String, model: String, prompt: String, schema: String, audio: List<ByteArray>): String {
    val body = buildJsonObject {
        putJsonArray("contents") {
            addJsonObject {
                put("role", "user")
                putJsonArray("parts") {
                    addJsonObject { put("text", prompt) }
                    audio.forEach { bytes ->
                        addJsonObject {
                            putJsonObject("inlineData") {
                                put("mimeType", "audio/aiff")
                                put("data", Base64.getEncoder().encodeToString(bytes))
                            }
                        }
                    }
                }
            }
        }
        putJsonObject("generationConfig") {
            putJsonObject("responseFormat") {
                putJsonObject("text") {
                    put("mimeType", "APPLICATION_JSON")
                    put("schema", Json.parseToJsonElement(schema))
                }
            }
        }
    }
    val request = HttpRequest.newBuilder(URI("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"))
        // 키를 주소에 넣으면 접속 기록에 남아서 헤더로 보낸다.
        .header("x-goog-api-key", apiKey)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
        .build()
    val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
    check(response.statusCode() == 200) { "Gemini ${response.statusCode()}: ${response.body()}" }
    return Json.parseToJsonElement(response.body()).jsonObject["candidates"]!!.jsonArray.first()
        .jsonObject["content"]!!.jsonObject["parts"]!!.jsonArray.map { it.jsonObject }
        .filter { it["thought"]?.jsonPrimitive?.booleanOrNull != true }
        .joinToString("") { it["text"]?.jsonPrimitive?.contentOrNull.orEmpty() }
}

// 시험 팩의 AnswerResponse, SessionGradeResponse 스키마와 같아야 한다. 프롬프트가 스키마의 필드 이름을 가리켜서다.
val ANSWER_SCHEMA = """
{"type":"object","properties":{
 "transcript":{"type":"string"},
 "corrections":{"type":"array","items":{"type":"object","properties":{"original":{"type":"string"},"corrected":{"type":"string"}},"required":["original","corrected"]}},
 "upgrades":{"type":"array","items":{"type":"object","properties":{"original":{"type":"string"},"better":{"type":"string"}},"required":["original","better"]}},
 "tenseConsistent":{"type":"boolean"},
 "sentenceCount":{"type":"integer"},
 "wordsPerMinute":{"type":"number"},
 "pauses":{"type":"array","items":{"type":"object","properties":{"startSeconds":{"type":"number"},"lengthSeconds":{"type":"number"}},"required":["startSeconds","lengthSeconds"]}},
 "roleplayQuestionCount":{"type":"integer"},
 "pronunciation":{"type":"string"},
 "score":{"type":"integer"},
 "unscorableReason":{"type":"string","enum":["silent","wrong_language","too_short"]}
},"required":["transcript","corrections","upgrades","tenseConsistent","sentenceCount","wordsPerMinute","pauses","pronunciation","score"]}
""".trim()

val GRADE_SCHEMA = """
{"type":"object","properties":{
 "checks":{"type":"array","items":{"type":"object","properties":{"id":{"type":"string"},"met":{"type":"boolean"}},"required":["id","met"]}},
 "low":{"type":"string","enum":${GRADES.joinToString(",", "[", "]") { "\"$it\"" }}},
 "high":{"type":"string","enum":${GRADES.joinToString(",", "[", "]") { "\"$it\"" }}},
 "reasons":{"type":"array","items":{"type":"string"}},
 "brokeAt":{"type":"array","items":{"type":"integer"}}
},"required":["checks","low","high","reasons","brokeAt"]}
""".trim()
