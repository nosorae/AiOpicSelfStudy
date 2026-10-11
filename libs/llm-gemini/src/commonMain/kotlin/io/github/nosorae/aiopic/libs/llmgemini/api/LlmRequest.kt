package io.github.nosorae.aiopic.libs.llmgemini.api

// ponytail: 요청과 결과 타입만 정한다. 이걸 보내는 Gemini HTTP 클라이언트는 구현이 하나뿐이라 n-gemini 노드에서 final class 로 만든다.

/** Gemini 생성 요청 하나. 분석 포트와 프롬프트를 모르는 HTTP 클라이언트가 보낸다. */
public data class LlmRequest(
    public val apiKey: String,
    public val model: String,
    public val prompt: String,
    public val audio: List<AudioPart> = emptyList(),
    /** 응답 JSON 스키마. 스키마 없이 JSON 을 요청하면 마크다운 표가 올 때가 있어서 JSON 응답에는 늘 준다(설계 11절). */
    public val responseSchema: String? = null,
    /** 생각 수준(`low` 등). 없으면 모델 기본값이다. 낮추면 답이 빨리 온다(#216). */
    public val thinkingLevel: String? = null,
) {
    /** 로그나 오류 메시지에 키가 찍히지 않게 키를 가린다. */
    override fun toString(): String = "LlmRequest(model=$model, audio=${audio.size}, responseSchema=${responseSchema != null})"
}

/** 녹음 하나. 조각을 이어 붙인 파일 내용이다. */
public class AudioPart(public val bytes: ByteArray, public val mimeType: String)

/**
 * 요청 하나의 결과. 실패를 예외 대신 이 값으로 돌려준다.
 * 부르는 쪽이 다시 보낼지, 설정 화면으로 보낼지, 기본 모델로 돌릴지를 고른다.
 */
public sealed interface LlmResult {
    /** 모델이 돌려준 글. [LlmRequest.responseSchema] 를 줬으면 JSON 이다. */
    public data class Success(public val text: String) : LlmResult

    /** 429, 408, 5xx, 연결 없음. 기다렸다 다시 보내면 될 수 있다. 연결이 없으면 [httpStatus] 가 null 이다. */
    public data class Retryable(public val httpStatus: Int?) : LlmResult

    /** 키가 없거나 틀렸다. 설정 화면으로 보낸다. */
    public data object InvalidKey : LlmResult

    /** 고른 모델이 내려갔다. 기본 모델로 돌리고 알린다(F5.8). */
    public data object ModelNotFound : LlmResult

    /** 그 밖의 거절. 다시 보내도 같다. */
    public data class Rejected(public val httpStatus: Int, public val message: String) : LlmResult
}
