package io.github.nosorae.aiopic.ui.featuresettings.haptics

/**
 * 진동을 낼지 기기에 남긴다. 기본은 켬이다. 앱 틀이 같은 값으로 화면의 진동을 막는다.
 * 실제 저장은 앱 층이 플랫폼 설정 저장소로 넣고, 테스트는 메모리 가짜를 쓴다.
 */
interface HapticsStore {
    suspend fun hapticsOn(): Boolean

    suspend fun setHapticsOn(on: Boolean)
}
