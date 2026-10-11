package io.github.nosorae.aiopic.libs.llmgemini

/**
 * 녹음을 실은 요청을 플랫폼이 대신 보내는 길(#216). iOS 는 앱이 멈춰도 시스템이 전송을 끝내는 백그라운드 업로드를 준다.
 * 없으면 [GeminiClient] 의 HTTP 클라이언트로 보낸다. 연결하지 못하면 null 이다.
 */
public fun interface GeminiUpload {
    public suspend fun post(url: String, apiKey: String, body: String): RawResponse?
}

/** 받은 응답의 상태 코드와 본문 그대로. */
public class RawResponse(public val status: Int, public val body: String)
