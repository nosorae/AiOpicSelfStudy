package io.github.nosorae.aiopic.engine.answer.port

import io.github.nosorae.aiopic.engine.answer.model.UnscorableReason
import io.github.nosorae.aiopic.engine.session.model.AnalysisError

/** 분석 포트의 결과. 실패를 예외로 던지지 않아서 경로 고르기와 대기열이 종류마다 다르게 처리한다. */
sealed interface AnalysisResult<out T> {
    data class Success<out T>(val value: T) : AnalysisResult<T>

    /** 무음, 다른 언어, 5초 미만이라 평가할 수 없다(F5.5). 다시 보내지 않는다. */
    data class Unscorable(val reason: UnscorableReason) : AnalysisResult<Nothing>

    data class Failed(val error: AnalysisError) : AnalysisResult<Nothing>
}
