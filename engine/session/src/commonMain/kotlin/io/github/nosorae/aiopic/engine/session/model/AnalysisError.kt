package io.github.nosorae.aiopic.engine.session.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 분석 포트가 결과를 내지 못한 이유. 답변과 회차 등급(#203)에 남아서 저장 이름을 고정한다.
 * 회차 모델이 들고 있어야 해서 답변 모듈이 아니라 회차 모듈에 둔다(답변 모듈이 회차 모듈을 본다).
 */
@Serializable
enum class AnalysisError {
    /** 429, 408, 5xx, 시간 초과. 분석 작업이 정한 횟수만큼 다시 보내고(F5.6), 다 쓰면 이 이유로 실패한다. */
    @SerialName("retryable")
    Retryable,

    /** 연결이 없다. 재시도 횟수를 쓰지 않고 네트워크를 기다린다(#205). 이 이유로 실패하지는 않는다. */
    @SerialName("offline")
    Offline,

    /** 음성 전송 동의가 없어 보내지 않았다(F1.3). 다시 동의한 뒤 다시 분석하기를 누르면 보낸다. */
    @SerialName("no_consent")
    NoConsent,

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
