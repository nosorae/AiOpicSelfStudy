package io.github.nosorae.aiopic.libs.debug.network

import io.github.nosorae.aiopic.libs.debug.DebugCall
import io.github.nosorae.aiopic.libs.debug.DebugCore
import io.github.nosorae.aiopic.libs.debug.DebugTools
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf

/**
 * 이 클라이언트의 요청과 응답(주소, 메서드, 상태 코드, 걸린 시간, 헤더, 본문)을 디버깅 도구 기록에 남긴다.
 * 키는 저장하기 전에 가리고 본문은 [io.github.nosorae.aiopic.libs.debug.DebugConfig.maxBodyBytes] 에서 자른다.
 * 실패한 요청도 남기고 예외는 그대로 앱에 넘긴다.
 */
public fun HttpClientConfig<*>.installDebugTools() {
    installDebugTools(DebugTools.core)
}

internal fun HttpClientConfig<*>.installDebugTools(core: DebugCore) {
    install(
        createClientPlugin("DebugTools") {
            on(Send) { request ->
                val start = core.clock.now()
                val method = request.method.value
                val url = request.url.buildString()
                val headers = request.headers.entries().pairs()
                val body = (request.body as? OutgoingContent)?.text().orEmpty()
                fun record(status: Int?, responseHeaders: Headers? = null, responseBody: String = "", error: Throwable? = null) = core.recordCall(
                    DebugCall(
                        time = start,
                        method = method,
                        url = url,
                        requestHeaders = headers,
                        requestBody = body,
                        status = status,
                        duration = core.clock.now() - start,
                        responseHeaders = responseHeaders?.entries()?.pairs().orEmpty(),
                        responseBody = responseBody,
                        error = error?.toString(),
                    ),
                )
                val call = try {
                    proceed(request)
                } catch (e: Throwable) {
                    record(status = null, error = e)
                    throw e
                }
                // 스트리밍이 아닌 응답 본문은 메모리에 남아 있어서 여기서 읽어도 앱이 다시 읽을 수 있다.
                // ponytail: 스트리밍 응답(prepareGet 등)도 여기서 끝까지 읽는다. 스트리밍을 쓰게 되면 그때 건너뛴다.
                record(call.response.status.value, call.response.headers, call.response.bodyAsText())
                call
            }
        },
    )
}

private fun Set<Map.Entry<String, List<String>>>.pairs() = flatMap { (name, values) -> values.map { name to it } }

// ponytail: 바이너리 본문도 글자로 읽는다. 이 앱은 JSON 만 보낸다. 바이너리를 보내게 되면 크기만 적는다.
private fun OutgoingContent.text(): String = when (this) {
    is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
    is OutgoingContent.NoContent -> ""
    else -> "(${this::class.simpleName} 본문은 기록하지 않는다)"
}

/**
 * 보내지 않고 늘 200 과 [json] 을 돌려주는 HTTP 엔진. 앱이 디버깅 스위치로 실제 서버 대신 쓴다.
 * 가짜 응답 엔진이 이 모듈에만 있어서 릴리스 빌드(debug-noop, 늘 null)에는 실리지 않는다.
 */
public fun fakeHttpEngine(json: String): HttpClientEngine? = MockEngine { respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
