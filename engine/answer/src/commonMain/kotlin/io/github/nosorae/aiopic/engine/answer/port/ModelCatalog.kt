package io.github.nosorae.aiopic.engine.answer.port

import io.github.nosorae.aiopic.engine.answer.model.ModelInfo

/**
 * 제공사 하나의 검증된 분석 모델 목록(F5.8, ADR 0019). 제공사 목록 API 에는 오디오 입력 여부가 없고 출력 검사 통과는
 * 모델마다 품질 확인 도구로 봐야 알아서, 확인한 모델만 앱이 들고 있는다.
 */
interface ModelCatalog {
    val models: List<ModelInfo>

    /** 사용자가 고르지 않았거나 고른 모델이 내려갔을 때 쓰는 안정 모델. [models] 안에 있다. */
    val defaultModel: ModelInfo
}
