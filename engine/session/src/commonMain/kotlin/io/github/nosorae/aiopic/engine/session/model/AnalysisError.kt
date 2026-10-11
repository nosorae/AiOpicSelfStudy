package io.github.nosorae.aiopic.engine.session.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 분석 포트가 결과를 내지 못한 이유. 답변과 회차 등급(#203)에 남아서 저장 이름을 고정한다.
 * 회차 모델이 들고 있어야 해서 답변 모듈이 아니라 회차 모듈에 둔다(답변 모듈이 회차 모듈을 본다).
 */
@Serializable
enum class AnalysisError {
    /** 429, 408, 5xx, 오프라인. 대기열이 다시 보낸다(F5.6). */
    @SerialName("retryable")
    Retryable,

    /** 키가 없거나 틀렸다. 설정 화면으로 보낸다. */
    @SerialName("invalid_key")
    InvalidKey,

    /** 고른 모델이 내려갔다. 기본 모델로 돌리고 알린다(F5.8). */
    @SerialName("model_unavailable")
    ModelUnavailable,

    /** 출력 검사(F5.4)를 다시 요청해도 통과하지 못했거나 그 밖의 거절이다. 다시 보내지 않는다. */
    @SerialName("rejected")
    Rejected,
}
