package io.github.nosorae.aiopic.engine.answer.port

import io.github.nosorae.aiopic.engine.answer.model.UnscorableReason
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 분석 포트의 결과. 실패를 예외로 던지지 않아서 경로 고르기와 대기열이 종류마다 다르게 처리한다. */
sealed interface AnalysisResult<out T> {
    data class Success<out T>(val value: T) : AnalysisResult<T>

    /** 무음, 다른 언어, 5초 미만이라 평가할 수 없다(F5.5). 다시 보내지 않는다. */
    data class Unscorable(val reason: UnscorableReason) : AnalysisResult<Nothing>

    data class Failed(val error: AnalysisError) : AnalysisResult<Nothing>
}

/** 분석하지 못한 답변에 이유로 남아서 저장 이름을 고정한다. */
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
