package io.github.nosorae.aiopic.ui.featuresettings.model

/**
 * 사용자가 고른 분석 모델 id 를 기기에 남긴다(F5.8). 고르지 않았으면 null 이고 기본 모델을 쓴다.
 * 실제 저장은 앱 층이 플랫폼 설정 저장소로 넣고, 테스트는 메모리 가짜를 쓴다.
 */
interface ModelSelectionStore {
    suspend fun selectedModelId(): String?

    suspend fun selectModel(id: String)
}
