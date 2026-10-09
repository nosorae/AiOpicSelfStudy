package io.github.nosorae.aiopic.engine.answer.check

import io.github.nosorae.aiopic.engine.answer.model.ModelInfo
import io.github.nosorae.aiopic.engine.answer.port.ModelCatalog

/** 이번 분석에 쓸 모델. [fellBackFrom] 이 있으면 고른 모델 대신 기본 모델로 돌렸다는 뜻이고 화면이 사용자에게 알린다(F5.8). */
data class ModelChoice(val model: ModelInfo, val fellBackFrom: String?)

/**
 * 고른 모델 id 로 쓸 모델을 정한다(F5.7, F5.8). 고르지 않았으면 기본 모델이다. 앱 업데이트로 목록에서 빠졌거나
 * 분석이 [io.github.nosorae.aiopic.engine.answer.port.AnalysisError.ModelUnavailable] 로 끝나 [unavailable] 이면 기본 모델로 돌린다.
 */
fun ModelCatalog.choose(selectedId: String?, unavailable: Boolean = false): ModelChoice {
    val selected = models.firstOrNull { it.id == selectedId }
    return when {
        selectedId == null -> ModelChoice(defaultModel, fellBackFrom = null)
        selected == null || unavailable -> ModelChoice(defaultModel, fellBackFrom = selectedId)
        else -> ModelChoice(selected, fellBackFrom = null)
    }
}
