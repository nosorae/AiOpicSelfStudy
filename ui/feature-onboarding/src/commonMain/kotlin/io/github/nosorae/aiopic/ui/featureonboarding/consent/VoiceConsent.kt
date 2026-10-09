package io.github.nosorae.aiopic.ui.featureonboarding.consent

/**
 * 음성 전송 동의를 기기에 남긴다(F1.3, ADR 0014). 실제 저장은 앱 층이 플랫폼 설정 저장소로 넣고, 테스트는 메모리 가짜를 쓴다.
 * 분석 대기열은 같은 값을 읽어 동의 전에는 녹음을 보내지 않는다.
 */
interface VoiceConsent {
    suspend fun isGiven(): Boolean

    suspend fun give()

    /** 설정에서 철회한다(F1.3). 다음 실행의 시작 분기가 동의 화면으로 간다. */
    suspend fun revoke()
}
